import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:forui/forui.dart';

import '../api/models/catalog.dart';
import '../api/trackrate_client.dart';
import 'detail_screen.dart';

class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key, required this.client, this.embedded = false});

  final TrackRateClient client;
  final bool embedded;

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final _controller = TextEditingController();
  String? _type;
  List<CatalogItem> _results = [];
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    final query = _controller.text.trim();
    if (query.isEmpty) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final results = await widget.client.searchCatalog(query: query, type: _type);
      setState(() => _results = results);
    } on TrackRateException catch (e) {
      setState(() => _error = e.message);
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final body = Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: const InputDecoration(
                      hintText: 'Artista, álbum o canción…',
                      border: OutlineInputBorder(),
                    ),
                    onSubmitted: (_) => _search(),
                  ),
                ),
                const SizedBox(width: 8),
                FButton(onPress: _search, child: const Text('Buscar')),
              ],
            ),
          ),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                for (final type in ['artist', 'album', 'track'])
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(type),
                      selected: _type == type,
                      onSelected: (selected) {
                        setState(() => _type = selected ? type : null);
                      },
                    ),
                  ),
              ],
            ),
          ),
          if (_loading) const LinearProgressIndicator(),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ),
          Expanded(
            child: ListView.builder(
              itemCount: _results.length,
              itemBuilder: (context, index) {
                final item = _results[index];
                return ListTile(
                  leading: _CoverThumb(url: item.imageUrl),
                  title: Text(item.title),
                  subtitle: Text('${item.type}${item.subtitle != null ? ' · ${item.subtitle}' : ''}'),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) => DetailScreen(client: widget.client, entityType: item.type, entityId: item.id),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );

    if (widget.embedded) {
      return body;
    }

    return FScaffold(
      header: FHeader(title: const Text('Buscar')),
      child: body,
    );
  }
}

class _CoverThumb extends StatelessWidget {
  const _CoverThumb({this.url});

  final String? url;

  @override
  Widget build(BuildContext context) {
    if (url == null) {
      return const CircleAvatar(child: Icon(Icons.music_note));
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(4),
      child: CachedNetworkImage(
        imageUrl: url!,
        width: 48,
        height: 48,
        fit: BoxFit.cover,
        errorWidget: (_, __, ___) => const Icon(Icons.music_note),
      ),
    );
  }
}
