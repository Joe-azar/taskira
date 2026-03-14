CREATE TABLE ticket_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    changed_by BIGINT NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    old_value VARCHAR(4000),
    new_value VARCHAR(4000),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ticket_history
    ADD CONSTRAINT fk_ticket_history_ticket
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE ticket_history
    ADD CONSTRAINT fk_ticket_history_changed_by
    FOREIGN KEY (changed_by) REFERENCES users(id);

CREATE INDEX idx_ticket_history_ticket_id ON ticket_history(ticket_id);
CREATE INDEX idx_ticket_history_changed_by ON ticket_history(changed_by);
CREATE INDEX idx_ticket_history_changed_at ON ticket_history(changed_at);