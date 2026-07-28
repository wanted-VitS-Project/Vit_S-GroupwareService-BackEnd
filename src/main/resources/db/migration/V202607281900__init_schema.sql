CREATE TABLE task (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      title VARCHAR(255) NOT NULL,
                      status VARCHAR(20) NOT NULL DEFAULT 'TODO',
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);