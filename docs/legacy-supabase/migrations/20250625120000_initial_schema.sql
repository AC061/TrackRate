drop table if exists public.list_items cascade;
drop table if exists public.lists cascade;
drop table if exists public.activities cascade;
drop table if exists public.follows cascade;
drop table if exists public.ratings cascade;
drop table if exists public.tracks cascade;
drop table if exists public.albums cascade;
drop table if exists public.artists cascade;
drop table if exists public.profiles cascade;
drop type if exists public.activity_type cascade;
drop type if exists public.music_entity_type cascade;
drop type if exists public.moderation_status cascade;


create type public.moderation_status as enum ('pending', 'approved', 'rejected');

create type public.music_entity_type as enum ('track', 'album', 'artist');

create type public.activity_type as enum ('rated', 'reviewed', 'updated');

-- ---------------------------------------------------------------------------
-- Perfiles
-- ---------------------------------------------------------------------------
create table public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    username text not null unique,
    display_name text,
    bio text,
    avatar_url text,
    is_admin boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profiles_username_format check (username ~ '^[a-z0-9_]{3,30}$')
);

create index profiles_username_idx on public.profiles (username);

-- ---------------------------------------------------------------------------
-- Catálogo musical (moderado)
-- ---------------------------------------------------------------------------
create table public.artists (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    bio text,
    image_url text,
    submitted_by uuid not null references public.profiles (id) on delete restrict,
    status public.moderation_status not null default 'pending',
    reviewed_by uuid references public.profiles (id) on delete set null,
    reviewed_at timestamptz,
    rejection_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index artists_status_idx on public.artists (status);
create index artists_name_idx on public.artists (name);
create index artists_submitted_by_idx on public.artists (submitted_by);

create table public.albums (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    artist_id uuid not null references public.artists (id) on delete restrict,
    release_year int,
    cover_url text,
    submitted_by uuid not null references public.profiles (id) on delete restrict,
    status public.moderation_status not null default 'pending',
    reviewed_by uuid references public.profiles (id) on delete set null,
    reviewed_at timestamptz,
    rejection_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint albums_release_year_range check (
        release_year is null or (release_year >= 1900 and release_year <= 2100)
    )
);

create index albums_status_idx on public.albums (status);
create index albums_title_idx on public.albums (title);
create index albums_artist_id_idx on public.albums (artist_id);
create index albums_submitted_by_idx on public.albums (submitted_by);

create table public.tracks (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    album_id uuid references public.albums (id) on delete set null,
    artist_id uuid not null references public.artists (id) on delete restrict,
    duration_ms int,
    cover_url text,
    submitted_by uuid not null references public.profiles (id) on delete restrict,
    status public.moderation_status not null default 'pending',
    reviewed_by uuid references public.profiles (id) on delete set null,
    reviewed_at timestamptz,
    rejection_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint tracks_duration_positive check (duration_ms is null or duration_ms > 0)
);

create index tracks_status_idx on public.tracks (status);
create index tracks_title_idx on public.tracks (title);
create index tracks_album_id_idx on public.tracks (album_id);
create index tracks_artist_id_idx on public.tracks (artist_id);
create index tracks_submitted_by_idx on public.tracks (submitted_by);

-- ---------------------------------------------------------------------------
-- Valoraciones
-- ---------------------------------------------------------------------------
create table public.ratings (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles (id) on delete cascade,
    entity_type public.music_entity_type not null,
    entity_id uuid not null,
    rating numeric(2, 1) not null,
    review text,
    listened_at date,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ratings_score_range check (
        rating >= 0.5 and rating <= 5.0 and mod((rating * 2)::numeric, 1) = 0
    ),
    unique (user_id, entity_type, entity_id)
);

create index ratings_user_id_idx on public.ratings (user_id);
create index ratings_entity_idx on public.ratings (entity_type, entity_id);

-- ---------------------------------------------------------------------------
-- Social
-- ---------------------------------------------------------------------------
create table public.follows (
    follower_id uuid not null references public.profiles (id) on delete cascade,
    following_id uuid not null references public.profiles (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (follower_id, following_id),
    constraint follows_no_self check (follower_id <> following_id)
);

create index follows_following_id_idx on public.follows (following_id);

create table public.activities (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles (id) on delete cascade,
    rating_id uuid not null references public.ratings (id) on delete cascade,
    activity_type public.activity_type not null,
    created_at timestamptz not null default now()
);

create index activities_user_id_created_at_idx on public.activities (user_id, created_at desc);
create index activities_created_at_idx on public.activities (created_at desc);

-- ---------------------------------------------------------------------------
-- Listas (fase 5; schema preparado)
-- ---------------------------------------------------------------------------
create table public.lists (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles (id) on delete cascade,
    title text not null,
    description text,
    is_public boolean not null default false,
    cover_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index lists_user_id_idx on public.lists (user_id);

create table public.list_items (
    list_id uuid not null references public.lists (id) on delete cascade,
    entity_type public.music_entity_type not null,
    entity_id uuid not null,
    position int not null default 0,
    created_at timestamptz not null default now(),
    primary key (list_id, entity_type, entity_id)
);

create index list_items_list_id_position_idx on public.list_items (list_id, position);
