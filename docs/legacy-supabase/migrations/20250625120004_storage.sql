-- TrackRate: Storage buckets y políticas

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
    (
        'avatars',
        'avatars',
        true,
        5242880,
        array['image/jpeg', 'image/png', 'image/webp']
    ),
    (
        'catalog-covers',
        'catalog-covers',
        true,
        10485760,
        array['image/jpeg', 'image/png', 'image/webp']
    ),
    (
        'artist-images',
        'artist-images',
        true,
        10485760,
        array['image/jpeg', 'image/png', 'image/webp']
    ),
    (
        'list-covers',
        'list-covers',
        true,
        10485760,
        array['image/jpeg', 'image/png', 'image/webp']
    )
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- avatars: {user_id}/avatar.*
-- ---------------------------------------------------------------------------
create policy "avatars_public_read"
    on storage.objects for select
    using (bucket_id = 'avatars');

create policy "avatars_upload_own_folder"
    on storage.objects for insert
    to authenticated
    with check (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "avatars_update_own_folder"
    on storage.objects for update
    to authenticated
    using (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "avatars_delete_own_folder"
    on storage.objects for delete
    to authenticated
    using (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- ---------------------------------------------------------------------------
-- catalog-covers: {user_id}/{entity_type}/{uuid}.*
-- ---------------------------------------------------------------------------
create policy "catalog_covers_public_read"
    on storage.objects for select
    using (bucket_id = 'catalog-covers');

create policy "catalog_covers_upload_authenticated"
    on storage.objects for insert
    to authenticated
    with check (
        bucket_id = 'catalog-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "catalog_covers_update_own"
    on storage.objects for update
    to authenticated
    using (
        bucket_id = 'catalog-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'catalog-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "catalog_covers_delete_own"
    on storage.objects for delete
    to authenticated
    using (
        bucket_id = 'catalog-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- ---------------------------------------------------------------------------
-- artist-images: {user_id}/{artist_id}.*
-- ---------------------------------------------------------------------------
create policy "artist_images_public_read"
    on storage.objects for select
    using (bucket_id = 'artist-images');

create policy "artist_images_upload_authenticated"
    on storage.objects for insert
    to authenticated
    with check (
        bucket_id = 'artist-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "artist_images_update_own"
    on storage.objects for update
    to authenticated
    using (
        bucket_id = 'artist-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'artist-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "artist_images_delete_own"
    on storage.objects for delete
    to authenticated
    using (
        bucket_id = 'artist-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- ---------------------------------------------------------------------------
-- list-covers: {user_id}/{list_id}.*
-- ---------------------------------------------------------------------------
create policy "list_covers_public_read"
    on storage.objects for select
    using (bucket_id = 'list-covers');

create policy "list_covers_upload_own"
    on storage.objects for insert
    to authenticated
    with check (
        bucket_id = 'list-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "list_covers_update_own"
    on storage.objects for update
    to authenticated
    using (
        bucket_id = 'list-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'list-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy "list_covers_delete_own"
    on storage.objects for delete
    to authenticated
    using (
        bucket_id = 'list-covers'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
