CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    owner_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    ticket_sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE projects
    ADD CONSTRAINT uk_projects_code UNIQUE (code);

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_owner
    FOREIGN KEY (owner_id) REFERENCES users(id);

CREATE INDEX idx_projects_owner_id ON projects(owner_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_name ON projects(name);