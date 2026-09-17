import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/trackrate_providers.dart';

/// Notifica a GoRouter cuando cambia la sesión (login/logout/restore).
class AuthRefreshListenable extends ChangeNotifier {
  AuthRefreshListenable(this._ref) {
    _ref.listen(authSessionProvider, (_, __) => notifyListeners());
  }

  final Ref _ref;
}

final authRefreshListenableProvider = Provider<AuthRefreshListenable>((ref) {
  final listenable = AuthRefreshListenable(ref);
  ref.onDispose(listenable.dispose);
  return listenable;
});
