CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE SERVER IF NOT EXISTS auth_server_link
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host 'auth-db', port '5432', dbname 'auth-database');

-- 3. Crear el mapeo de usuario (seguridad)
CREATE USER MAPPING IF NOT EXISTS FOR current_user
    SERVER auth_server_link
    OPTIONS (user 'admin', password '2feReSfelv74EzRuFnsY2xcp6W2rMtn5');

CREATE SCHEMA IF NOT EXISTS auth_schema;

IMPORT FOREIGN SCHEMA public
    LIMIT TO (users, user_role, role)
    FROM SERVER auth_server_link
    INTO auth_schema;