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
package github.koukobin.ermis.client.main.java.service.client.io_client;

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

import github.koukobin.ermis.client.main.java.service.client.ChatRequest;
import github.koukobin.ermis.client.main.java.service.client.ChatSession;
import github.koukobin.ermis.common.entry.CreateAccountInfo;
import github.koukobin.ermis.common.entry.EntryType;
import github.koukobin.ermis.common.entry.LoginInfo;
import github.koukobin.ermis.common.entry.Verification;
import github.koukobin.ermis.common.results.ResultHolder;
import github.koukobin.ermis.common.util.EnumIntConverter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 
 * @author Ilias Koukovinis
 */
public class Client {

	private static ByteBufInputStream in;
	private static ByteBufOutputStream out;
	
	private static SSLSocket sslSocket;
	
	private static AtomicBoolean isLoggedIn = new AtomicBoolean(false);

	private static MessageHandler messageHandler;
	
	public enum ServerCertificateVerification {
		VERIFY, IGNORE
	}
	
	private Client() throws IllegalAccessException {
		throw new IllegalAccessException("Client cannot be constructed since it is statically initialized!");
	}
	
	public static void initialize(InetAddress remoteAddress, int remotePort, ServerCertificateVerification scv) throws ClientInitializationException {
		
		if (remotePort <= 0) {
			throw new IllegalArgumentException("Port cannot be below zero");
		}

		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null, null);
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, null);
			
			TrustManager[] trustManagers = null;
			
			switch (scv) {
			case VERIFY -> {
				
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				tmf.init(ks);
				
				trustManagers = tmf.getTrustManagers();
			}
			case IGNORE -> {
				trustManagers = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}
					
					@Override
					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}
					
					@Override
					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				} };
			}
			default -> throw new IllegalArgumentException("Unknown type: " + scv);
			}

			// The TLS version is automatically negotiated with the server. In case this
			// does not suffice, instead use "SSLContext.getInstance("TLSv1.3");"
			// SSLContext sc = SSLContext.getDefault();
			SSLContext sc = SSLContext.getInstance("TLSv1.3");
			sc.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());

			SSLSocketFactory ssf = sc.getSocketFactory();
			sslSocket = (SSLSocket) ssf.createSocket(remoteAddress, remotePort);
			sslSocket.startHandshake();
			
			in = new ByteBufInputStream(sslSocket.getInputStream());
			out = new ByteBufOutputStream(sslSocket.getOutputStream());

			isLoggedIn.set(in.read().readBoolean());
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException
				| UnrecoverableKeyException | KeyManagementException e) {
			throw new ClientInitializationException(e.getMessage());
		}
	}
	
	public static class Entry<T extends EntryType.CredentialInterface> {

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
				int credentialInt = EnumIntConverter.getEnumAsInt((Enum<?>) credential.getKey());
				byte[] credentialValueBytes = credential.getValue().getBytes();

				ByteBuf payload = Unpooled.buffer(1 /* boolean */ + Integer.BYTES + credentialValueBytes.length);
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

	
	public static class CreateAccountEntry extends Entry<CreateAccountInfo.Credential> {

		private CreateAccountEntry() {
			super(EntryType.CREATE_ACCOUNT);
		}
	}
	
	public static class LoginEntry extends Entry<LoginInfo.Credential> {
		
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
	
	public static class BackupVerificationEntry {
		
		public ResultHolder getResult() throws IOException	{
			
			ByteBuf payload = in.read();
			
			isLoggedIn.set(payload.readBoolean());
			
			byte[] resultMessageBytes = new byte[payload.readableBytes()];
			payload.readBytes(resultMessageBytes);
			
			String resultMessage = new String(resultMessageBytes);
			
			return new ResultHolder(isLoggedIn(), resultMessage);
		}
	}

	public static class VerificationEntry {
		
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
	
	public static void startMessageHandler(MessageHandler messageHandler) throws IOException {

		if (!isLoggedIn()) {
			throw new IllegalStateException("User can't start writing server if he isn't logged in");
		}

		Client.messageHandler = messageHandler;
		Client.messageHandler.setByteBufInputStream(in);
		Client.messageHandler.setByteBufOutputStream(out);
		Client.messageHandler.startListeningToMessages();
	}

	public static void sendMessageToClient(String message, int chatSessionIndex) throws IOException {
		messageHandler.sendMessageToClient(message, chatSessionIndex);
	}

	public static void sendFile(File file, int chatSessionIndex) throws IOException {
		messageHandler.sendFile(file, chatSessionIndex);
	}

	public static void stopListeningToMessages() {
		messageHandler.stopListeningToMessages();
	}

	public static VerificationEntry createNewVerificationEntry() {
		return new VerificationEntry();
	}
	
	public static BackupVerificationEntry createNewBackupVerificationEntry() {
		return new BackupVerificationEntry();
	}
	
	public static CreateAccountEntry createNewCreateAccountEntry() {
		return new CreateAccountEntry();
	}
	
	public static LoginEntry createNewLoginEntry() {
		return new LoginEntry();
	}

	public static boolean isLoggedIn() {
		return isLoggedIn.get();
	}

	public static boolean isClientListeningToMessages() {
		return messageHandler.isClientListeningToMessages();
	}

	public static String getDisplayName() {
		return messageHandler.getUsername();
	}
	
	public static int getClientID() {
		return messageHandler.getClientID();
	}
	
	public static byte[] getAccountIcon() {
		return messageHandler.getAccountIcon();
	}

	public static List<ChatSession> getChatSessions() {
		return messageHandler.getChatSessions();
	}

	public static List<ChatRequest> getFriendRequests() {
		return messageHandler.getChatRequests();
	}
	
	public static MessageHandler.Commands getCommands() {
		return messageHandler.getCommands();
	}
	
	public static ByteBufInputStream getByteBufInputStream() {
		return in;
	}
	
	public static ByteBufOutputStream getByteBufOutputStream() {
		return out;
	}
	
	public static MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	public static void close() throws IOException {
		messageHandler.close();
		out.close();
		in.close();
		sslSocket.close();
		isLoggedIn.set(false);
	}
}