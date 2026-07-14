// ignore_for_file: avoid_web_libraries_in_flutter
import 'dart:convert';
import 'dart:html' as html;
import 'dart:ui_web' as ui_web;
import 'package:flutter/material.dart';

/// Web implementation: Renders the 3D Three.js indoor map via an iframe
/// pointing to the React FE dev server (localhost:5173).
/// Uses HtmlElementView instead of webview_flutter to avoid iframe rendering issues.
class Map3DViewer extends StatefulWidget {
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
  State<Map3DViewer> createState() => _Map3DViewerState();
}

class _Map3DViewerState extends State<Map3DViewer> {
  late final String _viewType;
  html.EventListener? _messageListener;

  @override
  void initState() {
    super.initState();
    _viewType = 'map-3d-${identityHashCode(this)}';

    // Xây dựng URL cho React FE 3D viewer
    // Hỗ trợ cả dev (localhost) và production (WAYFLO_WEB_URL)
    const prodUrl = String.fromEnvironment('WAYFLO_WEB_URL');
    const prodBackendUrl = String.fromEnvironment('WAYFLO_BACKEND_URL');

    String webUrl;
    if (prodUrl.isNotEmpty) {
      webUrl = '$prodUrl/?buildingId=${widget.buildingId}';
      if (prodBackendUrl.isNotEmpty) {
        webUrl += '&baseUrl=$prodBackendUrl';
      }
    } else {
      webUrl = 'http://localhost:5174/?buildingId=${widget.buildingId}&baseUrl=http://localhost:8080';
    }

    // Register platform view with iframe
    ui_web.platformViewRegistry.registerViewFactory(_viewType, (int viewId) {
      return html.IFrameElement()
        ..src = webUrl
        ..style.border = 'none'
        ..style.width = '100%'
        ..style.height = '100%';
    });

    // Listen for messages from the React FE iframe
    _messageListener = (html.Event event) {
      if (event is html.MessageEvent) {
        try {
          final data = jsonDecode(event.data as String) as Map<String, dynamic>;
          final type = data['type'] as String?;

          switch (type) {
            case 'POI_SELECTED':
              widget.onPoiSelected?.call(
                (data['payload'] as Map<String, dynamic>?) ?? {},
              );
              break;
            case 'FLOOR_CHANGED':
              widget.onFloorChanged?.call(
                (data['payload'] as String?) ?? '',
              );
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
