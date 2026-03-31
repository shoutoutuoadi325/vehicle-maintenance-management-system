-- 旅行日记/动态时间轨

CREATE TABLE IF NOT EXISTS journey_footprint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    map_id BIGINT NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    event_description VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_footprint_user FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT fk_footprint_map FOREIGN KEY (map_id) REFERENCES journey_map(id)
);

CREATE INDEX idx_footprint_user_created ON journey_footprint(user_id, created_at DESC);
CREATE INDEX idx_footprint_map_created ON journey_footprint(map_id, created_at DESC);
