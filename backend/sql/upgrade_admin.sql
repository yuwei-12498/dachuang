USE `city_trip_db`;

ALTER TABLE `trip_user` 
  ADD COLUMN `role` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户, 1-管理员' AFTER `nickname`,
  ADD COLUMN `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1-正常, 0-冻结封禁' AFTER `role`;

-- 为了方便验证，我们把 ID 最小的用户（往往是第一个注册的测试账号）升级为 admin
UPDATE `trip_user` SET `role` = 1 ORDER BY `id` ASC LIMIT 1;
