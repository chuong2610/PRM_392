import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb, defaultTargetPlatform;
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

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
  late final WebViewController _controller;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _initWebView();
  }

  void _initWebView() {
    // Cấu hình URL thích hợp cho môi trường Dev hoặc Production
    // Android Emulator sử dụng 10.0.2.2 để truy cập localhost máy chủ
    final isAndroid = !kIsWeb && defaultTargetPlatform == TargetPlatform.android;
    final host = isAndroid ? '10.0.2.2' : 'localhost';
    
    // Hỗ trợ truyền cấu hình qua --dart-define
    const prodUrl = String.fromEnvironment('WAYFLO_WEB_URL');
    const prodBackendUrl = String.fromEnvironment('WAYFLO_BACKEND_URL');

    String webUrl;
    if (prodUrl.isNotEmpty) {
      webUrl = '$prodUrl/?buildingId=${widget.buildingId}';
      if (prodBackendUrl.isNotEmpty) {
        webUrl += '&baseUrl=$prodBackendUrl';
      }
    } else {
      webUrl = 'http://$host:5173/?buildingId=${widget.buildingId}&baseUrl=http://$host:8080';
    }

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0x00000000))
      ..setNavigationDelegate(
        NavigationDelegate(
          onProgress: (int progress) {
            debugPrint('WebView loading progress: $progress%');
          },
          onPageStarted: (String url) {
            setState(() {
              _isLoading = true;
            });
          },
          onPageFinished: (String url) {
            setState(() {
              _isLoading = false;
            });
            debugPrint('WebView finished loading: $url');
          },
          onWebResourceError: (WebResourceError error) {
            debugPrint('WebView resource error: ${error.description}');
          },
        ),
      )
      // JS Channel (Đồng bộ tương tác từ Three.js Web -> App Flutter Native)
      ..addJavaScriptChannel(
        'WayFloChannel',
        onMessageReceived: (JavaScriptMessage message) {
          try {
            final data = jsonDecode(message.message) as Map<String, dynamic>;
            final type = data['type'];

            switch (type) {
              case 'POI_SELECTED':
                if (widget.onPoiSelected != null) {
                  widget.onPoiSelected!(data['payload']);
                }
                break;
              case 'FLOOR_CHANGED':
                if (widget.onFloorChanged != null) {
                  widget.onFloorChanged!(data['payload']);
                }
                break;
              default:
                debugPrint('Unknown event type from WebView: $type');
            }
          } catch (e) {
            debugPrint('Error decoding JSON from WebView JS Channel: $e');
          }
        },
      )
      ..loadRequest(Uri.parse(webUrl));
  }

  // Hàm hỗ trợ Flutter gửi lệnh xuống WebView (ví dụ: vẽ đường đi, đổi tầng)
  Future<void> sendCommand(String type, Map<String, dynamic> payload) async {
    final jsonStr = jsonEncode({'type': type, 'payload': payload});
    await _controller.runJavaScript('if (window.handleFlutterMessage) { window.handleFlutterMessage($jsonStr); }');
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        WebViewWidget(controller: _controller),
        if (_isLoading)
          const Center(
            child: CircularProgressIndicator(),
          ),
      ],
    );
  }
}
