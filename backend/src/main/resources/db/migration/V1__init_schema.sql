-- tabela uzytkownikow
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- tabela kont (z relacją do users oraz z relacją do samej siebie dla kont Junior)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_number VARCHAR(34) UNIQUE NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(50) NOT NULL,
    parent_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL
);

-- tabela transakcji
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    sender_account_number VARCHAR(34),
    receiver_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    receiver_account_number VARCHAR(34),
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- tabela kart
CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    card_number VARCHAR(20) UNIQUE NOT NULL,
    transaction_limit DECIMAL(15, 2),
    type VARCHAR(50) NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE
);