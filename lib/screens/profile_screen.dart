import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/trackrate_providers.dart';
import '../routing/app_routes.dart';

/// Placeholder — UI de perfil en una iteración posterior.
class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authSessionProvider).value;

    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(user?.profile?.label ?? user?.email ?? 'Perfil'),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: () => context.push(AppRoutes.diary),
            child: const Text('Mi diario'),
          ),
        ],
      ),
    );
  }
}
