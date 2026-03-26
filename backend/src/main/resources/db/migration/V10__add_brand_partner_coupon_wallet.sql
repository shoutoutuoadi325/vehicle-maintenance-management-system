-- 品牌合作方
CREATE TABLE IF NOT EXISTS brand_partner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand_name VARCHAR(120) NOT NULL,
    brand_code VARCHAR(60) NOT NULL,
    logo_url VARCHAR(500) NULL,
    description VARCHAR(500) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_brand_partner_code UNIQUE (brand_code)
);

-- 城市专属优惠券/福利
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand_partner_id BIGINT NOT NULL,
    city_index INT NOT NULL,
    coupon_title VARCHAR(255) NOT NULL,
    coupon_description VARCHAR(1000) NULL,
    win_probability DECIMAL(6,4) NOT NULL DEFAULT 0.2000,
    stock INT NULL,
    total_issued INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    expire_time DATETIME NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT fk_coupon_brand_partner FOREIGN KEY (brand_partner_id) REFERENCES brand_partner(id)
);

CREATE INDEX idx_coupon_city_enabled ON coupon(city_index, enabled);
CREATE INDEX idx_coupon_brand ON coupon(brand_partner_id);

-- 用户卡券包
CREATE TABLE IF NOT EXISTS user_coupon_wallet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    brand_partner_id BIGINT NOT NULL,
    city_index INT NOT NULL,
    coupon_title VARCHAR(255) NOT NULL,
    coupon_description VARCHAR(1000) NULL,
    coupon_status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    source_action VARCHAR(120) NOT NULL,
    draw_time DATETIME NOT NULL,
    expire_time DATETIME NULL,
    redeem_time DATETIME NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT fk_wallet_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(id),
    CONSTRAINT fk_wallet_brand_partner FOREIGN KEY (brand_partner_id) REFERENCES brand_partner(id),
    CONSTRAINT uk_wallet_source UNIQUE (source_action)
);

CREATE INDEX idx_wallet_user_status ON user_coupon_wallet(user_id, coupon_status);
CREATE INDEX idx_wallet_city ON user_coupon_wallet(city_index);

-- 品牌方初始化
INSERT INTO brand_partner (brand_name, brand_code, logo_url, description, enabled, update_time)
SELECT '壳牌', 'SHELL', '/assets/brands/shell.svg', '机油与车辆养护合作品牌', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM brand_partner WHERE brand_code = 'SHELL');

INSERT INTO brand_partner (brand_name, brand_code, logo_url, description, enabled, update_time)
SELECT '博世车联', 'BOSCH', '/assets/brands/bosch.svg', '智能检测与维修服务合作品牌', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM brand_partner WHERE brand_code = 'BOSCH');

INSERT INTO brand_partner (brand_name, brand_code, logo_url, description, enabled, update_time)
SELECT '米其林', 'MICHELIN', '/assets/brands/michelin.svg', '轮胎与道路安全合作品牌', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM brand_partner WHERE brand_code = 'MICHELIN');

-- 城市专属券初始化（每城至少一个）
INSERT INTO coupon (brand_partner_id, city_index, coupon_title, coupon_description, win_probability, stock, total_issued, enabled, expire_time, update_time)
SELECT bp.id, 0, '壳牌机油免单一次', '成都服务区可兑换壳牌全合成机油保养一次', 0.1800, 500, 0, 1, DATE_ADD(NOW(), INTERVAL 180 DAY), NOW()
FROM brand_partner bp
WHERE bp.brand_code = 'SHELL'
  AND NOT EXISTS (SELECT 1 FROM coupon c WHERE c.city_index = 0 AND c.coupon_title = '壳牌机油免单一次');

INSERT INTO coupon (brand_partner_id, city_index, coupon_title, coupon_description, win_probability, stock, total_issued, enabled, expire_time, update_time)
SELECT bp.id, 1, '博世高原检测8折券', '康定服务区可用的高原专项检测折扣券', 0.2200, 400, 0, 1, DATE_ADD(NOW(), INTERVAL 180 DAY), NOW()
FROM brand_partner bp
WHERE bp.brand_code = 'BOSCH'
  AND NOT EXISTS (SELECT 1 FROM coupon c WHERE c.city_index = 1 AND c.coupon_title = '博世高原检测8折券');

INSERT INTO coupon (brand_partner_id, city_index, coupon_title, coupon_description, win_probability, stock, total_issued, enabled, expire_time, update_time)
SELECT bp.id, 2, '米其林滤芯护理礼包', '理塘服务区可领取空调滤芯检测与护理礼包', 0.2600, 350, 0, 1, DATE_ADD(NOW(), INTERVAL 180 DAY), NOW()
FROM brand_partner bp
WHERE bp.brand_code = 'MICHELIN'
  AND NOT EXISTS (SELECT 1 FROM coupon c WHERE c.city_index = 2 AND c.coupon_title = '米其林滤芯护理礼包');

INSERT INTO coupon (brand_partner_id, city_index, coupon_title, coupon_description, win_probability, stock, total_issued, enabled, expire_time, update_time)
SELECT bp.id, 3, '博世雨季制动保养券', '林芝服务区制动系统检测与保养抵扣券', 0.2000, 300, 0, 1, DATE_ADD(NOW(), INTERVAL 180 DAY), NOW()
FROM brand_partner bp
WHERE bp.brand_code = 'BOSCH'
  AND NOT EXISTS (SELECT 1 FROM coupon c WHERE c.city_index = 3 AND c.coupon_title = '博世雨季制动保养券');

INSERT INTO coupon (brand_partner_id, city_index, coupon_title, coupon_description, win_probability, stock, total_issued, enabled, expire_time, update_time)
SELECT bp.id, 4, '壳牌高原冬季养护券', '拉萨服务区冬季机油与电瓶健康检查专属福利', 0.2400, 280, 0, 1, DATE_ADD(NOW(), INTERVAL 180 DAY), NOW()
FROM brand_partner bp
WHERE bp.brand_code = 'SHELL'
  AND NOT EXISTS (SELECT 1 FROM coupon c WHERE c.city_index = 4 AND c.coupon_title = '壳牌高原冬季养护券');
