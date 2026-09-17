import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/models/social.dart';
import '../api/trackrate_client.dart';
import '../services/auth_storage.dart';

final authStorageProvider = Provider<AuthStorage>((ref) {
  throw UnimplementedError('AuthStorage no inicializado');
});

final trackRateClientProvider = Provider<TrackRateClient>((ref) {
  return TrackRateClient();
});

final authSessionProvider = AsyncNotifierProvider<AuthSessionNotifier, AuthUser?>(
  AuthSessionNotifier.new,
);

class AuthSessionNotifier extends AsyncNotifier<AuthUser?> {
  @override
  Future<AuthUser?> build() async {
    final storage = ref.read(authStorageProvider);
    final client = ref.read(trackRateClientProvider);
    final cached = await storage.read();
    if (cached == null) {
      return null;
    }

    client.setAccessToken(cached.accessToken);
    try {
      return await client.getMe();
    } catch (_) {
      await storage.clear();
      client.setAccessToken(null);
      return null;
    }
  }

  Future<void> login({
    required String identifier,
    required String password,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final client = ref.read(trackRateClientProvider);
      final user = await client.login(identifier: identifier, password: password);
      await ref.read(authStorageProvider).save(user);
      return user;
    });
    if (state.hasError) {
      ref.read(trackRateClientProvider).setAccessToken(null);
    }
  }

  Future<void> register({
    required String email,
    required String password,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final client = ref.read(trackRateClientProvider);
      final user = await client.register(email: email, password: password);
      await ref.read(authStorageProvider).save(user);
      return user;
    });
    if (state.hasError) {
      ref.read(trackRateClientProvider).setAccessToken(null);
    }
  }

  Future<void> logout() async {
    await ref.read(authStorageProvider).clear();
    ref.read(trackRateClientProvider).setAccessToken(null);
    state = const AsyncData(null);
  }
}

final apiHealthProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  return ref.read(trackRateClientProvider).health();
});
