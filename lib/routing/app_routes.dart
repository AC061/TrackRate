/// Rutas nombradas de la app. Usar siempre estas constantes en lugar de strings sueltos.
abstract final class AppRoutes {
  static const root = '/';
  static const home = '/home';
  static const search = '/search';
  static const feed = '/feed';
  static const me = '/me';
  static const diary = '/me/diary';
  static const login = '/login';

  static const catalogTypes = {'artist', 'album', 'track'};

  static String catalogDetail(String entityType, String entityId) {
    assert(catalogTypes.contains(entityType), 'entityType inválido: $entityType');
    return '/$entityType/$entityId';
  }

  static bool requiresAuth(String location) {
    return location == feed ||
        location.startsWith('/me');
  }

  static bool isLogin(String location) => location == login;
}
