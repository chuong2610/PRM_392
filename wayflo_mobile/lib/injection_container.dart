import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:dio/dio.dart';
import 'features/auth/presentation/providers/auth_provider.dart';
import 'features/map/presentation/providers/map_provider.dart';
import 'features/map/presentation/providers/floor_provider.dart';
import 'features/navigation/presentation/providers/route_provider.dart';
import 'features/search/presentation/providers/search_provider.dart';
import 'features/qr_scanner/presentation/providers/qr_provider.dart';
import 'features/building_list/presentation/providers/building_list_provider.dart';

final sl = GetIt.instance;

Future<void> init() async {
  // External
  final sharedPreferences = await SharedPreferences.getInstance();
  sl.registerLazySingleton(() => sharedPreferences);
  sl.registerLazySingleton(() => Dio());

  // Providers
  sl.registerFactory(() => AuthProvider());
  sl.registerFactory(() => BuildingListProvider());
  sl.registerFactory(() => MapProvider());
  sl.registerFactory(() => FloorProvider());
  sl.registerFactory(() => RouteProvider());
  sl.registerFactory(() => SearchProvider());
  sl.registerFactory(() => QrProvider());
}
