USE `city_trip_db`;

-- From upgrade_dynamic_itinerary.sql
ALTER TABLE `poi`
    ADD COLUMN `closed_weekdays` VARCHAR(100) NULL AFTER `close_time`,
    ADD COLUMN `temporarily_closed` TINYINT(1) NOT NULL DEFAULT 0 AFTER `closed_weekdays`,
    ADD COLUMN `status_note` VARCHAR(255) NULL AFTER `temporarily_closed`,
    ADD COLUMN `status_source` VARCHAR(50) NOT NULL DEFAULT 'seed' AFTER `status_note`,
    ADD COLUMN `status_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `status_source`;

UPDATE `poi` SET `open_time` = COALESCE(`open_time`, '09:00:00'),
                 `close_time` = COALESCE(`close_time`, '18:00:00'),
                 `status_source` = COALESCE(NULLIF(`status_source`, ''), 'seed'),
                 `status_updated_at` = COALESCE(`status_updated_at`, NOW());

CREATE TABLE IF NOT EXISTS `saved_itinerary` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `request_json` LONGTEXT NOT NULL,
  `itinerary_json` LONGTEXT NOT NULL,
  `custom_title` VARCHAR(120) NULL,
  `is_public` TINYINT(1) NOT NULL DEFAULT 0,
  `node_count` INT NOT NULL DEFAULT 0,
  `total_duration` INT NULL,
  `total_cost` DECIMAL(10, 2) NULL,
  `route_signature` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_saved_itinerary_user` (`user_id`)
) ENGINE=InnoDB COMMENT='Saved itinerary snapshots';

-- From upgrade_admin.sql
ALTER TABLE `trip_user` 
  ADD COLUMN `role` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户, 1-管理员' AFTER `nickname`,
  ADD COLUMN `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1-正常, 0-冻结封禁' AFTER `role`;

UPDATE `trip_user` SET `role` = 1 ORDER BY `id` ASC LIMIT 1;

ALTER TABLE `poi`
    ADD COLUMN `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN `created_by` BIGINT NULL,
    ADD COLUMN `updated_by` BIGINT NULL;

-- From upgrade_itinerary_custom_title.sql
ALTER TABLE `saved_itinerary` 
    MODIFY COLUMN `custom_title` VARCHAR(120) NULL DEFAULT NULL;

ALTER TABLE `saved_itinerary`
    ADD COLUMN `is_public` TINYINT(1) NOT NULL DEFAULT 0;

-- From upgrade_itinerary_history_favorite.sql
ALTER TABLE `saved_itinerary`
    ADD COLUMN `is_favorite` TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN `is_history` TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN `source_page` VARCHAR(50) NULL COMMENT '生成来源页面标识';
-- From upgrade_community_social.sql
ALTER TABLE `saved_itinerary`
    ADD COLUMN `share_note` VARCHAR(300) NULL AFTER `custom_title`;

CREATE TABLE IF NOT EXISTS `community_comment` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `itinerary_id` BIGINT NOT NULL,
  `parent_id` BIGINT NULL,
  `user_id` BIGINT NOT NULL,
  `content` VARCHAR(300) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_community_comment_itinerary` (`itinerary_id`),
  KEY `idx_community_comment_parent` (`parent_id`),
  KEY `idx_community_comment_user` (`user_id`)
) ENGINE=InnoDB COMMENT='Community itinerary comments';

CREATE TABLE IF NOT EXISTS `community_like` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `itinerary_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_community_like_user_itinerary` (`itinerary_id`, `user_id`),
  KEY `idx_community_like_user` (`user_id`)
) ENGINE=InnoDB COMMENT='Community itinerary likes';
