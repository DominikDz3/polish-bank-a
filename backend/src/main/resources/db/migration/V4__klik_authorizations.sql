CREATE TABLE klik_authorizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    klik_transaction_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    klik_code_id UUID REFERENCES klik_codes(id),
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    is_on_us BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_AUTH',
    reject_reason VARCHAR(50),
    expiry_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_klik_auth_user_status ON klik_authorizations(user_id, status);
CREATE INDEX idx_klik_auth_klik_tx ON klik_authorizations(klik_transaction_id);