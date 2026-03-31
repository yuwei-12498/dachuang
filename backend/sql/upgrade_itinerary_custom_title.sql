USE `city_trip_db`;

ALTER TABLE `saved_itinerary`
    ADD COLUMN `custom_title` VARCHAR(120) NULL AFTER `itinerary_json`;
