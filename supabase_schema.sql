-- ============================================================
-- Supabase Schema — NotificacionesApp (Notibank)
-- LIMPIEZA + RECREACIÓN COMPLETA
-- Ejecutar SOLO si puedes perder los datos existentes (dev/test)
-- ============================================================

-- ============================================================
-- 0. LIMPIEZA: eliminar objetos antiguos en orden seguro
-- ============================================================

-- Políticas RLS
DO $$
DECLARE
    pol RECORD;
BEGIN
    FOR pol IN
        SELECT policyname, tablename
        FROM pg_policies
        WHERE schemaname = 'public'
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', pol.policyname, pol.tablename);
    END LOOP;
END $$;

-- Triggers
DROP TRIGGER IF EXISTS set_users_updated_at ON public.users;
DROP TRIGGER IF EXISTS set_devices_updated_at ON public.devices;

-- Trigger auto-registro (si existe de un intento previo)
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

-- Funciones
DROP FUNCTION IF EXISTS public.handle_new_user();
DROP FUNCTION IF EXISTS public.update_updated_at();
DROP FUNCTION IF EXISTS public.is_admin();
DROP FUNCTION IF EXISTS public.is_employee();

-- Tablas (orden: hijas primero, luego padres)
DROP TABLE IF EXISTS public.devices CASCADE;
DROP TABLE IF EXISTS public.relayed_notifications CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;

-- ============================================================
-- 1. Tabla: users
-- Datos de perfil de usuario (admin + empleados)
-- NOTA: id es UUID y referencia auth.users.id
-- ============================================================
CREATE TABLE public.users (
    id              UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email           TEXT NOT NULL UNIQUE,
    firstname       TEXT NOT NULL DEFAULT '',
    lastname        TEXT NOT NULL DEFAULT '',
    phone           TEXT NOT NULL DEFAULT '',
    birthdate       TEXT NOT NULL DEFAULT '',
    role            TEXT NOT NULL DEFAULT 'employee',
    adminid         UUID,
    isdisabled      BOOLEAN NOT NULL DEFAULT false,
    disabledreason  TEXT,
    replacedby      UUID,
    isresetaccount  BOOLEAN NOT NULL DEFAULT false,
    originalemail   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 2. Tabla: relayed_notifications
-- Historial de notificaciones bancarias procesadas
-- ============================================================
CREATE TABLE public.relayed_notifications (
    id              TEXT PRIMARY KEY,
    packagename     TEXT NOT NULL,
    appname         TEXT NOT NULL,
    title           TEXT NOT NULL,
    content         TEXT NOT NULL,
    type            TEXT NOT NULL DEFAULT 'other',
    amount          TEXT NOT NULL DEFAULT '',
    sender          TEXT NOT NULL DEFAULT '',
    admin_id        UUID REFERENCES public.users(id) ON DELETE SET NULL,
    timestamp       TIMESTAMPTZ NOT NULL,
    isprocessed     BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 3. Tabla: devices (opcional — futura)
-- ============================================================
CREATE TABLE public.devices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    fcm_token       TEXT,
    platform        TEXT NOT NULL DEFAULT 'android',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- ÍNDICES
-- ============================================================
CREATE INDEX idx_users_role ON public.users(role);
CREATE INDEX idx_users_adminid ON public.users(adminid);
CREATE INDEX idx_users_email ON public.users(email);
CREATE INDEX idx_notifications_packagename ON public.relayed_notifications(packagename);
CREATE INDEX idx_notifications_timestamp ON public.relayed_notifications(timestamp DESC);
CREATE INDEX idx_notifications_type ON public.relayed_notifications(type);
CREATE INDEX idx_notifications_adminid ON public.relayed_notifications(admin_id);
CREATE INDEX idx_devices_user_id ON public.devices(user_id);

-- ============================================================
-- FUNCIÓN SECURITY DEFINER (rompe la dependencia circular RLS)
-- ============================================================
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users
        WHERE id = auth.uid() AND role = 'admin'
    );
$$;

CREATE OR REPLACE FUNCTION public.is_employee()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users
        WHERE id = auth.uid() AND role = 'employee'
    );
$$;

-- ============================================================
-- TRIGGER: creación automática de perfil en public.users
-- al registrarse en auth.users (evita bugs de registro)
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    INSERT INTO public.users (id, email, role)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data ->> 'role', 'employee')
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- ============================================================
-- HABILITAR ROW LEVEL SECURITY
-- ============================================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.relayed_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- POLÍTICAS RLS — users
-- ============================================================

-- SELECT: Admins ven todos los usuarios
CREATE POLICY "rls_users_select_admin"
    ON public.users FOR SELECT
    USING (public.is_admin());

-- SELECT: Usuario ve su propio perfil
CREATE POLICY "rls_users_select_own"
    ON public.users FOR SELECT
    USING (auth.uid() = id);

-- INSERT: Insertar tu propio perfil (al registrarte)
CREATE POLICY "rls_users_insert_own"
    ON public.users FOR INSERT
    WITH CHECK (auth.uid() = id);

-- INSERT: Admin puede crear empleados u otros usuarios
CREATE POLICY "rls_users_insert_admin"
    ON public.users FOR INSERT
    WITH CHECK (public.is_admin());

-- UPDATE: Usuario actualiza su propio perfil
CREATE POLICY "rls_users_update_own"
    ON public.users FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- UPDATE: Admin actualiza cualquier usuario
CREATE POLICY "rls_users_update_admin"
    ON public.users FOR UPDATE
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- DELETE: Admin elimina cualquier usuario
CREATE POLICY "rls_users_delete_admin"
    ON public.users FOR DELETE
    USING (public.is_admin());

-- DELETE: Usuario elimina su propio perfil
CREATE POLICY "rls_users_delete_own"
    ON public.users FOR DELETE
    USING (auth.uid() = id);

-- ============================================================
-- POLÍTICAS RLS — relayed_notifications
-- ============================================================

-- SELECT: Admin ve sus propias notificaciones
CREATE POLICY "rls_notif_select_own"
    ON public.relayed_notifications FOR SELECT
    USING (auth.uid() = admin_id);

-- SELECT: Empleado ve notificaciones de su admin
CREATE POLICY "rls_notif_select_by_admin"
    ON public.relayed_notifications FOR SELECT
    USING (
        auth.uid() IS NOT NULL
        AND admin_id IS NOT NULL
        AND EXISTS (
            SELECT 1 FROM public.users
            WHERE id = auth.uid()
              AND adminid = relayed_notifications.admin_id
        )
    );

-- INSERT: Usuario autenticado inserta notificaciones (admin desde su dispositivo)
CREATE POLICY "rls_notif_insert_auth"
    ON public.relayed_notifications FOR INSERT
    WITH CHECK (auth.uid() IS NOT NULL);

-- INSERT: Admins insertan notificaciones (broadcast)
CREATE POLICY "rls_notif_insert_admin"
    ON public.relayed_notifications FOR INSERT
    WITH CHECK (public.is_admin());

-- UPDATE: Admins actualizan notificaciones
CREATE POLICY "rls_notif_update_admin"
    ON public.relayed_notifications FOR UPDATE
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- DELETE: Admins eliminan notificaciones
CREATE POLICY "rls_notif_delete_admin"
    ON public.relayed_notifications FOR DELETE
    USING (public.is_admin());

-- ============================================================
-- POLÍTICAS RLS — devices
-- ============================================================

CREATE POLICY "rls_devices_select_own"
    ON public.devices FOR SELECT
    USING (user_id = auth.uid());

CREATE POLICY "rls_devices_insert_own"
    ON public.devices FOR INSERT
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "rls_devices_delete_own"
    ON public.devices FOR DELETE
    USING (user_id = auth.uid());

-- ============================================================
-- TRIGGER: updated_at automático
-- ============================================================
CREATE OR REPLACE FUNCTION public.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = '';

CREATE TRIGGER set_users_updated_at
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at();

CREATE TRIGGER set_devices_updated_at
    BEFORE UPDATE ON public.devices
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at();

-- ============================================================
-- PERMISOS: GRANT para clientes Kotlin (Data API)
-- ============================================================

-- Tabla users
GRANT SELECT, INSERT, UPDATE, DELETE ON public.users TO authenticated;
GRANT ALL ON public.users TO service_role;

-- Tabla relayed_notifications
GRANT SELECT, INSERT, UPDATE, DELETE ON public.relayed_notifications TO authenticated;
GRANT ALL ON public.relayed_notifications TO service_role;

-- Tabla devices
GRANT SELECT, INSERT, DELETE ON public.devices TO authenticated;
GRANT ALL ON public.devices TO service_role;

-- Funciones auxiliares
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_employee() TO authenticated;
GRANT EXECUTE ON FUNCTION public.update_updated_at() TO authenticated;
GRANT EXECUTE ON FUNCTION public.handle_new_user() TO authenticated;

-- Revocar acceso a anon y PUBLIC para funciones SECURITY DEFINER
-- (evita que usuarios no autenticados las llamen por la API REST)
REVOKE EXECUTE ON FUNCTION public.is_admin() FROM anon, PUBLIC;
REVOKE EXECUTE ON FUNCTION public.is_employee() FROM anon, PUBLIC;
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM anon, PUBLIC;
REVOKE EXECUTE ON FUNCTION public.update_updated_at() FROM anon, PUBLIC;

-- Secuencias
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO authenticated;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO service_role;

-- ============================================================
-- SUPABASE REALTIME
-- Marca relayed_notifications para recibir eventos realtime
-- ============================================================
ALTER PUBLICATION supabase_realtime ADD TABLE public.relayed_notifications;
