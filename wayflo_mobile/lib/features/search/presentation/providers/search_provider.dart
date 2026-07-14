import 'package:flutter/material.dart';

class SearchProvider extends ChangeNotifier {
  List<dynamic> _results = [];
  List<dynamic> get results => _results;

  Future<void> search(String query) async {
    _results = [];
    notifyListeners();
  }
}
