-- TrackRate: vistas de catálogo aprobado y funciones helper

-- ---------------------------------------------------------------------------
-- Vistas (solo entidades aprobadas; usar en Search / Detail público)
-- ---------------------------------------------------------------------------
create or replace view public.approved_artists as
select
    id,
    name,
    bio,
    image_url,
    created_at,
    updated_at
from public.artists
where status = 'approved';

create or replace view public.approved_albums as
select
    a.id,
    a.title,
    a.artist_id,
    ar.name as artist_name,
    a.release_year,
    a.cover_url,
    a.created_at,
    a.updated_at
from public.albums a
join public.artists ar on ar.id = a.artist_id and ar.status = 'approved'
where a.status = 'approved';

create or replace view public.approved_tracks as
select
    t.id,
    t.title,
    t.album_id,
    al.title as album_title,
    t.artist_id,
    ar.name as artist_name,
    t.duration_ms,
    t.cover_url,
    t.created_at,
    t.updated_at
from public.tracks t
join public.artists ar on ar.id = t.artist_id and ar.status = 'approved'
left join public.albums al on al.id = t.album_id and al.status = 'approved'
where t.status = 'approved';

-- ---------------------------------------------------------------------------
-- Helpers
-- ---------------------------------------------------------------------------
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce(
        (select p.is_admin from public.profiles p where p.id = auth.uid()),
        false
    );
$$;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create or replace function public.catalog_entity_is_approved(
    p_entity_type public.music_entity_type,
    p_entity_id uuid
)
returns boolean
language plpgsql
stable
security definer
set search_path = public
as $$
declare
    entity_status public.moderation_status;
begin
    if p_entity_type = 'artist' then
        select status into entity_status from public.artists where id = p_entity_id;
    elsif p_entity_type = 'album' then
        select status into entity_status from public.albums where id = p_entity_id;
    elsif p_entity_type = 'track' then
        select status into entity_status from public.tracks where id = p_entity_id;
    else
        return false;
    end if;

    return entity_status = 'approved';
end;
$$;

grant select on public.approved_artists to anon, authenticated;
grant select on public.approved_albums to anon, authenticated;
grant select on public.approved_tracks to anon, authenticated;
