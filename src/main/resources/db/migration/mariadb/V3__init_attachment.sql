CREATE TABLE attachment (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id        BIGINT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(100) NOT NULL,
    content_type      VARCHAR(150),
    file_size         BIGINT       NOT NULL,
    upload_token      VARCHAR(64),
    created_at        TIMESTAMP    NOT NULL,
    INDEX idx_attachment_article (article_id),
    INDEX idx_attachment_token (upload_token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
