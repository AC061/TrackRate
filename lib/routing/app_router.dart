import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/trackrate_providers.dart';
import '../screens/detail_screen.dart';
import '../screens/diary_screen.dart';
import '../screens/feed_screen.dart';
import '../screens/home_tab_screen.dart';
import '../screens/login_screen.dart';
import '../screens/profile_screen.dart';
import '../screens/search_screen.dart';
import '../widgets/app_shell.dart';
import 'app_routes.dart';
import 'auth_refresh_listenable.dart';

final rootNavigatorKey = GlobalKey<NavigatorState>(debugLabel: 'root');

final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ref.watch(authRefreshListenableProvider);

  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: AppRoutes.home,
    refreshListenable: refresh,
    redirect: (context, state) => _redirect(ref, state),
    routes: [
      GoRoute(
        path: AppRoutes.login,
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => LoginScreen(
          redirectTo: state.uri.queryParameters['redirect'],
        ),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return AppShell(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.home,
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: HomeTabScreen(),
                ),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.search,
                pageBuilder: (context, state) {
                  final query = state.uri.queryParameters['q'];
                  return NoTransitionPage(
                    child: SearchScreen(initialQuery: query),
                  );
                },
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.feed,
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: FeedScreen(),
                ),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.me,
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: ProfileScreen(),
                ),
                routes: [
                  GoRoute(
                    path: 'diary',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) => const DiaryScreen(),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
      for (final type in AppRoutes.catalogTypes)
        GoRoute(
          path: '/$type/:entityId',
          parentNavigatorKey: rootNavigatorKey,
          builder: (context, state) => DetailScreen(
            entityType: type,
            entityId: state.pathParameters['entityId']!,
          ),
        ),
    ],
  );
});

String? _redirect(Ref ref, GoRouterState state) {
  final auth = ref.read(authSessionProvider);
  final location = state.matchedLocation;

  if (auth.isLoading) {
    return null;
  }

  final loggedIn = auth.value != null;

  if (location == AppRoutes.root) {
    return AppRoutes.home;
  }

  if (AppRoutes.isLogin(location)) {
    if (loggedIn) {
      final redirect = state.uri.queryParameters['redirect'];
      if (redirect != null && redirect.isNotEmpty && redirect.startsWith('/')) {
        return redirect;
      }
      return AppRoutes.home;
    }
    return null;
  }

  if (AppRoutes.requiresAuth(location) && !loggedIn) {
    return '${AppRoutes.login}?redirect=${Uri.encodeComponent(state.uri.toString())}';
  }

  return null;
}
