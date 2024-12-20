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

import 'package:ermis_client/main_ui/custom_textfield.dart';
import 'package:ermis_client/util/device_utils.dart';
import 'package:ermis_client/util/dialogs_utils.dart';
import 'package:flutter/material.dart';

import '../../client/common/entry/added_info.dart';
import '../../client/common/entry/create_account_info.dart';
import '../../client/common/entry/entry_type.dart';
import '../../client/common/entry/login_info.dart';
import '../../client/common/results/ResultHolder.dart';
import '../../client/common/results/entry_result.dart';
import '../../constants/app_constants.dart';
import '../../theme/app_theme.dart';
import '../../main.dart';
import '../../util/database_service.dart';
import '../../util/top_app_bar_utils.dart';
import '../../client/client.dart';
import '../../util/transitions_util.dart';

class RegistrationInterface extends StatefulWidget {
  final EntryType entryType;
  const RegistrationInterface({this.entryType = EntryType.login, super.key});

  @override
  State<RegistrationInterface> createState() => RegistrationInterfaceState();
  
}

class RegistrationInterfaceState extends State<RegistrationInterface> {
  static bool hasSwitched = false;

  // Controllers for user input
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  bool _useBackupverificationCode = false;

  late List<Widget> loginWidgets;
  late List<Widget> createAccountWidgets;
  
  late Widget switchToCreateAccount;
  late Widget switchToLogin;

  @override
  void initState() {
    super.initState();
    // Can't access context here, so use didChangeDependencies if necessary
  }

    @override
  void dispose() {
    super.dispose();
    hasSwitched = false;
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final appColors = Theme.of(context).extension<AppColors>()!;

    // Buttons to switch
    switchToCreateAccount = _buildButton(
        label: "Create Account",
        icon: Icons.account_circle,
        backgroundColor: appColors.primaryColor,
        textColor: appColors.secondaryColor,
        onPressed: () {
          if (hasSwitched) {
            Navigator.of(context).pop();
            return;
          }
          hasSwitched = true;
          Navigator.of(context).push(
              createVerticalTransition(
                  RegistrationInterface(entryType: EntryType.createAccount),
                  DirectionYAxis.bottomToTop));
        });

    switchToLogin = _buildButton(
      label: "Login",
      icon: Icons.login,
      backgroundColor: appColors.primaryColor,
      textColor: appColors.secondaryColor,
      onPressed: () {
        if (hasSwitched) {
          Navigator.of(context).pop();
          return;
        }
        hasSwitched = false;
        Navigator.of(context).push(
            createVerticalTransition(
                RegistrationInterface(entryType: EntryType.login),
                DirectionYAxis.bottomToTop));
      },
    );
  }

  Future<bool> performVerification() async {
    Entry verificationEntry = Client.getInstance().createNewVerificationEntry();
    EntryResult entryResult;

    bool isSuccessful = false;

    while (!verificationEntry.isVerificationComplete) {
      await _showVerificationDialog(
          context: context,
          title: "Verification",
          promptMessage: "Enter verification code sent to your email",
          onResendCode: () => verificationEntry.resendVerificationCode(),
          onSumbittedCode: (int code) =>
              verificationEntry.sendVerificationCode(code));

      entryResult = await verificationEntry.getResult();
      isSuccessful = entryResult.isSuccessful;
      String resultMessage = entryResult.message;

      if (isSuccessful) {
        showSimpleAlertDialog(
            context: context,
            title: "Verification successful",
            content: resultMessage);
        Database.createDBConnection().setUserInformation(
            UserInformation(
                email: _emailController.text,
                passwordHash: entryResult.addedInfo[AddedInfo.passwordHash]!),
            Client.getInstance().serverInfo);
        break;
      }

      showSimpleAlertDialog(
          context: context,
          title: "Verification Failed",
          content: resultMessage);
    }

    return isSuccessful;
  }

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;

    loginWidgets = [];
    loginWidgets.addAll([
      CustomTextField(controller: _emailController, hint: "Email"),
      SizedBox(height: 8),
      CustomTextField(controller: _usernameController, hint: "Password", obscureText: true),
      SizedBox(height: 8),
      _buildButton(
        label: "Login",
        icon: Icons.login,
        backgroundColor: appColors.secondaryColor,
        textColor: appColors.primaryColor,
        onPressed: () async {
          LoginEntry loginEntry = Client.getInstance().createNewLoginEntry();
          loginEntry.sendEntryType();
          loginEntry.addDeviceInfo(await getDeviceType(), await getDeviceDetails());

          if (_useBackupverificationCode) {
            loginEntry.togglePasswordType();
          }

          loginEntry.sendCredentials({
            LoginCredential.email : _emailController.text,
            LoginCredential.password : _passwordController.text,
          });

          ResultHolder entryResult = await loginEntry.getCredentialsExchangeResult();

          bool isSuccessful = entryResult.isSuccessful;
          String resultMessage = entryResult.message;

          if (!isSuccessful) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text("Registration failed: $resultMessage")),
            );
            return;
          }

          if (!_useBackupverificationCode) {
            isSuccessful = await performVerification();
          }

          if (isSuccessful) {
            Client.getInstance().startMessageHandler();
            await showLoadingDialog(
                context, Client.getInstance().fetchUserInformation());
            // Navigate to the main interface
            Navigator.pushAndRemoveUntil(
              context,
              MaterialPageRoute(builder: (context) => MainInterface()),
              (route) => false, // Removes all previous routes.
            );
          }

        },
      ),
      SizedBox(height: 8),
      _buildTextButton(
          label: "${_useBackupverificationCode ? "Unuse" : "Use"} backup verification code",
          icon: null,
          backgroundColor: appColors.tertiaryColor,
          textColor: appColors.primaryColor,
          onPressed: () {
            setState(() {
              _useBackupverificationCode = !_useBackupverificationCode;
            });
          }),
    ]);

    createAccountWidgets = [];
    createAccountWidgets.addAll([
      CustomTextField(controller: _emailController, hint: "Email"),
      SizedBox(height: 8),
      CustomTextField(controller: _usernameController, hint: "Display Name"),
      SizedBox(height: 8),
      CustomTextField(controller: _passwordController, hint: "Password", obscureText: true),
      SizedBox(height: 8),
      _buildButton(
        label: "Create Account",
        icon: Icons.account_circle,
        backgroundColor: appColors.secondaryColor,
        textColor: appColors.primaryColor,
        onPressed: () async {
          CreateAccountEntry createAccountEntry = Client.getInstance().createNewCreateAccountEntry();
          createAccountEntry.sendEntryType();
          createAccountEntry.addDeviceInfo(await getDeviceType(), await getDeviceDetails());
          createAccountEntry.sendCredentials({
            CreateAccountCredential.email: _emailController.text,
            CreateAccountCredential.username: _usernameController.text,
            CreateAccountCredential.password: _passwordController.text,
          });

          ResultHolder entryResult = await createAccountEntry.getCredentialsExchangeResult();

          bool isSuccessful = entryResult.isSuccessful;
          String resultMessage = entryResult.message;

          if (!isSuccessful) {
            showSnackBarDialog(context: context, content: resultMessage);
            return;
          }

          isSuccessful = await performVerification();

          if (isSuccessful) {
            Client.getInstance().startMessageHandler();
            await showLoadingDialog(
                context, Client.getInstance().fetchUserInformation());
            // Navigate to the main interface
            Navigator.pushAndRemoveUntil(
              context,
              MaterialPageRoute(builder: (context) => MainInterface()),
              (route) => false, // Removes all previous routes.
            );
          }
        },
      )
    ]);

    return widget.entryType == EntryType.login ? _loginScaffold() : _createAccountScaffold();
  }

  Scaffold _createAccountScaffold() {
    return _mainScaffold(createAccountWidgets, switchToLogin);
  }

  Scaffold _loginScaffold() {
    return _mainScaffold(loginWidgets, switchToCreateAccount);
  }

  Scaffold _mainScaffold(List<Widget> children, Widget switchButton) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Scaffold(
      appBar: const ErmisAppBar(),
      backgroundColor: appColors.tertiaryColor,
      resizeToAvoidBottomInset: false, // Prevent resizing when keyboard opens
      body: Padding(
        padding: const EdgeInsets.fromLTRB(16.0, 60.0, 16.0, 16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // App icon display
            Image.asset(
              appIconPath,
              width: 100,
              height: 100,
            ),

            // Form section for login
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(top: 60),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: children
                ),
              ),
            ),

            switchButton
          ],
        ),
      ),
    );
  }

  Widget _buildButton({
    required String label,
    required IconData icon,
    required Color backgroundColor,
    required Color textColor,
    required VoidCallback onPressed,
  }) {
    return ElevatedButton.icon(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: backgroundColor,
        padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(10),
        ),
      ),
      icon: Icon(
        icon,
        color: textColor,
      ),
      label: Text(
        label,
        style: TextStyle(fontSize: 18, color: textColor),
      ),
    );
  }
}

Widget _buildTextButton({
  required String label,
  IconData? icon,
  required Color backgroundColor,
  required Color textColor,
  required VoidCallback onPressed,
}) {
  return TextButton.icon(
    onPressed: onPressed,
    style: TextButton.styleFrom(
      backgroundColor: backgroundColor,
      padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
      textStyle: TextStyle(fontStyle: FontStyle.italic),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10),
      ),
    ),
    icon: icon == null ? null : Icon(
      icon,
      color: textColor,
    ),
    label: Text(
      label,
      style: TextStyle(fontSize: 18, color: textColor),
    ),
  );
}

Future<void> _showVerificationDialog({
  required BuildContext context,
  required String title,
  required String promptMessage,
  required VoidCallback onResendCode,
  required void Function(int code) onSumbittedCode,
}) async {
  final TextEditingController codeController = TextEditingController();
  bool isSubmitting = false;

  await showDialog(
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
                              final codeString = codeController.text.trim();
                              if (codeString.isEmpty) {
                                showSnackBarDialog(
                                    context: context,
                                    content:
                                        'Please enter the verification code');
                                return;
                              }

                              int? codeInt = int.tryParse(codeString);

                              if (codeInt == null) {
                                showSnackBarDialog(
                                    context: context,
                                    content:
                                        "Verification code must be number");
                                return;
                              }

                              setState(() {
                                isSubmitting = true;
                              });

                              // Set a delay to close dialog
                              Future.delayed(const Duration(seconds: 1), () {
                                Navigator.of(context).pop();
                                onSumbittedCode(codeInt);
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
  ).then((_) => codeController.clear());
}
