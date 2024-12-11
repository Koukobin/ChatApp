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
package github.koukobin.ermis.server.main.java.server;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import github.koukobin.ermis.server.main.java.configs.ServerSettings;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

/**
 * @author Ilias Koukovinis
 *
 */
public final class ServerUDP {

	private static final Logger logger;
	
	private static Channel serverSocketChannel;

	private static EpollEventLoopGroup workerGroup;
	
	private static AtomicBoolean isRunning;

	private ServerUDP() throws IllegalAccessException {
		throw new IllegalAccessException("Server cannot be constructed since it is statically initialized!");
	}

	static {
		logger = LogManager.getLogger("server");
	}
	
	static {
		try {
			
			logger.info("Initializing...");
			
			workerGroup = new EpollEventLoopGroup(ServerSettings.WORKER_THREADS);
			
			isRunning = new AtomicBoolean(false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void start() {

		if (ServerUDP.isRunning.get()) {
			throw new IllegalStateException("Server cannot start since the server is already running");
		}
		
		try {
			
			InetSocketAddress localAddress = new InetSocketAddress(ServerSettings.SERVER_ADDRESS, ServerSettings.UDP_PORT);
			
	    	Bootstrap bootstrapUDP = new Bootstrap();
	        bootstrapUDP.group(workerGroup)
	            .channel(EpollDatagramChannel.class)
	            .localAddress(localAddress)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.option(ChannelOption.SO_BACKLOG, ServerSettings.SERVER_BACKLOG)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ServerSettings.CONNECT_TIMEOUT_MILLIS)
	            .handler(new Test());
	        
			serverSocketChannel = bootstrapUDP.bind().sync().channel();

			ServerUDP.isRunning.set(true);

			InetSocketAddress socketAddress = (InetSocketAddress) serverSocketChannel.localAddress();
			
			logger.info("UDP Server started succesfully on port {} and at address {}", socketAddress.getPort(),
					socketAddress.getHostName());
			logger.info("Waiting for incoming calls...");
		}  catch (Exception e) {
			throw new RuntimeException("Failed to start UDP server", e);
		}
	}
	
	private static class Test extends SimpleChannelInboundHandler<DatagramPacket> {
    	
    	private static final Map<InetSocketAddress, InetSocketAddress> senders = new ConcurrentHashMap<>();
    	private static final Map<InetSocketAddress, InetSocketAddress> senders2 = new ConcurrentHashMap<>();
    	
        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        	logger.debug("Packet received");
        	
        	InetSocketAddress recipient = senders.get(packet.sender());
        	
        	if (recipient == null) {
        		recipient = senders2.get(packet.sender());
        		
        		if (recipient == null) {
        			throw new Exception("What the fuck is going on");
        		}
        	}
        	
            ByteBuf responseContent = Unpooled.copiedBuffer("Hello, client!", CharsetUtil.UTF_8);
            DatagramPacket responsePacket = new DatagramPacket(responseContent, recipient);
            ctx.channel().writeAndFlush(responsePacket);
        }
    }
	
	public static void addVoiceChat(InetSocketAddress client1, InetSocketAddress client2) {
		Test.senders.put(client1, client2);
		Test.senders.put(client2, client1);
	}
	
	public static void stop() {

		if (!ServerUDP.isRunning.get()) {
			throw new IllegalStateException("Server has not started therefore cannot be stopped");
		}

		workerGroup.shutdownGracefully();

		ServerUDP.isRunning.set(false);
		
		logger.info("Server stopped succesfully on port {} and at address {}",
				((InetSocketAddress) serverSocketChannel.localAddress()).getHostName(), ((InetSocketAddress) serverSocketChannel.localAddress()).getPort());

		logger.info("Stopped waiting for incoming calls");
	}
}



