import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'app.dart';
import 'injection_container.dart' as di;
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_web/webview_flutter_web.dart';
import 'features/auth/presentation/providers/auth_provider.dart';
import 'features/map/presentation/providers/map_provider.dart';
import 'features/map/presentation/providers/floor_provider.dart';
import 'features/navigation/presentation/providers/route_provider.dart';
import 'features/search/presentation/providers/search_provider.dart';
import 'features/qr_scanner/presentation/providers/qr_provider.dart';
import 'features/building_list/presentation/providers/building_list_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  if (kIsWeb) {
    WebViewPlatform.instance = WebWebViewPlatform();
  }

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

