CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP NOT NULL,
    actor_id BIGINT,
    actor_email VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(50) NOT NULL,
    detail VARCHAR(2000),
    request_id VARCHAR(64),
    ip_address VARCHAR(45)
);

ALTER TABLE audit_events
    ADD CONSTRAINT fk_audit_events_actor
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_audit_events_occurred_at ON audit_events(occurred_at);
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_events_actor_id ON audit_events(actor_id);
CREATE INDEX idx_audit_events_request_id ON audit_events(request_id);
