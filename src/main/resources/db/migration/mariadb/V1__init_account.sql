CREATE TABLE account (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    role       VARCHAR(30)  NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_account_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO account (username, password, role, enabled, created_at)
VALUES ('admin', '$2y$10$69cTX9fDoWkT9q1drXwByOJ8vxH4x13hm.WwGUgMyrHJHov4/w7rK', 'ADMIN', TRUE, CURRENT_TIMESTAMP);
