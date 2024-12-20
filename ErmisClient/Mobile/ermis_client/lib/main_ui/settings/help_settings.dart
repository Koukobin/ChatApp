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
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../constants/app_constants.dart';
import '../../theme/app_theme.dart';
import '../../util/dialogs_utils.dart';
import '../../util/top_app_bar_utils.dart';

class HelpSettings extends StatefulWidget {

  const HelpSettings({super.key});

  @override
  State<HelpSettings> createState() => HelpSettingsState();
}

class HelpSettingsState extends State<HelpSettings> {

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
        backgroundColor: appColors.secondaryColor,
        appBar: const GoBackBar(title: "Help & Settings"),
        body: Padding(
          padding: const EdgeInsets.all(8.0),
          child: ListView(children: [
            ListTile(
                leading: Icon(FontAwesomeIcons.github),
                title: Text(
                  "Source Code",
                  style: const TextStyle(
                    fontSize: 16,
                  ),
                ),
                onTap: () async {
                  final Uri url = Uri.parse(sourceCodeURL);
                  if (!await launchUrl(url)) {
                    showErrorDialog(context, "Unable to open the URL: $url");
                  }
                }),
            Divider(
              height: 1,
              thickness: 0.5,
              color: Colors.grey,
            ),
            ListTile(
                leading: Icon(Icons.attach_money_outlined),
                title: Text(
                  "Donation Page",
                  style: const TextStyle(
                    fontSize: 16,
                  ),
                ),
                onTap: () async {
                  showSnackBarDialog(
                      context: context,
                      content: "Functionality not implemented yet!");
                }),
            ListTile(
              leading: Icon(FontAwesomeIcons.listOl),
              title: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "Version",
                    style: TextStyle(color: Colors.grey[500], fontSize: 14),
                  ),
                  Text(
                    applicationVersion,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
