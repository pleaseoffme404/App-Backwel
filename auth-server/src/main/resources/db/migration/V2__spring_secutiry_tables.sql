-- =====================================================
-- ARCHIVO V2__spring_security.sql
-- Tablas de infraestructura de Spring Security OAuth2
-- Con validaciones estrictas del archivo original
-- =====================================================

-- 1. Tabla de Spring Authorization (con tipos TIMESTAMPTZ del original)
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
    authorization_code_issued_at  TIMESTAMPTZ  DEFAULT NULL,
    authorization_code_expires_at TIMESTAMPTZ  DEFAULT NULL,
    authorization_code_metadata   text         DEFAULT NULL,
    access_token_value            text         DEFAULT NULL,
    access_token_issued_at        TIMESTAMPTZ  DEFAULT NULL,
    access_token_expires_at       TIMESTAMPTZ  DEFAULT NULL,
    access_token_metadata         text         DEFAULT NULL,
    access_token_type             varchar(255) DEFAULT NULL,
    access_token_scopes           text         DEFAULT NULL,
    refresh_token_value           text         DEFAULT NULL,
    refresh_token_issued_at       TIMESTAMPTZ  DEFAULT NULL,
    refresh_token_expires_at      TIMESTAMPTZ  DEFAULT NULL,
    refresh_token_metadata        text         DEFAULT NULL,
    oidc_id_token_value           text         DEFAULT NULL,
    oidc_id_token_issued_at       TIMESTAMPTZ  DEFAULT NULL,
    oidc_id_token_expires_at      TIMESTAMPTZ  DEFAULT NULL,
    oidc_id_token_metadata        text         DEFAULT NULL,
    oidc_id_token_claims          text         DEFAULT NULL,
    user_code_value               text         DEFAULT NULL,
    user_code_issued_at           TIMESTAMPTZ  DEFAULT NULL,
    user_code_expires_at          TIMESTAMPTZ  DEFAULT NULL,
    user_code_metadata            text         DEFAULT NULL,
    device_code_value             text         DEFAULT NULL,
    device_code_issued_at         TIMESTAMPTZ  DEFAULT NULL,
    device_code_expires_at        TIMESTAMPTZ  DEFAULT NULL,
    device_code_metadata          text         DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 2. Tabla de Spring Authorization Consent
CREATE TABLE oauth2_authorization_consent
(
    registered_client_id varchar(255) NOT NULL,
    principal_name       varchar(255) NOT NULL,
    authorities          text         NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- 3. Índices para optimización de consultas de Spring Security
CREATE INDEX idx_oauth2_auth_principal ON oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_auth_client ON oauth2_authorization (registered_client_id);
CREATE INDEX idx_oauth2_auth_issued_at ON oauth2_authorization (access_token_issued_at);
CREATE INDEX idx_oauth2_auth_expires_at ON oauth2_authorization (access_token_expires_at);