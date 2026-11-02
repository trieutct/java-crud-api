-- Schema ban đầu, khớp với những gì Hibernate đã sinh từ Entity User
CREATE TABLE users (
    id            INT NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(20),
    status        ENUM('ACTIVE', 'INACTIVE') NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;
