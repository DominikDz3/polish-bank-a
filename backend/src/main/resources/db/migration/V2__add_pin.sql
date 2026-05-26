-- Dodanie obsługi PIN-u (4 cyfry, hashowany BCryptem) + mechanizm blokady po nieudanych próbach
ALTER TABLE users
    ADD COLUMN pin_hash VARCHAR(255),
    ADD COLUMN pin_failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN pin_locked_until TIMESTAMP;