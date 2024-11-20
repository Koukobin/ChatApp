import 'dart:io';

import 'chat_interface.dart';
import 'i_dont_know_name.dart';
import 'settings.dart';
import 'package:flutter/material.dart';

import 'client/client.dart';

void main() async {
  runApp(MaterialApp(
    theme: ThemeData(
      primarySwatch: Colors.orange, // Primary color
      visualDensity:
          VisualDensity.adaptivePlatformDensity, // Adapts to platform
      textTheme: const TextTheme(
        bodyMedium: TextStyle(color: Colors.black), // Customize text style
      ),
    ),
    home: const ChooseServer(),
  ));
}

AlertDialog createSimpleAlertDialog(
    BuildContext context, String title, String content) {
  return AlertDialog(
    title: Text(title),
    content: Text(content),
    actions: <Widget>[
      TextButton(
        onPressed: () {
          Navigator.pop(context);
        },
        child: const Text('OK'),
      ),
    ],
  );
}

class ChooseServer extends StatefulWidget {
  const ChooseServer({super.key});

  @override
  State<ChooseServer> createState() => ChooseServerState();
}

class ChooseServerState extends State<ChooseServer> {
  Uri? url;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const CustomAppBar(),
      body: Center(
        child: Row(
          mainAxisAlignment:
              MainAxisAlignment.spaceEvenly, // Align children horizontally
          children: <Widget>[
            SizedBox(
              width: 250.0,
              child: TextField(
                decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  labelText: 'Choose Server',
                ),
                onSubmitted: (String value) async {
                  await showDialog<void>(
                    context: context,
                    builder: (BuildContext context) {
                      url = Uri.parse(value);

                      // Check if url is valid
                      if (url!.hasScheme && url!.hasAuthority) {
                        return createSimpleAlertDialog(
                            context, "Info", "Valid url: $value");
                      }

                      return createSimpleAlertDialog(
                          context, "Info", "Invalid url: $value");
                    },
                  );
                },
              ),
            ),
            TextButton(
              onPressed: () async {
                url = Uri.parse("https://192.168.10.103:8080/");
                var remoteAddress = InternetAddress(url!.host);
                var remotePort = url!.port;
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(builder: (context) => const Primary()),
                );

                bool isSuccessful = await Client.initialize(remoteAddress, remotePort, ServerCertificateVerification.ignore);
                if (isSuccessful) {
                  Client.startMessageHandler();
                }
              },
              child: const Text("Connect"),
            ),
          ],
        ),
      ),
    );
  }
}

class Primary extends StatefulWidget {
  const Primary({super.key});

  @override
  State<Primary> createState() => PrimaryState();
}

class PrimaryState extends State<Primary> {
  int _selectedIndex = 0;

  static const TextStyle optionStyle = TextStyle(fontSize: 30, fontWeight: FontWeight.bold);

  static const List<StatefulWidget> _widgetOptions = <StatefulWidget>[
    Chats(),
    ChatRequests(),
    Settings(),
    AccountSettings()
  ];

  late List<BottomNavigationBarItem> _barItems;

  BottomNavigationBarItem _buildNavItem(
      IconData activeIcon, IconData inActiveIcon, String label, int index) {
    return BottomNavigationBarItem(
      icon: Container(
        child: _selectedIndex == index ? Icon(activeIcon) : Icon(inActiveIcon),
      ),
      label: label,
    );
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    _barItems = <BottomNavigationBarItem>[
      _buildNavItem(Icons.chat, Icons.chat_outlined, "Chats", 0),
      _buildNavItem(Icons.person_add_alt_1, Icons.person_add_alt_1_outlined,
          "Chat Requests", 1),
      _buildNavItem(Icons.settings, Icons.settings_outlined, "Settings", 2),
      _buildNavItem(
          Icons.account_circle, Icons.account_circle_outlined, "Account", 3),
    ];

    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: _widgetOptions,
      ),
      bottomNavigationBar: BottomNavigationBar(
          fixedColor: Colors.green,
          backgroundColor: Colors.black,
          unselectedItemColor: Colors.white,
          type: BottomNavigationBarType.fixed,
          currentIndex: _selectedIndex,
          onTap: _onItemTapped,
          items: _barItems),
    );
  }
}
