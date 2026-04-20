ALTER TABLE `saved_itinerary`
    ADD COLUMN `is_public` TINYINT(1) NOT NULL DEFAULT 0 AFTER `favorite_time`;
