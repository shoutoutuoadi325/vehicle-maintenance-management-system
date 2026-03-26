CREATE TABLE IF NOT EXISTS journey_completion_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    completed_at DATETIME NOT NULL,
    grand_prize_granted TINYINT(1) NOT NULL DEFAULT 0,
    sticker_claimed TINYINT(1) NOT NULL DEFAULT 0,
    consignee_name VARCHAR(100) NULL,
    consignee_phone VARCHAR(30) NULL,
    shipping_address VARCHAR(500) NULL,
    shipping_status VARCHAR(30) NOT NULL DEFAULT 'NOT_CLAIMED',
    shipment_tracking_no VARCHAR(80) NULL,
    shipped_at DATETIME NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_journey_completion_user UNIQUE (user_id),
    CONSTRAINT fk_journey_completion_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_journey_completion_status ON journey_completion_record(shipping_status);
