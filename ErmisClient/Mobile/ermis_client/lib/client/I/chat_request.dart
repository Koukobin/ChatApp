class ChatRequest {
  final int clientID;

  const ChatRequest(this.clientID);

  @override
  String toString() {
    return 'clientID: $clientID';
  }
}
