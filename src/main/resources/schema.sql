CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    operation_number SERIAL NOT NULL UNIQUE,
    side TEXT NOT NULL,
    ticker TEXT NOT NULL,
    qty INTEGER NOT NULL,
    price DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_ticker ON orders(ticker);
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON orders(updated_at DESC);

CREATE TABLE IF NOT EXISTS orders_queue (
    id UUID PRIMARY KEY,
    position SERIAL NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_queue_position ON orders_queue(position ASC)