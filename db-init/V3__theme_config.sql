INSERT INTO page_config (section, config) 
VALUES (
  'theme', 
  '{
    "dark_bg_primary": "#0F172A",
    "dark_bg_secondary": "#1E293B",
    "dark_brand_primary": "#38BDF8",
    "dark_brand_secondary": "#7DD3FC",
    "dark_accent": "#F97316",
    "dark_text_primary": "#F8FAFC",
    "light_bg_primary": "#F4F7FE",
    "light_bg_secondary": "#FFFFFF",
    "light_brand_primary": "#0A3C51",
    "light_brand_secondary": "#126385",
    "light_accent": "#E85D04",
    "light_text_primary": "#1A202C"
  }'
)
ON CONFLICT (section) DO UPDATE SET config = EXCLUDED.config;