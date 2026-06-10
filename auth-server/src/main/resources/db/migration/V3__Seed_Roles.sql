-- Insertar los roles definidos en el Enum de Java
INSERT INTO role (role_name)
VALUES ('OWNER'), ('ADMIN'), ('MANAGER'), ('STAFF'), ('USER')
ON CONFLICT (role_name) DO NOTHING;