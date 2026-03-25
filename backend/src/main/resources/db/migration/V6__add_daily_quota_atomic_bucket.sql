CREATE TABLE IF NOT EXISTS green_daily_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    quota_date DATE NOT NULL,
    used_energy INT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_green_daily_quota_user_date UNIQUE (user_id, quota_date),
    CONSTRAINT fk_green_daily_quota_user FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_green_daily_quota_date (quota_date)
);
