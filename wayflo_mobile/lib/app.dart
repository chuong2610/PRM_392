import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'features/building_list/presentation/pages/building_list_page.dart';
import 'features/map/presentation/pages/map_page.dart';
import 'features/search/presentation/pages/search_page.dart';
import 'features/qr_scanner/presentation/pages/qr_scanner_page.dart';
import 'features/auth/presentation/pages/login_page.dart';

final GoRouter _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const BuildingListPage(),
    ),
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginPage(),
    ),
    GoRoute(
      path: '/map/:buildingId',
      builder: (context, state) {
        final buildingId = state.pathParameters['buildingId'] ?? '';
        return MapPage(buildingId: buildingId);
      },
    ),
    GoRoute(
      path: '/search',
      builder: (context, state) => const SearchPage(),
    ),
    GoRoute(
      path: '/scan',
      builder: (context, state) => const QrScannerPage(),
    ),
  ],
);

class WayFloApp extends StatelessWidget {
  const WayFloApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'WayFlo Navigation',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xff1d4ed8),
          primary: const Color(0xff1d4ed8),
        ),
      ),
      routerConfig: _router,
    );
  }
}
