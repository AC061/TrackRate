-- TrackRate: Row Level Security

alter table public.profiles enable row level security;
alter table public.artists enable row level security;
alter table public.albums enable row level security;
alter table public.tracks enable row level security;
alter table public.ratings enable row level security;
alter table public.follows enable row level security;
alter table public.activities enable row level security;
alter table public.lists enable row level security;
alter table public.list_items enable row level security;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------
create policy "profiles_select_public"
    on public.profiles for select
    using (true);

create policy "profiles_insert_own"
    on public.profiles for insert
    with check (auth.uid() = id);

create policy "profiles_update_own"
    on public.profiles for update
    using (auth.uid() = id)
    with check (
        auth.uid() = id
        and is_admin = (select p.is_admin from public.profiles p where p.id = auth.uid())
    );

-- ---------------------------------------------------------------------------
-- Catálogo: políticas compartidas (artists, albums, tracks)
-- ---------------------------------------------------------------------------
create policy "artists_select_approved_or_own_or_admin"
    on public.artists for select
    using (
        status = 'approved'
        or submitted_by = auth.uid()
        or public.is_admin()
    );

create policy "artists_insert_authenticated"
    on public.artists for insert
    to authenticated
    with check (
        auth.uid() is not null
        and submitted_by = auth.uid()
        and status = 'pending'
    );

create policy "artists_update_submitter_pending"
    on public.artists for update
    to authenticated
    using (
        submitted_by = auth.uid()
        and status = 'pending'
    )
    with check (
        submitted_by = auth.uid()
        and status = 'pending'
    );

create policy "artists_update_admin_moderation"
    on public.artists for update
    to authenticated
    using (public.is_admin())
    with check (public.is_admin());

create policy "albums_select_approved_or_own_or_admin"
    on public.albums for select
    using (
        status = 'approved'
        or submitted_by = auth.uid()
        or public.is_admin()
    );

create policy "albums_insert_authenticated"
    on public.albums for insert
    to authenticated
    with check (
        auth.uid() is not null
        and submitted_by = auth.uid()
        and status = 'pending'
    );

create policy "albums_update_submitter_pending"
    on public.albums for update
    to authenticated
    using (
        submitted_by = auth.uid()
        and status = 'pending'
    )
    with check (
        submitted_by = auth.uid()
        and status = 'pending'
    );

create policy "albums_update_admin_moderation"
    on public.albums for update
    to authenticated
    using (public.is_admin())
    with check (public.is_admin());

create policy "tracks_select_approved_or_own_or_admin"
    on public.tracks for select
    using (
        status = 'approved'
        or submitted_by = auth.uid()
        or public.is_admin()
    );

create policy "tracks_insert_authenticated"
    on public.tracks for insert
    to authenticated
    with check (
        auth.uid() is not null
        and submitted_by = auth.uid()
        and status = 'pending'
    );

create policy "tracks_update_submitter_pending"
    on public.tracks for update
    to authenticated
    using (
        submitted_by = auth.uid()
        and status = 'pending'
    )
    with check (
        submitted_by = auth.uid()
        and status = 'pending'
    );

create policy "tracks_update_admin_moderation"
    on public.tracks for update
    to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- ---------------------------------------------------------------------------
-- ratings
-- ---------------------------------------------------------------------------
create policy "ratings_select_public"
    on public.ratings for select
    using (true);

create policy "ratings_insert_own"
    on public.ratings for insert
    to authenticated
    with check (
        auth.uid() = user_id
        and public.catalog_entity_is_approved(entity_type, entity_id)
    );

create policy "ratings_update_own"
    on public.ratings for update
    to authenticated
    using (auth.uid() = user_id)
    with check (
        auth.uid() = user_id
        and public.catalog_entity_is_approved(entity_type, entity_id)
    );

create policy "ratings_delete_own"
    on public.ratings for delete
    to authenticated
    using (auth.uid() = user_id);

-- ---------------------------------------------------------------------------
-- follows
-- ---------------------------------------------------------------------------
create policy "follows_select_public"
    on public.follows for select
    using (true);

create policy "follows_insert_own"
    on public.follows for insert
    to authenticated
    with check (auth.uid() = follower_id);

create policy "follows_delete_own"
    on public.follows for delete
    to authenticated
    using (auth.uid() = follower_id);

-- ---------------------------------------------------------------------------
-- activities
-- ---------------------------------------------------------------------------
create policy "activities_select_authenticated"
    on public.activities for select
    to authenticated
    using (true);

create policy "lists_select_public_or_own"
    on public.lists for select
    using (is_public or auth.uid() = user_id);

create policy "lists_insert_own"
    on public.lists for insert
    to authenticated
    with check (auth.uid() = user_id);

create policy "lists_update_own"
    on public.lists for update
    to authenticated
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "lists_delete_own"
    on public.lists for delete
    to authenticated
    using (auth.uid() = user_id);

create policy "list_items_select_via_list"
    on public.list_items for select
    using (
        exists (
            select 1
            from public.lists l
            where l.id = list_items.list_id
              and (l.is_public or l.user_id = auth.uid())
        )
    );

create policy "list_items_insert_own_list"
    on public.list_items for insert
    to authenticated
    with check (
        exists (
            select 1
            from public.lists l
            where l.id = list_items.list_id
              and l.user_id = auth.uid()
        )
        and public.catalog_entity_is_approved(entity_type, entity_id)
    );

create policy "list_items_update_own_list"
    on public.list_items for update
    to authenticated
    using (
        exists (
            select 1
            from public.lists l
            where l.id = list_items.list_id
              and l.user_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from public.lists l
            where l.id = list_items.list_id
              and l.user_id = auth.uid()
        )
        and public.catalog_entity_is_approved(entity_type, entity_id)
    );

create policy "list_items_delete_own_list"
    on public.list_items for delete
    to authenticated
    using (
        exists (
            select 1
            from public.lists l
            where l.id = list_items.list_id
              and l.user_id = auth.uid()
        )
    );
