-- 随机突发事件状态：挂起题目与冻结里程

ALTER TABLE green_energy_account
    ADD COLUMN IF NOT EXISTS journey_status VARCHAR(40) NOT NULL DEFAULT 'NORMAL';

ALTER TABLE green_energy_account
    ADD COLUMN IF NOT EXISTS pending_random_quiz_id BIGINT NULL;

ALTER TABLE green_energy_account
    ADD COLUMN IF NOT EXISTS frozen_mileage INT NOT NULL DEFAULT 0;

ALTER TABLE green_energy_account
    ADD CONSTRAINT fk_green_energy_pending_quiz FOREIGN KEY (pending_random_quiz_id) REFERENCES green_quiz(id);

INSERT INTO green_rule_config (rule_key, rule_value, value_type, enabled, update_time)
SELECT 'RANDOM_EVENT_TRIGGER_PROBABILITY', '0.2', 'DOUBLE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM green_rule_config WHERE rule_key = 'RANDOM_EVENT_TRIGGER_PROBABILITY');
