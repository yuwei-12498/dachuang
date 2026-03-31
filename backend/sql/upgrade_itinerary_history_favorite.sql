USE `city_trip_db`;

ALTER TABLE `saved_itinerary`
    ADD COLUMN  `favorited` TINYINT(1) NOT NULL DEFAULT 0 AFTER `itinerary_json`,
    ADD COLUMN  `favorite_time` DATETIME NULL AFTER `favorited`;

UPDATE `saved_itinerary`
SET `favorited` = COALESCE(`favorited`, 0);
