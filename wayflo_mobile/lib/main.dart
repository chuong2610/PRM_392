import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'app.dart';
import 'injection_container.dart' as di;
import 'features/auth/presentation/providers/auth_provider.dart';
import 'features/map/presentation/providers/map_provider.dart';
import 'features/map/presentation/providers/floor_provider.dart';
import 'features/navigation/presentation/providers/route_provider.dart';
import 'features/search/presentation/providers/search_provider.dart';
import 'features/qr_scanner/presentation/providers/qr_provider.dart';
import 'features/building_list/presentation/providers/building_list_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Khởi tạo Dependency Injection (get_it)
  await di.init();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => di.sl<AuthProvider>()),
        ChangeNotifierProvider(create: (_) => di.sl<BuildingListProvider>()),
        ChangeNotifierProvider(create: (_) => di.sl<MapProvider>()),
        ChangeNotifierProvider(create: (_) => di.sl<FloorProvider>()),
        ChangeNotifierProvider(create: (_) => di.sl<RouteProvider>()),
        ChangeNotifierProvider(create: (_) => di.sl<SearchProvider>()),
        ChangeNotifierProvider(create: (_) => di.sl<QrProvider>()),
      ],
      child: const WayFloApp(),
    ),
  );
}

