-- 动态多地图：路线表、节点表、用户当前路线与节点状态扩展

CREATE TABLE IF NOT EXISTS journey_map (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    map_name VARCHAR(120) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_journey_map_name UNIQUE (map_name)
);

CREATE TABLE IF NOT EXISTS journey_map_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    map_id BIGINT NOT NULL,
    city_index INT NOT NULL,
    city_name VARCHAR(120) NOT NULL,
    required_mileage INT NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_journey_map_node UNIQUE (map_id, city_index),
    CONSTRAINT fk_journey_map_node_map FOREIGN KEY (map_id) REFERENCES journey_map(id)
);

INSERT INTO journey_map (id, map_name, enabled, update_time)
SELECT 1, '318川藏线', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map WHERE id = 1);

INSERT INTO journey_map (id, map_name, enabled, update_time)
SELECT 2, '海南环岛线', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map WHERE id = 2);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 1, 0, '成都', 0, 70, 470, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 1 AND city_index = 0);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 1, 1, '康定', 120, 285, 345, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 1 AND city_index = 1);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 1, 2, '理塘', 260, 470, 270, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 1 AND city_index = 2);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 1, 3, '林芝', 420, 680, 195, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 1 AND city_index = 3);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 1, 4, '拉萨', 580, 940, 95, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 1 AND city_index = 4);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 2, 0, '海口', 0, 90, 430, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 2 AND city_index = 0);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 2, 1, '文昌', 95, 260, 320, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 2 AND city_index = 1);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 2, 2, '三亚', 230, 540, 430, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 2 AND city_index = 2);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 2, 3, '儋州', 360, 760, 280, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 2 AND city_index = 3);

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
SELECT 2, 4, '海口', 500, 920, 430, NOW()
WHERE NOT EXISTS (SELECT 1 FROM journey_map_node WHERE map_id = 2 AND city_index = 4);

ALTER TABLE green_energy_account
    ADD COLUMN IF NOT EXISTS current_map_id BIGINT NULL;

UPDATE green_energy_account
SET current_map_id = 1
WHERE current_map_id IS NULL;

ALTER TABLE green_energy_account
    MODIFY COLUMN current_map_id BIGINT NOT NULL;

ALTER TABLE green_energy_account
    ADD CONSTRAINT fk_green_energy_current_map FOREIGN KEY (current_map_id) REFERENCES journey_map(id);

ALTER TABLE green_journey_node_state
    ADD COLUMN IF NOT EXISTS map_id BIGINT NULL;

UPDATE green_journey_node_state
SET map_id = 1
WHERE map_id IS NULL;

ALTER TABLE green_journey_node_state
    MODIFY COLUMN map_id BIGINT NOT NULL;

ALTER TABLE green_journey_node_state
    ADD CONSTRAINT fk_green_journey_map FOREIGN KEY (map_id) REFERENCES journey_map(id);

ALTER TABLE green_journey_node_state
    DROP INDEX uk_green_journey_user_city;

ALTER TABLE green_journey_node_state
    ADD CONSTRAINT uk_green_journey_user_map_city UNIQUE (user_id, map_id, city_index);

CREATE INDEX idx_green_journey_user_map ON green_journey_node_state(user_id, map_id);
