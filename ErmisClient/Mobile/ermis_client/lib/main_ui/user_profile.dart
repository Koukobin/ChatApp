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

import 'package:ermis_client/main_ui/loading_state.dart';
import 'package:flutter/material.dart';

import '../client/client.dart';
import '../theme/app_theme.dart';

class UserProfilePhoto extends StatefulWidget {

  final double? radius;
  final Uint8List profileBytes;

  const UserProfilePhoto({this.radius, required this.profileBytes, super.key});

  @override
  LoadingState<UserProfilePhoto> createState() => UserProfilePhotoState();
}

class UserProfilePhotoState extends LoadingState<UserProfilePhoto> {

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build0(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Container(
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          color: appColors.primaryColor,
          width: 3.0,
        ),
      ),
      child: CircleAvatar(
        radius: widget.radius,
        backgroundColor: Colors.grey[200],
        backgroundImage: MemoryImage(widget.profileBytes),
      ),
    );
  }

  @override
  Widget buildLoadingScreen() {
    return Center(child: CircularProgressIndicator());
  }

}

class PersonalProfilePhoto extends StatefulWidget {

  final double? radius;

  const PersonalProfilePhoto({this.radius, super.key});

  @override
  LoadingState<PersonalProfilePhoto> createState() => PersonalProfilePhotoState();
}

class PersonalProfilePhotoState extends LoadingState<PersonalProfilePhoto> {

  static Uint8List? _profileBytes = Client.getInstance().profilePhoto;

  @override
  void initState() {
    super.initState();

    // Determine initial loading state based on availability of profile photo
    isLoading = _profileBytes == null;

    Client.getInstance().whenProfilePhotoReceived((Uint8List photoBytes) {
      setState(() {
        _profileBytes = photoBytes;
        isLoading = false;
      });
    });
  }

  @override
  Widget build0(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Container(
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          color: appColors.primaryColor,
          width: 3.0,
        ),
      ),
      child: CircleAvatar(
        radius: widget.radius,
        backgroundColor: Colors.grey[200],
        backgroundImage: MemoryImage(_profileBytes ?? Uint8List(0)),
        child: (_profileBytes == null || _profileBytes!.isEmpty)
            ? Icon(
                Icons.person_rounded,
                color: Colors.grey,
                size: widget.radius == null ? 40 : widget.radius! * 2,
              )
            : null,
      ),
    );
  }

  @override
  Widget buildLoadingScreen() {
    return Center(child: CircularProgressIndicator());
  }

}