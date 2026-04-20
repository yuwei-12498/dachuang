import 'package:flutter_dotenv/flutter_dotenv.dart';

enum AppFlavor {
  dev,
  prod,
}

abstract final class AppEnv {
  static late final AppFlavor flavor;
  static late final String baseUrl;
  static late final Duration connectTimeout;
  static late final Duration sendTimeout;
  static late final Duration receiveTimeout;

  static bool _initialized = false;

  static Future<void> load() async {
    if (_initialized) {
      return;
    }

    final rawFlavor = const String.fromEnvironment(
      'APP_ENV',
      defaultValue: 'dev',
    );

    flavor = AppFlavor.values.firstWhere(
      (item) => item.name == rawFlavor,
      orElse: () => AppFlavor.dev,
    );

    await dotenv.load(fileName: '.env.${flavor.name}');

    baseUrl = dotenv.get('API_BASE_URL');
    connectTimeout = _readDuration(
      key: 'CONNECT_TIMEOUT_MS',
      fallback: const Duration(seconds: 10),
    );
    sendTimeout = _readDuration(
      key: 'SEND_TIMEOUT_MS',
      fallback: const Duration(seconds: 10),
    );
    receiveTimeout = _readDuration(
      key: 'RECEIVE_TIMEOUT_MS',
      fallback: const Duration(seconds: 15),
    );

    _initialized = true;
  }

  static bool get isDevelopment => flavor == AppFlavor.dev;
  static bool get isProduction => flavor == AppFlavor.prod;

  static Duration _readDuration({
    required String key,
    required Duration fallback,
  }) {
    final rawValue = dotenv.maybeGet(key);
    final value = int.tryParse(rawValue ?? '');
    if (value == null) {
      return fallback;
    }

    return Duration(milliseconds: value);
  }
}
