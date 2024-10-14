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
package github.chatapp.server.main.java.server.netty_handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.primitives.Ints;

import github.chatapp.common.LoadedInMemoryFile;
import github.chatapp.common.message_types.ClientCommandResultType;
import github.chatapp.common.message_types.ClientCommandType;
import github.chatapp.common.message_types.ClientMessageType;
import github.chatapp.common.message_types.ContentType;
import github.chatapp.common.message_types.Message;
import github.chatapp.common.message_types.ServerMessageType;
import github.chatapp.common.results.ResultHolder;
import github.chatapp.common.util.EnumIntConverter;
import github.chatapp.server.main.java.configs.ServerSettings;
import github.chatapp.server.main.java.databases.postgresql.chatapp_database.ChatAppDatabase;
import github.chatapp.server.main.java.databases.postgresql.chatapp_database.DatabaseChatMessage;
import github.chatapp.server.main.java.server.ActiveChatSessions;
import github.chatapp.server.main.java.server.ChatSession;
import github.chatapp.server.main.java.server.ClientInfo;
import github.chatapp.server.main.java.server.codec.MessageHandlerDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.epoll.EpollSocketChannel;

/**
 * @author Ilias Koukovinis
 * 
 */
final class MessageHadler extends ParentHandler {
	
	private static final Map<Integer, ClientInfo> clientIDSToActiveClients = new ConcurrentHashMap<>(ServerSettings.SERVER_BACKLOG);

	private CompletableFuture<?> commandsToBeExecutedQueue = 
			CompletableFuture.runAsync(() -> {}); // initialize it like this for thenRunAsync to work (i don't know why)
	
	public MessageHadler(ClientInfo clientInfo) {
		super(clientInfo);
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {

		MessageHandlerDecoder decoder = new MessageHandlerDecoder(ServerSettings.MAX_CLIENT_MESSAGE_TEXT_BYTES, ServerSettings.MAX_CLIENT_MESSAGE_FILE_BYTES);
		ctx.pipeline().replace("decoder", "decoder", decoder);
		
		try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
			
			int clientID = conn.getClientID(clientInfo.getChannel().remoteAddress().getAddress());
			
			clientInfo.setUsername(conn.getUsername(clientID));
			clientInfo.setEmail(conn.getEmailAddress(clientID));
			clientInfo.setClientID(clientID);

			Integer[] chatSessionsIDS = conn.getChatSessionsUserBelongsTo(clientID);

			List<ChatSession> chatSessions = new ArrayList<>(chatSessionsIDS.length);
			clientInfo.setChatSessions(chatSessions);
			for (int i = 0; i < chatSessionsIDS.length; i++) {

				int chatSessionID = chatSessionsIDS[i];

				ChatSession chatSession = ActiveChatSessions.getChatSession(chatSessionID);

				if (chatSession == null) {

					List<Integer> membersList;

					{
						Integer[] members = conn.getMembersOfChatSession(chatSessionID);
						membersList = new ArrayList<>(members.length);
						Collections.addAll(membersList, members);
					}

					// The client will become active in the chat session once he calls
					// GET_CHAT_SESSIONS command. The reason that this happens is because if he
					// hadn't gotten the chat session with GET_CHAT_SESSIONS command then if
					// there was a message sent in the chat session, the server would send that to
					// the client but the client would not know how to proccess it and in what chat
					// session the message belongs to
					List<ClientInfo> activeMembersList = new ArrayList<>(membersList.size());
					chatSession = new ChatSession(chatSessionID, activeMembersList, membersList);
					ActiveChatSessions.addChatSession(chatSessionID, chatSession);
				}

				chatSessions.add(chatSession);
			}

			{
				Integer[] chatRequests = conn.getChatRequests(clientID);
				List<Integer> chatRequestsList = new ArrayList<>(chatRequests.length);
				Collections.addAll(chatRequestsList, chatRequests);

				clientInfo.setChatRequests(chatRequestsList);
			}
		}

		clientIDSToActiveClients.put(clientInfo.getClientID(), clientInfo);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		clientIDSToActiveClients.remove(clientInfo.getClientID());
		
		List<ChatSession> userChatSessions = clientInfo.getChatSessions();
		for (int i = 0; i < userChatSessions.size(); i++) {
			
			ChatSession chatSession = userChatSessions.get(i);
			chatSession.getActiveMembers().remove(clientInfo);

			if (chatSession.getActiveMembers().isEmpty()) {
				ActiveChatSessions.removeChatSession(chatSession.getChatSessionID());
			}
		}
		
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

		ClientMessageType msgType = EnumIntConverter.getIntAsEnum(msg.readInt(), ClientMessageType.class);

		if (msgType == ClientMessageType.CLIENT_CONTENT) {

			ContentType contentType = EnumIntConverter.getIntAsEnum(msg.readInt(), ContentType.class);

			ChatSession chatSession;
			
			try {
				int indexOfChatSession = msg.readInt();
				chatSession = clientInfo.getChatSessions().get(indexOfChatSession);
			} catch (IndexOutOfBoundsException ioobe) {
				ByteBuf payload = ctx.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
				payload.writeBytes("Chat session selected doesn't exist. (May have been deleted by the other user)".getBytes());
				ctx.channel().writeAndFlush(payload);
				return;
			}

			int chatSessionID = chatSession.getChatSessionID();
			byte[] usernameBytes = clientInfo.getUsername().getBytes();

			byte[] textBytes = null;

			byte[] fileNameBytes = null;
			byte[] fileBytes = null;
					
			ByteBuf payload = ctx.alloc().ioBuffer();
			payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.CLIENT_CONTENT));
			payload.writeInt(EnumIntConverter.getEnumAsInt(contentType));

			switch (contentType) {
			case TEXT -> {
				
				int textLength = msg.readInt();
				textBytes = new byte[textLength];
				msg.readBytes(textBytes);
				
				payload.writeInt(textLength);
				payload.writeBytes(textBytes);
			}
			case FILE -> {
				
				int fileNameLength = msg.readInt();
				fileNameBytes = new byte[fileNameLength];
				msg.readBytes(fileNameBytes);
				
				fileBytes = new byte[msg.readableBytes()];
				msg.readBytes(fileBytes);

				payload.writeInt(fileNameLength);
				payload.writeBytes(fileNameBytes);
			}
			}
			
			int messageIDInDatabase = 0;
			try (ChatAppDatabase.WriteChatMessagesDBConnection conn = ChatAppDatabase.getWriteChatMessagesConnection()) {

				DatabaseChatMessage chatMessage = new DatabaseChatMessage(
						clientInfo.getClientID(),
						chatSessionID,
						textBytes,
						fileNameBytes,
						fileBytes, 
						contentType);

				messageIDInDatabase = conn.addMessage(chatMessage);
			}

			payload.writeInt(usernameBytes.length);
			payload.writeBytes(usernameBytes);
			
			payload.writeInt(clientInfo.getClientID());
			
			payload.writeInt(messageIDInDatabase);
			payload.writeInt(chatSessionID);
			
			broadcastMessageToChatSession(payload, chatSession);
		} else if (msgType == ClientMessageType.COMMAND) {

			ClientCommandType commandType = EnumIntConverter.getIntAsEnum(msg.readInt(), ClientCommandType.class);

			switch (commandType.getCommandLevel()) {
			case HEAVY -> {
				msg.retain(); // increase reference count by 1 for executeCommand
				commandsToBeExecutedQueue = commandsToBeExecutedQueue.thenRunAsync(() -> executeCommand(commandType, msg));
			}
			case LIGHT -> {
				executeCommand(commandType, msg);
			}
			default -> { /* Do nothing. */ }
			}
			
		}
	}
	
	private static void broadcastMessageToChatSession(ByteBuf payload, ChatSession chatSession) {

		List<ClientInfo> membersOfChatSession = chatSession.getActiveMembers();
		payload.retain(membersOfChatSession.size()); // increase reference count by the amount of clients that this message is gonna be sent to
		for (int i = 0; i < membersOfChatSession.size(); i++) {
			EpollSocketChannel channel = membersOfChatSession.get(i).getChannel();
			channel.writeAndFlush(payload.duplicate());
		}

		payload.release();
	}

	/**
	 * This method can be used by the client to execute various commands, such as to
	 * change his username or to get his clientID
	 * 
	 */
	private void executeCommand(ClientCommandType commandType, ByteBuf args) {
		
		EpollSocketChannel channel = clientInfo.getChannel();
		
		try {
			switch (commandType) {
			case CHANGE_USERNAME -> {

				byte[] newUsernameBytes = new byte[args.readableBytes()];
				args.readBytes(newUsernameBytes);

				String newUsername = new String(newUsernameBytes);
				String currentUsername = clientInfo.getUsername();
				
				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
				
				if (newUsername.equals(currentUsername)) {
					payload.writeBytes("Username cannot be the same as old username!".getBytes());
				} else {

					ResultHolder resultHolder;
					try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
						resultHolder = conn.changeUsername(clientInfo.getClientID(), newUsername);
					}

					payload.writeBytes(resultHolder.getResultMessage().getBytes());
					
					if (resultHolder.isSuccesfull()) {
						clientInfo.setUsername(newUsername);
					}
				}

				channel.writeAndFlush(payload);
			}
			case CHANGE_PASSWORD -> {
				
				byte[] newPasswordBytes = new byte[args.readableBytes()];
				args.readBytes(newPasswordBytes);

				// Note that unlike the CHANGE_USERNAME command we don't check wether or not the
				// password is the same for security reasons
				String newPassword = new String(newPasswordBytes);

				ResultHolder resultHolder;
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					resultHolder = conn.changePassword(clientInfo.getEmail(), newPassword);
				}

				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
				payload.writeBytes(resultHolder.getResultMessage().getBytes());
				channel.writeAndFlush(payload);
			}
			case DOWNLOAD_FILE -> {
				
				int chatSessionIndex = args.readInt();
				int messageID = args.readInt();
				
				LoadedInMemoryFile file;
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					file = conn.getFile(messageID, clientInfo.getChatSessions().get(chatSessionIndex).getChatSessionID());
				}

				byte[] fileBytes = file.getFileBytes();
				byte[] fileNameBytes = file.getFileName().getBytes();

				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
				payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.DOWNLOAD_FILE));
				payload.writeInt(fileNameBytes.length);
				payload.writeBytes(fileNameBytes);
				payload.writeBytes(fileBytes);
				
				channel.writeAndFlush(payload);
			}
			case SEND_CHAT_REQUEST -> {
				
				int receiverID = args.readInt();
				int senderClientID = clientInfo.getClientID();
				
				int resultUpdate;
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					resultUpdate = conn.sendChatRequest(receiverID, senderClientID);
				}
				
				boolean isSuccessfull = resultUpdate == 1;
				
				if (!isSuccessfull) {
					ByteBuf payload = channel.alloc().ioBuffer();
					payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
					payload.writeBytes("An error occured while trying to send chat request!".getBytes());
					channel.writeAndFlush(payload);
				} else {
					clientIDSToActiveClients.get(receiverID).getChatRequests().add(senderClientID);
				}
			}
			case ACCEPT_CHAT_REQUEST -> {
				
				int senderClientID = args.readInt();
				int receiverClientID = clientInfo.getClientID();
				
				int chatSessionID;
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					chatSessionID = conn.acceptChatRequestIfExists(receiverClientID, senderClientID);
				}

				if (chatSessionID != -1) {

					List<Integer> membersList = Ints.asList(receiverClientID, senderClientID);
					List<ClientInfo> activeMembersList = new ArrayList<>(membersList.size());

					ChatSession chatSession = new ChatSession(chatSessionID, activeMembersList, membersList);

					ActiveChatSessions.addChatSession(chatSessionID, chatSession);

					clientInfo.getChatRequests().remove(Integer.valueOf(senderClientID));
					clientInfo.getChatSessions().add(chatSession);

					ClientInfo senderClientInfo = clientIDSToActiveClients.get(senderClientID);

					if (senderClientInfo != null) {
						senderClientInfo.getChatSessions().add(chatSession);
					}
				} else {
					ByteBuf payload = channel.alloc().ioBuffer();
					payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
					payload.writeBytes("Something went wrong while trying accept chat request!".getBytes());
					channel.writeAndFlush(payload);
				}
			}
			case DECLINE_CHAT_REQUEST -> {
				
				int senderClientID = args.readInt();
				int receiverClientID = clientInfo.getClientID();
				
				int resultUpdate;
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					resultUpdate = conn.deleteChatRequest(receiverClientID, senderClientID);
				}
				
				boolean isSuccessfull = resultUpdate == 1;
				
				if (!isSuccessfull) {
					ByteBuf payload = channel.alloc().ioBuffer();
					payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
					payload.writeBytes("Something went wrong while trying to decline chat request!".getBytes());
					channel.writeAndFlush(payload);
				} else {
					clientInfo.getChatRequests().remove(Integer.valueOf(senderClientID));
				}
			}
			case DELETE_CHAT_SESSION -> {
				
				int chatSessionID;
				
				{
					int chatSessionIndex = args.readInt();
					chatSessionID = clientInfo.getChatSessions().get(chatSessionIndex).getChatSessionID();
				}
				
				int resultUpdate;
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					resultUpdate = conn.deleteChatSession(chatSessionID);
				}
				
				boolean isSuccessfull = resultUpdate == 1;

				if (!isSuccessfull) {
					ByteBuf payload = channel.alloc().ioBuffer();
					payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
					payload.writeBytes("Something went wrong while trying delete chat session!".getBytes());
					channel.writeAndFlush(payload);
				} else {
					
					ChatSession chatSession = ActiveChatSessions.getChatSession(chatSessionID);
					
					if (chatSession != null) {
						
						List<ClientInfo> activeMembers = chatSession.getActiveMembers();
						for (int i = 0; i < activeMembers.size(); i++) {
							activeMembers.get(i).getChatSessions().remove(chatSession);
						}
						
						ActiveChatSessions.removeChatSession(chatSessionID);
					}
				}
			}
			case DELETE_CHAT_MESSAGE -> {

				int chatSessionID;

				{
					int chatSessionIndex = args.readInt();
					chatSessionID = clientInfo.getChatSessions().get(chatSessionIndex).getChatSessionID();
				}

				int messageID = args.readInt();

				int resultUpdate;
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					resultUpdate = conn.deleteChatMessage(chatSessionID, messageID);
				}

				boolean isSuccesfull = resultUpdate == 1;

				if (isSuccesfull) {

					ByteBuf payload = channel.alloc().ioBuffer();
					payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
					payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.DELETE_CHAT_MESSAGE));
					payload.writeInt(chatSessionID);
					payload.writeInt(messageID);

					broadcastMessageToChatSession(payload, ActiveChatSessions.getChatSession(chatSessionID));
				}
			}
			case LOGOUT -> {
				
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					conn.logout(channel.remoteAddress().getAddress(), clientInfo.getClientID());
				}
				
				channel.close();
			}
			case GET_USERNAME -> {
				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
				payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.GET_USERNAME));
				payload.writeBytes(clientInfo.getUsername().getBytes());
				channel.writeAndFlush(payload);
			}
			case GET_CLIENT_ID -> {
				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
				payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.GET_CLIENT_ID));
				payload.writeInt(clientInfo.getClientID());
				channel.writeAndFlush(payload);
			}
			case GET_WRITTEN_TEXT -> {
				
				int chatSessionIndex = args.readInt();
				int chatSessionID = clientInfo.getChatSessions().get(chatSessionIndex).getChatSessionID();

				int numOfMessagesAlreadySelected = args.readInt();
				
				Message[] messages;
				
				try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
					messages = conn.selectMessages(chatSessionID, numOfMessagesAlreadySelected, ServerSettings.NUMBER_OF_MESSAGES_TO_READ_FROM_THE_DATABASE_AT_A_TIME);
				}
				
				if (messages.length > 0) {
					
					ByteBuf payload = channel.alloc().ioBuffer();
					payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
					payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.GET_WRITTEN_TEXT));
					payload.writeInt(chatSessionIndex);

					for (int i = 0; i < messages.length; i++) {

						Message message = messages[i];
						byte[] messageBytes = message.getText();
						byte[] fileNameBytes = message.getFileName();
						byte[] usernameBytes = message.getUsername().getBytes();
						ContentType contentType = message.getContentType();
						
						payload.writeInt(EnumIntConverter.getEnumAsInt(contentType));
						payload.writeInt(message.getClientID());
						payload.writeInt(message.getMessageID());

						payload.writeInt(usernameBytes.length);
						payload.writeBytes(usernameBytes);

						switch (contentType) {
						case TEXT -> {
							payload.writeInt(messageBytes.length);
							payload.writeBytes(messageBytes);
						}
						case FILE -> {
							payload.writeInt(fileNameBytes.length);
							payload.writeBytes(fileNameBytes);
						}
						}
					}

					channel.writeAndFlush(payload);
				}
			}
			case GET_CHAT_REQUESTS -> {
				
				List<Integer> chatRequests = clientInfo.getChatRequests();
				
				ByteBuf payload = channel.alloc().ioBuffer(Integer.BYTES * 3 + Integer.BYTES * chatRequests.size());
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
				payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.GET_CHAT_REQUESTS));
				payload.writeInt(chatRequests.size());
				
				if (!chatRequests.isEmpty()) {
					for (int i = 0; i < chatRequests.size(); i++) {
						int clientID = chatRequests.get(i);
						payload.writeInt(clientID);
					}
				}
				
				channel.writeAndFlush(payload);
			}
			case GET_CHAT_SESSIONS -> {
				
				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
				payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.GET_CHAT_SESSIONS));
				
				List<ChatSession> chatSessions = clientInfo.getChatSessions();

				payload.writeInt(chatSessions.size());
				if (!chatSessions.isEmpty()) {
					for (int i = 0; i < chatSessions.size(); i++) {

						ChatSession chatSession = chatSessions.get(i);
						int chatSessionID = chatSession.getChatSessionID();
						List<Integer> membersClientIDS = chatSession.getMembers();

						payload.writeInt(chatSessionID);

						// The one that is subtracted is attributed to the user inquring this command
						payload.writeInt(membersClientIDS.size() - 1);
						try (ChatAppDatabase.GeneralPurposeDBConnection conn = ChatAppDatabase.getGeneralPurposeConnection()) {
							for (int j = 0; j < membersClientIDS.size(); j++) {

								int clientID = membersClientIDS.get(j);
								byte[] usernameBytes;

								ClientInfo clientInfo = clientIDSToActiveClients.get(clientID);
								
								if (this.clientInfo.equals(clientInfo)) {
									continue;
								}
								
								if (clientInfo == null) {
									usernameBytes = conn.getUsername(clientID).getBytes();
								} else {
									usernameBytes = clientInfo.getUsername().getBytes();
								}

								payload.writeInt(clientID);
								payload.writeInt(usernameBytes.length);
								payload.writeBytes(usernameBytes);
							}

						}

						if (!chatSession.getActiveMembers().contains(clientInfo)) {
							chatSession.getActiveMembers().add(clientInfo);
						}
						
					}
				}
				
				channel.writeAndFlush(payload);
			}
			case GET_DONATION_PAGE -> {
				
				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
				payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.GET_DONATION_PAGE));
				payload.writeInt(ServerSettings.Donations.HTML_PAGE.length());
				payload.writeBytes(ServerSettings.Donations.HTML_PAGE.getBytes());
				payload.writeBytes(ServerSettings.Donations.HTML_FILE_NAME.getBytes());

				channel.writeAndFlush(payload);
			}
			case GET_SERVER_SOURCE_CODE_PAGE -> {

				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.COMMAND_RESULT));
				payload.writeInt(EnumIntConverter.getEnumAsInt(ClientCommandResultType.GET_SERVER_SOURCE_CODE_PAGE));
				payload.writeBytes(ServerSettings.SOURCE_CODE_URL.getBytes());

				channel.writeAndFlush(payload);
			}
			default -> {

				ByteBuf payload = channel.alloc().ioBuffer();
				payload.writeInt(EnumIntConverter.getEnumAsInt(ServerMessageType.SERVER_MESSAGE_INFO));
				payload.writeBytes(("Command:" + commandType.toString() + "not implemented!").getBytes());

				channel.writeAndFlush(payload);
			}
			}
		} finally {
			args.release();
		}

	}

}