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
import 'package:ermis_client/util/notifications_util.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';

import 'theme/app_theme.dart';
import 'chat_interface.dart';
import 'settings_interface.dart';
import 'package:flutter/material.dart';
import 'package:timezone/data/latest.dart' as tz;

Future<void> main() async {
  // Ensure that Flutter bindings are initialized before running the app
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize the background service
  // Initialize the foreground task
  FlutterForegroundTask.init(
    androidNotificationOptions: AndroidNotificationOptions(
      channelId: 'foreground_service_channel',
      channelName: 'Foreground Service Channel',
      channelDescription: 'This channel is used for the foreground service notification.',
      channelImportance: NotificationChannelImportance.LOW,
      priority: NotificationPriority.LOW,
    ),
    iosNotificationOptions: IOSNotificationOptions(
      showNotification: true,
    ),
    foregroundTaskOptions: ForegroundTaskOptions(eventAction: ForegroundTaskEventAction.once()),
  );  

  // Start the foreground task when the app runs
  FlutterForegroundTask.startService(
    notificationTitle: 'App is running in the background',
      notificationText: 'Your background task is active',
      callback: () => debugPrint("Flutter foreground service callback"));

  await NotificationService.init();
  tz.initializeTimeZones();

  // Defining themes
  const AppColors lightAppColors = AppColors(
      primaryColor: Colors.green,
      secondaryColor: Colors.white,
      tertiaryColor: Color.fromARGB(255, 233, 233, 233),
      quaternaryColor: Color.fromARGB(255, 150, 150, 150),
      inferiorColor: Colors.black);

  const AppColors darkAppColors = AppColors(
    primaryColor: Colors.green,
    secondaryColor: Color.fromARGB(255, 17, 17, 17),
    tertiaryColor: Color.fromARGB(221, 30, 30, 30),
    quaternaryColor: Color.fromARGB(255, 46, 46, 46),
    inferiorColor: Colors.white,
  );

  runApp(_MyApp(
    lightAppColors: lightAppColors,
    darkAppColors: darkAppColors,
  ));
}

class _MyApp extends StatefulWidget {
  final AppColors lightAppColors;
  final AppColors darkAppColors;

  const _MyApp({
    required this.lightAppColors,
    required this.darkAppColors,
  });
  
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<_MyApp> with WidgetsBindingObserver { 

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.paused:
        // App is moved to the background
        break;
      case AppLifecycleState.resumed:
        // App is moved to the foreground (resumed)
        break;
      case AppLifecycleState.detached:
        // App is being terminated
        break;
      case AppLifecycleState.inactive:
        // App is inactive (e.g., a phone call or overlay)
        break;
      case AppLifecycleState.hidden:
        // App is hidden
        break;
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      themeMode: ThemeMode.system, // Automatically switch between light and dark
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        extensions: [widget.darkAppColors],
        visualDensity: VisualDensity.adaptivePlatformDensity, // Adapts to platform
        splashFactory: InkRipple.splashFactory, // Smooth ripple
        primaryColor: widget.darkAppColors.primaryColor,
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: widget.darkAppColors.primaryColor,
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
          backgroundColor: widget.darkAppColors.tertiaryColor.withOpacity(1.0),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(15),
          ),
          titleTextStyle: TextStyle(
            color: widget.darkAppColors.primaryColor,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
          contentTextStyle: TextStyle(
            color: widget.darkAppColors.inferiorColor,
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
            foregroundColor: WidgetStateProperty.all(widget.darkAppColors.secondaryColor),
            backgroundColor: WidgetStateProperty.all(Colors.green),
            overlayColor: WidgetStateProperty.resolveWith((states) {
              if (states.contains(WidgetState.pressed)) {
                return Colors.green.withOpacity(0.2); // Splash effect color
              }
              return null; // Default for other states
            }),
          )),
          progressIndicatorTheme:
              ProgressIndicatorThemeData(color: Colors.grey),
          bottomSheetTheme:
              BottomSheetThemeData(backgroundColor: widget.darkAppColors.tertiaryColor.withOpacity(1.0))),
      theme: ThemeData(
        brightness: Brightness.light,
        extensions: [widget.lightAppColors],
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
          hintStyle: TextStyle(color: widget.lightAppColors.tertiaryColor),
          labelStyle: TextStyle(color: widget.lightAppColors.primaryColor),
          border: OutlineInputBorder(
            borderSide: BorderSide(color: widget.lightAppColors.primaryColor),
            borderRadius: BorderRadius.circular(8),
          ),
          focusedBorder: OutlineInputBorder(
            borderSide: BorderSide(color: widget.lightAppColors.primaryColor, width: 2),
          ),
          enabledBorder: OutlineInputBorder(
            borderSide: BorderSide(color: widget.lightAppColors.primaryColor),
          ),
        ),
        dialogTheme: DialogTheme(
          backgroundColor: widget.lightAppColors.tertiaryColor.withOpacity(1.0),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(15),
          ),
          titleTextStyle: TextStyle(
            color: widget.lightAppColors.primaryColor,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
          contentTextStyle: TextStyle(
            color: widget.lightAppColors.inferiorColor,
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
        progressIndicatorTheme: ProgressIndicatorThemeData(color: Colors.grey),
        bottomSheetTheme: BottomSheetThemeData(
            backgroundColor:
                widget.darkAppColors.tertiaryColor.withOpacity(1.0)),
      ),
      home: SplashScreen(),
    );
  }
}

class MainInterface extends StatefulWidget {

  const MainInterface({super.key});

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
    WidgetsBinding.instance.addPostFrameCallback((_) async {

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
