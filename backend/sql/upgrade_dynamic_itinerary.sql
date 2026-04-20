USE `city_trip_db`;



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

UPDATE `poi` SET `open_time` = '10:00:00', `close_time` = '22:30:00' WHERE `id` IN (2, 4, 6, 7, 12, 20);
UPDATE `poi` SET `open_time` = '08:00:00', `close_time` = '17:30:00' WHERE `id` IN (8, 14);
UPDATE `poi` SET `open_time` = '08:30:00', `close_time` = '17:30:00' WHERE `id` = 9;
UPDATE `poi` SET `open_time` = '11:00:00', `close_time` = '23:00:00' WHERE `id` = 10;
UPDATE `poi` SET `open_time` = '18:00:00', `close_time` = '23:30:00' WHERE `id` = 11;
UPDATE `poi` SET `open_time` = '09:30:00', `close_time` = '22:00:00' WHERE `id` = 17;
UPDATE `poi` SET `open_time` = '06:00:00', `close_time` = '22:00:00' WHERE `id` = 18;
UPDATE `poi` SET `closed_weekdays` = 'MONDAY', `status_note` = 'Closed on Mondays except public holidays.' WHERE `id` IN (5, 19);

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
