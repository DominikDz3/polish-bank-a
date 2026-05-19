ALTER TABLE cards
    ADD COLUMN daily_limit DECIMAL(15, 2);

ALTER TABLE transactions
    ADD COLUMN card_id UUID REFERENCES cards(id) ON DELETE SET NULL;

CREATE INDEX idx_transactions_card_id ON transactions(card_id);