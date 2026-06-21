ALTER TABLE klik_aliases ADD COLUMN IF NOT EXISTS klik_alias_id UUID;
ALTER TABLE klik_aliases ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP;

ALTER TABLE klik_aliases DROP CONSTRAINT IF EXISTS klik_aliases_alias_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_klik_alias_active ON klik_aliases(alias) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_klik_alias_user ON klik_aliases(user_id);