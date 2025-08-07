INSERT IGNORE INTO roles (role_name, description, created_at, updated_at)
VALUES
    ('ROLE_USER',  '일반 사용자', NOW(), NOW()),
    ('ROLE_ADMIN', '관리자',    NOW(), NOW());