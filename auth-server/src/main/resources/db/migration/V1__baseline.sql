-- 1. Crear secuencia para la tabla Role
CREATE SEQUENCE IF NOT EXISTS role_seq START WITH 1 INCREMENT BY 50;

-- 2. Tabla de Roles
CREATE TABLE role
(
    id        BIGINT             NOT NULL DEFAULT nextval('role_seq'),
    role_name VARCHAR(20) UNIQUE NOT NULL,
    CONSTRAINT pk_role PRIMARY KEY (id)
);

-- 3. Tabla de Usuarios
CREATE TABLE users
(
    id                  UUID         NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255),
    auth_provider       VARCHAR(20) DEFAULT 'LOCAL',
    expired             BOOLEAN     DEFAULT FALSE,
    locked              BOOLEAN     DEFAULT FALSE,
    credentials_expired BOOLEAN     DEFAULT FALSE,
    disabled            BOOLEAN     DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    last_login_at       TIMESTAMPTZ,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- 4 Índice tabla de usuarios
CREATE INDEX idx_user_email ON users (email);

-- 5 Tabla de Roles
CREATE TABLE user_role
(
    user_id UUID   NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_on_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_on_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);


-- 6 Tabla de Spring Authorization
CREATE TABLE oauth2_authorization
(
    id                            varchar(255) NOT NULL,
    registered_client_id          varchar(255) NOT NULL,
    principal_name                varchar(255) NOT NULL,
    authorization_grant_type      varchar(255) NOT NULL,
    authorized_scopes             text         DEFAULT NULL,
    attributes                    text         DEFAULT NULL,
    state                         text         DEFAULT NULL,
    authorization_code_value      text         DEFAULT NULL,
    authorization_code_issued_at  timestamp    DEFAULT NULL,
    authorization_code_expires_at timestamp    DEFAULT NULL,
    authorization_code_metadata   text         DEFAULT NULL,
    access_token_value            text         DEFAULT NULL,
    access_token_issued_at        timestamp    DEFAULT NULL,
    access_token_expires_at       timestamp    DEFAULT NULL,
    access_token_metadata         text         DEFAULT NULL,
    access_token_type             varchar(255) DEFAULT NULL,
    access_token_scopes           text         DEFAULT NULL,
    refresh_token_value           text         DEFAULT NULL,
    refresh_token_issued_at       timestamp    DEFAULT NULL,
    refresh_token_expires_at      timestamp    DEFAULT NULL,
    refresh_token_metadata        text         DEFAULT NULL,
    oidc_id_token_value           text         DEFAULT NULL,
    oidc_id_token_issued_at       timestamp    DEFAULT NULL,
    oidc_id_token_expires_at      timestamp    DEFAULT NULL,
    oidc_id_token_metadata        text         DEFAULT NULL,
    oidc_id_token_claims          text         DEFAULT NULL,
    user_code_value               text         DEFAULT NULL,
    user_code_issued_at           timestamp    DEFAULT NULL,
    user_code_expires_at          timestamp    DEFAULT NULL,
    user_code_metadata            text         DEFAULT NULL,
    device_code_value             text         DEFAULT NULL,
    device_code_issued_at         timestamp    DEFAULT NULL,
    device_code_expires_at        timestamp    DEFAULT NULL,
    device_code_metadata          text         DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 7 Tabla de Spring Authorization Consent
CREATE TABLE oauth2_authorization_consent
(
    registered_client_id varchar(255) NOT NULL,
    principal_name       varchar(255) NOT NULL,
    authorities          text         NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);