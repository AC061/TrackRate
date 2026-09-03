class CatalogItem {
  const CatalogItem({
    required this.id,
    required this.type,
    required this.title,
    this.subtitle,
    this.imageUrl,
    this.year,
  });

  factory CatalogItem.fromJson(Map<String, dynamic> json) => CatalogItem(
    id: json['id'] as String,
    type: json['type'] as String,
    title: json['title'] as String,
    subtitle: json['subtitle'] as String?,
    imageUrl: json['image_url'] as String?,
    year: json['year'] as int?,
  );

  final String id;
  final String type;
  final String title;
  final String? subtitle;
  final String? imageUrl;
  final int? year;
}

class CatalogDetail {
  const CatalogDetail({
    required this.id,
    required this.type,
    required this.title,
    this.subtitle,
    this.extra,
    this.description,
    this.imageUrl,
    this.year,
    this.durationMs,
    this.artistId,
    this.albumId,
  });

  factory CatalogDetail.fromJson(Map<String, dynamic> json) => CatalogDetail(
    id: json['id'] as String,
    type: json['type'] as String,
    title: json['title'] as String,
    subtitle: json['subtitle'] as String?,
    extra: json['extra'] as String?,
    description: json['description'] as String?,
    imageUrl: json['image_url'] as String?,
    year: json['year'] as int?,
    durationMs: json['duration_ms'] as int?,
    artistId: json['artist_id'] as String?,
    albumId: json['album_id'] as String?,
  );

  final String id;
  final String type;
  final String title;
  final String? subtitle;
  final String? extra;
  final String? description;
  final String? imageUrl;
  final int? year;
  final int? durationMs;
  final String? artistId;
  final String? albumId;
}

class TopRatedEntity {
  const TopRatedEntity({
    required this.id,
    required this.type,
    required this.title,
    required this.averageRating,
    required this.ratingCount,
    this.subtitle,
    this.imageUrl,
  });

  factory TopRatedEntity.fromJson(Map<String, dynamic> json) => TopRatedEntity(
    id: json['id'] as String,
    type: json['type'] as String,
    title: json['title'] as String,
    subtitle: json['subtitle'] as String?,
    imageUrl: json['image_url'] as String?,
    averageRating: (json['average_rating'] as num).toDouble(),
    ratingCount: json['rating_count'] as int,
  );

  final String id;
  final String type;
  final String title;
  final String? subtitle;
  final String? imageUrl;
  final double averageRating;
  final int ratingCount;
}

class RatingStats {
  const RatingStats({required this.average, required this.count});

  factory RatingStats.fromJson(Map<String, dynamic> json) => RatingStats(
    average: (json['average'] as num).toDouble(),
    count: json['count'] as int,
  );

  final double average;
  final int count;
}
