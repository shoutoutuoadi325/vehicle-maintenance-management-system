INSERT INTO journey_map (id, map_name, enabled, update_time)
VALUES (1, 'Test Route', TRUE, CURRENT_TIMESTAMP());

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
VALUES (1, 0, 'Start City', 0, 0, 0, CURRENT_TIMESTAMP());
