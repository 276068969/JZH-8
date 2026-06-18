CREATE DATABASE IF NOT EXISTS toy_guard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE toy_guard;

CREATE TABLE IF NOT EXISTS user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  role VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS merchant (
  id BIGINT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  license_no VARCHAR(80) NOT NULL,
  contact VARCHAR(120),
  status VARCHAR(32) NOT NULL,
  blacklisted BOOLEAN NOT NULL DEFAULT FALSE,
  remark VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS toy_product (
  id BIGINT PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  category VARCHAR(80) NOT NULL,
  merchant_id BIGINT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL,
  certification_no VARCHAR(100),
  report_name VARCHAR(160),
  status VARCHAR(32) NOT NULL,
  audit_remark VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS complaint (
  id BIGINT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  reporter VARCHAR(80) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS notice (
  id BIGINT PRIMARY KEY,
  title VARCHAR(160) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  published_at DATE NOT NULL
);

INSERT INTO user (id, username, password, display_name, role) VALUES
(1, 'admin', '123456', '系统管理员', 'ADMIN'),
(2, 'regulator', '123456', '监管人员', 'REGULATOR'),
(3, 'merchant', '123456', '星河玩具商家', 'MERCHANT'),
(4, 'user', '123456', '普通用户', 'USER')
ON DUPLICATE KEY UPDATE username = VALUES(username);
