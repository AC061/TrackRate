import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../api/models/catalog.dart';
import '../api/trackrate_client.dart';
import 'detail_screen.dart';
import 'search_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.client});

  final TrackRateClient client;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tab = 0;
  List<TopRatedEntity> _topRated = [];
  bool _loadingTop = true;

  @override
  void initState() {
    super.initState();
    _loadTopRated();
  }

  Future<void> _loadTopRated() async {
    try {
      final items = await widget.client.getTopRated(type: 'track');
      setState(() {
        _topRated = items;
        _loadingTop = false;
      });
    } on TrackRateException {
      setState(() => _loadingTop = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_tab == 0 ? 'TrackRate' : 'Buscar')),
      body: IndexedStack(
        index: _tab,
        children: [
          _TopRatedTab(
            loading: _loadingTop,
            items: _topRated,
            client: widget.client,
          ),
          SearchScreen(client: widget.client, embedded: true),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (index) => setState(() => _tab = index),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home), label: 'Inicio'),
          NavigationDestination(icon: Icon(Icons.search), label: 'Buscar'),
        ],
      ),
    );
  }
}

class _TopRatedTab extends StatelessWidget {
  const _TopRatedTab({
    required this.loading,
    required this.items,
    required this.client,
  });

  final bool loading;
  final List<TopRatedEntity> items;
  final TrackRateClient client;

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (items.isEmpty) {
      return const Center(child: Text('Sin valoraciones aún. Busca música para empezar.'));
    }
    return ListView.builder(
      itemCount: items.length,
      itemBuilder: (context, index) {
        final item = items[index];
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
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (_) => DetailScreen(
                client: client,
                entityType: item.type,
                entityId: item.id,
              ),
            ),
          ),
        );
      },
    );
  }
}
