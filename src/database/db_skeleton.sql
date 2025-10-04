-- Create database
DROP DATABASE IF EXISTS card_watchdog;
CREATE DATABASE IF NOT EXISTS card_watchdog;
USE card_watchdog;

-- Users table (Client)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Cards table (base table for all card types)
CREATE TABLE cards (
    id INT AUTO_INCREMENT PRIMARY KEY, 
    card_number CHAR(36) UNIQUE NOT NULL, 
    expiration_date VARCHAR(7) NOT NULL, -- MM/YYYY format
    status ENUM('ACTIVE', 'BLOCKED', 'EXPIRED', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    card_type ENUM('CREDIT', 'DEBIT', 'PREPAID') NOT NULL,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Credit cards specific data
CREATE TABLE credit_cards (
    card_id INT PRIMARY KEY,
    monthly_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00, 
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

-- Debit cards specific data
CREATE TABLE debit_cards (
    card_id INT PRIMARY KEY,
    daily_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

-- Prepaid cards specific data
CREATE TABLE prepaid_cards (
    card_id INT PRIMARY KEY,
    available_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

-- Card operations table
CREATE TABLE card_operations (
    id CHAR(36) PRIMARY KEY, -- UUID
    date DATETIME NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    type ENUM('ACHAT', 'RETRAIT', 'PAIEMENTENLIGNE') NOT NULL,
    location VARCHAR(255) NOT NULL,
    card_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

-- Fraud alerts table
CREATE TABLE fraud_alerts (
    id CHAR(36) PRIMARY KEY, -- UUID
    description TEXT NOT NULL,
    level ENUM('INFO', 'AVERTISSEMENT', 'CRITIQUE') NOT NULL,
    card_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

-- Indexes for better performance
CREATE INDEX idx_cards_user_id ON cards(user_id);
CREATE INDEX idx_cards_status ON cards(status);
CREATE INDEX idx_cards_type ON cards(card_type);
CREATE INDEX idx_operations_card_id ON card_operations(card_id);
CREATE INDEX idx_operations_date ON card_operations(date);
CREATE INDEX idx_operations_type ON card_operations(type);
CREATE INDEX idx_alerts_card_id ON fraud_alerts(card_id);
CREATE INDEX idx_alerts_level ON fraud_alerts(level);
CREATE INDEX idx_alerts_created_at ON fraud_alerts(created_at);

-- Insert sample data for testing
INSERT INTO users (name, email, phone_number) VALUES
('Amanar Marouane', 'marouane@gmail.com', '+212644311735'),
('Ouyacho Omar', 'omar@gmail.com', '+212644311736');

-- Sample cards with proper structure
INSERT INTO cards (card_number, expiration_date, status, card_type, user_id) VALUES
(UUID(), '12/2025', 'ACTIVE', 'CREDIT', 1),
(UUID(), '01/2026', 'ACTIVE', 'DEBIT', 1),
(UUID(), '06/2024', 'ACTIVE', 'PREPAID', 2);

-- Sample credit card data
INSERT INTO credit_cards (card_id, monthly_limit, interest_rate) VALUES
(1, 5000.00, 0.1850);

-- Sample debit card data
INSERT INTO debit_cards (card_id, daily_limit) VALUES
(2, 1000.00);

-- Sample prepaid card data
INSERT INTO prepaid_cards (card_id, available_balance) VALUES
(3, 500.00);
