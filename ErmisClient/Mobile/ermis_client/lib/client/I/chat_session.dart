import 'dart:typed_data';

import 'message.dart';

class ChatSession {
  final int _chatSessionID;
  final int _chatSessionIndex;
  List<Member> _members;
  List<Message> _messages;
  bool _haveChatMessagesBeenCached;

  ChatSession(this._chatSessionID, this._chatSessionIndex)
      : _members = [],
        _messages = [],
        _haveChatMessagesBeenCached = false;

  ChatSession.withDetails(
      this._chatSessionID, this._chatSessionIndex, this._messages, this._members, this._haveChatMessagesBeenCached);

  void setMembers(List<Member> members) => this._members = members;

  void setMessages(List<Message> messages) => this._messages = messages;

  void setHaveChatMessagesBeenCached(bool haveChatMessagesBeenCached) =>
      this._haveChatMessagesBeenCached = haveChatMessagesBeenCached;

  int get chatSessionID => _chatSessionID;

  int get chatSessionIndex => _chatSessionIndex;

  List<Member> get getMembers => _members;

  List<Message> get getMessages => _messages;

  bool get haveChatMessagesBeenCached => _haveChatMessagesBeenCached;

  @override
  int get hashCode => _chatSessionID.hashCode ^ _chatSessionIndex.hashCode;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ChatSession) return false;
    return _chatSessionID == other._chatSessionID &&
        _chatSessionIndex == other._chatSessionIndex &&
        _haveChatMessagesBeenCached == other._haveChatMessagesBeenCached &&
        _members == other._members &&
        _messages == other._messages;
  }

  @override
  String toString() {
    return _members.map((member) => member.toString()).join(', ');
  }
}

class Member {
  String username;
  int clientID;
  Uint8List? icon;

  Member([this.username = '', this.clientID = 0]);

  void setUsername(String username) => this.username = username;

  void setClientID(int clientID) => this.clientID = clientID;

  String get getUsername => username;

  int get getClientID => clientID;

  Uint8List? get getIcon => icon;

  @override
  int get hashCode => clientID.hashCode;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! Member) return false;
    return clientID == other.clientID &&
        icon == other.icon &&
        username == other.username;
  }

  @override
  String toString() => '$username@$clientID';
}