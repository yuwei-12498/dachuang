USE `city_trip_db`;

DROP PROCEDURE IF EXISTS AddColumnIfMissing;
DELIMITER //
CREATE PROCEDURE AddColumnIfMissing(IN tableName VARCHAR(64), IN columnName VARCHAR(64), IN columnDef VARCHAR(255))
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND COLUMN_NAME = columnName
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDef);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL AddColumnIfMissing('saved_itinerary', 'share_note', 'VARCHAR(300) NULL AFTER `custom_title`');

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

CALL AddColumnIfMissing('community_comment', 'parent_id', 'BIGINT NULL AFTER `itinerary_id`');

CREATE TABLE IF NOT EXISTS `community_like` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `itinerary_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_community_like_user_itinerary` (`itinerary_id`, `user_id`),
  KEY `idx_community_like_user` (`user_id`)
) ENGINE=InnoDB COMMENT='Community itinerary likes';

DROP PROCEDURE IF EXISTS AddColumnIfMissing;
