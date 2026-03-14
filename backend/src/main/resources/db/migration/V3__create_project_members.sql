CREATE TABLE project_members (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    project_role VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE project_members
    ADD CONSTRAINT uk_project_members_project_user UNIQUE (project_id, user_id);

ALTER TABLE project_members
    ADD CONSTRAINT fk_project_members_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

ALTER TABLE project_members
    ADD CONSTRAINT fk_project_members_user
    FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_project_members_project_id ON project_members(project_id);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);