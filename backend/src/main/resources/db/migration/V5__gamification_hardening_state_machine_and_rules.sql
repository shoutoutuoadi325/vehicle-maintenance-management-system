-- 零碳公路：并发安全、状态机、防刷风控、动态规则

ALTER TABLE green_energy_account
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS green_journey_node_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    city_index INT NOT NULL,
    node_state VARCHAR(20) NOT NULL,
    checkin_at DATETIME NULL,
    update_time DATETIME NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_green_journey_user_city UNIQUE (user_id, city_index),
    CONSTRAINT fk_green_journey_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS green_reward_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    action_key VARCHAR(60) NOT NULL,
    energy_delta INT NOT NULL,
    mileage_delta INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    reason VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_green_reward_source_action UNIQUE (source_type, source_id, action_key),
    CONSTRAINT fk_green_reward_user FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_green_reward_user_time (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS green_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_key VARCHAR(100) NOT NULL,
    rule_value VARCHAR(255) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_green_rule_key UNIQUE (rule_key)
);

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'EMISSION_TO_ENERGY_RATIO', '100', 'DOUBLE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'EMISSION_TO_ENERGY_RATIO');

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'MAX_EMISSION_PER_ORDER', '20', 'DOUBLE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'MAX_EMISSION_PER_ORDER');

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'ORDER_REWARD_CAP', '600', 'INT', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'ORDER_REWARD_CAP');

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'DAILY_ENERGY_CAP', '1200', 'INT', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'DAILY_ENERGY_CAP');

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'DAILY_QUIZ_CHECKIN_LIMIT', '8', 'INT', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'DAILY_QUIZ_CHECKIN_LIMIT');
