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

import 'package:ermis_client/util/transitions_util.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import 'theme/app_theme.dart';
import 'util/buttons_utils.dart';
import 'client/client.dart';
import 'util/dialogs_utils.dart';
import 'util/top_app_bar_utils.dart';
import 'util/file_utils.dart';

class Settings extends StatefulWidget {

  const Settings({super.key});

  @override
  State<Settings> createState() => SettingsState();
}

class SettingsState extends State<Settings> {

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      backgroundColor: appColors.tertiaryColor,
      appBar: ErmisAppBar(
        title: Text(
          'Settings',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
        ),
      ),
      body: ListView(
        children: [
          ListTile(
            leading: const LoadingProfilePhoto(),
            title: const LoadingDisplayName(),
            subtitle: Text('Privacy, security, change number'),
            onTap: () {
              pushHorizontalTransition(context, const AccountSettings());
            },
          ),
          ListTile(
            leading: Icon(Icons.lock),
            title: Text('Account'),
            subtitle: Text('Privacy, security, change number'),
            onTap: () {
              // Navigate to Account settings
            },
          ),
          ListTile(
            leading: Icon(Icons.chat),
            title: Text('Chats'),
            subtitle: Text('Theme, wallpapers, chat history'),
            onTap: () {
              // Navigate to Chats settings
            },
          ),
          ListTile(
            leading: Icon(Icons.notifications),
            title: Text('Notifications'),
            subtitle: Text('Message, group, and call tones'),
            onTap: () {
              // Navigate to Notifications settings
            },
          ),
          ListTile(
            leading: Icon(Icons.data_usage),
            title: Text('Storage and Data'),
            subtitle: Text('Network usage, auto-download'),
            onTap: () {
              // Navigate to Storage and Data settings
            },
          ),
          ListTile(
            leading: Icon(Icons.help),
            title: Text('Help'),
            subtitle: Text('FAQ, contact us, terms and privacy policy'),
            onTap: () {
              pushHorizontalTransition(context, const HelpSettings());
            },
          ),
          Divider(),
          ListTile(
            leading: Icon(Icons.link),
            title: Text('Linked Devices'),
            onTap: () {
              // Navigate to Linked Devices
            },
          ),
          ListTile(
            leading: Icon(
              Icons.logout,
              color: Colors.redAccent,
            ),
            title: Text(
              "Logout From Account",
              style: const TextStyle(
                fontSize: 16,
                color: Colors.redAccent,
                fontWeight: FontWeight.bold,
                fontStyle: FontStyle.italic,
              ),
            ),
            onTap: () {
              confirmExitDialog(context, "Are you sure you want to logout?",
                  () {
                Client.getInstance().commands.logout();
                SystemNavigator.pop();
              });
            },
          )
        ],
      ),
    );
  }

}

class HelpSettings extends StatefulWidget {

  const HelpSettings({super.key});

  @override
  State<HelpSettings> createState() => HelpSettingsState();
}

class HelpSettingsState extends State<HelpSettings> {

  String? htmlPage;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {

    List<Widget> settings = [
      createReallyNiceButton(context, "Source Code", Icons.code, () async {
        final Uri url = Uri.parse('https://github.com/Koukobin/Ermis');
        if (!await launchUrl(url)) {
          showErrorDialog(context, "Unable to open the URL: $url");
        }
      }),
      createReallyNiceButton(context, "Donation Page", Icons.attach_money_rounded, () {
        // Do nothing
      }),
    ];

    final appColors = Theme.of(context).extension<AppColors>()!;

    return Scaffold(
        backgroundColor: appColors.secondaryColor,
        appBar: const GoBackBar(title: "Help & Settings"),
        body: ListView.builder(
          padding: const EdgeInsets.symmetric(vertical: 10),
          itemCount: settings.length,
          itemBuilder: (context, index) => settings[index],
        ));
  }

}
class AccountSettings extends StatefulWidget {
  const AccountSettings({super.key});

  @override
  State<AccountSettings> createState() => _AccountSettingsState();
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

class LoadingProfilePhoto extends StatefulWidget {

  final double? radius;

  const LoadingProfilePhoto({this.radius, super.key});

  @override
  LoadingState<LoadingProfilePhoto> createState() => LoadingProfilePhotoState();
}

class LoadingProfilePhotoState extends LoadingState<LoadingProfilePhoto> {

  static MemoryImage? _profileImage = MemoryImage(Client.getInstance().profilePhoto!);

  @override
  void initState() {
    super.initState();
    if (_profileImage != null) {
      isLoading = false;
    }

    Client.getInstance().whenProfilePhotoReceived((Uint8List photoBytes) {
      setState(() {
        _profileImage = MemoryImage(photoBytes);
        isLoading = false;
      });
    });
  }

  @override
  Widget build0(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return CircleAvatar(
      radius: widget.radius,
      backgroundColor: appColors.secondaryColor,
      backgroundImage: _profileImage,
      child: _profileImage!.bytes.isEmpty
          ? Icon(
              Icons.account_circle_outlined,
              color: Colors.white,
              size: widget.radius == null ? 40 : widget.radius! * 2,
            )
          : null,
    );
  }

  @override
  Widget buildLoadingScreen() {
    return Center(child: CircularProgressIndicator());
  }

}

class LoadingDisplayName extends StatefulWidget {
  const LoadingDisplayName({super.key});

  @override
  State<LoadingDisplayName> createState() => LoadingDisplayNameState();
}

class LoadingDisplayNameState extends State<LoadingDisplayName> {
  static String _displayName = Client.getInstance().displayName ?? "";

  @override
  void initState() {
    super.initState();

    Client.getInstance().whenUsernameReceived((String displayName) {
      setState(() {
        _displayName = displayName;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Text(
      _displayName,
      style: TextStyle(fontSize: 18),
    );
  }
  
}

class _AccountSettingsState extends State<AccountSettings> {

  int _clientID = Client.getInstance().clientID;

  @override
  void initState() {
    super.initState();

    Client.getInstance().whenClientIDReceived((int id) {
      setState(() {
        _clientID = id;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      backgroundColor: appColors.secondaryColor,
      appBar: const ErmisAppBar(
          title: Text(
        "Account Settings",
        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
      )),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Profile Image Section
              Card(
                elevation: 5,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(100),
                ),
                child: GestureDetector(
                  onTap: onChangeProfileImage,
                  child: CircleAvatar(
                    radius: 80,
                    backgroundColor: appColors.primaryColor.withOpacity(0.1),
                    child: LoadingProfilePhoto(radius: 80),
                  ),
                ),
              ),
              SizedBox(height: 20),
              // Client ID Section
              Card(
                elevation: 3,
                margin: EdgeInsets.symmetric(vertical: 10),
                child: Padding(
                  padding: const EdgeInsets.all(15.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "ID: $_clientID",
                        style: TextStyle(
                          fontSize: 18,
                          color: appColors.primaryColor,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      SizedBox(height: 10),
                      Text(
                        "Your Name:",
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w400,
                        ),
                      ),
                      SizedBox(height: 5),
                      LoadingDisplayName()
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void onChangeProfileImage() {
    attachSingleFile(context, (String profilePhotoName, Uint8List photoBytes) {
        Client.getInstance().commands.setAccountIcon(photoBytes);
      },
    );
  }

}
