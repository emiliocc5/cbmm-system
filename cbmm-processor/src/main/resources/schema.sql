DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;

CREATE TABLE accounts (
                          id VARCHAR(255) PRIMARY KEY,
                          balance DECIMAL(19, 4) NOT NULL,
                          currency VARCHAR(3) NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
                              id VARCHAR(255) PRIMARY KEY,
                              account_id VARCHAR(255) NOT NULL,
                              event_id VARCHAR(255) NOT NULL,
                              currency VARCHAR(3) NOT NULL,
                              amount DECIMAL(19, 4) NOT NULL,
                              balance_after DECIMAL(19, 4) NOT NULL,
                              type VARCHAR(50) NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              operation_date TIMESTAMP NOT NULL,
                              processed_at TIMESTAMP,
                              CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_event_id ON transactions(event_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_operation_date ON transactions(operation_date);
CREATE INDEX idx_accounts_currency ON accounts(currency);