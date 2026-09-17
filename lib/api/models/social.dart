class UserProfile {
  const UserProfile({
    required this.id,
    required this.username,
    required this.firstName,
    required this.lastName,
    this.displayName,
    this.avatarUrl,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
    id: json['id'] as String,
    username: json['username'] as String,
    firstName: json['first_name'] as String,
    lastName: json['last_name'] as String,
    displayName: json['display_name'] as String?,
    avatarUrl: json['avatar_url'] as String?,
  );

  final String id;
  final String username;
  final String firstName;
  final String lastName;
  final String? displayName;
  final String? avatarUrl;

  String get label => displayName ?? username;
}

class AuthUser {
  const AuthUser({
    required this.id,
    required this.email,
    required this.accessToken,
    this.profile,
  });

  factory AuthUser.fromTokenResponse(Map<String, dynamic> json) {
    final token = json['access_token'] as String;
    final user = json['user'] as Map<String, dynamic>;
    final profileJson = user['profile'] as Map<String, dynamic>?;
    return AuthUser(
      id: user['id'] as String,
      email: user['email'] as String,
      accessToken: token,
      profile: profileJson != null ? UserProfile.fromJson(profileJson) : null,
    );
  }

  factory AuthUser.fromMeResponse(Map<String, dynamic> json, String accessToken) {
    final profileJson = json['profile'] as Map<String, dynamic>?;
    return AuthUser(
      id: json['id'] as String,
      email: json['email'] as String,
      accessToken: accessToken,
      profile: profileJson != null ? UserProfile.fromJson(profileJson) : null,
    );
  }

  final String id;
  final String email;
  final String accessToken;
  final UserProfile? profile;
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
