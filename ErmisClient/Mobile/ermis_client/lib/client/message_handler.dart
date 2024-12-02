/* Copyright (C) 2024 Ilias Koukovinis <ilias.koukovinis@gmail.com>
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

import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';

import 'common/common.dart';
import 'io/byte_buf.dart';
import 'common/chat_request.dart';
import 'common/chat_session.dart';
import 'common/file_heap.dart';
import 'common/html_page.dart';
import 'io/input_stream.dart';
import 'common/message.dart';
import 'io/output_stream.dart';

class MessageHandler {
  late final ByteBufInputStream _inputStream;
  late final ByteBufOutputStream _outputStream;
  late final SecureSocket _secureSocket;

  late String _username;
  int clientID = -1;

  final Map<int, ChatSession> _chatSessionIDSToChatSessions = {};
  final List<ChatSession> _chatSessions = List.empty(growable: true);
	final List<ChatRequest> _chatRequests = List.empty(growable: true);

  bool _isClientListeningToMessages = false;
  
  late final Commands _commands;

  MessageHandler();

  void setByteBufInputStream(ByteBufInputStream inputStream) {
    _inputStream = inputStream;
  }

  void setByteBufOutputStream(ByteBufOutputStream outputStream) {
    _outputStream = outputStream;
    _commands = Commands(_outputStream, _chatSessions);
  }

  void setSecureSocket(SecureSocket secureSocket) {
    _secureSocket = secureSocket;
  }

  void sendMessageToClient(String text, int chatSessionIndex) {
    Uint8List textBytes = utf8.encode(text);

    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.clientContent.id);
    payload.writeInt(ContentType.text.id);
    payload.writeInt(chatSessionIndex);
    payload.writeInt(textBytes.length);
    payload.writeBytes(textBytes);

    _outputStream.write(payload);
  }

  void sendFileToClient(String fileName, Uint8List fileContentBytes, int chatSessionIndex) {

    Uint8List fileNameBytes = utf8.encode(fileName);

    ByteBuf payload = ByteBuf(4 * 4 + fileNameBytes.length + fileContentBytes.length);
    payload.writeInt(ClientMessageType.clientContent.id);
    payload.writeInt(ContentType.file.id);
    payload.writeInt(chatSessionIndex);
    payload.writeInt(fileNameBytes.length);
    payload.writeBytes(fileNameBytes);
    payload.writeBytes(fileContentBytes);

    _outputStream.write(payload);
  }

  void sendImageToClient(String fileName, Uint8List fileContentBytes, int chatSessionIndex) {
    Uint8List fileNameBytes = utf8.encode(fileName);

    ByteBuf payload = ByteBuf(4 * 4 + fileNameBytes.length + fileContentBytes.length);
    payload.writeInt(ClientMessageType.clientContent.id);
    payload.writeInt(ContentType.image.id);
    payload.writeInt(chatSessionIndex);
    payload.writeInt(fileNameBytes.length);
    payload.writeBytes(fileNameBytes);
    payload.writeBytes(fileContentBytes);

    _outputStream.write(payload);
  }

  void Function(String username) usernameFunction = (username) => {};
  void whenUsernameReceived(void Function(String username) runThis) {
    usernameFunction = runThis;
  }
  
  void Function(Message message, int chatSessionIndex) messageFunction = (message, chatSessionIndex) => {};
  void whenMessageReceived(void Function(Message message, int chatSessionIndex) runThis) {
    messageFunction = runThis;
  }

  void Function(ChatSession chatSession) writtenTextFunction = (chatSession) => {};
  void whenAlreadyWrittenTextReceived(void Function(ChatSession chatSession) runThis) {
    writtenTextFunction = runThis;
  }
  
  void Function(String message) serverMessageReceivedFunction = (message) => {};
  void whenServerMessageReceived(void Function(String message) runThis) {
    serverMessageReceivedFunction = runThis;
  }
  
  void Function(LoadedInMemoryFile file) whenFileDownloadedFunction = (file) => {};
  void whenFileDownloaded(void Function(LoadedInMemoryFile file) runThis) {
    whenFileDownloadedFunction = runThis;
  }

  void Function(LoadedInMemoryFile, int) whenImageDownloadedFunction = (image, messageID) => {};
  void whenImageDownloaded(void Function(LoadedInMemoryFile file, int messageID) runThis) {
    whenImageDownloadedFunction = runThis;
  }

  void Function(DonationHtmlPage donationPage) donationPageFunction = (donationPage) => {};
  void whenDonationPageReceived(void Function(DonationHtmlPage donationPage) runThis) {
    donationPageFunction = runThis;
  }  

  void Function(String serverSourceCodeURL) serverSourceCodeReceivedFuction = (serverSourceCodeURL) => {};
  void whenServerSourceCodeReceived(void Function(String serverSourceCodeURL) runThis) {
    serverSourceCodeReceivedFuction = runThis;
  }

  void Function(int clientID) clientIDFunction = (clientID) => {};
  void whenClientIDReceived(void Function(int clientID) runThis) {
    clientIDFunction = runThis;
  }

  void Function(List<ChatRequest> chatRequests) chatRequestsReceived = (chatRequests) => {};
  void whenChatRequestsReceived(void Function(List<ChatRequest> chatRequests) runThis) {
    chatRequestsReceived = runThis;
  }

  void Function(List<ChatSession> chatSessions) chatSessionsReceived = (chatSessions) => {};
  void whenChatSessionsReceived(void Function(List<ChatSession> chatSessions) runThis) {
    chatSessionsReceived = runThis;
  }

  void Function(ChatSession chatSession, int messageIDOfDeletedMessage) messageDeleted = (chatSession, messageIDOfDeletedMessage) => {};
  void whenMessageDeleted(void Function(ChatSession chatSession, int messageIDOfDeletedMessage) runThis) {
    messageDeleted = runThis;
  }

  void Function(Uint8List icon) profilePhotoReceived = (icon) => {};
  void whenProfilePhotoReceived(void Function(Uint8List iconBytes) runThis) {
    profilePhotoReceived = runThis;
  }

  void fetchUserInformation() {
    getCommands.fetchUsername();
    getCommands.fetchClientID();
    getCommands.fetchChatSessions();
    getCommands.fetchChatRequests();
    getCommands.fetchAccountIcon();
  }

  Future<void> startListeningToMessages() async {
    if (isListeningToMessages) {
      throw StateError("Client is already listening to messages!");
    }

    _isClientListeningToMessages = true;

    _inputStream.stream.listen(
      (Uint8List data) async {
        ByteBuf message = await ByteBufInputStream.decodeSimple(data);
        if (message.capacity > 0) {
          handleMessage(message);
        }
      },
      onDone: () {
        _secureSocket.destroy();
      },
      onError: (e) {
        if (kDebugMode) {
          debugPrint(e.toString());
        }
      },
    );
  }

  void handleMessage(ByteBuf msg) {
    try {
      ServerMessageType msgType = ServerMessageType.fromId(msg.readInt32());

      switch (msgType) {
        case ServerMessageType.serverMessageInfo:
          Uint8List content = msg.readBytes(msg.readableBytes);
          serverMessageReceivedFunction(String.fromCharCodes(content));
          break;

        case ServerMessageType.clientContent:
          Message message = Message.empty();

          ContentType contentType = ContentType.fromId(msg.readInt32());
          int timeWritten = msg.readInt64();
          Uint8List? text;
          Uint8List? fileNameBytes;

          switch (contentType) {
            case ContentType.text:
              var textLength = msg.readInt32();
              text = msg.readBytes(textLength);
              break;
            case ContentType.file || ContentType.image:
              var fileNameLength = msg.readInt32();
              fileNameBytes = msg.readBytes(fileNameLength);
              break;
          }

          var usernameLength = msg.readInt32();
          var usernameBytes = msg.readBytes(usernameLength);
          String username = String.fromCharCodes(usernameBytes);

          int clientID = msg.readInt32();
          int messageID = msg.readInt32();
          int chatSessionID = msg.readInt32();

          message.setContentType(contentType);
          message.setUsername(username);
          message.setClientID(clientID);
          message.setMessageID(messageID);
          message.setChatSessionID(chatSessionID);
          message.setChatSessionIndex(_chatSessionIDSToChatSessions[chatSessionID]!.chatSessionIndex);
          message.setText(text);
          message.setFileName(fileNameBytes);
          message.setTimeWritten(timeWritten);

          ChatSession chatSession =
              _chatSessionIDSToChatSessions[chatSessionID]!;
          if (chatSession.haveChatMessagesBeenCached) {
            chatSession.getMessages.add(message);
          }

          messageFunction(message, chatSession.chatSessionIndex);
          break;

        case ServerMessageType.commandResult:
          ClientCommandResultType commandResult =
              ClientCommandResultType.fromId(msg.readInt32());

          switch (commandResult) {
            case ClientCommandResultType.downloadFile:
              var fileNameLength = msg.readInt32();
              var fileNameBytes = msg.readBytes(fileNameLength);
              var fileBytes = msg.readBytes(msg.readableBytes);

              var file = LoadedInMemoryFile(
                  String.fromCharCodes(fileNameBytes), 
                  fileBytes);

              whenFileDownloadedFunction(file);
              break;
            case ClientCommandResultType.downloadImage:
              var messageID = msg.readInt32();
              var fileNameLength = msg.readInt32();
              var fileNameBytes = msg.readBytes(fileNameLength);
              var fileBytes = msg.readBytes(msg.readableBytes);

              var file = LoadedInMemoryFile(
                  String.fromCharCodes(fileNameBytes), fileBytes);

              whenImageDownloadedFunction(file, messageID);
              break;
            case ClientCommandResultType.getDisplayName:
              var usernameBytes = msg.readBytes(msg.readableBytes);
              var username = String.fromCharCodes(usernameBytes);
              usernameFunction(username);
              break;

            case ClientCommandResultType.getClientId:
              clientID = msg.readInt32();
              clientIDFunction(clientID);
              break;

            case ClientCommandResultType.getChatSessions:
              _chatSessions.clear();
              int chatSessionsSize = msg.readInt32();
              for (int i = 0; i < chatSessionsSize; i++) {
                int chatSessionIndex = i;
                int chatSessionID = msg.readInt32();

                ChatSession chatSession =
                    ChatSession(chatSessionID, chatSessionIndex);

                int membersSize = msg.readInt32();
                List<Member> members = <Member>[];
                for (int j = 0; j < membersSize; j++) {
                  int memberClientID = msg.readInt32();
                  bool isActive = msg.readBoolean();
                  int usernameLength = msg.readInt32();
                  String username = String.fromCharCodes(msg.readBytes(usernameLength));
                  Uint8List iconBytes = msg.readBytes(msg.readInt32());

                  members.add(Member(username, memberClientID, iconBytes, isActive));
                }

                chatSession.setMembers(members);
                _chatSessions.insert(chatSessionIndex, chatSession);
                _chatSessionIDSToChatSessions[chatSessionID] = chatSession;
              }
              chatSessionsReceived(_chatSessions);
              break;

            case ClientCommandResultType.getChatRequests:
              _chatRequests.clear();
              int friendRequestsLength = msg.readInt32();
              for (int i = 0; i < friendRequestsLength; i++) {
                int clientID = msg.readInt32();
                _chatRequests.add(ChatRequest(clientID));
              }
              chatRequestsReceived(_chatRequests);
              break;

            case ClientCommandResultType.getWrittenText:
              int chatSessionIndex = msg.readInt32();
              ChatSession chatSession = _chatSessions[chatSessionIndex];
              List<Message> messages = chatSession.getMessages;

              while (msg.readableBytes > 0) {
                ContentType contentType = ContentType.fromId(msg.readInt32());
                int clientID = msg.readInt32();
                int messageID = msg.readInt32();
                String username = String.fromCharCodes(msg.readBytes(msg.readInt32()));

                Uint8List? messageBytes;
                Uint8List? fileNameBytes;
                int timeWritten = msg.readInt64();

                switch (contentType) {
                  case ContentType.text:
                    messageBytes = msg.readBytes(msg.readInt32());
                    break;
                  case ContentType.file || ContentType.image:
                    fileNameBytes = msg.readBytes(msg.readInt32());
                    break;
                }

                messages.add(Message(
                    username: username,
                    clientID: clientID,
                    messageID: messageID,
                    chatSessionID: chatSession.chatSessionID,
                    chatSessionIndex: chatSessionIndex,
                    text: messageBytes,
                    fileName: fileNameBytes,
                    timeWritten: timeWritten,
                    contentType: contentType));
              }

              messages.sort((a, b) => a.messageID.compareTo(b.messageID));
              chatSession.setHaveChatMessagesBeenCached(true);
              writtenTextFunction(chatSession);
              break;
            case ClientCommandResultType.deleteChatMessage:
              // TODO: Handle this case.
              break;
            case ClientCommandResultType.fetchAccountIcon:
              Uint8List iconBytes = msg.readBytes(msg.readableBytes);
              profilePhotoReceived(iconBytes);
              break;
            case ClientCommandResultType.getDonationPage:
              Uint8List htmlBytes = msg.readBytes(msg.readInt32());

              Uint8List htmlFileName = msg.readBytes(msg.readableBytes);

              DonationHtmlPage htmlPage = DonationHtmlPage(
                  String.fromCharCodes(htmlBytes, 0),
                  String.fromCharCodes(htmlFileName, 0));

              donationPageFunction(htmlPage);
              break;
            case ClientCommandResultType.getServerSourceCodePage:
              // TODO: Handle this case.
              break;
          }
          break;
      }
    } catch (e) {
      if (kDebugMode) {
        debugPrint('A useful message');
      }
    }

    // _isClientListeningToMessages = false;
    // Do something
  }

  bool get isListeningToMessages => _isClientListeningToMessages;
  Commands get getCommands => _commands;
  String get username => _username;
}

class Commands {
  final ByteBufOutputStream out;
  final List<ChatSession> _chatSessions;

  Commands(this.out, this._chatSessions);

  void changeDisplayName(String newDisplayName) {
    var newUsernameBytes = utf8.encode(newDisplayName);

    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.changeUsername.id);
    payload.writeBytes(newUsernameBytes);

    out.write(payload);
  }

  void changePassword(String newPassword) {
    var newPasswordBytes = utf8.encode(newPassword);

    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.changePassword.id);
    payload.writeBytes(newPasswordBytes);

    out.write(payload);
  }

  void fetchUsername() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.fetchUsername.id);

    out.write(payload);
  }

  void fetchClientID() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.fetchClientId.id);

    out.write(payload);
  }

  void fetchWrittenText(int chatSessionIndex) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.fetchWrittenText.id);
    payload.writeInt(chatSessionIndex);
    payload.writeInt(_chatSessions[chatSessionIndex]
        .getMessages
        .length /* Amount of messages client already has */);

    out.write(payload);
  }

  void fetchChatRequests() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.fetchChatRequests.id);

    out.write(payload);
  }

  void fetchChatSessions() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.fetchChatSessions.id);

    out.write(payload);
  }

  void requestDonationHTMLPage() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.requestDonationPage.id);

    out.write(payload);
  }

  void requestServerSourceCodeHTMLPage() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.requestSourceCodePage.id);

    out.write(payload);
  }

  void sendChatRequest(int userClientID) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.sendChatRequest.id);
    payload.writeInt(userClientID);

    out.write(payload);
  }

  void acceptChatRequest(int userClientID) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.acceptChatRequest.id);
    payload.writeInt(userClientID);

    out.write(payload);

    fetchChatRequests();
    fetchChatSessions();
  }

  void declineChatRequest(int userClientID) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.declineChatRequest.id);
    payload.writeInt(userClientID);

    out.write(payload);

    fetchChatRequests();
  }

  void deleteChatSession(int chatSessionIndex) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.deleteChatSession.id);
    payload.writeInt(chatSessionIndex);

    out.write(payload);

    fetchChatSessions();
  }

  void deleteMessage(int chatSessionIndex, int messageID) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.deleteChatMessage.id);
    payload.writeInt(chatSessionIndex);
    payload.writeInt(messageID);

    out.write(payload);
  }

  void downloadFile(int messageID, int chatSessionIndex) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.downloadFile.id);
    payload.writeInt(chatSessionIndex);
    payload.writeInt(messageID);

    out.write(payload);
  }

  void downloadImage(int messageID, int chatSessionIndex) {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.downloadImage.id);
    payload.writeInt(chatSessionIndex);
    payload.writeInt(messageID);

    out.write(payload);
  }

  void logout() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.logout.id);

    out.write(payload);
  }

  Future<void> setAccountIcon(Uint8List accountIconBytes) async {
    ByteBuf payload = ByteBuf.smallBuffer(growable: true);
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.addAccountIcon.id);
    payload.writeBytes(accountIconBytes);

    out.write(payload);
  }

  void fetchAccountIcon() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.fetchAccountIcon.id);
    out.write(payload);
  }
}