INSERT INTO page_config (section, config) 
VALUES (
  'business', 
  '{
    "business_name": "Backwel Store",
    "legal_name": "Backwel S.A. de C.V.",
    "support_email": "soporte@backwel.com",
    "phone": "+52 55 1234 5678",
    "address": "Av. Tecnológico 123, Ciudad de México",
    "tax_id": "BAC210101XYZ",
    "logo_url": ""
  }'
)
ON CONFLICT (section) DO UPDATE SET config = EXCLUDED.config;