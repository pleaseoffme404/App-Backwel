CREATE TABLE permission
(
    id              UUID         NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_permission PRIMARY KEY (id)
);

CREATE TABLE role
(
    id        UUID         NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_role PRIMARY KEY (id)
);

CREATE TABLE role_permissions
(
    permission_id UUID NOT NULL,
    role_id       UUID NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (permission_id, role_id)
);

CREATE TABLE user_pin
(
    user_id         UUID                                   NOT NULL,
    pin_hash        VARCHAR(255)                           NOT NULL,
    failed_attempts INTEGER                                NOT NULL,
    locked_until    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT pk_userpin PRIMARY KEY (user_id)
);

CREATE TABLE user_role
(
    role_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (role_id, user_id)
);

CREATE TABLE users
(
    id                  UUID                        NOT NULL,
    email               VARCHAR(255)                NOT NULL,
    password            VARCHAR(255),
    auth_provider       VARCHAR(20) DEFAULT 'LOCAL' NOT NULL,
    role_id             UUID                        NOT NULL,
    expired             BOOLEAN                     NOT NULL,
    locked              BOOLEAN                     NOT NULL,
    credentials_expired BOOLEAN                     NOT NULL,
    disabled            BOOLEAN                     NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE,
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    last_login_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE permission
    ADD CONSTRAINT uc_permission_permissionname UNIQUE (permission_name);

ALTER TABLE role
    ADD CONSTRAINT uc_role_rolename UNIQUE (role_name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

CREATE INDEX idx_user_email ON users (email);

ALTER TABLE user_pin
    ADD CONSTRAINT FK_USERPIN_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE users
    ADD CONSTRAINT FK_USERS_ON_ROLE FOREIGN KEY (role_id) REFERENCES role (id);

ALTER TABLE role_permissions
    ADD CONSTRAINT fk_rolper_on_permission FOREIGN KEY (permission_id) REFERENCES permission (id);

ALTER TABLE role_permissions
    ADD CONSTRAINT fk_rolper_on_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE;

ALTER TABLE user_role
    ADD CONSTRAINT fk_user_role_on_role FOREIGN KEY (role_id) REFERENCES role (id);

ALTER TABLE user_role
    ADD CONSTRAINT fk_user_role_on_user FOREIGN KEY (user_id) REFERENCES users (id);