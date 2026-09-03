import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../api/models/catalog.dart';
import '../api/trackrate_client.dart';

class DetailScreen extends StatefulWidget {
  const DetailScreen({
    super.key,
    required this.client,
    required this.entityType,
    required this.entityId,
  });

  final TrackRateClient client;
  final String entityType;
  final String entityId;

  @override
  State<DetailScreen> createState() => _DetailScreenState();
}

class _DetailScreenState extends State<DetailScreen> {
  CatalogDetail? _detail;
  RatingStats? _stats;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final detail = await widget.client.getCatalogDetail(
        entityType: widget.entityType,
        entityId: widget.entityId,
      );
      final stats = await widget.client.getRatingStats(
        entityType: widget.entityType,
        entityId: widget.entityId,
      );
      setState(() {
        _detail = detail;
        _stats = stats;
        _loading = false;
      });
    } on TrackRateException catch (e) {
      setState(() {
        _error = e.message;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_detail?.title ?? 'Detalle')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(child: Text(_error!));
    }
    final detail = _detail!;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (detail.imageUrl != null)
            Center(
              child: CachedNetworkImage(
                imageUrl: detail.imageUrl!,
                height: 200,
                fit: BoxFit.contain,
              ),
            ),
          const SizedBox(height: 16),
          Text(detail.title, style: Theme.of(context).textTheme.headlineSmall),
          if (detail.subtitle != null) Text(detail.subtitle!),
          if (detail.year != null) Text('Año: ${detail.year}'),
          if (detail.durationMs != null)
            Text('Duración: ${(detail.durationMs! / 1000).round()} s'),
          const SizedBox(height: 8),
          Text('Tipo: ${detail.type}'),
          Text('MBID: ${detail.id}', style: Theme.of(context).textTheme.bodySmall),
          if (_stats != null) ...[
            const SizedBox(height: 16),
            Text(
              'Valoración media: ${_stats!.average} (${_stats!.count} votos)',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ],
        ],
      ),
    );
  }
}
