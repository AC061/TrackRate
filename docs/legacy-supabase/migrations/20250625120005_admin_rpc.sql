-- TrackRate: RPC para gestión de administradores
-- Permite que un admin promueva o revoque el rol admin de otro usuario por username.
-- SECURITY DEFINER: se ejecuta con privilegios del owner (bypassa RLS), pero valida
-- explícitamente que quien llama sea admin mediante public.is_admin().

create or replace function public.set_user_admin(
    target_username text,
    make_admin boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.is_admin() then
        raise exception 'Only admins can manage admin roles';
    end if;

    update public.profiles
    set is_admin = make_admin
    where username = target_username;

    if not found then
        raise exception 'User % not found', target_username;
    end if;
end;
$$;

grant execute on function public.set_user_admin(text, boolean) to authenticated;
