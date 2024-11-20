import 'package:flutter/material.dart';

const String applicationTitle = "Ermis";

class CustomAppBar extends StatelessWidget implements PreferredSizeWidget {
  const CustomAppBar({super.key});

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      backgroundColor: Colors.black,
      foregroundColor: Colors.green,
      title: const Text(applicationTitle),
    );
  }
}

class GoBackBar extends StatefulWidget implements PreferredSizeWidget {
  const GoBackBar({super.key});

  @override
  State<GoBackBar> createState() => GoBackState();
  
  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);
}

class GoBackState extends State<GoBackBar> {
  @override
  Widget build(BuildContext context) {
    return AppBar(
      backgroundColor: Colors.black,
      foregroundColor: Colors.green,
      leading: IconButton(
        icon: const Icon(Icons.arrow_back), // Back arrow icon
        onPressed: () {
          Navigator.pop(context); // Navigate back
        },
      ),
      title: const Text("Go Back"),
    );
  }
}
