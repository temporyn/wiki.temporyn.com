CREATE TABLE directory (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    parent_id  BIGINT,
    sort_order INT         NOT NULL DEFAULT 0
);

CREATE TABLE article (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    directory_id BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    content      VARCHAR(4000) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0
);

INSERT INTO directory (id, name, parent_id, sort_order) VALUES
    (1, 'temp',        NULL, 1),
    (2, 'temp-group',  NULL, 2),
    (3, 'temp-nested', 2,    1),
    (4, 'temp-etc',    NULL, 3);

INSERT INTO article (id, directory_id, title, content, sort_order) VALUES
    (1, 1, 'temp-01', '## 개요

임시 더미 문서: `temp/temp-01`

실제 내용은 데이터베이스에서 **Markdown** 으로 저장되고, 조회 시 HTML 로 렌더링됩니다.

- 더미 항목 1
- 더미 항목 2', 1),
    (2, 1, 'temp-02', '## 개요

임시 더미 문서: `temp/temp-02`

실제 내용은 데이터베이스에서 제공됩니다.', 2),
    (3, 1, 'temp-03', '## 개요

임시 더미 문서: `temp/temp-03`

실제 내용은 데이터베이스에서 제공됩니다.', 3),
    (4, 1, 'temp-04', '## 개요

임시 더미 문서: `temp/temp-04`

실제 내용은 데이터베이스에서 제공됩니다.', 4),
    (5, 2, 'temp-01', '## 개요

임시 더미 문서: `temp-group/temp-01`

실제 내용은 데이터베이스에서 제공됩니다.', 1),
    (6, 2, 'temp-02', '## 개요

임시 더미 문서: `temp-group/temp-02`

실제 내용은 데이터베이스에서 제공됩니다.', 2),
    (7, 3, 'temp-01', '## 개요

임시 더미 문서: `temp-group/temp-nested/temp-01`

다중 깊이 디렉토리 하위 문서입니다.', 1),
    (8, 3, 'temp-02', '## 개요

임시 더미 문서: `temp-group/temp-nested/temp-02`

다중 깊이 디렉토리 하위 문서입니다.', 2),
    (9, 4, 'temp-01', '## 개요

임시 더미 문서: `temp-etc/temp-01`

실제 내용은 데이터베이스에서 제공됩니다.', 1),
    (10, 4, 'temp-02', '## 개요

임시 더미 문서: `temp-etc/temp-02`

실제 내용은 데이터베이스에서 제공됩니다.', 2),
    (11, 4, 'temp-03', '## 개요

임시 더미 문서: `temp-etc/temp-03`

실제 내용은 데이터베이스에서 제공됩니다.', 3);
