import 'package:flutter/material.dart';
import 'package:forui/forui.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'providers/trackrate_providers.dart';
import 'routing/app_router.dart';
import 'services/auth_storage.dart';
import 'theme/theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final authStorage = await AuthStorage.create();

  runApp(
    ProviderScope(
      overrides: [
        authStorageProvider.overrideWithValue(authStorage),
      ],
      child: const Application(),
    ),
  );
}

class Application extends ConsumerWidget {
  const Application({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);

    return MaterialApp.router(
      title: 'TrackRate',
      supportedLocales: FLocalizations.supportedLocales,
      localizationsDelegates: const [...FLocalizations.localizationsDelegates],
      theme: lightTheme.toApproximateMaterialTheme(),
      darkTheme: darkTheme.toApproximateMaterialTheme(),
      builder: (context, child) => FTheme(
        data: Theme.brightnessOf(context) == .light ? lightTheme : darkTheme,
        child: FToaster(child: FTooltipGroup(child: child!)),
      ),
      routerConfig: router,
    );
  }
}
