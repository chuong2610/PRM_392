import 'package:flutter/material.dart';

class RouteProvider extends ChangeNotifier {
  bool _isRouting = false;
  bool get isRouting => _isRouting;

  Future<void> calculateRoute(String originId, String destinationId) async {
    _isRouting = true;
    notifyListeners();
    await Future.delayed(const Duration(milliseconds: 500));
    _isRouting = false;
    notifyListeners();
  }
}
