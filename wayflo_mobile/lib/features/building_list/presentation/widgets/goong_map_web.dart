// ignore_for_file: avoid_web_libraries_in_flutter
import 'dart:convert';
import 'dart:html' as html;
import 'dart:ui_web' as ui_web;
import 'package:flutter/material.dart';

/// Web implementation: Renders Goong Maps via an iframe pointing to goong_map.html
/// served from the same origin (Flutter web/), bypassing all CORS restrictions.
class GoongMapWidget extends StatefulWidget {
  final String apiKey;
  final String directionApiKey;
  final VoidCallback? onMarkerTap;
  final VoidCallback? onMapTap;
  final VoidCallback? onView3D;

  const GoongMapWidget({
    super.key,
    required this.apiKey,
    this.directionApiKey = 'wyhX8DQA8ZgJdYysmD3PARo1i1aE8wvv44xoZvUV',
    this.onMarkerTap,
    this.onMapTap,
    this.onView3D,
  });

  @override
  State<GoongMapWidget> createState() => _GoongMapWidgetState();
}

class _GoongMapWidgetState extends State<GoongMapWidget> {
  late final String _viewType;
  html.EventListener? _messageListener;

  @override
  void initState() {
    super.initState();
    _viewType = 'goong-map-${identityHashCode(this)}';

    // Register a platform view that creates an iframe loading goong_map.html
    ui_web.platformViewRegistry.registerViewFactory(_viewType, (int viewId) {
      final iframe = html.IFrameElement()
        ..src = 'goong_map.html?key=${widget.apiKey}&apikey=${widget.directionApiKey}'
        ..style.border = 'none'
        ..style.width = '100%'
        ..style.height = '100%';
      // Cấp đầy đủ quyền geolocation cho iframe
      iframe.setAttribute('allow', 'geolocation *; fullscreen');
      iframe.setAttribute('sandbox',
        'allow-scripts allow-same-origin allow-popups allow-forms');
      return iframe;
    });

    // Listen for postMessage events from the iframe
    _messageListener = (html.Event event) {
      if (event is html.MessageEvent) {
        try {
          final data = jsonDecode(event.data as String) as Map<String, dynamic>;
          final type = data['type'] as String?;
          switch (type) {
            case 'TAP_BUILDING':
              widget.onMarkerTap?.call();
              break;
            case 'TAP_MAP':
              widget.onMapTap?.call();
              break;
            case 'VIEW_3D':
              widget.onView3D?.call();
              break;
          }
        } catch (_) {
          // Ignore non-JSON messages
        }
      }
    };
    html.window.addEventListener('message', _messageListener!);
  }

  @override
  void dispose() {
    if (_messageListener != null) {
      html.window.removeEventListener('message', _messageListener!);
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return HtmlElementView(viewType: _viewType);
  }
}
