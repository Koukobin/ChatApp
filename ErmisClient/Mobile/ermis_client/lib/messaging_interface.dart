import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import 'chat_interface.dart';
import 'client/client.dart';
import 'client/common/chat_session.dart';
import 'client/common/common.dart';
import 'client/common/file_heap.dart';
import 'client/common/message.dart';
import 'theme/app_theme.dart';
import 'util/dialogs_utils.dart';
import 'util/file_utils.dart';
import 'util/top_app_bar_utils.dart';

class MessagingInterface extends StatefulWidget {
  final int chatSessionIndex;
  final ChatSession chatSession;

  const MessagingInterface(
      {super.key, required this.chatSessionIndex, required this.chatSession});

  @override
  State<MessagingInterface> createState() => MessagingInterfaceState();
}

class MessagingInterfaceState extends LoadingState<MessagingInterface> {
  late final int _chatSessionIndex;
  late final ChatSession _chatSession;

  final List<Message> _messages = [];
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _chatSessionIndex = widget.chatSessionIndex;
    _chatSession = widget.chatSession;

    // Fetch cached messages or load from the server
    if (!_chatSession.haveChatMessagesBeenCached) {
      Client.getInstance().getCommands.fetchWrittenText(_chatSessionIndex);
    } else {
      _updateMessages(_chatSession.getMessages);
      setState(() {
        isLoading = false;
      });
    }

    // Register message listeners
    _setupListeners();
  }

  void _setupListeners() {
    Client.getInstance().whenAlreadyWrittenTextReceived((chatSession) {
      _updateMessages(chatSession.getMessages);
      setState(() {
        isLoading = false;
      });
    });

    Client.getInstance().whenMessageReceived((msg, chatSessionIndex) {
      _addMessage(msg);
    });

    Client.getInstance().whenFileDownloaded((file) async {
      await saveFileToDownloads(context, file.fileBytes, file.fileName);
      showSimpleAlertDialog(context: context, title: "Info", content: "Downloaded file");
    });

    Client.getInstance().whenImageDownloaded((file, messageID) async {
      _updateImageMessage(file, messageID);
    });
  }

  void _updateMessages(List<Message> messages) {
    setState(() {
      _messages.clear();
      _messages.addAll(messages);
    });
  }

  void _addMessage(Message msg) {
    setState(() {
      _messages.add(msg);
    });
  }

  void _updateImageMessage(LoadedInMemoryFile file, int messageID) {
    for (final message in _messages) {
      if (message.messageID == messageID) {
        setState(() {
          message.fileName = Uint8List.fromList(utf8.encode(file.fileName));
          message.imageBytes = file.fileBytes;
        });
        break;
      }
    }
  }

  void _sendMessage() {
    if (_inputController.text.trim().isEmpty) return;

    Client.getInstance().sendMessageToClient(_inputController.text, _chatSessionIndex);
    _inputController.clear();
  }

  @override
  Widget build0(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;

    return Scaffold(
      backgroundColor: appColors.tertiaryColor,
      appBar: _buildAppBar(appColors),
      body: Column(
        children: [
          _buildMessageList(appColors),
          _buildInputField(appColors),
        ],
      ),
    );
  }

  AppBar _buildAppBar(AppColors appColors) {
    return AppBar(
      backgroundColor: appColors.tertiaryColor,
      leading: IconButton(
        icon: Icon(Icons.arrow_back, color: appColors.inferiorColor),
        onPressed: () {
          Navigator.pop(context);
        },
      ),
      title: Row(
        children: [
          UserAvatar(
            imageBytes: _chatSession.getMembers[0].getIcon,
            isOnline: _chatSession.getMembers[0].isActive,
          ),
          const SizedBox(width: 10),
          Text(
            "Chat with ${widget.chatSession.getMembers[0].username}",
            style: TextStyle(color: appColors.inferiorColor),
          ),
        ],
      ),
      bottom: DividerBottom(dividerColor: appColors.inferiorColor),
    );
  }

  Widget _buildMessageList(AppColors appColors) {
    return Expanded(
      child: RefreshIndicator(
        onRefresh: () async {
          // If user reaches top of conversation retrieve more messages
          Client.getInstance().getCommands.fetchWrittenText(_chatSessionIndex);
        },
        child: ListView.builder(
          controller: _scrollController,
          reverse: true,
          itemCount: _messages.length,
          itemBuilder: (context, index) {
            final Message message = _messages[_messages.length - index - 1];
            return MessageBubble(message: message, appColors: appColors);
          },
        ),
      ),
    );
  }

  Widget _buildInputField(AppColors appColors) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      color: appColors.tertiaryColor,
      child: Row(
        children: [
          SizedBox(width: 5),
          TestWidget(chatSessionIndex: _chatSessionIndex),
          SizedBox(width: 15),
          Expanded(
            child: TextField(
              controller: _inputController,
              decoration: InputDecoration(
                hintText: "Type a message...",
                filled: true,
                fillColor: appColors.secondaryColor,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(25),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
          ),
          IconButton(
            onPressed: _sendMessage,
            icon: Icon(Icons.send, color: appColors.inferiorColor),
          ),
        ],
      ),
    );
  }


  @override
  Widget buildLoadingScreen() {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      backgroundColor: appColors.tertiaryColor,
      appBar: AppBar(
        backgroundColor: appColors.tertiaryColor,
        leading: IconButton(
          icon: Icon(Icons.arrow_back,
              color: appColors.inferiorColor), // Back arrow icon
          onPressed: () {
            Navigator.pop(context);
          },
        ),
        title: Row(
          children: [
            CircleAvatar(
              backgroundImage: widget.chatSession.getMembers[0].getIcon.isEmpty
                  ? null
                  : MemoryImage(widget.chatSession.getMembers[0].getIcon),
              backgroundColor: Colors.grey[200],
              child: widget.chatSession.getMembers[0].getIcon.isEmpty
                  ? Icon(
                      Icons.person,
                      color: Colors.grey,
                    )
                  : null,
            ),
            const SizedBox(width: 10),
            Text("Chat with ${widget.chatSession.getMembers[0].username}",
                style: TextStyle(color: appColors.inferiorColor)),
          ],
        ),
        bottom: DividerBottom(dividerColor: appColors.inferiorColor),
      ),
      body: Center(child: CircularProgressIndicator()),
    );
  }
}

class MessageBubble extends StatelessWidget {
  final Message message;
  final AppColors appColors;

  const MessageBubble({required this.message, required this.appColors, super.key});

  @override
  Widget build(BuildContext context) {
    final bool isMessageOwner = message.clientID == Client.getInstance().clientID;
    final formattedTime = DateFormat("HH:mm").format(
      DateTime.fromMillisecondsSinceEpoch(message.getTimeWritten, isUtc: true).toLocal(),
    );

    return Align(
      alignment: isMessageOwner ? Alignment.centerRight : Alignment.centerLeft,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (!isMessageOwner) Text(formattedTime, style: TextStyle(color: appColors.inferiorColor)),
          
          Container(
            margin: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
            padding: const EdgeInsets.all(10),
            constraints: BoxConstraints(maxWidth: 225, maxHeight: 300), // Constraint size
            decoration: BoxDecoration(
              color: isMessageOwner ? appColors.primaryColor : const Color.fromARGB(100, 100, 100, 100),
              borderRadius: BorderRadius.circular(10),
            ),
            child: _buildMessageContent(context, message),
          ),

          if (isMessageOwner) Text(formattedTime, style: TextStyle(color: appColors.inferiorColor)),
        ],
      ),
    );
  }

  Widget _buildMessageContent(BuildContext context, Message message) {
    switch (message.contentType) {
      case ContentType.text:
        return Text(
          utf8.decode(message.getText!.toList()),
                  softWrap: true, // Enable text wrapping
                  overflow: TextOverflow.visible, // Add ellipsis for overflow
                );
      case ContentType.file:
        return Row(
          children: [
            Text(
              utf8.decode(message.getFileName!.toList()),
              softWrap: true, // Enable text wrapping
              overflow: TextOverflow.visible, // Add ellipsis for overflow
            ), // Optional: limit to a certain number of lines), // Visible overflow),
            GestureDetector(
              onTap: () {
                Client.getInstance().getCommands.downloadFile(message.getMessageID, message.chatSessionIndex);
              },
              child: Icon(Icons.download, size: 24, color: appColors.inferiorColor),
            ),
          ],
        );
      case ContentType.image:
        final image = message.imageBytes != null ? Image.memory(message.imageBytes!) : null;
        Size? dimensions = message.imageBytes != null ? getImageDimensions(message.imageBytes!) : null;
        if (dimensions != null) {
          // double ratio = dimensions.width / dimensions.height;
          // if (dimensions.width > 225) {
          //   dimensions = Size(225, 225 / ratio);
          // } else if (dimensions.height > 225) {
          //   dimensions = Size(225 / ratio, 225);
          // }
        }
        return GestureDetector(
          onDoubleTap: () {
            Client.getInstance().getCommands.downloadImage(message.getMessageID, message.chatSessionIndex);
          },
          child: Container(
            width: dimensions?.width ?? 225,
            height: dimensions?.height ?? 150,
            color: appColors.secondaryColor,
            child: image != null
                ? GestureDetector(
                    onTap: () {
                      // Display fullscreen image
                      showDialog(
                        context: context,
                        builder: (context) => GestureDetector(
                          onTap: () {
                            // Close the fullscreen view on tap
                            Navigator.of(context).pop();
                          },
                          child: Container(
                            color: Colors.black,
                            child: Center(
                              child: FittedBox(
                                fit: BoxFit.contain,
                                child: image,
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                    child: FittedBox(fit: BoxFit.contain, child: image))
                : null,
          ),
        );
      default:
        return const Text("Unsupported message type");
    }
  }
}

class TestWidget extends StatefulWidget {

  final int chatSessionIndex;
  const TestWidget({required this.chatSessionIndex, super.key});

  @override
  State<StatefulWidget> createState() => TestWidgetState();
  
}

class TestWidgetState extends State<TestWidget> {

    @override
  void initState() {
    super.initState();

    Client.getInstance().whenFileDownloaded((LoadedInMemoryFile file) async {
      await saveFileToDownloads(context, file.fileBytes, file.fileName);
      showSimpleAlertDialog(context: context, title: "Info", content: "Downloaded file");
    });
  }

  void _sendFile(String fileName, Uint8List fileBytes) async {
    Client.getInstance().sendFileToClient(fileName, fileBytes, widget.chatSessionIndex);
  }

  void _sendImageFile(String fileName, Uint8List fileBytes) async {
    Client.getInstance().sendImageToClient(fileName, fileBytes, widget.chatSessionIndex);
  }

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return PopupMenuButton<void Function()>(
        color: appColors.secondaryColor,
        onSelected: (value) {
          value(); // Run the given function
        },
        itemBuilder: (context) => [
              PopupMenuItem<void Function()>(
                value: () async => await attachSingleFile(context,
                    (String fileName, Uint8List fileBytes) {
                  _sendFile(fileName, fileBytes);
                }),
                child: Row(
                  children: [
                    Icon(
                      Icons.file_copy,
                      color: appColors.primaryColor,
                    ),
                    const SizedBox(width: 10),
                    Text(
                      "Attach file",
                      style: TextStyle(
                        color: appColors.primaryColor,
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),
              ),
              PopupMenuItem<void Function()>(
                value: () {
                  attachSingleFile(context, (String fileName, Uint8List fileBytes) {
                    _sendImageFile(fileName, fileBytes);
                  });
                },
                child: Row(
                  children: [
                    Icon(
                      Icons.image_outlined,
                      color: appColors
                          .primaryColor, // Consistent color with other items
                    ),
                    const SizedBox(width: 10),
                    Text(
                      "Attach image",
                      style: TextStyle(
                        color: appColors.primaryColor, // Consistent color
                        fontSize: 16, // Readable font size
                      ),
                    ),
                  ],
                ),
              ),
              PopupMenuItem<void Function()>(
                value: () async {
                  XFile? file = await MyCamera.capturePhoto();

                  if (file == null) {
                    return;
                  }

                  String fileName = file.name;
                  Uint8List fileBytes = await file.readAsBytes();
                  
                  _sendImageFile(fileName, fileBytes);
                },
                child: Row(
                  children: [
                    Icon(
                      Icons.camera_alt_outlined,
                      color: appColors
                          .primaryColor, // Consistent color with other items
                    ),
                    const SizedBox(width: 10),
                    Text(
                      "Take photo",
                      style: TextStyle(
                        color: appColors.primaryColor, // Consistent color
                        fontSize: 16, // Readable font size
                      ),
                    ),
                  ],
                ),
              ),
            ],
        position: PopupMenuPosition.under,
        child: Icon(Icons.attach_file));
  }
}










// class MessagingInterfaceState1 extends LoadingState<MessagingInterface> {
//   late final int _chatSessionIndex;
//   late final ChatSession _chatSession;

//   final List<Message> _messages = List.empty(growable: true);
//   final TextEditingController _inputController = TextEditingController();

//   @override
//   void initState() {
//     super.initState();
//     _chatSessionIndex = widget.chatSessionIndex;
//     _chatSession = widget.chatSession;

//     if (!_chatSession.haveChatMessagesBeenCached) {
//       Client.getInstance().getCommands.fetchWrittenText(_chatSessionIndex);
//     } else {
//       _printMessages(_chatSession.getMessages, _chatSessionIndex, 1);
//       setState(() {
//         isLoading = false;
//       });
//     }

//     Client.getInstance().whenAlreadyWrittenTextReceived((ChatSession chatSession) {
//       List<Message> messages = chatSession.getMessages;
//       for (var i = 0; i < messages.length; i++) {
//         _printMessage(messages[i], chatSession.chatSessionIndex, 1);
//       }
//       setState(() {
//         isLoading = false;
//       });
//     });

//     Client.getInstance().whenMessageReceived((Message msg, int chatSessionIndex) {
//       _printMessage(msg, chatSessionIndex, 1);
//     });

//     Client.getInstance().whenFileDownloaded((LoadedInMemoryFile file) async {
//       await saveFileToDownloads(context, file.fileBytes, file.fileName);
//       showSimpleAlertDialog(context: context, title: "Info", content: "Downloaded file");
//     });
//     Client.getInstance().whenImageDownloaded((LoadedInMemoryFile file, int messageID) async {
//       for (final message in _messages) {
//         if (message.messageID != messageID) {
//           continue;
//         }

//         setState(() {
//           message.fileName = Uint8List.fromList(utf8.encode(file.fileName));
//           message.imageBytes = file.fileBytes;
//         });
//       }
//     });
//   }

//   void _sendMessage() {
//     if (_inputController.text.trim().isEmpty) return; // if message is empty return

//     Client.getInstance().sendMessageToClient(_inputController.text, _chatSessionIndex);
//     _inputController.clear();
//   }

//   void _printMessages(List<Message> messages, int chatSessionIndex, int activeChatSessionIndex) {
//     setState(() {
//       _messages.addAll(messages);
//     });
//   }

//   void _printMessage(Message msg, int chatSessionIndex, int activeChatSessionIndex) {
//     setState(() {
//       _messages.add(msg);
//     });
//   }

//   @override
//   Widget build0(BuildContext context) {
//     final appColors = Theme.of(context).extension<AppColors>()!;

//     return Scaffold(
//       backgroundColor: appColors.tertiaryColor,
//       appBar: AppBar(
//         backgroundColor: appColors.tertiaryColor,
//         leading: IconButton(
//           icon: Icon(Icons.arrow_back,
//               color: appColors.inferiorColor), // Back arrow icon
//           onPressed: () {
//             Navigator.pop(context); // Navigate back
//           },
//         ),
//         title: Row(
//           children: [
//             CircleAvatar(
//               backgroundImage: widget.chatSession.getMembers[0].getIcon.isEmpty ? null : MemoryImage(widget.chatSession.getMembers[0].getIcon),
//               backgroundColor: Colors.grey[200],
//               child: widget.chatSession.getMembers[0].getIcon.isEmpty
//                   ? Icon(
//                       Icons.person,
//                       color: Colors.grey,
//                     )
//                   : null,
//             ),
//             const SizedBox(width: 10),
//             Text("Chat with ${widget.chatSession.getMembers[0].username}", style: TextStyle(color: appColors.inferiorColor)),
//           ],
//         ),
//         bottom: DividerBottom(dividerColor: appColors.inferiorColor),
//       ),
//       body: Column(
//         children: [
//           // Message Area
//           Expanded(
//             child: ListView.builder(
//               scrollDirection: Axis.vertical,
//               addAutomaticKeepAlives: false,
//               reverse: true,
//               itemCount: _messages.length,
//               itemBuilder: (context, index) {
//                 // Choose index like this, in order for the messages
//                 // to be displayed from recent to oldest.
//                 // Not my proudest code, but it works
//                 final Message message = _messages[_messages.length - 1 - index];
//                 bool isMessageOwner = message.clientID == Client.getInstance().clientID;

//                 var dateTime = DateTime.fromMillisecondsSinceEpoch(
//                     message.getTimeWritten,
//                     isUtc: true);
//                 var dateLocal = dateTime.toLocal();
//                 String formattedTime = DateFormat("HH:mm").format(dateLocal);

//                 Widget messageWidget;
//                 switch (message.contentType) {
//                   case ContentType.text:
//                     messageWidget = Text(utf8.decode(message.getText!.toList()));
//                     break;
//                   case ContentType.file:
//                     messageWidget = Row(
//                       children: [
//                         Text(utf8.decode(message.getFileName!.toList())),
//                         GestureDetector(
//                           onTap: () {
//                             showSimpleAlertDialog(
//                                 context: context,
//                                 title: "Notice",
//                                 content: "Downloading file");
//                             Client.getInstance().getCommands.downloadFile(
//                                 message.getMessageID, _chatSessionIndex);
//                           },
//                           child: const Icon(
//                             Icons.download,
//                             size: 24, // Control the icon size here
//                           ),
//                         ),
//                       ],
//                     );
//                     break;
//                   case ContentType.image:
//                     Image? image = message.imageBytes != null ? Image.memory(message.imageBytes!) : null;
//                     messageWidget = Column(
//                       children: [
//                         GestureDetector(
//                           onDoubleTap: () {
//                             Client.getInstance().getCommands.downloadImage(message.getMessageID, _chatSessionIndex);
//                           },
//                           onTap: () {
//                             print("On tap");
//                           },
//                           child: Container(
//                             width: 225,
//                             height: 150,
//                             color: appColors.secondaryColor,
//                             child: FittedBox(fit: BoxFit.contain, child: image),
//                           ),
//                         ),
//                         Padding(
//                           padding: const EdgeInsets.all(8.0),
//                           child: Row(
//                             children: [
//                               Text(utf8.decode(message.getFileName!.toList())),
//                               GestureDetector(
//                                 onTap: () {
//                                   if (image == null) {
//                                     Client.getInstance()
//                                         .getCommands
//                                         .downloadFile(message.getMessageID,
//                                             _chatSessionIndex);
//                                   } else {
//                                     saveFileToDownloads(
//                                         context,
//                                         message.imageBytes!,
//                                         String.fromCharCodes(
//                                             message.fileName!));
//                                   }
//                                 },
//                                 child: const Icon(
//                                   Icons.download,
//                                   size: 24, // Control the icon size here
//                                 ),
//                               ),
//                             ],
//                           ),
//                         ),
//                       ],
//                     );
//                     break;
//                 }

//                 Container container = Container(
//                   margin:
//                       const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
//                   padding: const EdgeInsets.all(10),
//                   decoration: BoxDecoration(
//                     color: isMessageOwner ? appColors.primaryColor : const Color.fromARGB(100, 100, 100, 100),
//                     borderRadius: BorderRadius.circular(10),
//                   ),
//                   child: messageWidget,
//                 );

//                 Row row = Row(
//                   mainAxisSize: MainAxisSize.min, // Only takes space needed by its children
//                   children: [],
//                 );

//                 if (isMessageOwner) {
//                   row.children.addAll([container, Text(formattedTime, style: TextStyle(color: appColors.inferiorColor))]);
//                 } else {
//                   row.children.addAll([Text(formattedTime, style: TextStyle(color: appColors.inferiorColor)), container]);
//                 }

//                 return Align(
//                   alignment: isMessageOwner
//                       ? Alignment.centerRight
//                       : Alignment.centerLeft,
//                   child: row,
//                 );
//               },
//             ),
//           ),
//           // Input Field
//           Container(
//             padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
//             color: appColors.tertiaryColor,
//             child: Row(
//               children: [
//                 SizedBox(width: 5),
//                 TestWidget(chatSessionIndex: _chatSessionIndex),
//                 SizedBox(width: 15),
//                 Expanded(
//                   child: TextField(
//                     controller: _inputController,
//                     decoration: InputDecoration(
//                       hintText: "Type a message...",
//                       filled: true,
//                       fillColor: appColors.secondaryColor,
//                       border: OutlineInputBorder(
//                         borderRadius: BorderRadius.circular(25),
//                         borderSide: BorderSide.none,
//                       ),
//                     ),
//                   ),
//                 ),
//                 IconButton(
//                   onPressed: _sendMessage,
//                   icon: Icon(Icons.send, color: appColors.inferiorColor),
//                 ),
//               ],
//             ),
//           ),
//         ],
//       ),
//     );
//   }

//   @override
//   Widget buildLoadingScreen() {
//     final appColors = Theme.of(context).extension<AppColors>()!;
//     return Scaffold(
//       backgroundColor: appColors.tertiaryColor,
//       appBar: AppBar(
//         backgroundColor: appColors.tertiaryColor,
//         leading: IconButton(
//           icon: Icon(Icons.arrow_back,
//               color: appColors.inferiorColor), // Back arrow icon
//           onPressed: () {
//             Navigator.pop(context); // Navigate back
//           },
//         ),
//         title: Row(
//           children: [
//             CircleAvatar(
//               backgroundImage: widget.chatSession.getMembers[0].getIcon.isEmpty ? null : MemoryImage(widget.chatSession.getMembers[0].getIcon),
//               backgroundColor: Colors.grey[200],
//               child: widget.chatSession.getMembers[0].getIcon.isEmpty
//                   ? Icon(
//                       Icons.person,
//                       color: Colors.grey,
//                     )
//                   : null,
//             ),
//             const SizedBox(width: 10),
//             Text("Chat with ${widget.chatSession.getMembers[0].username}", style: TextStyle(color: appColors.inferiorColor)),
//           ],
//         ),
//         bottom: DividerBottom(dividerColor: appColors.inferiorColor),
//       ),
//       body: Center(child: CircularProgressIndicator()),
//     );
//   }
// }
