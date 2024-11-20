import 'dart:convert';

import 'package:flutter/material.dart';

import 'client/I/chat_request.dart';
import 'client/I/chat_session.dart';
import 'client/I/message.dart';
import 'client/client.dart';
import 'i_dont_know_name.dart';

class Chats extends StatefulWidget {

  const Chats({super.key});

  @override
  State<Chats> createState() => ChatsState();

}

SizedBox createUserIconButton(String text, GestureTapCallback onTap) {
  return createButton(text, Icons.account_circle_outlined, onTap);
}

SizedBox createButton(String text, IconData iconData, GestureTapCallback onTap) {
  return SizedBox.fromSize(
    size: const Size(0 /* Width set by list view */, 60),
    child: ClipRect(
      child: Material(
        color: Colors.black,
        child: InkWell(
          splashColor: Colors.green,
          onTap: onTap,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              Icon(iconData, color: Colors.white),
              Text(
                text,
                style: const TextStyle(
                    color: Colors.green, // Change the color here
                    fontSize: 16.0)),
            ],
          ),
        ),
      ),
    ),
  );
}

class ChatsState extends State<Chats> {

  List<SizedBox> _conversations = <SizedBox>[];

  ChatsState() {
    Client.messageHandler.whenChatSessionsReceived((List<ChatSession> chatSessions) {
      updateChatSessions(chatSessions);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const CustomAppBar(),
      body: ListView(
        children: _conversations,
      ),
    );
  }

  void updateChatSessions(List<ChatSession> chatSessions) {
    setState(() {
      _conversations.clear();
      var temp = <SizedBox>[];
      for (var i = 0; i < chatSessions.length; i++) {
        temp.add(createUserIconButton(chatSessions[i].toString(), () {

          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => MessagingInterface(chatSessionIndex: chatSessions[i].chatSessionIndex, chatSession: chatSessions[i])),
          );
        }));
      }
      _conversations = temp;
    });
  }


}

class MessagingInterface extends StatefulWidget {

  final int chatSessionIndex;
  final ChatSession chatSession;

  const MessagingInterface({super.key, required this.chatSessionIndex, required this.chatSession});

  @override
  State<MessagingInterface> createState() => MessagingInterfaceState();
}

class MessagingInterfaceState extends State<MessagingInterface> {

  late final int _chatSessionIndex;
  late final ChatSession _chatSession;

  @override
  void initState() {
    super.initState();
    _chatSessionIndex = widget.chatSessionIndex;
    _chatSession = widget.chatSession;
  }

  final List<Message> _messages = List.empty(growable: true);
  final TextEditingController _controller = TextEditingController();

  void _sendMessage() {
    if (_controller.text.trim().isEmpty) return; // if message is empty return

    Client.messageHandler.sendMessageToClient(_controller.text, _chatSessionIndex);
  }

  void _printMessage(Message msg, int chatSessionIndex, int activeChatSessionIndex) {
    _createMessage(msg, chatSessionIndex, activeChatSessionIndex);
    _controller.clear();
  }

  void _createMessage(Message msg, int chatSessionIndex, int activeChatSessionIndex) {
    setState(() {
        _messages.add(msg);
    });
  }

  @override
  Widget build(BuildContext context) {

    if (_messages.isEmpty) {
      Client.messageHandler.getCommands().fetchWrittenText(_chatSessionIndex);
    } else {
      for (var msg in _chatSession.getMessages) {
        _createMessage(msg, _chatSessionIndex, 1);
      }
    }

    Client.messageHandler.whenAlreadyWrittenTextReceived((ChatSession chatSession) {
      List<Message> messages = chatSession.getMessages;
      for (var i = 0; i < messages.length; i++) {
        _printMessage(messages[i], chatSession.chatSessionIndex, 1);
      }
    });
    Client.messageHandler.whenMessageReceived((Message msg, int chatSessionIndex) {
      _printMessage(msg, chatSessionIndex, 1);
    });
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back), // Back arrow icon
          onPressed: () {
            Navigator.pop(context); // Navigate back
          },
        ),
        title: const Row(
          children: [
            CircleAvatar(
              backgroundImage: NetworkImage(
                'https://via.placeholder.com/150', // Replace with user's profile pic URL
              ),
            ),
            SizedBox(width: 10),
            Text("Chat with User"),
          ],
        ),
      ),
      body: Column(
        children: [
          // Message Area
          Expanded(
            child: ListView.builder(
              reverse: true,
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final message = _messages[_messages.length - 1 - index];
                bool isMessageOwner = message.clientID == Client.messageHandler.clientID;
                return Align(
                  alignment: isMessageOwner
                      ? Alignment.centerRight
                      : Alignment.centerLeft,
                  child: Container(
                    margin: const EdgeInsets.symmetric(
                        vertical: 5, horizontal: 10),
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: isMessageOwner
                          ? Colors.blue[200]
                          : Colors.grey[300],
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                        message.getText != null
                            ? utf8.decode(message.getText!.toList())
                            : "",
                        style: const TextStyle(fontSize: 16)),
                  ),
                );
              },
            ),
          ),
          // Input Field
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            color: Colors.grey[200],
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: InputDecoration(
                      hintText: "Type a message...",
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(25),
                        borderSide: BorderSide.none,
                      ),
                      filled: true,
                      fillColor: Colors.white,
                    ),
                  ),
                ),
                IconButton(
                  onPressed: _sendMessage,
                  icon: const Icon(Icons.send, color: Colors.blue),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class ChatRequests extends StatefulWidget {

  const ChatRequests({super.key});

  @override
  State<ChatRequests> createState() => ChatRequestsState();
}

class ChatRequestsState extends State<ChatRequests> {

  List<SizedBox> _chatRequests = <SizedBox>[];

  @override
  Widget build(BuildContext context) {
    Client.messageHandler.whenChatRequestsReceived((List<ChatRequest> chatRequests) {
      updateChatRequests(chatRequests);
    });
    return Scaffold(
      appBar: const CustomAppBar(),
      body: ListView(
        children: _chatRequests,
      ),
    );
  }

  void updateChatRequests(List<ChatRequest> chatRequests) {
    setState(() {
      _chatRequests.clear();
      var temp = <SizedBox>[];
      for (var i = 0; i < chatRequests.length; i++) {
        temp.add(createUserIconButton(chatRequests[i].toString(), () {}));
      }
      _chatRequests = temp;
    });
  }

}
