ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS hold_type VARCHAR(20);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS external_transfer_id UUID REFERENCES external_transfers(id);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS swift_transfer_id UUID REFERENCES swift_transfers(id);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS amount NUMERIC(15,2);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS currency VARCHAR(3);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS receiver_info VARCHAR(255);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS triggered_rule VARCHAR(50);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS decision_at TIMESTAMP;
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS decision_by UUID REFERENCES users(id);
ALTER TABLE aml_holds ADD COLUMN IF NOT EXISTS decision_note VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_aml_user_status ON aml_holds(user_id, status);
CREATE INDEX IF NOT EXISTS idx_aml_status_created ON aml_holds(status, created_at DESC);