CREATE TABLE commerce_orders (
  order_no VARCHAR(40) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  product_name VARCHAR(200) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_order_owner ON commerce_orders(user_id, order_no);
CREATE TABLE commerce_logistics (
  order_no VARCHAR(40) PRIMARY KEY REFERENCES commerce_orders(order_no),
  carrier VARCHAR(100),
  tracking_no VARCHAR(100),
  status VARCHAR(32) NOT NULL,
  latest_event VARCHAR(500) NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
