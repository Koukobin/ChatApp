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

import 'package:ermis_client/client/common/user_device.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../theme/app_theme.dart';
import '../../client/client.dart';
import '../../util/top_app_bar_utils.dart';
import 'settings_interface.dart';

class LinkedDevices extends StatefulWidget {
  const LinkedDevices({super.key});

  @override
  State<LinkedDevices> createState() => LinkedDevicesState();
}

class LinkedDevicesState extends State<LinkedDevices> {
  List<UserDeviceInfo> devices = Client.getInstance().userDevices;

  @override
  void initState() {
    super.initState();

    Client.getInstance().whenUserDevicesReceived((List<UserDeviceInfo> devices) {
      setState(() {
        this.devices = devices;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      appBar: ErmisAppBar(
          title: Text('Linked Devices',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20))),
      body: Column(
        children: [
          Flexible(
            child: ListView.builder(
              itemCount: devices.length,
              itemBuilder: (context, index) {
                final device = devices[index];
                IconData deviceIcon = switch (device.deviceType) {
                  DeviceType.mobile => Icons.smartphone,
                  DeviceType.desktop => Icons.computer,
                  DeviceType.tablet => Icons.tablet,
                  DeviceType.unknown => Icons.device_unknown_outlined
                };
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16.0),
                  child: Card(
                    margin: const EdgeInsets.symmetric(vertical: 8.0),
                    color: appColors.tertiaryColor,
                    child: ListTile(
                      leading: Icon(deviceIcon, color: appColors.primaryColor),
                      title: Text(device.osName),
                      subtitle: Text('IP: ${device.ipAddress}'),
                      trailing: PopupMenuButton<String>(
                        onSelected: (value) {
                          if (value == 'logout') {
                            showLogoutConfirmationDialog(context,
                                'Are you sure you want to logout from ${device.formattedInfo()}?',
                                () {
                              Client.getInstance()
                                  .commands
                                  .logoutOtherDevice(device.ipAddress);
                            });
                          }
                        },
                        itemBuilder: (context) => [
                          PopupMenuItem(
                            value: 'logout',
                            child: Row(
                              children: [
                                Icon(Icons.delete, color: Colors.red),
                                SizedBox(width: 8),
                                Text('Logout'),
                              ],
                            ),
                          ),
                        ],
                      ),
                      onTap: () {
                        showLogoutConfirmationDialog(context,
                            'Are you sure you want to logout from ${device.formattedInfo()}?',
                            () {
                          Client.getInstance()
                              .commands
                              .logoutOtherDevice(device.ipAddress);
                        });
                      },
                    ),
                  ),
                );
              },
            ),
          ),
          Divider(),
          ListTile(
            leading: Icon(
              Icons.dangerous_outlined,
              size: 30,
              color: Colors.redAccent,
            ),
            title: Center(
              child: Text(
                "Logout From All Devices",
                style: const TextStyle(
                  fontSize: 16,
                  color: Colors.redAccent,
                  fontWeight: FontWeight.bold,
                  fontStyle: FontStyle.italic,
                ),
              ),
            ),
            onTap: () {
              showLogoutConfirmationDialog(context, "Are you sure you would like to logout from all devices?",
                  () {
                Client.getInstance().commands.logoutAllDevices();
                SystemNavigator.pop();
              });
            },
          )
        ],
      ),
    );
  }

}