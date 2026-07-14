import 'package:flutter/material.dart';

class QrScannerPage extends StatelessWidget {
  const QrScannerPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Quét mã QR'),
      ),
      body: const Center(
        child: Text('Trang quét mã QR định vị'),
      ),
    );
  }
}
