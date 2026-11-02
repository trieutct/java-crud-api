-- Thông tin ảnh đã upload, file thật nằm trên ổ đĩa server ở thư mục upload.dir
CREATE TABLE images (
    id            INT NOT NULL AUTO_INCREMENT,
    original_name VARCHAR(255) NOT NULL,
    file_path     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(50) NOT NULL,
    file_size     BIGINT NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_images_file_path UNIQUE (file_path)
) ENGINE = InnoDB;
