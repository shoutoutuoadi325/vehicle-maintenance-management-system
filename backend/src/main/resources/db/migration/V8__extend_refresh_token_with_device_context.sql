ALTER TABLE auth_refresh_token
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS device_name VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS last_active_at DATETIME NULL;

CREATE INDEX IF NOT EXISTS idx_auth_refresh_device ON auth_refresh_token(device_id);
