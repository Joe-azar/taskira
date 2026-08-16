CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_ticket
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_uploader
    FOREIGN KEY (uploader_id) REFERENCES users(id);

ALTER TABLE attachments
    ADD CONSTRAINT uk_attachments_storage_key UNIQUE (storage_key);

CREATE INDEX idx_attachments_ticket_id ON attachments(ticket_id);
CREATE INDEX idx_attachments_uploader_id ON attachments(uploader_id);
