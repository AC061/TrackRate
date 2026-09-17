import 'package:shared_preferences/shared_preferences.dart';

import '../api/models/social.dart';

/// Persistencia local del token JWT y datos mínimos de sesión.
class AuthStorage {
  AuthStorage(this._prefs);

  static const _tokenKey = 'trackrate_access_token';
  static const _userIdKey = 'trackrate_user_id';
  static const _emailKey = 'trackrate_user_email';

  final SharedPreferences _prefs;

  static Future<AuthStorage> create() async {
    return AuthStorage(await SharedPreferences.getInstance());
  }

  Future<void> save(AuthUser user) async {
    await _prefs.setString(_tokenKey, user.accessToken);
    await _prefs.setString(_userIdKey, user.id);
    await _prefs.setString(_emailKey, user.email);
  }

  Future<AuthUser?> read() async {
    final token = _prefs.getString(_tokenKey);
    final id = _prefs.getString(_userIdKey);
    final email = _prefs.getString(_emailKey);
    if (token == null || id == null || email == null) {
      return null;
    }
    return AuthUser(id: id, email: email, accessToken: token);
  }

  Future<void> clear() async {
    await _prefs.remove(_tokenKey);
    await _prefs.remove(_userIdKey);
    await _prefs.remove(_emailKey);
  }
}
