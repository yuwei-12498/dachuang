START TRANSACTION;
INSERT INTO trip_user (username, password_hash, password_salt, nickname, role, status, create_time, update_time)
VALUES ('admin', '$2a$12$uLxXu8cP4ztSCIYh/Dkei.IIC5c3p6GwE/lud7PqOzkxEWdjYWCou', 'BCRYPT', '系统管理员', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  password_salt = VALUES(password_salt),
  role = 1,
  status = 1,
  update_time = NOW();

UPDATE trip_user
SET role = 0,
    update_time = NOW()
WHERE username <> 'admin'
  AND role = 1;
COMMIT;
