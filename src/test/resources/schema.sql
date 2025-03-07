CREATE TABLE IF NOT EXISTS media (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     file_name VARCHAR(255) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    tag VARCHAR(255) NOT NULL,
    entity_type ENUM('CHALLENGE', 'THEME') NOT NULL,
    media_type ENUM('AUDIO', 'IMAGE', 'VIDEO') NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_by BIGINT NOT NULL
    );
