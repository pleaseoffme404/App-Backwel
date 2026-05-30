CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS business_config (
    id SERIAL PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    logo_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO business_config (business_name, logo_url) 
VALUES ('BACKWEL COMMERCE', 'https://ui-avatars.com/api/?name=Backwel&background=0A3C51&color=F4F7FE')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS page_config (
    id SERIAL PRIMARY KEY,
    section VARCHAR(50) UNIQUE NOT NULL,
    config JSONB NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO page_config (section, config) VALUES
('hero', '{"visible": true, "title": "Plataforma de Gestión Comercial", "subtitle": "Sincroniza tu inventario, ventas y tienda en un solo ecosistema optimizado.", "cta_text": "Empezar ahora", "bg_image_url": "https://images.unsplash.com/photo-1557804506-669a67965ba0?auto=format&fit=crop&q=80"}'),
('about', '{"visible": true, "title": "Nuestra Filosofía", "description": "Construimos herramientas robustas para empresas que exigen seguridad y rendimiento sin compromisos.", "image_url": "https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&q=80"}'),
('featured_products', '{"visible": true, "title": "Productos Destacados", "subtitle": "Tecnología de punta para tu negocio"}'),
('catalog_link', '{"visible": true, "banner_text": "¿Listo para explorar todo nuestro inventario?", "button_text": "Ver Catálogo Completo", "banner_image_url": "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80"}')
ON CONFLICT (section) DO UPDATE SET config = EXCLUDED.config;

CREATE TABLE IF NOT EXISTS featured_products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    image_url VARCHAR(255),
    visible BOOLEAN DEFAULT true
);

INSERT INTO featured_products (id, name, description, price, image_url, visible) VALUES
('e1f2b6a0-4d4a-4a6c-9a1f-1c4b8b6a1f2e', 'Laptop ThinkPad L14 Gen 2', 'Equipo robusto para desarrollo y administración de redes', 1250.00, 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?auto=format&fit=crop&w=600&q=80', true),
('c4e3b2a1-1d2a-3a4c-8a0f-0c1b7b5a0f1d', 'Memoria RAM 64GB DDR4', 'Kit de alto rendimiento para servidores locales', 180.00, 'https://images.unsplash.com/photo-1562976540-1502f75a6c0c?auto=format&fit=crop&w=600&q=80', true),
('b3d2a1f0-0c1a-2a3b-7a9e-9b0a6a4f9e0c', 'Pasta Térmica Noctua NT-H2', 'Pasta térmica de grado entusiasta para mantenimiento', 25.00, 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=600&q=80', true),
('a2c1f0e9-9b0f-1a2b-6a8d-8a9f5a3e8d9b', 'Procesador Ryzen 9 5900XT', '16 núcleos para máxima carga de trabajo y virtualización', 499.99, 'https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?auto=format&fit=crop&w=600&q=80', true),
('f1b0e9d8-8a9e-0f1a-5a7c-7a8e4a2d7c8a', 'Switch Gigabit 24 Puertos', 'Switch administrable para infraestructura red', 220.00, 'https://images.unsplash.com/photo-1558227691-41ea78d1f631?auto=format&fit=crop&w=600&q=80', true),
('d0a9f8e7-7f8d-9e0f-4a6b-6a7d3a1c6b79', 'Power Bank 100W 60000mAh', 'Respaldo masivo de energía para trabajo móvil', 145.00, 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?auto=format&fit=crop&w=600&q=80', true);