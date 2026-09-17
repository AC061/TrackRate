import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/trackrate_providers.dart';
import '../routing/app_routes.dart';
import '../routing/navigation_extensions.dart';

class AppShell extends ConsumerWidget {
  const AppShell({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  static const _destinations = [
    (icon: Icons.home, label: 'Inicio', route: AppRoutes.home),
    (icon: Icons.search, label: 'Buscar', route: AppRoutes.search),
    (icon: Icons.dynamic_feed, label: 'Feed', route: AppRoutes.feed),
    (icon: Icons.person, label: 'Perfil', route: AppRoutes.me),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authSessionProvider);
    final user = auth.value;
    final title = _destinations[navigationShell.currentIndex].label;

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          if (user != null)
            PopupMenuButton<String>(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: Row(
                  children: [
                    Text(user.profile?.label ?? user.email),
                    const Icon(Icons.arrow_drop_down),
                  ],
                ),
              ),
              onSelected: (value) {
                switch (value) {
                  case 'diary':
                    context.push(AppRoutes.diary);
                  case 'logout':
                    ref.read(authSessionProvider.notifier).logout();
                }
              },
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'diary', child: Text('Mi diario')),
                PopupMenuItem(value: 'logout', child: Text('Cerrar sesión')),
              ],
            )
          else
            TextButton(
              onPressed: () => context.goLogin(
                redirect: GoRouterState.of(context).uri.toString(),
              ),
              child: const Text('Entrar'),
            ),
        ],
      ),
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: (index) {
          navigationShell.goBranch(
            index,
            initialLocation: index == navigationShell.currentIndex,
          );
        },
        destinations: [
          for (final dest in _destinations)
            NavigationDestination(icon: Icon(dest.icon), label: dest.label),
        ],
      ),
    );
  }
}
