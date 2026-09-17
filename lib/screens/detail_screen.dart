import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/models/catalog.dart';
import '../api/trackrate_client.dart';
import '../providers/trackrate_providers.dart';

class DetailScreen extends ConsumerStatefulWidget {
  const DetailScreen({
    super.key,
    required this.entityType,
    required this.entityId,
  });

  final String entityType;
  final String entityId;

  @override
  ConsumerState<DetailScreen> createState() => _DetailScreenState();
}

class _DetailScreenState extends ConsumerState<DetailScreen> {
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
    final client = ref.read(trackRateClientProvider);
    try {
      final detail = await client.getCatalogDetail(
        entityType: widget.entityType,
        entityId: widget.entityId,
      );
      final stats = await client.getRatingStats(
        entityType: widget.entityType,
        entityId: widget.entityId,
      );
      if (!mounted) {
        return;
      }
      setState(() {
        _detail = detail;
        _stats = stats;
        _loading = false;
      });
    } on TrackRateException catch (e) {
      if (!mounted) {
        return;
      }
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
