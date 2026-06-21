CREATE TABLE external_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_payment_id VARCHAR(64) NOT NULL UNIQUE,
    sender_account_id UUID NOT NULL REFERENCES accounts(id),
    sender_account_number VARCHAR(64) NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    receiver_account_number VARCHAR(64) NOT NULL,
    receiver_name VARCHAR(255) NOT NULL,
    receiver_bank_bicfi VARCHAR(32) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PLN',
    title VARCHAR(255) NOT NULL,
    routing_system VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    rejection_reason VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    settled_at TIMESTAMP
);

CREATE INDEX idx_ext_status ON external_transfers(status, routing_system);
CREATE INDEX idx_ext_sender ON external_transfers(sender_account_id, created_at DESC);