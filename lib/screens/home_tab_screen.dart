import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/models/catalog.dart';
import '../api/trackrate_client.dart';
import '../providers/trackrate_providers.dart';
import '../routing/navigation_extensions.dart';

class HomeTabScreen extends ConsumerStatefulWidget {
  const HomeTabScreen({super.key});

  @override
  ConsumerState<HomeTabScreen> createState() => _HomeTabScreenState();
}

class _HomeTabScreenState extends ConsumerState<HomeTabScreen> {
  List<TopRatedEntity> _topRated = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadTopRated();
  }

  Future<void> _loadTopRated() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final items = await ref.read(trackRateClientProvider).getTopRated(type: 'track');
      if (!mounted) {
        return;
      }
      setState(() {
        _topRated = items;
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
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            FilledButton(onPressed: _loadTopRated, child: const Text('Reintentar')),
          ],
        ),
      );
    }
    if (_topRated.isEmpty) {
      return const Center(child: Text('Sin valoraciones aún. Busca música para empezar.'));
    }
    return ListView.builder(
      itemCount: _topRated.length,
      itemBuilder: (context, index) {
        final item = _topRated[index];
        return ListTile(
          leading: item.imageUrl != null
              ? CachedNetworkImage(
                  imageUrl: item.imageUrl!,
                  width: 48,
                  height: 48,
                  fit: BoxFit.cover,
                )
              : const Icon(Icons.music_note),
          title: Text(item.title),
          subtitle: Text(
            '${item.subtitle ?? item.type} · ★ ${item.averageRating} (${item.ratingCount})',
          ),
          onTap: () => context.pushCatalogDetail(item.type, item.id),
        );
      },
    );
  }
}
