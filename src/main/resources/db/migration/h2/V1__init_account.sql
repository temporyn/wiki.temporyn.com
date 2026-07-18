CREATE TABLE account (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    role       VARCHAR(30)  NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO account (username, password, role, enabled, created_at)
VALUES ('admin', '$2y$10$69cTX9fDoWkT9q1drXwByOJ8vxH4x13hm.WwGUgMyrHJHov4/w7rK', 'ADMIN', TRUE, CURRENT_TIMESTAMP);
