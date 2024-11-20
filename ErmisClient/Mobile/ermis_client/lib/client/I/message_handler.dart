import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import '../common/common.dart';
import 'byte_buf.dart';
import 'chat_request.dart';
import 'chat_session.dart';
import 'file_heap.dart';
import 'html_page.dart';
import 'input_stream.dart';
import 'message.dart';
import 'output_stream.dart';

class MessageHandler {
  late final ByteBufInputStream _inputStream;
  late final ByteBufOutputStream _outputStream;
  late final SecureSocket _secureSocket;

  late String _username;
  late int clientID;

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

  late void Function(String username) usernameFunction;
  void whenUsernameReceived(void Function(String username) runThis) {
    this.usernameFunction = runThis;
  }
  
  late void Function(Message message, int chatSessionIndex) messageFunction;
  void whenMessageReceived(void Function(Message message, int chatSessionIndex) runThis) {
    this.messageFunction = runThis;
  }

  late void Function(ChatSession chatSession) writtenTextFunction;
  void whenAlreadyWrittenTextReceived(void Function(ChatSession chatSession) runThis) {
    this.writtenTextFunction = runThis;
  }
  
  late void Function(String message) serverMessageReceivedFunction;
  void whenServerMessageReceived(void Function(String message) runThis) {
    this.serverMessageReceivedFunction = runThis;
  }
  
  late void Function(LoadedInMemoryFile file) whenFileDownloadedFunction;
  void whenFileDownloaded(void Function(LoadedInMemoryFile file) runThis) {
    this.whenFileDownloadedFunction = runThis;
  }

  late void Function(DonationHtmlPage donationPage) donationPageFunction;
  void whenDonationPageReceived(void Function(DonationHtmlPage donationPage) runThis) {
    this.donationPageFunction = runThis;
  }  

  late void Function(String serverSourceCodeURL) serverSourceCodeReceivedFuction;
  void whenServerSourceCodeReceived(void Function(String serverSourceCodeURL) runThis) {
    this.serverSourceCodeReceivedFuction = runThis;
  }

  late void Function(int clientID) clientIDFunction;
  void whenClientIDReceived(void Function(int clientID) runThis) {
    this.clientIDFunction = runThis;
  }

  late void Function(List<ChatRequest> chatRequests) chatRequestsReceived;
  void whenChatRequestsReceived(void Function(List<ChatRequest> chatRequests) runThis) {
    this.chatRequestsReceived = runThis;
  }

  late void Function(List<ChatSession> chatSessions) chatSessionsReceived;
  void whenChatSessionsReceived(void Function(List<ChatSession> chatSessions) runThis) {
    this.chatSessionsReceived = runThis;
  }

  late void Function(ChatSession chatSession, int messageIDOfDeletedMessage) messageDeleted;
  void whenMessageDeleted(void Function(ChatSession chatSession, int messageIDOfDeletedMessage) runThis) {
    this.messageDeleted = runThis;
  }

  late void Function(Uint8List icon) iconReceived;
  void whenIconReceived(void Function(Uint8List icon) runThis) {
    this.iconReceived = runThis;
  }

  Future<void> startListeningToMessages() async {
    if (isListeningToMessages) {
      throw StateError("Client is already listening to messages!");
    }

    _isClientListeningToMessages = true;

    _inputStream.stream.listen(
      (Uint8List data) {
        ByteBuf message = ByteBufInputStream.decode(data);
        handleMessage(message);
      },
      onDone: () {
        _secureSocket.destroy();
      },
      onError: (e) {
        print('Error: $e');
      },
    );

    getCommands().fetchUsername();
    getCommands().fetchClientID();
    getCommands().fetchChatSessions();
    getCommands().fetchChatRequests();
    // getCommands().fetchAccountIcon();
  }

  void handleMessage(ByteBuf msg){
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
              case ContentType.file:
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
            message.setText(text);
            message.setFileName(fileNameBytes);
            message.setTimeWritten(timeWritten);

            ChatSession chatSession = _chatSessionIDSToChatSessions[chatSessionID]!;
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
                whenFileDownloadedFunction(LoadedInMemoryFile(String.fromCharCodes(fileNameBytes), fileBytes));
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
                  int usernameLength = msg.readInt32();
                  var username =
                      String.fromCharCodes(msg.readBytes(usernameLength));

                  members.add(Member(username, memberClientID));
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
                  ContentType contentType =
                      ContentType.fromId(msg.readInt32());
                  int clientID = msg.readInt32();
                  int messageID = msg.readInt32();
                  String username = String.fromCharCodes(msg.readBytes(msg.readInt32()));

                  Uint8List? messageBytes;
                  Uint8List? fileNameBytes;
                  int timeWritten =
                      msg.readInt64();

                  switch (contentType) {
                    case ContentType.text:
                      messageBytes = msg.readBytes(msg.readInt32());
                      break;
                    case ContentType.file:
                      fileNameBytes = msg.readBytes(msg.readInt32());
                      break;
                  }

                  messages.add(Message(
                      username,
                      clientID,
                      messageID,
                      chatSession.chatSessionID,
                      messageBytes,
                      fileNameBytes,
                      timeWritten,
                      contentType));
                                  }
                messages.sort((a, b) => a.messageID.compareTo(b.messageID));
                chatSession.setHaveChatMessagesBeenCached(true);
                writtenTextFunction(chatSession);
                break;
              case ClientCommandResultType.deleteChatMessage:
                // TODO: Handle this case.
                break;
              case ClientCommandResultType.fetchAccountIcon:
                // TODO: Handle this case.
                break;
              case ClientCommandResultType.getDonationPage:
                // TODO: Handle this case.
                break;
              case ClientCommandResultType.getSourceCodePage:
                // TODO: Handle this case.
                break;
            }
            break;
        }
      } catch (e) {
        print('Error while processing message: $e');
      }

    // _isClientListeningToMessages = false;
    // Do something
  }

  bool get isListeningToMessages => _isClientListeningToMessages;
  Commands getCommands() => _commands;
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

  void logout() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.logout.id);

    out.write(payload);
  }

  Future<void> addAccountIcon(File accountIcon) async {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.addAccountIcon.id);
    payload.writeBytes((await accountIcon.readAsBytes()));

    out.write(payload);
  }

  void fetchAccountIcon() {
    ByteBuf payload = ByteBuf.smallBuffer();
    payload.writeInt(ClientMessageType.command.id);
    payload.writeInt(ClientCommandType.fetchAccountIcon.id);
    out.write(payload);
  }
}