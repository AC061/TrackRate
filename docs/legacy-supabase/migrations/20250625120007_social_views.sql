-- TrackRate: vistas para feed social y estadísticas de perfil

-- Feed de actividades con datos de usuario, valoración y entidad resueltos.
create or replace view public.activity_feed as
select
    a.id,
    a.user_id,
    a.activity_type,
    a.created_at,
    p.username,
    p.display_name,
    p.avatar_url,
    r.id as rating_id,
    r.rating,
    r.review,
    r.entity_type,
    r.entity_id,
    r.listened_at,
    case r.entity_type
        when 'artist' then (select ar.name from public.artists ar where ar.id = r.entity_id)
        when 'album' then (select al.title from public.albums al where al.id = r.entity_id)
        when 'track' then (select tr.title from public.tracks tr where tr.id = r.entity_id)
    end as entity_title,
    case r.entity_type
        when 'album' then (
            select ar.name
            from public.albums al
            join public.artists ar on ar.id = al.artist_id
            where al.id = r.entity_id
        )
        when 'track' then (
            select ar.name
            from public.tracks tr
            join public.artists ar on ar.id = tr.artist_id
            where tr.id = r.entity_id
        )
        else null
    end as entity_subtitle
from public.activities a
join public.profiles p on p.id = a.user_id
join public.ratings r on r.id = a.rating_id;

-- Contadores de seguidores, seguidos y valoraciones por perfil.
create or replace view public.profile_stats as
select
    p.id,
    coalesce(followers.cnt, 0)::int as follower_count,
    coalesce(following.cnt, 0)::int as following_count,
    coalesce(ratings.cnt, 0)::int as rating_count
from public.profiles p
left join lateral (
    select count(*) as cnt from public.follows f where f.following_id = p.id
) followers on true
left join lateral (
    select count(*) as cnt from public.follows f where f.follower_id = p.id
) following on true
left join lateral (
    select count(*) as cnt from public.ratings r where r.user_id = p.id
) ratings on true;

grant select on public.activity_feed to authenticated;
grant select on public.profile_stats to anon, authenticated;
