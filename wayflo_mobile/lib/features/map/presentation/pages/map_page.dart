import 'package:flutter/material.dart';

// Conditional import: Web dùng HtmlElementView, Mobile dùng WebViewWidget
import '../widgets/map_3d_stub.dart'
    if (dart.library.html) '../widgets/map_3d_web.dart';

class MapPage extends StatefulWidget {
  final String buildingId;

  const MapPage({super.key, required this.buildingId});

  @override
  State<MapPage> createState() => _MapPageState();
}

class _MapPageState extends State<MapPage> {
  String _selectedPoiName = 'Chưa chọn';
  String _activeFloor = 'Đang tải...';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Bản đồ 3D - ${widget.buildingId.substring(0, 8)}...'),
      ),
      body: Column(
        children: [
          // Khu vực hiển thị thông tin đồng bộ từ WebView
          Container(
            padding: const EdgeInsets.all(12),
            color: Colors.grey[100],
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Tầng: $_activeFloor',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                Text(
                  'Địa điểm: $_selectedPoiName',
                  style: const TextStyle(color: Colors.blueAccent, fontWeight: FontWeight.bold),
                ),
              ],
            ),
          ),
          // 3D Viewer (Web: HtmlElementView iframe, Mobile: WebViewWidget)
          Expanded(
            child: Map3DViewer(
              buildingId: widget.buildingId,
              onFloorChanged: (floorId) {
                setState(() {
                  _activeFloor = floorId;
                });
              },
              onPoiSelected: (poiData) {
                setState(() {
                  _selectedPoiName = poiData['name'] ?? 'Không rõ';
                });
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Đã chọn địa điểm: $_selectedPoiName'),
                    duration: const Duration(seconds: 2),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
