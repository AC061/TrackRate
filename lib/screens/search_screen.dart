import 'dart:async';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:forui/forui.dart';

import '../api/models/catalog.dart';
import '../api/trackrate_client.dart';
import '../providers/trackrate_providers.dart';
import '../routing/navigation_extensions.dart';

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key, this.initialQuery});

  final String? initialQuery;

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen> {
  final _controller = TextEditingController();
  String? _type;
  List<CatalogItem> _results = [];
  List<String> _suggestions = [];
  bool _loading = false;
  bool _loadingSuggestions = false;
  String? _error;
  Timer? _suggestDebounce;

  TrackRateClient get _client => ref.read(trackRateClientProvider);

  @override
  void initState() {
    super.initState();
    if (widget.initialQuery != null && widget.initialQuery!.isNotEmpty) {
      _controller.text = widget.initialQuery!;
    }
    _controller.addListener(_onQueryChanged);
    if (widget.initialQuery != null && widget.initialQuery!.isNotEmpty) {
      _search();
    }
  }

  @override
  void dispose() {
    _suggestDebounce?.cancel();
    _controller.removeListener(_onQueryChanged);
    _controller.dispose();
    super.dispose();
  }

  void _onQueryChanged() {
    _suggestDebounce?.cancel();
    final query = _controller.text.trim();
    if (query.length < 2) {
      setState(() => _suggestions = []);
      return;
    }
    _suggestDebounce = Timer(const Duration(milliseconds: 300), () {
      _fetchSuggestions(query);
    });
  }

  Future<void> _fetchSuggestions(String query) async {
    setState(() => _loadingSuggestions = true);
    try {
      final suggestions = await _client.suggestCatalog(
        query: query,
        type: _type,
      );
      if (!mounted || _controller.text.trim() != query) return;
      setState(() => _suggestions = suggestions);
    } on TrackRateException {
      if (mounted) setState(() => _suggestions = []);
    } finally {
      if (mounted) setState(() => _loadingSuggestions = false);
    }
  }

  void _applySuggestion(String suggestion) {
    _controller.text = suggestion;
    _controller.selection = TextSelection.collapsed(offset: suggestion.length);
    setState(() => _suggestions = []);
    _search();
  }

  Future<void> _search() async {
    final query = _controller.text.trim();
    if (query.isEmpty) return;

    setState(() {
      _loading = true;
      _error = null;
      _suggestions = [];
    });

    try {
      final results = await _client.searchCatalog(query: query, type: _type);
      setState(() => _results = results);
    } on TrackRateException catch (e) {
      setState(() => _error = e.message);
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
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
                      setState(() {
                        _type = selected ? type : null;
                        _suggestions = [];
                      });
                    },
                  ),
                ),
            ],
          ),
        ),
        if (_loadingSuggestions) const LinearProgressIndicator(minHeight: 2),
        if (_suggestions.isNotEmpty)
          Material(
            elevation: 2,
            child: ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _suggestions.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final suggestion = _suggestions[index];
                return ListTile(
                  dense: true,
                  leading: const Icon(Icons.search, size: 20),
                  title: Text(suggestion),
                  onTap: () => _applySuggestion(suggestion),
                );
              },
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
                onTap: () => context.pushCatalogDetail(item.type, item.id),
              );
            },
          ),
        ),
      ],
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
