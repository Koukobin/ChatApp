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


import 'package:ermis_client/client/common/common.dart';
import 'package:ermis_client/util/dialogs_utils.dart';
import 'package:flutter/material.dart';

import 'constants/app_constants.dart';
import 'theme/app_theme.dart';
import 'main.dart';
import 'util/top_app_bar_utils.dart';
import 'client/client.dart';

class RegistrationInterface extends StatefulWidget {
  const RegistrationInterface({super.key});

  @override
  State<RegistrationInterface> createState() => RegistrationInterfaceState();
  
}
class RegistrationInterfaceState extends State<RegistrationInterface> {
  // Controllers for user input
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  EntryType _entryType = EntryType.login;
  bool useBackupverificationCode = false;

  Key key = UniqueKey();

  // final List<Widget> loginWidgets = [];
  // Widget? login;

  // final List<Widget> createAccountWidgets = [];
  // Widget? createAccount;

  @override
  void initState() {
    super.initState();
  }

  Future<bool> performVerification() async {
    Entry verificationEntry = Client.getInstance().createNewVerificationEntry();
    ResultHolder entryResult;

    bool isSuccessful = false;

    while (!verificationEntry.isVerificationComplete) {
      showVerificationDialog(context: context, 
      title: "Verification", 
      promptMessage: "Enter verification code sent to your email", 
      onResendCode: () => verificationEntry.resendVerificationCode(), 
      onSubmitCode: (String verificationCode) {
        verificationEntry.sendVerificationCode(verificationCode);
      });

      entryResult = await verificationEntry.getResult();
      isSuccessful = entryResult.isSuccessful;
      String resultMessage = entryResult.message;

      if (isSuccessful) {
        showSimpleAlertDialog(context: context, title: "Verification successful", content: resultMessage);
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
    final List<Widget> loginWidgets = [];
    loginWidgets.addAll([
      _buildTextField(_emailController, "Email", false),
      SizedBox(height: 8),
      _buildTextField(_passwordController, "Password", true),
      SizedBox(height: 8),
      _buildButton(
        label: "Login",
        icon: Icons.login,
        backgroundColor: appColors.secondaryColor,
        textColor: appColors.primaryColor,
        onPressed: () async {
          LoginEntry loginEntry = Client.getInstance().createNewLoginEntry();
          loginEntry.sendEntryType();

          if (useBackupverificationCode) {
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
            showSimpleAlertDialog(context: context, title: "Registration failed", content: resultMessage);
            return;
          }

          if (!useBackupverificationCode) {
            isSuccessful = await performVerification();
          }

          if (isSuccessful) {
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
          label: "${useBackupverificationCode ? "Unuse" : "Use"} backup verification code",
          icon: null,
          backgroundColor: appColors.tertiaryColor,
          textColor: appColors.primaryColor,
          onPressed: () {
            setState(() {
              useBackupverificationCode = !useBackupverificationCode;
            });
          }),
    ]);

    final List<Widget> createAccountWidgets = [];
    createAccountWidgets.addAll([
      _buildTextField(_emailController, "Email", false),
      SizedBox(height: 8),
      _buildTextField(_usernameController, "Display Name", false),
      SizedBox(height: 8),
      _buildTextField(_passwordController, "Password", true),
      SizedBox(height: 8),
      _buildButton(
        label: "Create Account",
        icon: Icons.account_circle,
        backgroundColor: appColors.secondaryColor,
        textColor: appColors.primaryColor,
        onPressed: () async {
          CreateAccountEntry createAccountEntry = Client.getInstance().createNewCreateAccountEntry();
          createAccountEntry.sendEntryType();
          createAccountEntry.sendCredentials({
            CreateAccountCredential.email: _emailController.text,
            CreateAccountCredential.username: _usernameController.text,
            CreateAccountCredential.password: _passwordController.text,
          });

          ResultHolder entryResult = await createAccountEntry.getCredentialsExchangeResult();

          bool isSuccessful = entryResult.isSuccessful;
          String resultMessage = entryResult.message;

          if (!isSuccessful) {
            showSimpleAlertDialog(context: context, title: "Registration failed", content: resultMessage);
            return;
          }

          isSuccessful = await performVerification();

          if (isSuccessful) {
            Navigator.pushAndRemoveUntil(
              context,
              MaterialPageRoute(builder: (context) => MainInterface()),
              (route) => false, // Removes all previous routes.
            );
          }

        },
      )
    ]);

    Widget switchToCreateAccount = _buildButton(
        label: "Create Account",
        icon: Icons.account_circle,
        backgroundColor: appColors.primaryColor,
        textColor: appColors.secondaryColor,
        onPressed: () {
          setState(() {
            _entryType = EntryType.createAccount;
          });
        });

    Widget switchToLogin = _buildButton(
      label: "Login",
      icon: Icons.login,
      backgroundColor: appColors.primaryColor,
      textColor: appColors.secondaryColor,
      onPressed: () {
        setState(() {
          _entryType = EntryType.login;
        });
      },
    );

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
                  children: _entryType == EntryType.login ? loginWidgets : createAccountWidgets,
                ),
              ),
            ),

            _entryType == EntryType.login ? switchToCreateAccount : switchToLogin
          ],
        ),
      ),
    );
  }

  bool obscureText = true;
  Widget _buildTextField(TextEditingController controller, String hint, bool obscureText) {
    final appColors = Theme.of(context).extension<AppColors>()!;
    return Row(
      children: [
        Expanded(
          child: TextField(
            controller: controller,
            obscureText: obscureText ? this.obscureText : false,
            decoration: InputDecoration(
              hintText: hint,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              filled: true,
              fillColor: appColors.inferiorColor,
              suffixIcon: obscureText ? IconButton(
                icon: Icon(
                  this.obscureText ? Icons.visibility : Icons.visibility_off,
                  color: Colors.black54,
                ),
                onPressed: () {
                  setState(() {
                    this.obscureText = !this.obscureText;
                  });
                },
              ) : null,
            ),
            style: TextStyle(color: appColors.secondaryColor),
          ),
        ),

      ],
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
