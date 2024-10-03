/* Copyright (C) 2023 Ilias Koukovinis <ilias.koukovinis@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package main.java.server;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import main.java.configs.ServerSettings;
import main.java.databases.postgresql.chatapp_database.ChatAppDatabase;
import main.java.server.codec.Encoder;
import main.java.server.codec.SimpleDecoder;
import main.java.server.netty_handlers.StartingEntryHandler;

/**
 * @author Ilias Koukovinis
 *
 */
public final class Server {

	private static final Logger logger;
	
	private static EpollServerSocketChannel serverSocketChannel;

	private static EpollEventLoopGroup bossGroup;
	private static EpollEventLoopGroup workerGroup;
	
	private static ClientConnector clientConnector;

	private static AtomicBoolean isRunning;

	private Server() throws IllegalAccessException {
		throw new IllegalAccessException("Server cannot be constructed since it is statically initialized!");
	}

	static {
		logger = LogManager.getLogger("server");
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
	}
	
	static {
		try {
			
			logger.info("Initializing...");
			
			EmailerService.initialize();
			ChatAppDatabase.initialize();
			
			bossGroup = new EpollEventLoopGroup(1, (Runnable r) -> new Thread(r, "Thread-ClientConnector"));
			workerGroup = new EpollEventLoopGroup(ServerSettings.WORKER_THREADS);
			
			clientConnector = new ClientConnector();
			
			isRunning = new AtomicBoolean(false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void start() {

		if (Server.isRunning.get()) {
			throw new IllegalStateException("Server cannot start since the server is already running");
		}
		
		try {
			
			InetSocketAddress localAddress = new InetSocketAddress(ServerSettings.SERVER_ADDRESS, ServerSettings.SERVER_PORT);
			
			ServerBootstrap bootstrapTCP = new ServerBootstrap();
			bootstrapTCP.group(bossGroup, workerGroup)
				.channel(EpollServerSocketChannel.class)
				.childHandler(clientConnector)
				.localAddress(localAddress)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.option(ChannelOption.SO_BACKLOG, ServerSettings.SERVER_BACKLOG)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ServerSettings.CONNECT_TIMEOUT_MILLIS)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
			
			// If server isn't production ready we add a logging handler for more detailed logging
			if (!ServerSettings.IS_PRODUCTION_READY) {
				bootstrapTCP.handler(new LoggingHandler(LogLevel.INFO));
			}
			
			serverSocketChannel = (EpollServerSocketChannel) bootstrapTCP.bind().sync().channel();

			Server.isRunning.set(true);

			InetSocketAddress socketAddress = serverSocketChannel.localAddress();
			
			logger.info("Server started succesfully on port {} and at address {}", socketAddress.getPort(),
					socketAddress.getHostName());
			logger.info("Waiting for new connections...");
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class ClientConnector extends ChannelInitializer<EpollSocketChannel> {

		private static final SslContext sslContext;
		
		static {
			try {

				char[] certificatePassword = ServerSettings.SSL.CERTIFICATE_PASSWORD.toCharArray();
				
				KeyStore ks = KeyStore.getInstance(ServerSettings.SSL.CERTIFICATE_TYPE);
				ks.load(new FileInputStream(ServerSettings.SSL.CERTIFICATE_LOCATION), certificatePassword);
				
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(ks, certificatePassword);
				
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(ks);
				
				sslContext = SslContextBuilder.forServer(kmf)
						.trustManager(tmf)
						.protocols(ServerSettings.SSL.getEnabledProtocols())
						.sslProvider(SslProvider.OPENSSL)
						.ciphers(Arrays.asList(ServerSettings.SSL.getEnabledCipherSuites()), SupportedCipherSuiteFilter.INSTANCE)
						.applicationProtocolConfig(
								new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
										SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2))
						.build();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void initChannel(EpollSocketChannel ch) {

			ChannelPipeline pipeline = ch.pipeline();

			// If SSL is enabled we add SSL handler first to encrypt and decrypt everything.
			// P.S I forgot to add the if statement
			SSLEngine engine = sslContext.newEngine(ch.alloc());
			engine.setUseClientMode(false);
			pipeline.addLast("ssl", new SslHandler(engine));

			pipeline.addLast("decoder",
					new SimpleDecoder(500 /*
											 * max length doesn't need to be too big as this decoder will only be used
											 * to send simple small messages and will be replaced with the main decoder
											 * once it reaches the message handler
											 */));
			pipeline.addLast("encoder", new Encoder());

			ClientInfo clientInfo = new ClientInfo();
			clientInfo.setChannel(ch);
			
			pipeline.addLast(StartingEntryHandler.class.getName(), new StartingEntryHandler(clientInfo));
		}
	}
	
	public static void stop() {
		
		if (!Server.isRunning.get()) {
			throw new IllegalStateException("Server has not started therefore cannot be stopped");
		}

		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
		
		Server.isRunning.set(false);

		logger.info("Server stopped succesfully on port {} and at address {}",
				serverSocketChannel.localAddress().getHostName(), serverSocketChannel.localAddress().getPort());
		
		logger.info("Stopped waiting for new connections...");
	}
}
