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
package main.java.service.client.io_client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.chatapp.commons.entry.EntryType;
import org.chatapp.commons.entry.LoginInfo;
import org.chatapp.commons.entry.LoginInfo.Action;
import org.chatapp.commons.entry.Verification;
import org.chatapp.commons.EnumIntConverter;
import org.chatapp.commons.ResultHolder;
import org.chatapp.commons.entry.CreateAccountInfo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import main.java.service.client.ChatRequest;
import main.java.service.client.ChatSession;

/**
 * 
 * @author Ilias Koukovinis
 */
public class Client implements AutoCloseable {

	private ByteBufInputStream in;
	private ByteBufOutputStream out;
	
	private SSLSocket sslSocket;
	
	private AtomicBoolean isLoggedIn = new AtomicBoolean(false);

	private MessageHandler messageHandler;
	
	public enum ServerCertificateVerification {
		VERIFY, IGNORE
	}
	
	public Client(InetAddress remoteAddress, int remotePort, ServerCertificateVerification serverCertificateVerification) throws IOException {
		
		if (remotePort <= 0) {
			throw new IllegalArgumentException("Port cannot be below zero");
		}

		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null, null);

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, null);

			TrustManager[] trustManagers = null;
			
			switch (serverCertificateVerification) {
			case VERIFY -> {
				
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509"); 
				tmf.init(ks);
				
				trustManagers = tmf.getTrustManagers();
			}
			case IGNORE -> {
				trustManagers = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}

					@Override
					public void checkClientTrusted(X509Certificate[] certs, String authType) {}

					@Override
					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				} };
			}
			}

			SSLContext sc = SSLContext.getInstance("TLSv1.3");
			sc.init(kmf.getKeyManagers(), trustManagers, new SecureRandom()); 

			SSLSocketFactory ssf = sc.getSocketFactory();
			sslSocket = (SSLSocket) ssf.createSocket(remoteAddress, remotePort);
			sslSocket.startHandshake();
			
			in = new ByteBufInputStream(sslSocket.getInputStream());
			out = new ByteBufOutputStream(sslSocket.getOutputStream());

			isLoggedIn.set(in.read().readBoolean());
		} catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | CertificateException e) {
			e.printStackTrace();
		}
	}
	
	public class Entry<T extends Enum<T>> {

		private final EntryType entryType;
		
		private Entry(EntryType entryType) {
			
			if (isLoggedIn.get()) {
				throw new IllegalStateException("User is already logged in");
			}

			this.entryType = entryType;
		}
		
		public ResultHolder getCredentialsExchangeResult() throws IOException {

			ByteBuf msg = in.read();

			boolean isSuccesfull = msg.readBoolean();

			byte[] resultMessageBytes = new byte[msg.readableBytes()];
			msg.readBytes(resultMessageBytes);
			
			return new ResultHolder(isSuccesfull, new String(resultMessageBytes));
		}
		
		public void sendCredentials(Map<T, String> credentials) throws IOException {
			for (Map.Entry<T, String> credential : credentials.entrySet()) {

				boolean isAction = false;
				int credentialInt = EnumIntConverter.getEnumAsInt(credential.getKey());
				byte[] credentialValueBytes = credential.getValue().getBytes();

				ByteBuf payload = Unpooled.buffer(1 + Integer.BYTES + credentialValueBytes.length);
				payload.writeBoolean(isAction);
				payload.writeInt(credentialInt);
				payload.writeBytes(credentialValueBytes);

				out.write(payload);
			}
		}
		
		public void sendEntryType() throws IOException {
			out.write(Unpooled.copyInt(EnumIntConverter.getEnumAsInt(entryType)));
		}
	}

	
	public class CreateAccountEntry extends Entry<CreateAccountInfo.Credential> {

		private CreateAccountEntry() {
			super(EntryType.CREATE_ACCOUNT);
		}
	}
	
	public class LoginEntry extends Entry<LoginInfo.Credential> {
		
		private LoginEntry() {
			super(EntryType.LOGIN);
		}
		
		public void togglePasswordType() throws IOException {

			boolean isAction = true;
			int actionInt = EnumIntConverter.getEnumAsInt(LoginInfo.Action.TOGGLE_PASSWORD_TYPE);

			ByteBuf payload = Unpooled.buffer();
			payload.writeBoolean(isAction);
			payload.writeInt(actionInt);

			out.write(payload);
		}
	}
	
	public class BackupVerificationEntry {
		
		public ResultHolder getResult() throws IOException	{
			
			ByteBuf payload = in.read();
			
			isLoggedIn.set(payload.readBoolean());
			
			byte[] resultMessageBytes = new byte[payload.readableBytes()];
			payload.readBytes(resultMessageBytes);
			
			String resultMessage = new String(resultMessageBytes);
			
			return new ResultHolder(isLoggedIn(), resultMessage);
		}
	}

	public class VerificationEntry {
		
		private boolean isVerificationComplete = false;
		
		private VerificationEntry() {}
		
		public void sendVerificationCode(String verificationCode) throws IOException {
			
			boolean isAction = false;
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeBoolean(isAction);
			payload.writeBytes(verificationCode.getBytes());
			
			out.write(payload);
		}
		
		public ResultHolder getResult() throws IOException {
			
			ResultHolder resultHolder;
			
			ByteBuf msg = in.read();
			
			isVerificationComplete = msg.readBoolean();
			isLoggedIn.set(msg.readBoolean());
			
			byte[] resultMessageBytes = new byte[msg.readableBytes()];
			msg.readBytes(resultMessageBytes);
			
			resultHolder = new ResultHolder(isLoggedIn(), new String(resultMessageBytes));
			
			return resultHolder;
		}
		
		public void resendVerificationCode() throws IOException {
			
			boolean isAction = true;
			
			ByteBuf payload = Unpooled.buffer(1 + Integer.BYTES);
			payload.writeBoolean(isAction);
			payload.writeInt(EnumIntConverter.getEnumAsInt(Verification.Action.RESEND_CODE));
			
			out.write(payload);
		}
		
		public boolean isVerificationComplete() {
			return isVerificationComplete;
		}
	}
	
	public void startMessageHandler(MessageHandler messageHandler) throws IOException {

		if (!isLoggedIn()) {
			throw new IllegalStateException("User can't start writing server if he isn't logged in");
		}

		this.messageHandler = messageHandler;
		this.messageHandler.setByteBufInputStream(in);
		this.messageHandler.setByteBufOutputStream(out);
		this.messageHandler.startListeningToMessages();
	}

	public void sendMessageToClient(String message, int chatSessionIndex) throws IOException {
		messageHandler.sendMessageToClient(message, chatSessionIndex);
	}

	public void sendFile(File file, int chatSessionIndex) throws IOException {
		messageHandler.sendFile(file, chatSessionIndex);
	}

	public void stopListeningToMessages() {
		messageHandler.stopListeningToMessages();
	}

	public VerificationEntry createNewVerificationEntry() {
		return new VerificationEntry();
	}
	
	public BackupVerificationEntry createNewBackupVerificationEntry() {
		return new BackupVerificationEntry();
	}
	
	public CreateAccountEntry createNewCreateAccountEntry() {
		return new CreateAccountEntry();
	}
	
	public LoginEntry createNewLoginEntry() {
		return new LoginEntry();
	}

	public boolean isLoggedIn() {
		return isLoggedIn.get();
	}

	public boolean isClientListeningToMessages() {
		return messageHandler.isClientListeningToMessages();
	}

	public String getUsername() {
		return messageHandler.getUsername();
	}
	
	public int getClientID() {
		return messageHandler.getClientID();
	}

	public List<ChatSession> getChatSessions() {
		return messageHandler.getChatSessions();
	}

	public List<ChatRequest> getFriendRequests() {
		return messageHandler.getChatRequests();
	}
	
	public MessageHandler.Commands getCommands() {
		return messageHandler.getCommands();
	}
	
	public ByteBufInputStream getByteBufInputStream() {
		return in;
	}
	
	public ByteBufOutputStream getByteBufOutputStream() {
		return out;
	}
	
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	@Override
	public void close() throws IOException {
		messageHandler.close();
		out.close();
		in.close();
		sslSocket.close();
		isLoggedIn.set(false);
	}
}