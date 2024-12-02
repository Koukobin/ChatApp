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

import '../theme/app_theme.dart';

Future<void> confirmExitDialog(BuildContext context, String content, GestureTapCallback runOnConfirmation) async {
  final shouldExit = await showDialog<bool>(
    context: context,
    builder: (BuildContext context) {
      return AlertDialog(
        title: const Text("Confirmation!"),
        content: Text(content),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false), // Cancel
            child: const Text("No"),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true), // Confirm
            child: const Text("Yes"),
          ),
        ],
      );
    },
  );

  if (shouldExit ?? false) {
    runOnConfirmation();
  }
}

Future<void> showExceptionDialog(BuildContext context, String exception) async {
  // String exceptionMessage = exception.toString();
  // String simpleMessage = exceptionMessage.substring(
  //     exceptionMessage.lastIndexOf(':'), exceptionMessage.length);

  try {
    int.parse("not a number");
  } catch (e) {
    if (e is FormatException) {
      e.message;
      print("Error: ${e.message}");
    } else {
      print("Error: ${e.toString()}");
    }
  }

  await showSimpleAlertDialog(
    context: context,
    title: "An error occurred",
    content: exception,
  );
}

Future<void> showErrorDialog(BuildContext context, String message) async {
  await showSimpleAlertDialog(
    context: context,
    title: "Error",
    content: message,
  );
}

Future<void> showSimpleAlertDialog({
  required BuildContext context,
  required String title,
  required String content,
}) async {
  await showDialog<void>(
    context: context,
    builder: (BuildContext context) {
      final appColors = Theme.of(context).extension<AppColors>()!;
      return AlertDialog(
        backgroundColor: appColors.tertiaryColor,
        title: Text(
          title,
          style: TextStyle(
            color: appColors.primaryColor,
            fontWeight: FontWeight.bold,
          ),
        ),
        content: Text(
          content,
          style: TextStyle(color: appColors.inferiorColor),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              Navigator.pop(context);
            },
            style: TextButton.styleFrom(
              foregroundColor: appColors.primaryColor,
            ),
            child: const Text('OK'),
          ),
        ],
      );
    },
  );
}

AlertDialog createSimpleAlertDialog(BuildContext context, String title, String content) {
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

Future<String?> showInputDialog({
  required BuildContext context,
  required String title,
  String hintText = "",
}) async {
  final TextEditingController controller = TextEditingController();
  String? input;

  await showDialog<void>(
    context: context,
    builder: (BuildContext context) {
      final appColors = Theme.of(context).extension<AppColors>()!;
      return AlertDialog(
        backgroundColor: appColors.tertiaryColor,
        title: Text(
          title,
          style: TextStyle(
            color: appColors.primaryColor,
            fontWeight: FontWeight.bold,
          ),
        ),
        content: TextField(
          controller: controller,
          cursorColor: appColors.primaryColor,
          style: TextStyle(color: appColors.inferiorColor),
          decoration: InputDecoration(
            hintText: hintText,
            hintStyle: TextStyle(color: appColors.inferiorColor),
            enabledBorder: UnderlineInputBorder(
              borderSide: BorderSide(color: appColors.secondaryColor),
            ),
            focusedBorder: UnderlineInputBorder(
              borderSide: BorderSide(color: appColors.primaryColor),
            ),
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              input = null; // Clear input if canceled
              Navigator.pop(context);
            },
            style: TextButton.styleFrom(
              foregroundColor: appColors.secondaryColor,
            ),
            child: Text('Cancel', style: TextStyle(color: appColors.inferiorColor),),
          ),
          ElevatedButton(
            onPressed: () {
              input = controller.text.trim();
              Navigator.pop(context);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: appColors.primaryColor,
            ),
            child: const Text('OK'),
          ),
        ],
      );
    },
  );

  return input;
}

Future<void> showVerificationDialog({
  required BuildContext context,
  required String title,
  required String promptMessage,
  required VoidCallback onResendCode,
  required void Function(String) onSubmitCode,
}) {
  final TextEditingController codeController = TextEditingController();
  bool isSubmitting = false;

  return showDialog(
    context: context,
    builder: (BuildContext context) {
      return StatefulBuilder(
        builder: (BuildContext context, StateSetter setState) {
          return AlertDialog(
            title: Text(title),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(promptMessage),
                const SizedBox(height: 16.0),
                TextField(
                  controller: codeController,
                  decoration: const InputDecoration(
                    labelText: 'Enter Verification Code',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16.0),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    TextButton(
                      onPressed: isSubmitting
                          ? null
                          : () {
                              onResendCode();
                            },
                      child: const Text('Resend Code'),
                    ),
                    ElevatedButton(
                      onPressed: isSubmitting
                          ? null
                          : () {
                              final code = codeController.text.trim();
                              if (code.isEmpty) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text('Please enter the verification code'),
                                  ),
                                );
                                return;
                              }

                              setState(() {
                                isSubmitting = true;
                              });

                              Future.delayed(const Duration(seconds: 1), () {
                                onSubmitCode(code);
                                Navigator.of(context).pop();
                              }).whenComplete(() {
                                setState(() {
                                  isSubmitting = false;
                                });
                              });
                            },
                      child: isSubmitting
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('Submit'),
                    ),
                  ],
                ),
              ],
            ),
          );
        },
      );
    },
  ).then((_) {
    print("thios");
    codeController.clear();
  });
}