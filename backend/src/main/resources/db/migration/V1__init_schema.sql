-- 1. USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_number VARCHAR(8) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    role VARCHAR(50) NOT NULL
);

-- 2. ACCOUNTS
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_number VARCHAR(34) UNIQUE NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    blocked_funds DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(50) NOT NULL,
    parent_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL
);

-- 3. TRANSACTIONS
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    sender_account_number VARCHAR(34),
    receiver_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    receiver_account_number VARCHAR(34),
    receiver_bank_bic VARCHAR(11),
    receiver_name VARCHAR(255),
    title VARCHAR(255),
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    external_payment_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_date TIMESTAMP
);

-- 4. CARDS
CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    card_number VARCHAR(20) UNIQUE NOT NULL,
    transaction_limit DECIMAL(15, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'PLN',
    expiry_date DATE,
    type VARCHAR(50) NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE
);

-- 5. PENDING_APPROVALS (junior)
CREATE TABLE pending_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    junior_account_id UUID NOT NULL REFERENCES accounts(id),
    parent_user_id UUID NOT NULL REFERENCES users(id),
    transaction_id UUID REFERENCES transactions(id),
    amount DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- 6. KLIK_CODES (Kody BLIK)
CREATE TABLE klik_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    code VARCHAR(6) NOT NULL,
    amount DECIMAL(15, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    idempotency_key VARCHAR(64)
);

-- 7. KLIK_ALIASES (Aliasy telefonu)
CREATE TABLE klik_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    alias VARCHAR(20) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. AML_HOLDS (System Anti-Money Laundering)
CREATE TABLE aml_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    transaction_id UUID REFERENCES transactions(id),
    reason VARCHAR(500),
    client_explanation TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP
);

-- INDEKSY
CREATE INDEX idx_transactions_external_id ON transactions(external_payment_id);
CREATE INDEX idx_transactions_sender_acc ON transactions(sender_account_id);
CREATE INDEX idx_pending_parent ON pending_approvals(parent_user_id);
CREATE INDEX idx_klik_status ON klik_codes(user_id, status);
CREATE INDEX idx_aml_account ON aml_holds(account_id, status);