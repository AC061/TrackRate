-- TrackRate: vistas para estadísticas de usuario y detalle de listas

create or replace view public.user_rating_stats as
select
    user_id,
    count(*)::int as total_ratings,
    round(avg(rating), 2) as average_rating,
    count(*) filter (
        where review is not null and length(trim(review)) > 0
    )::int as review_count
from public.ratings
group by user_id;

create or replace view public.list_item_details as
select
    li.list_id,
    li.entity_type,
    li.entity_id,
    li.position,
    l.title as list_title,
    l.user_id as list_owner_id,
    case li.entity_type
        when 'artist' then (select ar.name from public.artists ar where ar.id = li.entity_id)
        when 'album' then (select al.title from public.albums al where al.id = li.entity_id)
        when 'track' then (select tr.title from public.tracks tr where tr.id = li.entity_id)
    end as entity_title,
    case li.entity_type
        when 'album' then (
            select ar.name
            from public.albums al
            join public.artists ar on ar.id = al.artist_id
            where al.id = li.entity_id
        )
        when 'track' then (
            select ar.name
            from public.tracks tr
            join public.artists ar on ar.id = tr.artist_id
            where tr.id = li.entity_id
        )
        else null
    end as entity_subtitle
from public.list_items li
join public.lists l on l.id = li.list_id;

grant select on public.user_rating_stats to anon, authenticated;
grant select on public.list_item_details to authenticated;
