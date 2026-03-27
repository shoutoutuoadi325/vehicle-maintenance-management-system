-- O2O 核销闭环：记录核销门店与技师

ALTER TABLE user_coupon_wallet
    ADD COLUMN IF NOT EXISTS redeem_shop_id BIGINT NULL;

ALTER TABLE user_coupon_wallet
    ADD COLUMN IF NOT EXISTS redeem_technician_id BIGINT NULL;

ALTER TABLE user_coupon_wallet
    ADD CONSTRAINT fk_wallet_redeem_shop FOREIGN KEY (redeem_shop_id) REFERENCES brand_partner(id);

ALTER TABLE user_coupon_wallet
    ADD CONSTRAINT fk_wallet_redeem_technician FOREIGN KEY (redeem_technician_id) REFERENCES technician(id);

CREATE INDEX idx_wallet_redeem_shop ON user_coupon_wallet(redeem_shop_id);
CREATE INDEX idx_wallet_redeem_technician ON user_coupon_wallet(redeem_technician_id);
