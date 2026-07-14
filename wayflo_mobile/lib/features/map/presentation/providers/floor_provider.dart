import 'package:flutter/material.dart';

class FloorProvider extends ChangeNotifier {
  String _activeFloorId = '';
  String get activeFloorId => _activeFloorId;

  void setActiveFloor(String floorId) {
    _activeFloorId = floorId;
    notifyListeners();
  }
}
