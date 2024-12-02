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

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import 'package:webview_flutter/webview_flutter.dart';
import 'theme/app_theme.dart';
import 'util/buttons_utils.dart';
import 'client/common/html_page.dart';
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

    final List<SizedBox> settings = <SizedBox>[];
    settings.add(createSimpleButton(context, "Help", Icons.help_outline, () {
      Navigator.push(context,
          MaterialPageRoute(builder: (context) => const HelpSettings()));
    }));
    
    return Scaffold(
      backgroundColor: appColors.secondaryColor,
      appBar: const ErmisAppBar(),
      body: ListView(
        children: settings,
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

  late final WebViewController controller;
  String? htmlPage;

  @override
  void initState() {
    super.initState();
    controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted);

    Client.getInstance().whenDonationPageReceived((DonationHtmlPage donationPage) {
      setState(() {
        htmlPage = donationPage.html;
      });
    });
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
      createReallyNiceButton(context, "Donation Page", Icons.attach_money_rounded,
          () => Client.getInstance().getCommands.requestDonationHTMLPage()),
    ];

    final appColors = Theme.of(context).extension<AppColors>()!;
    Widget widget;

    if (htmlPage != null) {
      widget = WebViewWidget(
        controller: controller,
      );
      controller.loadHtmlString(htmlPage!);
    } else {
      widget = ListView.builder(
        padding: const EdgeInsets.symmetric(vertical: 10),
        itemCount: settings.length,
        itemBuilder: (context, index) => settings[index],
      );
    }
    return Scaffold(
        backgroundColor: appColors.secondaryColor,
        appBar: const GoBackBar(title: "Help & Settings"),
        body: widget
      );
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
  const LoadingProfilePhoto({super.key});

  @override
  LoadingState<LoadingProfilePhoto> createState() => LoadingProfilePhotoState();
}

class LoadingProfilePhotoState extends LoadingState<LoadingProfilePhoto> {

  MemoryImage? profileImage;

  @override
  void initState() {
    super.initState();
    Client.getInstance().whenProfilePhotoReceived((Uint8List photoBytes) {
      setState(() {
        profileImage = MemoryImage(photoBytes);
        isLoading = false;
      });
    });
  }

  @override
  Widget build0(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    const double profileSize = 300;

    return CircleAvatar(
      radius: profileSize / 2,
      backgroundColor: appColors.primaryColor.withOpacity(0.1),
      backgroundImage: profileImage,
      child: profileImage == null
          ? Icon(
              Icons.account_circle_outlined,
              color: Colors.white,
              size: profileSize,
            )
          : null,
    );
  }

  @override
  Widget buildLoadingScreen() {
    return Center(child: CircularProgressIndicator());
  }

}

class _AccountSettingsState extends State<AccountSettings> {

  MemoryImage? profileImage;
  int clientID = 0;

  @override
  void initState() {
    super.initState();

    Client.getInstance().whenClientIDReceived((int id) {
      setState(() {
        clientID = id;
      });
    });

    Client.getInstance().whenProfilePhotoReceived((Uint8List photoBytes) {
      setState(() {
        profileImage = MemoryImage(photoBytes);
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      backgroundColor: appColors.secondaryColor,
      appBar: const ErmisAppBar(),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 15),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              // Profile Image Section
              GestureDetector(
                onTap: onChangeProfileImage,
                child: LoadingProfilePhoto(),
              ),
              // Client ID Section
              Text(
                "ID: $clientID",
                style: TextStyle(
                  fontSize: 18,
                  color: appColors.primaryColor,
                  fontWeight: FontWeight.w500,
                ),
                textAlign: TextAlign.center,
              ),
              TextButton.icon(
                onPressed: () {
                  confirmExitDialog(context, "Are you sure you want to logout?",
                      () {
                    Client.getInstance().getCommands.logout();
                    SystemNavigator.pop();
                  });
                },
                style: TextButton.styleFrom(
                  backgroundColor: appColors.secondaryColor,
                  padding:
                      const EdgeInsets.symmetric(vertical: 16, horizontal: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                icon: Icon(Icons.logout),
                label: Text(
                  "Logout From Account",
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    fontStyle: FontStyle.italic,
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
        Client.getInstance().getCommands.setAccountIcon(photoBytes);
      },
    );
  }

}
