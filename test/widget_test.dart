import 'package:flutter_test/flutter_test.dart';

import 'package:trackrate_flutter/main.dart';

void main() {
  testWidgets('Brand preview renders TrackRate logos', (WidgetTester tester) async {
    await tester.pumpWidget(const Application());
    await tester.pumpAndSettle();

    expect(find.bySemanticsLabel('TrackRate'), findsWidgets);
    expect(find.text('Icono compacto'), findsOneWidget);
    expect(find.text('Branding de toolbar'), findsOneWidget);
    expect(find.text('Detalle'), findsOneWidget);
  });
}
