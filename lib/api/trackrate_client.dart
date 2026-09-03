import 'package:dio/dio.dart';

import '../config/api_config.dart';
import 'models/catalog.dart';
import 'models/social.dart';

class TrackRateException implements Exception {
  TrackRateException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

class TrackRateClient {
  TrackRateClient({Dio? dio, String? baseUrl, String? accessToken})
    : _dio =
          dio ??
          Dio(
            BaseOptions(
              baseUrl: baseUrl ?? ApiConfig.baseUrl,
              connectTimeout: const Duration(seconds: 15),
              receiveTimeout: const Duration(seconds: 15),
              headers: {'Accept': 'application/json'},
            ),
          ) {
    if (accessToken != null) {
      setAccessToken(accessToken);
    }
  }

  final Dio _dio;

  void setAccessToken(String? token) {
    if (token == null || token.isEmpty) {
      _dio.options.headers.remove('Authorization');
    } else {
      _dio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  Future<List<CatalogItem>> searchCatalog({
    required String query,
    String? type,
  }) async {
    final response = await _get<List<dynamic>>(
      '/catalog/search',
      queryParameters: {
        'q': query,
        if (type != null) 'type': type,
      },
    );
    return response.map((e) => CatalogItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<CatalogDetail> getCatalogDetail({
    required String entityType,
    required String entityId,
  }) async {
    final response = await _get<Map<String, dynamic>>(
      '/catalog/$entityType/$entityId',
    );
    return CatalogDetail.fromJson(response);
  }

  Future<String?> getCoverUrl({
    required String entityType,
    required String entityId,
  }) async {
    final response = await _get<Map<String, dynamic>>(
      '/catalog/$entityType/$entityId/cover',
    );
    return response['url'] as String?;
  }

  Future<List<TopRatedEntity>> getTopRated({
    String type = 'track',
    int limit = 20,
  }) async {
    final response = await _get<List<dynamic>>(
      '/entities/top-rated',
      queryParameters: {'type': type, 'limit': limit},
    );
    return response.map((e) => TopRatedEntity.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<RatingStats?> getRatingStats({
    required String entityType,
    required String entityId,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/entities/$entityType/$entityId/rating-stats',
    );
    if (response.statusCode == 200 && response.data != null) {
      return RatingStats.fromJson(response.data!);
    }
    return null;
  }

  Future<AuthUser> login({required String email, required String password}) async {
    final response = await _post<Map<String, dynamic>>(
      '/auth/login',
      data: {'email': email, 'password': password},
    );
    final token = response['access_token'] as String;
    final user = response['user'] as Map<String, dynamic>;
    setAccessToken(token);
    return AuthUser(
      id: user['id'] as String,
      email: user['email'] as String,
      accessToken: token,
    );
  }

  Future<List<FeedItem>> getFeed({int limit = 50}) async {
    final response = await _get<List<dynamic>>(
      '/feed',
      queryParameters: {'limit': limit},
    );
    return response.map((e) => FeedItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<RatingDetail>> getDiary() async {
    final response = await _get<List<dynamic>>('/me/diary');
    return response.map((e) => RatingDetail.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<void> upsertRating({
    required String entityType,
    required String entityId,
    required double rating,
    String? review,
  }) async {
    await _put<void>(
      '/me/ratings',
      data: {
        'entity_type': entityType,
        'entity_id': entityId,
        'rating': rating,
        if (review != null) 'review': review,
      },
    );
  }

  Future<T> _get<T>(String path, {Map<String, dynamic>? queryParameters}) async {
    try {
      final response = await _dio.get<T>(path, queryParameters: queryParameters);
      return response.data as T;
    } on DioException catch (e) {
      throw _wrap(e);
    }
  }

  Future<T> _post<T>(String path, {Object? data}) async {
    try {
      final response = await _dio.post<T>(path, data: data);
      return response.data as T;
    } on DioException catch (e) {
      throw _wrap(e);
    }
  }

  Future<T> _put<T>(String path, {Object? data}) async {
    try {
      final response = await _dio.put<T>(path, data: data);
      return response.data as T;
    } on DioException catch (e) {
      throw _wrap(e);
    }
  }

  TrackRateException _wrap(DioException error) {
    final status = error.response?.statusCode;
    final detail = error.response?.data;
    if (detail is Map && detail['detail'] != null) {
      return TrackRateException('${detail['detail']}', statusCode: status);
    }
    return TrackRateException(
      error.message ?? 'Error de red',
      statusCode: status,
    );
  }
}
