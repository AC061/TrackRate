-- TrackRate: vistas de apoyo para valoraciones

-- Promedio y conteo de valoraciones por entidad (para mostrar en Detail).
create or replace view public.entity_rating_stats as
select
    entity_type,
    entity_id,
    round(avg(rating), 2) as average,
    count(*)::int as count
from public.ratings
group by entity_type, entity_id;

-- Valoraciones con el título/subtítulo de la entidad resuelto (para el Diario).
-- Las subconsultas resuelven el nombre aunque la entidad ya no esté aprobada.
create or replace view public.rating_details as
select
    r.id,
    r.user_id,
    r.entity_type,
    r.entity_id,
    r.rating,
    r.review,
    r.listened_at,
    r.created_at,
    r.updated_at,
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
from public.ratings r;

grant select on public.entity_rating_stats to anon, authenticated;
grant select on public.rating_details to anon, authenticated;
