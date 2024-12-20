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

import 'dart:typed_data';

import 'package:ermis_client/util/dialogs_utils.dart';
import 'package:flutter/material.dart';

import 'messaging_interface.dart';
import '../../theme/app_theme.dart';
import '../../util/buttons_utils.dart';
import '../../client/common/chat_request.dart';
import '../../client/common/chat_session.dart';
import '../../client/client.dart';
import '../../util/top_app_bar_utils.dart';

class Chats extends StatefulWidget {
  const Chats({super.key});

  @override
  State<Chats> createState() => ChatsState();
}

abstract class LoadingState<T extends StatefulWidget> extends State<T> {
  bool isLoading = true;

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return buildLoadingScreen();
    }

    return build0(context);
  }

  Widget buildLoadingScreen();
  Widget build0(BuildContext context);
}

class ChatsState extends LoadingState<Chats> {
  List<ChatSession>? _conversations;

  bool _isSearching = false;
  final TextEditingController _searchController = TextEditingController();
  late FocusNode _focusNode;

  ChatsState() {
    _conversations = Client.getInstance().chatSessions;
    if (_conversations != null) {
      super.isLoading = false;
    }
  }

  @override
  void initState() {
    super.initState();

    Client.getInstance().whenChatSessionsReceived((List<ChatSession> chatSessions) {
      updateChatSessions(chatSessions);
    });

    Client.getInstance().whenServerMessageReceived((String message) async {
      await showSimpleAlertDialog(
          context: context, title: "Server Message Info", content: message);
    });

    // Whenever text changes performs search
    _searchController.addListener(() {
      performSearch();
    });

    _focusNode = FocusNode();
  }

  @override
  void dispose() {
    super.dispose();
    _focusNode.dispose();
  }

  Widget _buildSearchField() {
    final appColors = Theme.of(context).extension<AppColors>()!;
    _focusNode.requestFocus();
    return Container(
      key: ValueKey(_isSearching),
      height: 45,
      decoration: BoxDecoration(
        color: appColors.secondaryColor.withOpacity(0.4),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: appColors.secondaryColor, width: 1.5),
      ),
      child: Theme(
        data: Theme.of(context).copyWith(
          inputDecorationTheme: InputDecorationTheme(
            hintStyle: const TextStyle(color: Colors.grey),
            labelStyle: const TextStyle(color: Colors.white),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(20),
              borderSide: BorderSide.none,
            ),
            filled: true,
            fillColor: Colors.white.withOpacity(0.2),
            contentPadding: EdgeInsets.symmetric(vertical: 5),
          ),
        ),
        child: TextField(
            focusNode: _focusNode,
            controller: _searchController,
            decoration: InputDecoration(
              prefixIcon: Icon(Icons.search),
              suffixIcon: GestureDetector(
                  onTap: () {
                    setState(() {
                      _isSearching = false;
                    });
                    _focusNode.unfocus();
                    _searchController.clear();
                  },
                  child: Icon(Icons.clear)),
              hintText: 'Search...',
              fillColor: appColors.tertiaryColor,
              filled: true,
            )),
      ),
    );
  }

  @override
  Widget build0(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;

    return Scaffold(
      appBar: _isSearching
          ? ErmisAppBar(
              title: AnimatedSwitcher(
                  duration: Duration(seconds: 2),
                  transitionBuilder: (child, animation) {
                    return FadeTransition(opacity: animation, child: child);
                  },
                  child: _buildSearchField()),
            )
          : ErmisAppBar(
              actions: [
                IconButton(
                  icon: Icon(Icons.search),
                  onPressed: () {
                    setState(() {
                      _isSearching = true;
                    });
                  },
                ),
                SizedBox(width: 15),
                Icon(Icons.menu),
                SizedBox(width: 15),
              ],
            ),
      backgroundColor: appColors.secondaryColor,
      floatingActionButton: Padding(
        padding: const EdgeInsets.all(8.0),
        child: FloatingActionButton(
          onPressed: () async {
            final String? input = await showInputDialog(
                context: context,
                title: "Send Chat Request",
                hintText: "Enter client id");

            if (input == null) return;
            if (int.tryParse(input) == null) {
              showSnackBarDialog(
                  context: context, content: "Client id must be a number");
              return;
            }

            final int clientID = int.parse(input);
            Client.getInstance().commands.sendChatRequest(clientID);
          },
          backgroundColor: appColors.primaryColor,
          child: Icon(Icons.add),
        ),
      ),
      floatingActionButtonLocation:  FloatingActionButtonLocation.endFloat, // Position at the bottom right
      body: RefreshIndicator(
        // if user scrolls downwards refresh chat requests
        onRefresh: _refreshContent,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          child: _conversations!.isNotEmpty
              ? ListView.separated(
                itemCount: _conversations!.length,
                itemBuilder: (context, index) => buildChatButton(index),
                separatorBuilder: (context, index) => Divider(
                  color: appColors.primaryColor.withOpacity(0.0),
                  thickness: 1,
                  height: 10,
                ),
              )
              : // Wrap in a list view to ensure it is scrollable for refresh indicator
              ListView(
                  children: [
                    SizedBox(
                      height: MediaQuery.of(context).size.height - 150,
                      width: MediaQuery.of(context).size.width,
                      child: Center(
                        child: Text(
                          "No conversations available",
                          style: TextStyle(
                            color: appColors.inferiorColor,
                            fontSize: 16,
                            fontStyle: FontStyle.italic,
                          ),
                        ),
                      ),
                    )
                  ],
                ),
        ),
      ),
    );
  }

  Widget buildChatButton(int index) {
    int startingIndex =
        _conversations![index].toString().indexOf(_searchController.text);
    int endIndex = startingIndex + _searchController.text.length;
    final appColors = Theme.of(context).extension<AppColors>()!;
    return createOutlinedButton(
        context: context,
        text: startingIndex != -1 && endIndex != -1
            ? Text.rich(
                TextSpan(
                  text: _conversations![index]
                      .toString()
                      .substring(0, startingIndex),
                  style: TextStyle(fontSize: 16, color: appColors.primaryColor),
                  children: [
                    TextSpan(
                      text: _conversations![index]
                          .toString()
                          .substring(startingIndex, endIndex),
                      style: TextStyle(
                          color: appColors.inferiorColor,
                          fontWeight: FontWeight.bold),
                    ),
                    TextSpan(
                      text:
                          _conversations![index].toString().substring(endIndex),
                    ),
                  ],
                ),
              )
            : Text(
                _conversations![index].toString(),
                style: TextStyle(
                  color: appColors.primaryColor,
                  fontSize: 16.0,
                ),
              ),
        avatar: UserAvatar(
            imageBytes: _conversations![index].getMembers[0].getIcon,
            isOnline: _conversations![index].getMembers[0].isActive),
        onTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(
                builder: (context) => MessagingInterface(
                    chatSessionIndex: _conversations![index].chatSessionIndex,
                    chatSession: _conversations![index])),
          );
        });
  }

  Future<void> _refreshContent() async {
    Client.getInstance().commands.fetchChatSessions();
    setState(() {
      isLoading = true;
    });
  }

  void updateChatSessions(List<ChatSession> chatSessions) {
    setState(() {
      _conversations = chatSessions;
      isLoading = false;
    });
  }

  @override
  Widget buildLoadingScreen() {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      appBar: const ErmisAppBar(),
      backgroundColor: appColors.secondaryColor,
      body: Center(child: CircularProgressIndicator()),
    );
  }

  void performSearch() {
    for (var i = 0; i < _conversations!.length; i++) {
      for (var j = 0; j < _conversations!.length; j++) {
        if (_conversations![j].toString().contains(_searchController.text)) {
          setState(() {
            ChatSession temp = _conversations![j];
            if (j > 0) {
              _conversations![j] = _conversations![j - 1];
              _conversations![j - 1] = temp;
            }
          });
        }
      }
    }
  }

}

class UserAvatar extends StatelessWidget {
  final Uint8List imageBytes;
  final bool isOnline;

  const UserAvatar({
    super.key,
    required this.imageBytes,
    required this.isOnline,
  });

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Stack(
      children: [
        CircleAvatar(
          radius: 20,
          backgroundImage: imageBytes.isEmpty ? null : MemoryImage(imageBytes),
          backgroundColor: Colors.grey[200],
          child: imageBytes.isEmpty
              ? Icon(
                  Icons.person,
                  color: Colors.grey,
                )
              : null,
        ),
        // Online/Offline Indicator
        Positioned(
          bottom: 0,
          left: 30,
          child: Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(
              color: isOnline
                  ? Colors.green
                  : Colors.red, // Online or offline color
              shape: BoxShape.circle,
              border: Border.all(
                color: appColors
                    .secondaryColor, // Border to separate the indicator from the avatar
                width: 1,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class ChatRequests extends StatefulWidget {
  const ChatRequests({super.key});

  @override
  State<ChatRequests> createState() => ChatRequestsState();
}

class ChatRequestsState extends LoadingState<ChatRequests> {
  List<ChatRequest>? _chatRequests = Client.getInstance().chatRequests;

  ChatRequestsState() {
    if (_chatRequests != null) {
      super.isLoading = false;
    }
  }

  @override
  void initState() {
    super.initState();
    Client.getInstance().whenChatRequestsReceived((List<ChatRequest> chatRequests) {
      updateChatRequests(chatRequests);
    });
  }

  @override
  Widget build0(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
        backgroundColor: appColors.secondaryColor,
        appBar: const ErmisAppBar(),
        body: RefreshIndicator(
          // if user scrolls downwards refresh chat requests
          onRefresh: _refreshContent,
          child: Padding(
            padding:
                const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
            child: _chatRequests!.isNotEmpty
                ? ListView.separated(
                    itemCount: _chatRequests!.length,
                    itemBuilder: (context, index) =>
                        buildChatRequestButton(index),
                    separatorBuilder: (context, index) => Divider(
                      color: appColors.tertiaryColor.withOpacity(0.0),
                      thickness: 1,
                      height: 16,
                    ),
                  )
                :
                // Wrap in a list view to ensure it is scrollable for refresh indicator
                ListView(
                    children: [
                      SizedBox(
                        height: MediaQuery.of(context).size.height -
                            150, // Substract number to center
                        child: Center(
                          child: Text(
                            "No chat requests available",
                            style: TextStyle(
                              color: appColors.inferiorColor,
                              fontSize: 16,
                              fontStyle: FontStyle.italic,
                            ),
                          ),
                        ),
                      )
                    ],
                  ),
          ),
        ));
  }

  @override
  Widget buildLoadingScreen() {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      appBar: const ErmisAppBar(),
      backgroundColor: appColors.secondaryColor,
      body: Center(child: CircularProgressIndicator()),
    );
  }

  Widget buildChatRequestButton(int index) {
    return createOutlinedButton(
        context: context,
        text: Text(_chatRequests![index].toString()),
        avatar: UserAvatar(
            imageBytes: Uint8List.fromList(List.empty()), isOnline: false),
        otherWidgets: [
          GestureDetector(
            onTap: () => Client.getInstance()
                .commands
                .acceptChatRequest(_chatRequests![index].clientID),
            child: Icon(
              Icons.check,
              color: Colors.greenAccent,
            ),
          ),
          GestureDetector(
            onTap: () => Client.getInstance()
                .commands
                .declineChatRequest(_chatRequests![index].clientID),
            child: Icon(
              Icons.cancel_outlined,
              color: Colors.redAccent,
            ),
          ),
        ],
        onTap: () {});
  }

  Future<void> _refreshContent() async {
    Client.getInstance().commands.fetchChatRequests();
    setState(() {
      isLoading = true;
    });
  }

  void updateChatRequests(List<ChatRequest> chatRequests) {
    setState(() {
      _chatRequests = chatRequests;
      isLoading = false;
    });
  }
}
