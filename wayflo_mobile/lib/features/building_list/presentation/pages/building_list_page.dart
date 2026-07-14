import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

// Conditional import: sử dụng GoongMapWidget phù hợp với nền tảng
import '../widgets/goong_map_stub.dart'
    if (dart.library.html) '../widgets/goong_map_web.dart';

class BuildingListPage extends StatefulWidget {
  const BuildingListPage({super.key});

  @override
  State<BuildingListPage> createState() => _BuildingListPageState();
}

class _BuildingListPageState extends State<BuildingListPage> {
  final String _targetBuildingId = 'c6a870d9-90e3-4ed0-af4a-1bd16ace8279';

  void _navigateTo3D() {
    context.push('/map/$_targetBuildingId');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('WayFlo Malls Map'),
      ),
      body: GoongMapWidget(
        apiKey: 'vDjD4QcSyzhC66t59iBBK8ZhRGzUTwEFsat16jbG',
        onView3D: _navigateTo3D,
      ),
    );
  }
}
