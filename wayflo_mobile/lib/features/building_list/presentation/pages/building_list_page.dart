import 'package:flutter/material.dart';

class BuildingListPage extends StatelessWidget {
  const BuildingListPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tòa nhà WayFlo'),
      ),
      body: const Center(
        child: Text('Danh sách tòa nhà trống'),
      ),
    );
  }
}
