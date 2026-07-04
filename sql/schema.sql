CREATE DATABASE IF NOT EXISTS short_link DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE short_link;

CREATE TABLE IF NOT EXISTS short_link (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键，用于生成短码',
    short_code   VARCHAR(8)   NULL UNIQUE                 COMMENT '6位短码，Base62编码结果',
    original_url VARCHAR(512) NOT NULL                    COMMENT '原始长链接',
    visit_count  INT          DEFAULT 0                   COMMENT '累计访问次数',
    expire_time  DATETIME     DEFAULT NULL                COMMENT '过期时间，NULL表示永久有效',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_short_code (short_code),
    INDEX idx_original_url (original_url(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接映射表';
