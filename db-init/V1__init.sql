CREATE TABLE IF NOT EXISTS category_metadata (
    category_id UUID PRIMARY KEY,
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_category_metadata_modtime
    BEFORE UPDATE ON category_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();