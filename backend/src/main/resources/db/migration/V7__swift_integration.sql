-- SWIFT international transfers
CREATE TABLE swift_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL UNIQUE REFERENCES transactions(id) ON DELETE CASCADE,
    uetr VARCHAR(36) UNIQUE,
    message_id VARCHAR(100),
    instruction_id VARCHAR(100),
    sender_bic VARCHAR(11) NOT NULL,
    receiver_bic VARCHAR(11) NOT NULL,
    receiver_country VARCHAR(2),
    receiver_iban VARCHAR(34) NOT NULL,
    charge_bearer VARCHAR(8) NOT NULL,
    charge_bearer_input VARCHAR(4) NOT NULL,
    route TEXT,
    fee_total DECIMAL(15, 2),
    fee_sender DECIMAL(15, 2),
    fee_receiver DECIMAL(15, 2),
    fee_intermediary DECIMAL(15, 2),
    estimated_seconds DECIMAL(10, 2),
    status VARCHAR(20) NOT NULL,
    return_reason VARCHAR(255),
    delivered_at TIMESTAMP,
    returned_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_swift_status ON swift_transfers(status);
CREATE INDEX idx_swift_uetr ON swift_transfers(uetr);