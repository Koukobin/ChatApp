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
package github.chatapp.client.main.java.service.client.io_client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import github.chatapp.client.main.java.service.client.ChatRequest;
import github.chatapp.client.main.java.service.client.ChatSession;
import github.chatapp.client.main.java.service.client.DonationHtmlPage;
import github.chatapp.common.LoadedInMemoryFile;
import github.chatapp.common.message_types.ClientCommandResultType;
import github.chatapp.common.message_types.ClientCommandType;
import github.chatapp.common.message_types.ClientMessageType;
import github.chatapp.common.message_types.ContentType;
import github.chatapp.common.message_types.Message;
import github.chatapp.common.message_types.ServerMessageType;
import github.chatapp.common.util.EnumIntConverter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 
 * @author Ilias Koukovinis
 * 
 */
public abstract class MessageHandler implements AutoCloseable {

	private ByteBufInputStream in;
	private ByteBufOutputStream out;

	private String username;
	private int clientID;
	
	private Map<Integer, ChatSession> chatSessionIDSToChatSessions = new HashMap<>();
	private List<ChatSession> chatSessions = new ArrayList<>();
	private List<ChatRequest> chatRequests = new ArrayList<>();

	private AtomicBoolean isClientListeningToMessages = new AtomicBoolean(false);
	private Commands commands = new Commands();
	
	void setByteBufInputStream(ByteBufInputStream in) {
		this.in = in;
	}
	
	void setByteBufOutputStream(ByteBufOutputStream out) {
		this.out = out;
	}
	
	public void sendMessageToClient(String text, int chatSessionIndex) throws IOException {

		byte[] textBytes = text.getBytes();
		
		ByteBuf payload = Unpooled.buffer();
		payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.CLIENT_CONTENT));
		payload.writeInt(EnumIntConverter.getEnumAsInt(ContentType.TEXT));
		payload.writeInt(chatSessionIndex);
		payload.writeInt(textBytes.length);
		payload.writeBytes(textBytes);
		
		out.write(payload);
	}
	
	public void sendFile(File file, int chatSessionIndex) throws IOException {

		byte[] fileNameBytes = file.getName().getBytes();
		byte[] fileBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));

		ByteBuf payload = Unpooled.buffer();
		payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.CLIENT_CONTENT));
		payload.writeInt(EnumIntConverter.getEnumAsInt(ContentType.FILE));
		payload.writeInt(chatSessionIndex);
		payload.writeInt(fileNameBytes.length);
		payload.writeBytes(fileNameBytes);
		payload.writeBytes(fileBytes);

		out.write(payload);
	}

	public abstract void usernameReceived(String username);
	public abstract void messageReceived(Message message, int chatSessionIndex);
	public abstract void alreadyWrittenTextReceived(ChatSession chatSession);
	public abstract void serverMessageReceived(String message);
	public abstract void fileDownloaded(LoadedInMemoryFile file);
	public abstract void donationPageReceived(DonationHtmlPage donationPage);
	public abstract void serverSourceCodeReceived(String serverSourceCodeURL);
	public abstract void clientIDReceived(int clientID);
	public abstract void chatRequestsReceived(List<ChatRequest> chatRequests);
	public abstract void chatSessionsReceived(List<ChatSession> chatSessions);
	public abstract void messageDeleted(ChatSession chatSession, int messageIDOfDeletedMessage);
	
	public class Commands {

		public void changeUsername(String newUsername) throws IOException {

			byte[] newUsernameBytes = newUsername.getBytes();

			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.CHANGE_USERNAME));
			payload.writeBytes(newUsernameBytes);

			out.write(payload);
		}
		
		public void changePassword(String newPassword) throws IOException {
			
			byte[] newPasswordBytes = newPassword.getBytes();

			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.CHANGE_PASSWORD));
			payload.writeBytes(newPasswordBytes);

			out.write(payload);
		}

		public void sendGetUsername() throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.GET_USERNAME));
			
			out.write(payload);
		}
		
		public void sendGetClientID() throws IOException {
			
			ByteBuf payload = Unpooled.buffer(Integer.BYTES * 2);
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.GET_CLIENT_ID));
			
			out.write(payload);
		}
		
		public void sendGetWrittenText(int chatSessionIndex) throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.GET_WRITTEN_TEXT));
			payload.writeInt(chatSessionIndex);
			payload.writeInt(chatSessions.get(chatSessionIndex).getMessages().size() /* Amount of messages client already has */);
			
			out.write(payload);
		}
		
		public void sendGetChatRequests() throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.GET_CHAT_REQUESTS));

			out.write(payload);
		}
		
		public void sendGetChatSessions() throws IOException {

			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.GET_CHAT_SESSIONS));

			out.write(payload);
		}
		
		public void sendGetDonationHTMLPage() throws IOException {

			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.GET_DONATION_PAGE));
			
			out.write(payload);
		}
		
		public void sendGetServerSourceCodeHTMLPage() throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.GET_SERVER_SOURCE_CODE_PAGE));
			
			out.write(payload);
		}

		public void sendChatRequest(int userClientID) throws IOException {

			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.SEND_CHAT_REQUEST));
			payload.writeInt(userClientID);
			
			out.write(payload);
		}
		
		public void acceptChatRequest(int userClientID) throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.ACCEPT_CHAT_REQUEST));
			payload.writeInt(userClientID);
			
			out.write(payload);
			
			sendGetChatRequests();
			sendGetChatSessions();
		}
		
		public void declineChatRequest(int userClientID) throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.DECLINE_CHAT_REQUEST));
			payload.writeInt(userClientID);
			
			out.write(payload);

			sendGetChatRequests();
		}
		
		public void deleteChatSession(int chatSessionIndex) throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.DELETE_CHAT_SESSION));
			payload.writeInt(chatSessionIndex);
			
			out.write(payload);
			
			sendGetChatSessions();
		}
		
		public void deleteMessage(int chatSessionIndex, int messageID) throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.DELETE_CHAT_MESSAGE));
			payload.writeInt(chatSessionIndex);
			payload.writeInt(messageID);
			
			out.write(payload);
		}
		
		public void downloadFile(int messageID, int chatSessionIndex) throws IOException {

			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.DOWNLOAD_FILE));
			payload.writeInt(chatSessionIndex);
			payload.writeInt(messageID);
			
			out.write(payload);
		}
		
		public void logout() throws IOException {
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientMessageType.COMMAND));
			payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandType.LOGOUT));
			
			out.write(payload);
		}

	}
	
	/**
	 * reads incoming messages sent from the server
	 */
	public void startListeningToMessages() throws IOException {
		
		if (isClientListeningToMessages()) {
			throw new IllegalStateException("Client is already listening to messages!");
		}
		
		isClientListeningToMessages.set(true);

		Thread thread = new Thread("Thread-listenToMessages") {
			
			@Override
			public void run() {
				while (isClientListeningToMessages()) {
					try {
						
						ByteBuf msg = in.read();
						
						ServerMessageType msgType = EnumIntConverter.getIntAsEnum(msg.readInt(), ServerMessageType.class);

						switch (msgType) {
						case SERVER_MESSAGE_INFO -> {
							
							byte[] content = new byte[msg.readableBytes()];
							msg.readBytes(content);
							
							serverMessageReceived(new String(content));
						}
						case CLIENT_CONTENT -> {

							Message message = new Message();
							
							ContentType contentType = EnumIntConverter.getIntAsEnum(msg.readInt(), ContentType.class);
							
							byte[] text = null;
							byte[] fileNameBytes = null;
							
							switch (contentType) {
							case TEXT -> {
								
								text = new byte[msg.readInt()];
								msg.readBytes(text);
							}
							case FILE -> {
								
								fileNameBytes = new byte[msg.readInt()];
								msg.readBytes(fileNameBytes);
							}
							}
							
							byte[] usernameBytes = new byte[msg.readInt()];
							msg.readBytes(usernameBytes);
							
							String username = new String(usernameBytes);
							int clientID = msg.readInt();
							int messageID = msg.readInt();
							int chatSessionID = msg.readInt();
							
							message.setContentType(contentType);
							message.setUsername(username);
							message.setClientID(clientID);
							message.setMessageID(messageID);
							message.setChatSessionID(chatSessionID);
							message.setText(text);
							message.setFileName(fileNameBytes);
							
							ChatSession chatSession = chatSessionIDSToChatSessions.get(chatSessionID);
							
							if (chatSession.haveChatMessagesBeenCached()) {
								chatSession.getMessages().add(message);
							}
							
							messageReceived(message, chatSession.getChatSessionIndex());
						}
						case COMMAND_RESULT -> {

							ClientCommandResultType commandResult = EnumIntConverter.getIntAsEnum(msg.readInt(), ClientCommandResultType.class);

							switch (commandResult) {
							case DOWNLOAD_FILE -> {
								
								byte[] fileNameBytes = new byte[msg.readInt()];
								msg.readBytes(fileNameBytes);
								
								byte[] fileBytes = new byte[msg.readableBytes()];
								msg.readBytes(fileBytes);
								
								fileDownloaded(new LoadedInMemoryFile(new String(fileNameBytes), fileBytes));
							}
							case GET_USERNAME -> {
								
								byte[] usernameBytes = new byte[msg.readableBytes()];
								msg.readBytes(usernameBytes);

								username = new String(usernameBytes);
								
								usernameReceived(username);
							}
							case GET_CLIENT_ID -> {
								clientID = msg.readInt();
								clientIDReceived(clientID);
							}
							case GET_CHAT_SESSIONS -> {

								chatSessions.clear();
								
								int chatSessionsSize = msg.readInt();
								for (int i = 0; i < chatSessionsSize; i++) {

									int chatSessionIndex = i;
									int chatSessionID = msg.readInt();

									ChatSession chatSession = new ChatSession(chatSessionID, chatSessionIndex);

									int membersSize = msg.readInt();

									List<ChatSession.Member> members = new ArrayList<>(membersSize);

									for (int j = 0; j < membersSize; j++) {

										int clientID = msg.readInt();

										byte[] usernameBytes = new byte[msg.readInt()];
										msg.readBytes(usernameBytes);

										members.add(new ChatSession.Member(new String(usernameBytes), clientID));
									}

									chatSession.setMembers(members);

									chatSessions.add(chatSessionIndex, chatSession);
									chatSessionIDSToChatSessions.put(chatSessionID, chatSession);
								}
								
								chatSessionsReceived(chatSessions);
							}
							case GET_CHAT_REQUESTS -> {
								
								chatRequests.clear();
								
								int friendRequestsLength = msg.readInt();

								for (int i = 0; i < friendRequestsLength; i++) {
									int clientID = msg.readInt();
									chatRequests.add(new ChatRequest(clientID));
								}
								
								chatRequestsReceived(chatRequests);
							}
							case GET_WRITTEN_TEXT -> {

								ChatSession chatSession;

								{
									int chatSessionIndex = msg.readInt();
									
									chatSession = chatSessions.get(chatSessionIndex);
								}
								
								List<Message> messages = chatSession.getMessages();
								while (msg.readableBytes() > 0) {

									ContentType contentType = EnumIntConverter.getIntAsEnum(msg.readInt(), ContentType.class);
									
									int clientID = msg.readInt();
									int messageID = msg.readInt();

									String username;
									
									{
										byte[] usernameBytes = new byte[msg.readInt()];
										msg.readBytes(usernameBytes);
								
										username = new String(usernameBytes);
									}

									byte[] messageBytes = null;
									byte[] fileNameBytes = null;
									
									switch (contentType) {
									case TEXT -> {
										messageBytes = new byte[msg.readInt()];
										msg.readBytes(messageBytes);
									}
									case FILE -> {
										fileNameBytes = new byte[msg.readInt()];
										msg.readBytes(fileNameBytes);
									}
									}
									
									if (contentType != null) {
										Message message = new Message(username, clientID, messageID, chatSession.getChatSessionID(), messageBytes, fileNameBytes, contentType);
										messages.add(message);
									}
								}
								
								messages.sort(Comparator.comparing(Message::getMessageID));
								
								chatSession.setHaveChatMessagesBeenCached(true);
								alreadyWrittenTextReceived(chatSession);
							}
							case GET_DONATION_PAGE -> {

								byte[] htmlBytes = new byte[msg.readInt()];
								msg.readBytes(htmlBytes);
								
								byte[] htmlFileName = new byte[msg.readableBytes()];
								msg.readBytes(htmlFileName);

								DonationHtmlPage htmlPage = new DonationHtmlPage(new String(htmlBytes), new String(htmlFileName));

								donationPageReceived(htmlPage);
							}
							case GET_SERVER_SOURCE_CODE_PAGE -> {
								
								byte[] htmlPageURL = new byte[msg.readableBytes()];
								msg.readBytes(htmlPageURL);
								
								serverSourceCodeReceived(new String(htmlPageURL));
							}
							case DELETE_CHAT_MESSAGE -> {

								int chatSessionID = msg.readInt();
								int messageID = msg.readInt();

								messageDeleted(chatSessionIDSToChatSessions.get(chatSessionID), messageID);
							}
							}
						}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				isClientListeningToMessages.set(false);
			}
		};
		thread.setDaemon(true);
		thread.start();
		
		getCommands().sendGetUsername();
		getCommands().sendGetClientID();
		getCommands().sendGetChatSessions();
		getCommands().sendGetChatRequests();
	}

	public void stopListeningToMessages() {
		
		if (!isClientListeningToMessages()) {
			throw new IllegalStateException("Client isn't listening to messages to stop listening to messages!");
		}
		
		isClientListeningToMessages.set(false);
	}
	
	public boolean isClientListeningToMessages() {
		return isClientListeningToMessages.get();
	}
	
	public Commands getCommands() {
		return commands;
	}

	public Map<Integer, ChatSession> getChatSessionIDSToChatSessions() {
		return chatSessionIDSToChatSessions;
	}

	public List<ChatSession> getChatSessions() {
		return chatSessions;
	}

	public List<ChatRequest> getChatRequests() {
		return chatRequests;
	}

	public String getUsername() {
		return username;
	}

	public int getClientID() {
		return clientID;
	}

	@Override
	public void close() {
		stopListeningToMessages();
	}
}
