import 'package:flutter/material.dart';
import 'package:forui/forui.dart';

import 'api/trackrate_client.dart';
import 'screens/home_screen.dart';
import 'theme/theme.dart';

void main() {
  runApp(const Application());
}

class Application extends StatelessWidget {
  const Application({super.key});

  @override
  Widget build(BuildContext context) {
    final client = TrackRateClient();

    return MaterialApp(
      title: 'TrackRate',
      supportedLocales: FLocalizations.supportedLocales,
      localizationsDelegates: const [...FLocalizations.localizationsDelegates],
      theme: lightTheme.toApproximateMaterialTheme(),
      darkTheme: darkTheme.toApproximateMaterialTheme(),
      builder: (context, child) => FTheme(
        data: Theme.brightnessOf(context) == .light ? lightTheme : darkTheme,
        child: FToaster(child: FTooltipGroup(child: child!)),
      ),
      home: HomeScreen(client: client),
    );
  }
}
