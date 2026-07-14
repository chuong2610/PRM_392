import 'package:flutter/material.dart';

/// Stub implementation for non-web platforms (Android/iOS).
class GoongMapWidget extends StatelessWidget {
  final String apiKey;
  final String directionApiKey;
  final VoidCallback? onMarkerTap;
  final VoidCallback? onMapTap;
  final VoidCallback? onView3D;

  const GoongMapWidget({
    super.key,
    required this.apiKey,
    this.directionApiKey = '',
    this.onMarkerTap,
    this.onMapTap,
    this.onView3D,
  });

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text('Bản đồ chỉ khả dụng trên trình duyệt Web'),
    );
  }
}
