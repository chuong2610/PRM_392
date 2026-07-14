import 'package:flutter/material.dart';

class LoginPage extends StatelessWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Đăng nhập Keycloak'),
      ),
      body: const Center(
        child: Text('Trang đăng nhập'),
      ),
    );
  }
}
