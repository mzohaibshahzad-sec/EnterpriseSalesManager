-- ============================================================
--  Enterprise Product Sales Management System — MySQL Schema
--  Run this once on your MySQL server before first launch:
--      mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS sales_management
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sales_management;

-- ------------------------------------------------------------
-- Store configuration (single row, filled by Setup Wizard)
-- ------------------------------------------------------------
CREATE TABLE store_settings (
    id                INT PRIMARY KEY AUTO_INCREMENT,
    store_name        VARCHAR(150) NOT NULL,
    store_logo_path   VARCHAR(255),
    store_address     VARCHAR(255),
    store_contact     VARCHAR(50),
    store_email       VARCHAR(150),
    currency_symbol   VARCHAR(10) DEFAULT 'Rs',
    theme             ENUM('DARK','LIGHT') DEFAULT 'DARK',
    setup_completed   BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- Users (Owner + Salesperson only — Viewer has no account)
-- ------------------------------------------------------------
CREATE TABLE users (
    id                    INT PRIMARY KEY AUTO_INCREMENT,
    full_name             VARCHAR(150) NOT NULL,
    username              VARCHAR(50)  UNIQUE NOT NULL,
    gmail                 VARCHAR(150) UNIQUE NOT NULL,
    phone                 VARCHAR(30),
    password_hash         VARCHAR(255) NOT NULL,
    role                  ENUM('OWNER','SALESPERSON') NOT NULL,
    profile_picture_path  VARCHAR(255),
    is_active             BOOLEAN DEFAULT TRUE,
    last_login            TIMESTAMP NULL,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE login_history (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    user_id       INT NOT NULL,
    login_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address    VARCHAR(50),
    status        ENUM('SUCCESS','FAILED') NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE otp_codes (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    user_id       INT NOT NULL,
    otp_code      VARCHAR(10) NOT NULL,
    purpose       ENUM('LOGIN_2FA','PASSWORD_RESET') DEFAULT 'LOGIN_2FA',
    is_used       BOOLEAN DEFAULT FALSE,
    attempts      INT DEFAULT 0,
    expires_at    TIMESTAMP NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Catalog
-- ------------------------------------------------------------
CREATE TABLE categories (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(100) UNIQUE NOT NULL,
    description  VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE suppliers (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(150) NOT NULL,
    contact_person  VARCHAR(150),
    phone           VARCHAR(30),
    email           VARCHAR(150),
    address         VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE products (
    id                 INT PRIMARY KEY AUTO_INCREMENT,
    name               VARCHAR(150) NOT NULL,
    category_id        INT,
    brand              VARCHAR(100),
    sku                VARCHAR(100) UNIQUE,
    wholesale_price    DECIMAL(12,2) NOT NULL DEFAULT 0,
    selling_price      DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_quantity     INT NOT NULL DEFAULT 0,
    sold_quantity      INT NOT NULL DEFAULT 0,
    min_stock_level    INT DEFAULT 5,
    supplier_id        INT,
    image_path         VARCHAR(255),
    description        TEXT,
    is_active          BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    INDEX idx_product_name (name),
    INDEX idx_product_sku (sku)
) ENGINE=InnoDB;

-- available_quantity and profit_per_item are derived at query/service
-- level (total_quantity - sold_quantity) and (selling_price - wholesale_price)
-- rather than stored, to avoid update-anomaly drift.

CREATE TABLE customers (
    id      INT PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(150),
    phone   VARCHAR(30),
    email   VARCHAR(150)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Sales
-- ------------------------------------------------------------
CREATE TABLE sales (
    id                INT PRIMARY KEY AUTO_INCREMENT,
    invoice_number    VARCHAR(50) UNIQUE NOT NULL,
    salesperson_id    INT NOT NULL,
    customer_id       INT,
    customer_name     VARCHAR(150),
    sale_datetime     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subtotal          DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount          DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount      DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_profit      DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_method    ENUM('CASH','CARD','BANK_TRANSFER','OTHER') DEFAULT 'CASH',
    payment_status    ENUM('PAID','PARTIAL','UNPAID') DEFAULT 'PAID',
    notes             VARCHAR(255),
    FOREIGN KEY (salesperson_id) REFERENCES users(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    INDEX idx_sale_datetime (sale_datetime)
) ENGINE=InnoDB;

CREATE TABLE sale_items (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    sale_id       INT NOT NULL,
    product_id    INT NOT NULL,
    product_name  VARCHAR(150) NOT NULL,
    quantity      INT NOT NULL,
    unit_price    DECIMAL(12,2) NOT NULL,
    unit_cost     DECIMAL(12,2) NOT NULL,
    line_total    DECIMAL(12,2) NOT NULL,
    line_profit   DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE payments (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    sale_id  INT NOT NULL,
    amount   DECIMAL(12,2) NOT NULL,
    method   ENUM('CASH','CARD','BANK_TRANSFER','OTHER') DEFAULT 'CASH',
    paid_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Audit
-- ------------------------------------------------------------
CREATE TABLE activity_logs (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    user_id     INT,
    action      VARCHAR(255) NOT NULL,
    details     VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_log_date (created_at)
) ENGINE=InnoDB;

-- Seed a default category so the catalog isn't empty on first run
INSERT INTO categories (name, description) VALUES ('General', 'Default category');
