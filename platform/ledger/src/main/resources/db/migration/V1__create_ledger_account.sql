CREATE TABLE ledger_account (
    id UUID PRIMARY KEY,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ledger_account_type
        CHECK (account_type IN ('ASSET', 'LIABILITY', 'REVENUE', 'EXPENSE', 'EQUITY')),
    CONSTRAINT ck_ledger_account_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);
