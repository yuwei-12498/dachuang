USE `city_trip_db`;

ALTER TABLE `poi`
    ADD INDEX `idx_poi_planning_status_priority` (`temporarily_closed`, `priority_score`, `status_updated_at`, `id`);

ALTER TABLE `poi`
    ADD INDEX `idx_poi_planning_rain` (`temporarily_closed`, `indoor`, `rain_friendly`, `priority_score`, `id`);

