-- TrackRate: datos de desarrollo (admin por defecto + catálogo de prueba)
-- SOLO para entornos de desarrollo / testing. No usar en producción.
--
-- Credenciales del administrador:
--   Email:    admin@trackrate.dev
--   Password: TrackRateAdmin123!
--   Username: admin

create extension if not exists pgcrypto with schema extensions;

do $$
declare
    admin_id uuid := 'a0000000-0000-4000-8000-000000000001';
begin
    if exists (select 1 from auth.users where email = 'admin@trackrate.dev') then
        raise notice 'Seed omitido: admin@trackrate.dev ya existe';
        return;
    end if;

    -- -----------------------------------------------------------------------
    -- Usuario administrador (Auth + perfil)
    -- -----------------------------------------------------------------------
    insert into auth.users (
        id,
        instance_id,
        aud,
        role,
        email,
        encrypted_password,
        email_confirmed_at,
        raw_app_meta_data,
        raw_user_meta_data,
        created_at,
        updated_at,
        confirmation_token,
        recovery_token,
        email_change_token_new,
        email_change
    ) values (
        admin_id,
        '00000000-0000-0000-0000-000000000000',
        'authenticated',
        'authenticated',
        'admin@trackrate.dev',
        extensions.crypt('TrackRateAdmin123!', extensions.gen_salt('bf')),
        now(),
        '{"provider":"email","providers":["email"]}'::jsonb,
        '{"full_name":"TrackRate Admin"}'::jsonb,
        now(),
        now(),
        '',
        '',
        '',
        ''
    );

    insert into auth.identities (
        id,
        user_id,
        provider_id,
        identity_data,
        provider,
        last_sign_in_at,
        created_at,
        updated_at
    ) values (
        gen_random_uuid(),
        admin_id,
        admin_id::text,
        jsonb_build_object(
            'sub', admin_id::text,
            'email', 'admin@trackrate.dev',
            'email_verified', true
        ),
        'email',
        now(),
        now(),
        now()
    );

    -- handle_new_user crea el perfil al insertar en auth.users
    update public.profiles
    set
        username = 'admin',
        display_name = 'TrackRate Admin',
        bio = 'Cuenta administrador de desarrollo para pruebas locales.',
        is_admin = true
    where id = admin_id;

    -- -----------------------------------------------------------------------
    -- Catálogo aprobado (5 artistas, 5 álbumes, 5 canciones)
    -- Desactivamos triggers de pending para insertar directamente como approved.
    -- -----------------------------------------------------------------------
    alter table public.artists disable trigger artists_enforce_pending_on_insert;
    alter table public.albums disable trigger albums_enforce_pending_on_insert;
    alter table public.tracks disable trigger tracks_enforce_pending_on_insert;

    insert into public.artists (id, name, bio, submitted_by, status, reviewed_by, reviewed_at)
    values
        (
            'a0000001-0000-4000-8000-000000000001',
            'Radiohead',
            'Banda británica de rock alternativo formada en Oxford.',
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000001-0000-4000-8000-000000000002',
            'Bjork',
            'Cantautora y productora islandesa.',
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000001-0000-4000-8000-000000000003',
            'Daft Punk',
            'Dúo francés de música electrónica.',
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000001-0000-4000-8000-000000000004',
            'Kendrick Lamar',
            'Rapero y compositor estadounidense.',
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000001-0000-4000-8000-000000000005',
            'Fleetwood Mac',
            'Banda anglo-americana de rock.',
            admin_id, 'approved', admin_id, now()
        )
    on conflict (id) do nothing;

    insert into public.albums (id, title, artist_id, release_year, submitted_by, status, reviewed_by, reviewed_at)
    values
        (
            'a0000002-0000-4000-8000-000000000001',
            'OK Computer',
            'a0000001-0000-4000-8000-000000000001',
            1997,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000002-0000-4000-8000-000000000002',
            'Vespertine',
            'a0000001-0000-4000-8000-000000000002',
            2001,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000002-0000-4000-8000-000000000003',
            'Discovery',
            'a0000001-0000-4000-8000-000000000003',
            2001,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000002-0000-4000-8000-000000000004',
            'DAMN.',
            'a0000001-0000-4000-8000-000000000004',
            2017,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000002-0000-4000-8000-000000000005',
            'Rumours',
            'a0000001-0000-4000-8000-000000000005',
            1977,
            admin_id, 'approved', admin_id, now()
        )
    on conflict (id) do nothing;

    insert into public.tracks (id, title, album_id, artist_id, duration_ms, submitted_by, status, reviewed_by, reviewed_at)
    values
        (
            'a0000003-0000-4000-8000-000000000001',
            'Paranoid Android',
            'a0000002-0000-4000-8000-000000000001',
            'a0000001-0000-4000-8000-000000000001',
            383000,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000003-0000-4000-8000-000000000002',
            'Hidden Place',
            'a0000002-0000-4000-8000-000000000002',
            'a0000001-0000-4000-8000-000000000002',
            337000,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000003-0000-4000-8000-000000000003',
            'One More Time',
            'a0000002-0000-4000-8000-000000000003',
            'a0000001-0000-4000-8000-000000000003',
            320000,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000003-0000-4000-8000-000000000004',
            'HUMBLE.',
            'a0000002-0000-4000-8000-000000000004',
            'a0000001-0000-4000-8000-000000000004',
            177000,
            admin_id, 'approved', admin_id, now()
        ),
        (
            'a0000003-0000-4000-8000-000000000005',
            'Dreams',
            'a0000002-0000-4000-8000-000000000005',
            'a0000001-0000-4000-8000-000000000005',
            257000,
            admin_id, 'approved', admin_id, now()
        )
    on conflict (id) do nothing;

    alter table public.artists enable trigger artists_enforce_pending_on_insert;
    alter table public.albums enable trigger albums_enforce_pending_on_insert;
    alter table public.tracks enable trigger tracks_enforce_pending_on_insert;

    raise notice 'Seed completado: admin@trackrate.dev + 5 artistas, 5 álbumes, 5 canciones';
end $$;
