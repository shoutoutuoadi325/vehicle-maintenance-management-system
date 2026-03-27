-- 答错惩罚与冷却机制

ALTER TABLE green_journey_node_state
    ADD COLUMN IF NOT EXISTS next_retry_time DATETIME NULL;

ALTER TABLE green_energy_account
    ADD COLUMN IF NOT EXISTS random_event_next_retry_time DATETIME NULL;

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'WRONG_ANSWER_ENERGY_PENALTY', '50', 'INT', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'WRONG_ANSWER_ENERGY_PENALTY');

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'WRONG_ANSWER_COOLDOWN_MINUTES', '120', 'INT', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'WRONG_ANSWER_COOLDOWN_MINUTES');
