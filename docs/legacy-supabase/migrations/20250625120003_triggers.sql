-- TrackRate: triggers (auth, moderación, ratings, timestamps)

-- ---------------------------------------------------------------------------
-- updated_at automático
-- ---------------------------------------------------------------------------
create trigger profiles_set_updated_at
    before update on public.profiles
    for each row execute function public.set_updated_at();

create trigger artists_set_updated_at
    before update on public.artists
    for each row execute function public.set_updated_at();

create trigger albums_set_updated_at
    before update on public.albums
    for each row execute function public.set_updated_at();

create trigger tracks_set_updated_at
    before update on public.tracks
    for each row execute function public.set_updated_at();

create trigger ratings_set_updated_at
    before update on public.ratings
    for each row execute function public.set_updated_at();

create trigger lists_set_updated_at
    before update on public.lists
    for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Perfil al registrarse
-- ---------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    base_username text;
    final_username text;
    suffix int := 0;
begin
    base_username := split_part(coalesce(new.email, new.id::text), '@', 1);
    base_username := regexp_replace(lower(base_username), '[^a-z0-9_]', '_', 'g');
    base_username := left(base_username, 24);

    if base_username = '' or length(base_username) < 3 then
        base_username := 'user';
    end if;

    final_username := base_username;

    while exists (select 1 from public.profiles where username = final_username) loop
        suffix := suffix + 1;
        final_username := left(base_username, 24 - length(suffix::text)) || suffix::text;
    end loop;

    insert into public.profiles (id, username, display_name)
    values (
        new.id,
        final_username,
        coalesce(new.raw_user_meta_data ->> 'full_name', split_part(coalesce(new.email, final_username), '@', 1))
    );

    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- Moderación: forzar pending en INSERT de catálogo
-- ---------------------------------------------------------------------------
create or replace function public.enforce_catalog_pending_on_insert()
returns trigger
language plpgsql
as $$
begin
    new.status := 'pending';
    new.submitted_by := auth.uid();
    new.reviewed_by := null;
    new.reviewed_at := null;
    new.rejection_reason := null;
    return new;
end;
$$;

create trigger artists_enforce_pending_on_insert
    before insert on public.artists
    for each row execute function public.enforce_catalog_pending_on_insert();

create trigger albums_enforce_pending_on_insert
    before insert on public.albums
    for each row execute function public.enforce_catalog_pending_on_insert();

create trigger tracks_enforce_pending_on_insert
    before insert on public.tracks
    for each row execute function public.enforce_catalog_pending_on_insert();

-- ---------------------------------------------------------------------------
-- Moderación: registrar revisión admin al cambiar status
-- ---------------------------------------------------------------------------
create or replace function public.enforce_admin_moderation_update()
returns trigger
language plpgsql
as $$
begin
    if old.status is distinct from new.status then
        if not public.is_admin() then
            raise exception 'Only admins can change moderation status';
        end if;

        if new.status = 'pending' then
            raise exception 'Cannot revert catalog entry to pending';
        end if;

        new.reviewed_by := auth.uid();
        new.reviewed_at := now();

        if new.status = 'approved' then
            new.rejection_reason := null;
        elsif new.status = 'rejected' and (new.rejection_reason is null or length(trim(new.rejection_reason)) = 0) then
            raise exception 'Rejection reason is required';
        end if;
    end if;

    return new;
end;
$$;

create trigger artists_enforce_admin_moderation
    before update on public.artists
    for each row execute function public.enforce_admin_moderation_update();

create trigger albums_enforce_admin_moderation
    before update on public.albums
    for each row execute function public.enforce_admin_moderation_update();

create trigger tracks_enforce_admin_moderation
    before update on public.tracks
    for each row execute function public.enforce_admin_moderation_update();

-- ---------------------------------------------------------------------------
-- Ratings: validar entidad aprobada (defensa en profundidad junto a RLS)
-- ---------------------------------------------------------------------------
create or replace function public.validate_rating_entity()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.catalog_entity_is_approved(new.entity_type, new.entity_id) then
        raise exception 'Can only rate approved catalog entities';
    end if;
    return new;
end;
$$;

create trigger ratings_validate_entity
    before insert or update on public.ratings
    for each row execute function public.validate_rating_entity();

-- ---------------------------------------------------------------------------
-- Actividad social al valorar
-- ---------------------------------------------------------------------------
create or replace function public.handle_rating_activity()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    activity public.activity_type;
begin
    if tg_op = 'INSERT' then
        if new.review is not null and length(trim(new.review)) > 0 then
            activity := 'reviewed';
        else
            activity := 'rated';
        end if;

        insert into public.activities (user_id, rating_id, activity_type)
        values (new.user_id, new.id, activity);
    elsif tg_op = 'UPDATE' then
        insert into public.activities (user_id, rating_id, activity_type)
        values (new.user_id, new.id, 'updated');
    end if;

    return new;
end;
$$;

create trigger ratings_create_activity
    after insert or update on public.ratings
    for each row execute function public.handle_rating_activity();
