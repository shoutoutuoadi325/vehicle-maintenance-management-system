-- Feedback-driven self-iteration configuration.
-- These tables support the "heavy decision, light training" mechanism:
-- SQL aggregates deposited feedback samples, proposes prompt/dispatch-parameter changes,
-- and stores the new decision parameters in configuration tables without retraining a model.

CREATE TABLE IF NOT EXISTS agent_prompt_template_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_role VARCHAR(64) NOT NULL,
    template_key VARCHAR(100) NOT NULL,
    prompt_template TEXT NOT NULL,
    sample_window_days INT NOT NULL DEFAULT 30,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    update_reason VARCHAR(500) DEFAULT NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_agent_prompt_template_role_key UNIQUE (agent_role, template_key)
);

CREATE TABLE IF NOT EXISTS dispatch_weight_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL,
    rating_weight DECIMAL(6,4) NOT NULL,
    workload_weight DECIMAL(6,4) NOT NULL,
    experience_weight DECIMAL(6,4) NOT NULL,
    fatigue_penalty_weight DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
    sample_window_days INT NOT NULL DEFAULT 30,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    update_reason VARCHAR(500) DEFAULT NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_dispatch_weight_config_key UNIQUE (config_key)
);

INSERT INTO agent_prompt_template_config (
    agent_role, template_key, prompt_template, sample_window_days, enabled, update_reason, update_time
)
SELECT
    'PRE_DIAG',
    'default',
    '你是一个预诊客服。请从车主描述中提取核心症状、疑似部位、触发条件，并结合近期低分反馈强调遗漏症状追问。',
    30,
    1,
    '反馈样本聚合后生成的预诊 Agent 提示词模板配置。',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM agent_prompt_template_config
    WHERE agent_role = 'PRE_DIAG' AND template_key = 'default'
);

INSERT INTO agent_prompt_template_config (
    agent_role, template_key, prompt_template, sample_window_days, enabled, update_reason, update_time
)
SELECT
    'ARBITRATOR',
    'default',
    '你是总控车间主任。请综合多 Agent 结论，并根据近期反馈中的误判标签补充风险兜底和人工复核建议。',
    30,
    1,
    '反馈样本聚合后生成的仲裁 Agent 提示词模板配置。',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM agent_prompt_template_config
    WHERE agent_role = 'ARBITRATOR' AND template_key = 'default'
);

INSERT INTO dispatch_weight_config (
    config_key, rating_weight, workload_weight, experience_weight, fatigue_penalty_weight,
    sample_window_days, enabled, update_reason, update_time
)
SELECT
    'default',
    0.5000,
    0.3000,
    0.2000,
    0.0000,
    30,
    1,
    '反馈样本聚合后生成的派单权重参数配置。',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM dispatch_weight_config
    WHERE config_key = 'default'
);
