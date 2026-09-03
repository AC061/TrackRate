class AuthUser {
  const AuthUser({
    required this.id,
    required this.email,
    required this.accessToken,
  });

  final String id;
  final String email;
  final String accessToken;
}

class RatingDetail {
  const RatingDetail({
    required this.id,
    required this.entityType,
    required this.entityId,
    required this.rating,
    this.review,
    this.entityTitle,
    this.entitySubtitle,
  });

  factory RatingDetail.fromJson(Map<String, dynamic> json) => RatingDetail(
    id: json['id'] as String,
    entityType: json['entity_type'] as String,
    entityId: json['entity_id'] as String,
    rating: (json['rating'] as num).toDouble(),
    review: json['review'] as String?,
    entityTitle: json['entity_title'] as String?,
    entitySubtitle: json['entity_subtitle'] as String?,
  );

  final String id;
  final String entityType;
  final String entityId;
  final double rating;
  final String? review;
  final String? entityTitle;
  final String? entitySubtitle;
}

class FeedItem {
  const FeedItem({
    required this.id,
    required this.username,
    required this.entityType,
    required this.entityId,
    required this.rating,
    this.displayName,
    this.entityTitle,
    this.entitySubtitle,
    this.review,
  });

  factory FeedItem.fromJson(Map<String, dynamic> json) => FeedItem(
    id: json['id'] as String,
    username: json['username'] as String,
    displayName: json['display_name'] as String?,
    entityType: json['entity_type'] as String,
    entityId: json['entity_id'] as String,
    rating: (json['rating'] as num).toDouble(),
    review: json['review'] as String?,
    entityTitle: json['entity_title'] as String?,
    entitySubtitle: json['entity_subtitle'] as String?,
  );

  final String id;
  final String username;
  final String? displayName;
  final String entityType;
  final String entityId;
  final double rating;
  final String? review;
  final String? entityTitle;
  final String? entitySubtitle;
}
