import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:trackrate_flutter/main.dart';
import 'package:trackrate_flutter/providers/trackrate_providers.dart';
import 'package:trackrate_flutter/services/auth_storage.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('App arranca con navegación principal', (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({});
    final authStorage = AuthStorage(await SharedPreferences.getInstance());

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authStorageProvider.overrideWithValue(authStorage),
        ],
        child: const Application(),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.text('Inicio'), findsOneWidget);
    expect(find.text('Buscar'), findsOneWidget);
  });
}
