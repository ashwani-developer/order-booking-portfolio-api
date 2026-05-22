-- Orders table: stores all trading orders with lifecycle state
CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trader_id   VARCHAR(50)  NOT NULL,
    stock       VARCHAR(10)  NOT NULL,
    sector      VARCHAR(20)  NOT NULL,
    quantity    INT          NOT NULL CHECK (quantity > 0),
    side        VARCHAR(4)   NOT NULL CHECK (side IN ('BUY', 'SELL')),
    state       VARCHAR(10)  NOT NULL CHECK (state IN ('PENDING', 'FILLED', 'CANCELLED')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_trader_state ON orders (trader_id, state);

-- Holdings table: tracks current portfolio positions per trader
CREATE TABLE IF NOT EXISTS holdings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trader_id   VARCHAR(50)  NOT NULL,
    stock       VARCHAR(10)  NOT NULL,
    sector      VARCHAR(20)  NOT NULL,
    quantity    INT          NOT NULL CHECK (quantity >= 0),
    CONSTRAINT uk_holdings_trader_stock UNIQUE (trader_id, stock)
);

CREATE INDEX IF NOT EXISTS idx_holdings_trader ON holdings (trader_id);
