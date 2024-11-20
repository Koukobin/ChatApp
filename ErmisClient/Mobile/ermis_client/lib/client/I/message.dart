import 'dart:typed_data';

import '../common/common.dart';

class Message {
  late String username;
  late int clientID;
  late int messageID;
  late int chatSessionID;
  Uint8List? text;
  Uint8List? fileName;
  late int timeWritten;
  late ContentType contentType;

  Message(this.username, this.clientID, this.messageID, this.chatSessionID,
      this.text, this.fileName, this.timeWritten, this.contentType);

  Message.empty();

  void setUsername(String username) => this.username = username;
  void setClientID(int clientID) => this.clientID = clientID;
  void setMessageID(int messageID) => this.messageID = messageID;
  void setChatSessionID(int chatSessionID) => this.chatSessionID = chatSessionID;
  void setText(Uint8List? text) => this.text = text;
  void setFileName(Uint8List? fileName) => this.fileName = fileName;
  void setTimeWritten(int timeWritten) => this.timeWritten = timeWritten;
  void setContentType(ContentType contentType) => this.contentType = contentType;

  String get getUsername => username;
  int get getClientID => clientID;
  int get getMessageID => messageID;
  int get getChatSessionID => chatSessionID;
  Uint8List? get getText => text;
  Uint8List? get getFileName => fileName;
  int get getTimeWritten => timeWritten;
  ContentType get getContentType => contentType;

  @override
  int get hashCode => messageID.hashCode;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! Message) return false;
    return chatSessionID == other.chatSessionID &&
        clientID == other.clientID &&
        contentType == other.contentType &&
        messageID == other.messageID &&
        text == other.text &&
        fileName == other.fileName &&
        timeWritten == other.timeWritten &&
        username == other.username;
  }

  @override
  String toString() {
    return 'Message{username: $username, clientID: $clientID, messageID: $messageID, chatSessionID: $chatSessionID, '
        'text: $text, fileName: $fileName, timeWritten: $timeWritten, contentType: $contentType}';
  }
}