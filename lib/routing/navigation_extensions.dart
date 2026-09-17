import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import 'app_routes.dart';

extension TrackRateNavigation on BuildContext {
  void goCatalogDetail(String entityType, String entityId) {
    go(AppRoutes.catalogDetail(entityType, entityId));
  }

  void pushCatalogDetail(String entityType, String entityId) {
    push(AppRoutes.catalogDetail(entityType, entityId));
  }

  void goLogin({String? redirect}) {
    if (redirect != null && redirect.isNotEmpty) {
      go('${AppRoutes.login}?redirect=${Uri.encodeComponent(redirect)}');
      return;
    }
    go(AppRoutes.login);
  }
}
