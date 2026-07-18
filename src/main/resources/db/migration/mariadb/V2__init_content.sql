CREATE TABLE directory (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    parent_id  BIGINT,
    sort_order INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_directory_parent FOREIGN KEY (parent_id) REFERENCES directory (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE article (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    directory_id BIGINT        NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    content      VARCHAR(4000) NOT NULL,
    sort_order   INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_article_directory FOREIGN KEY (directory_id) REFERENCES directory (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO directory (id, name, parent_id, sort_order) VALUES
    (1, 'temp',        NULL, 1),
    (2, 'temp-group',  NULL, 2),
    (3, 'temp-nested', 2,    1),
    (4, 'temp-etc',    NULL, 3);

INSERT INTO article (id, directory_id, title, content, sort_order) VALUES
    (1, 1, 'temp-01', '<p>임시 더미 문서: <code>temp/temp-01</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p><ul><li>더미 항목 1</li><li>더미 항목 2</li></ul>', 1),
    (2, 1, 'temp-02', '<p>임시 더미 문서: <code>temp/temp-02</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 2),
    (3, 1, 'temp-03', '<p>임시 더미 문서: <code>temp/temp-03</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 3),
    (4, 1, 'temp-04', '<p>임시 더미 문서: <code>temp/temp-04</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 4),
    (5, 2, 'temp-01', '<p>임시 더미 문서: <code>temp-group/temp-01</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 1),
    (6, 2, 'temp-02', '<p>임시 더미 문서: <code>temp-group/temp-02</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 2),
    (7, 3, 'temp-01', '<p>임시 더미 문서: <code>temp-group/temp-nested/temp-01</code></p><h2>개요</h2><p>다중 깊이 디렉토리 하위 문서입니다.</p>', 1),
    (8, 3, 'temp-02', '<p>임시 더미 문서: <code>temp-group/temp-nested/temp-02</code></p><h2>개요</h2><p>다중 깊이 디렉토리 하위 문서입니다.</p>', 2),
    (9, 4, 'temp-01', '<p>임시 더미 문서: <code>temp-etc/temp-01</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 1),
    (10, 4, 'temp-02', '<p>임시 더미 문서: <code>temp-etc/temp-02</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 2),
    (11, 4, 'temp-03', '<p>임시 더미 문서: <code>temp-etc/temp-03</code></p><h2>개요</h2><p>실제 내용은 데이터베이스에서 제공됩니다.</p>', 3);
