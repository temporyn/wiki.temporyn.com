CREATE TABLE attachment (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    article_id        BIGINT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(100) NOT NULL,
    content_type      VARCHAR(150),
    file_size         BIGINT       NOT NULL,
    upload_token      VARCHAR(64),
    created_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_attachment_article ON attachment (article_id);
CREATE INDEX idx_attachment_token ON attachment (upload_token);
