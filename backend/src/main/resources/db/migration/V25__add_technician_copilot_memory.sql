-- Persist per-technician AI Copilot context so each technician receives continuity across difficult diagnoses.

CREATE TABLE IF NOT EXISTS technician_copilot_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    technician_id BIGINT NOT NULL,
    diagnosis_count INT NOT NULL DEFAULT 0,
    last_problem_text TEXT DEFAULT NULL,
    last_fault_type VARCHAR(255) DEFAULT NULL,
    last_suggestion TEXT DEFAULT NULL,
    last_history_case_summary TEXT DEFAULT NULL,
    last_confidence DOUBLE DEFAULT NULL,
    last_workflow_status VARCHAR(32) DEFAULT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_technician_copilot_memory_technician UNIQUE (technician_id),
    CONSTRAINT fk_technician_copilot_memory_technician
        FOREIGN KEY (technician_id) REFERENCES technician(id)
        ON DELETE CASCADE
);
