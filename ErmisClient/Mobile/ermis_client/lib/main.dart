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

import 'package:ermis_client/splash_screen.dart';

import 'theme/app_theme.dart';
import 'chat_interface.dart';
import 'settings_interface.dart';
import 'package:flutter/material.dart';

import 'client/client.dart';

void main() async {
  // Ensure that Flutter bindings are initialized before running the app
  WidgetsFlutterBinding.ensureInitialized();

  // Define light and dark color themes
  const AppColors lightAppColors = AppColors(
    primaryColor: Colors.green,
    secondaryColor: Colors.white,
    tertiaryColor: Color.fromARGB(255, 233, 233, 233),
    inferiorColor: Colors.black,
  );

  const AppColors darkAppColors = AppColors(
    primaryColor: Colors.green,
    secondaryColor: Colors.black,
    tertiaryColor: Color.fromARGB(221, 30, 30, 30),
    inferiorColor: Colors.white,
  );

  // Run the app
  runApp(MyApp(
    lightAppColors: lightAppColors,
    darkAppColors: darkAppColors,
  ));
}

class MyApp extends StatelessWidget {
  final AppColors lightAppColors;
  final AppColors darkAppColors;

  const MyApp({
    super.key,
    required this.lightAppColors,
    required this.darkAppColors,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      themeMode: ThemeMode.system, // Automatically switch between light and dark
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        extensions: [darkAppColors],
        visualDensity: VisualDensity.adaptivePlatformDensity, // Adapts to platform
        splashFactory: InkRipple.splashFactory, // Smooth ripple
        primaryColor: darkAppColors.primaryColor,
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: darkAppColors.primaryColor,
            textStyle: const TextStyle(fontSize: 15),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          hintStyle: const TextStyle(color: Colors.grey),
          labelStyle: const TextStyle(color: Colors.green),
          border: OutlineInputBorder(
            borderSide: const BorderSide(color: Colors.green),
            borderRadius: BorderRadius.circular(8),
          ),
          focusedBorder: const OutlineInputBorder(
            borderSide: BorderSide(color: Colors.green, width: 2),
          ),
          enabledBorder: const OutlineInputBorder(
            borderSide: BorderSide(color: Colors.green),
          ),
        ),
        dialogTheme: DialogTheme(
          backgroundColor: darkAppColors.tertiaryColor,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(15),
          ),
          titleTextStyle: TextStyle(
            color: darkAppColors.primaryColor,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
          contentTextStyle: TextStyle(
            color: darkAppColors.inferiorColor,
            fontSize: 16,
          ),
        ),
        textSelectionTheme: TextSelectionThemeData(
          cursorColor: Colors.green, // Color of the blinking text cursor
          selectionColor: Colors.greenAccent.withOpacity(0.5), // Color of the selected text background
          selectionHandleColor: Colors.green, // Color of the selection handles
        ),
        checkboxTheme: CheckboxThemeData(
          checkColor: WidgetStateProperty.all(Colors.white), // Checkmark color
          splashRadius: 20,
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
              style: ButtonStyle(
            foregroundColor: WidgetStateProperty.all(darkAppColors.secondaryColor),
            backgroundColor: WidgetStateProperty.all(Colors.green),
            overlayColor: WidgetStateProperty.resolveWith((states) {
              if (states.contains(WidgetState.pressed)) {
                return Colors.green.withOpacity(0.2); // Splash effect color
              }
              return null; // Default for other states
            }),
          ))),
      theme: ThemeData(
        brightness: Brightness.light,
        extensions: [lightAppColors],
        visualDensity: VisualDensity.adaptivePlatformDensity, // Adapts to platform
        splashFactory: InkRipple.splashFactory, // Smooth ripple
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: Colors.green,
            textStyle: const TextStyle(fontSize: 15),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          hintStyle: TextStyle(color: lightAppColors.tertiaryColor),
          labelStyle: TextStyle(color: lightAppColors.primaryColor),
          border: OutlineInputBorder(
            borderSide: BorderSide(color: lightAppColors.primaryColor),
            borderRadius: BorderRadius.circular(8),
          ),
          focusedBorder: OutlineInputBorder(
            borderSide: BorderSide(color: lightAppColors.primaryColor, width: 2),
          ),
          enabledBorder: OutlineInputBorder(
            borderSide: BorderSide(color: lightAppColors.primaryColor),
          ),
        ),
        dialogTheme: DialogTheme(
          backgroundColor: lightAppColors.tertiaryColor,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(15),
          ),
          titleTextStyle: TextStyle(
            color: lightAppColors.primaryColor,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
          contentTextStyle: TextStyle(
            color: lightAppColors.inferiorColor,
            fontSize: 16,
          ),
        ),
        textSelectionTheme: TextSelectionThemeData(
          cursorColor: Colors.green, // Color of the blinking text cursor
          selectionColor: Colors.greenAccent.withOpacity(0.5), // Color of the selected text background
          selectionHandleColor: Colors.green, // Color of the selection handles
        ),
        checkboxTheme: CheckboxThemeData(
          checkColor: WidgetStateProperty.all(Colors.white), // Checkmark color
          splashRadius: 20,
        ),
      ),
      home: SplashScreen(),
    );
  }
}

class MainInterface extends StatefulWidget {

  const MainInterface({super.key}) ;

  @override
  State<MainInterface> createState() => MainInterfaceState();
}

class MainInterfaceState extends State<MainInterface> {
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
  void initState() {
    super.initState();
    
    Client.getInstance().fetchUserInformation();
    // Begin message handler once interface is initialized
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Client.getInstance().startMessageHandler();
    });
  }

  @override
  Widget build(BuildContext context) {
    final appColors = Theme.of(context).extension<AppColors>()!;

    _barItems = <BottomNavigationBarItem>[
      _buildNavItem(Icons.chat, Icons.chat_outlined, "Chats", 0),
      _buildNavItem(Icons.person_add_alt_1, Icons.person_add_alt_1_outlined,
          "Requests", 1),
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
          fixedColor: appColors.primaryColor,
          backgroundColor: appColors.secondaryColor,
          unselectedItemColor: appColors.inferiorColor,
          type: BottomNavigationBarType.fixed,
          currentIndex: _selectedIndex,
          onTap: _onItemTapped,
          items: _barItems),
    );
  }
}
