-- Dodanie pól providera kart do tabeli cards
ALTER TABLE cards
    ADD COLUMN provider_token VARCHAR(64),
    ADD COLUMN provider_status VARCHAR(20),
    ADD COLUMN masked_pan VARCHAR(25),
    ADD COLUMN bin_prefix VARCHAR(10);

CREATE UNIQUE INDEX ux_cards_provider_token ON cards(provider_token) WHERE provider_token IS NOT NULL;

-- Tabela autoryzacji kartowych (HELD przed settlementem)
CREATE TABLE card_authorizations (
    id UUID PRIMARY KEY,
    authorization_code VARCHAR(40) NOT NULL UNIQUE,
    external_transaction_id VARCHAR(64) NOT NULL UNIQUE,
    card_id UUID REFERENCES cards(id) ON DELETE SET NULL,
    account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    merchant_name VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    settled_at TIMESTAMP
);

CREATE INDEX ix_card_auth_account ON card_authorizations(account_id);
CREATE INDEX ix_card_auth_status ON card_authorizations(status);