CREATE TABLE IF NOT EXISTS auth_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    jti VARCHAR(100) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    user_role VARCHAR(40) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_auth_refresh_token_jti UNIQUE (jti),
    INDEX idx_auth_refresh_user (user_id, user_role),
    INDEX idx_auth_refresh_exp (expires_at)
);
