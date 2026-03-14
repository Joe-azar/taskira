CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(30) NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    creator_id BIGINT NOT NULL,
    assignee_id BIGINT,
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tickets
    ADD CONSTRAINT uk_tickets_reference UNIQUE (reference);

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_creator
    FOREIGN KEY (creator_id) REFERENCES users(id);

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_assignee
    FOREIGN KEY (assignee_id) REFERENCES users(id);

CREATE INDEX idx_tickets_project_id ON tickets(project_id);
CREATE INDEX idx_tickets_creator_id ON tickets(creator_id);
CREATE INDEX idx_tickets_assignee_id ON tickets(assignee_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_priority ON tickets(priority);
CREATE INDEX idx_tickets_type ON tickets(type);