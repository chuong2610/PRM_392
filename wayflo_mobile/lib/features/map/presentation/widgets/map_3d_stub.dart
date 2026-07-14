import 'package:flutter/material.dart';

/// Stub implementation for mobile platforms - uses WebViewWidget.
/// On mobile this would use webview_flutter, but for now just a placeholder.
class Map3DViewer extends StatelessWidget {
  final String buildingId;
  final Function(Map<String, dynamic>)? onPoiSelected;
  final Function(String)? onFloorChanged;

  const Map3DViewer({
    super.key,
    required this.buildingId,
    this.onPoiSelected,
    this.onFloorChanged,
  });

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text('Bản đồ 3D chỉ khả dụng trên trình duyệt Web'),
    );
  }
}
