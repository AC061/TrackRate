-- Reparar login del admin de desarrollo si fue creado con identidad incompleta.
-- Ejecutar en SQL Editor si admin@trackrate.dev no puede iniciar sesión.
-- Contraseña resultante: TrackRateAdmin123!

create extension if not exists pgcrypto with schema extensions;

do $$
declare
    admin_id uuid := 'a0000000-0000-4000-8000-000000000001';
begin
    if not exists (select 1 from auth.users where id = admin_id) then
        raise notice 'Admin no encontrado; ejecuta primero 20250625120008_seed_dev_data.sql';
        return;
    end if;

    update auth.users
    set
        encrypted_password = extensions.crypt('TrackRateAdmin123!', extensions.gen_salt('bf')),
        email_confirmed_at = coalesce(email_confirmed_at, now()),
        updated_at = now()
    where id = admin_id;

    delete from auth.identities
    where user_id = admin_id and provider = 'email';

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
        admin_id,
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

    update public.profiles
    set
        username = 'admin',
        display_name = 'TrackRate Admin',
        is_admin = true
    where id = admin_id;

    raise notice 'Admin reparado: admin@trackrate.dev / TrackRateAdmin123!';
end $$;
