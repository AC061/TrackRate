import 'dart:io';

/// URL base de la API TrackRate.
///
/// Prioridad:
/// 1. `--dart-define=API_BASE_URL=http://100.126.35.7:8000` (dispositivo físico / Tailscale)
/// 2. Android emulador → `10.0.2.2:8000`
/// 3. iOS sim / desktop → `localhost:8000`
abstract final class ApiConfig {
  static const _envUrl = String.fromEnvironment('API_BASE_URL');

  static String get baseUrl {
    if (_envUrl.isNotEmpty) {
      return _envUrl;
    }
    if (Platform.isAndroid) {
      return 'http://10.0.2.2:8000';
    }
    return 'http://localhost:8000';
  }
}
