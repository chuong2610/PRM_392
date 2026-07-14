import 'package:flutter/material.dart';

class MapPage extends StatelessWidget {
  final String buildingId;

  const MapPage({super.key, required this.buildingId});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Bản đồ 3D - $buildingId'),
      ),
      body: const Center(
        child: Text('Khung cảnh 3D WayFlo'),
      ),
    );
  }
}
