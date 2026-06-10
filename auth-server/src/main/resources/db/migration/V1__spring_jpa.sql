-- =====================================================
-- ARCHIVO V1__spring_jpa.sql
-- TODAS las tablas de entidades JPA/Hibernate
-- Con validaciones estrictas del archivo original
-- =====================================================

-- 1. Crear secuencia para la tabla Role
CREATE SEQUENCE IF NOT EXISTS role_seq START WITH 1 INCREMENT BY 50;

-- 2. Tabla de Roles (con validaciones estrictas)
CREATE TABLE role
(
    id        BIGINT             NOT NULL DEFAULT nextval('role_seq'),
    role_name VARCHAR(20) UNIQUE NOT NULL,
    CONSTRAINT pk_role PRIMARY KEY (id)
);

-- 3. Tabla de Usuarios (con validaciones estrictas del original)
CREATE TABLE users
(
    id                  UUID         NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255),
    auth_provider       VARCHAR(20)  DEFAULT 'LOCAL',
    expired             BOOLEAN      DEFAULT FALSE,
    locked              BOOLEAN      DEFAULT FALSE,
    credentials_expired BOOLEAN      DEFAULT FALSE,
    disabled            BOOLEAN      DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    last_login_at       TIMESTAMPTZ,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- 4. Tabla UserPin (Entidad JPA - con validaciones estrictas)
CREATE TABLE user_pin
(
    user_id         UUID                                   NOT NULL,
    pin_hash        VARCHAR(255)                           NOT NULL,
    failed_attempts INTEGER                                NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ                            NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ                            NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_userpin PRIMARY KEY (user_id)
);

-- 5. Tabla UserRole (tabla join con validaciones estrictas)
CREATE TABLE user_role
(
    user_id UUID   NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_on_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_on_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);

-- 6. Índices adicionales para rendimiento
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_pin_locked_until ON user_pin (locked_until);
CREATE INDEX idx_user_pin_failed_attempts ON user_pin (failed_attempts);