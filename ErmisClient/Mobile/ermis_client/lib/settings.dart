import 'package:flutter/material.dart';

import 'i_dont_know_name.dart';

class Settings extends StatefulWidget {

  const Settings({super.key});

  @override
  State<Settings> createState() => SettingsState();

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

class SettingsState extends State<Settings> {

  final List<SizedBox> _settings = <SizedBox>[
    createButton("Help", Icons.help_outline, () {})
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const CustomAppBar(),
      body: ListView(
        children: _settings,
      ),
    );
  }

}

class AccountSettings extends StatefulWidget {

  const AccountSettings({super.key});

  @override
  State<AccountSettings> createState() => AccountSettingsState();

}

class AccountSettingsState extends State<AccountSettings> {

  final List<SizedBox> _settings = <SizedBox>[
    createButton("To do", Icons.help_outline, () {})
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const CustomAppBar(),
      body: ListView(
        children: _settings,
      ),
    );
  }

}