import 'package:flutter/material.dart';

class BuildingListProvider extends ChangeNotifier {
  List<dynamic> _buildings = [];
  List<dynamic> get buildings => _buildings;

  Future<void> fetchBuildings() async {
    _buildings = [];
    notifyListeners();
  }
}
