USE `city_trip_db`;

DROP FUNCTION IF EXISTS repair_mojibake_utf8;

DELIMITER //

CREATE FUNCTION repair_mojibake_utf8(inputText LONGTEXT)
RETURNS LONGTEXT
DETERMINISTIC
BEGIN
    DECLARE hexText LONGTEXT;
    DECLARE outHex LONGTEXT DEFAULT '';
    DECLARE idx BIGINT DEFAULT 1;
    DECLARE prefixHex CHAR(2);
    DECLARE secondHex CHAR(2);
    DECLARE secondVal INT;

    IF inputText IS NULL THEN
        RETURN NULL;
    END IF;

    SET hexText = HEX(CONVERT(inputText USING utf8mb4));

    WHILE idx <= CHAR_LENGTH(hexText) DO
        SET prefixHex = SUBSTRING(hexText, idx, 2);

        IF prefixHex = 'C2' AND idx + 3 <= CHAR_LENGTH(hexText) THEN
            SET secondHex = SUBSTRING(hexText, idx + 2, 2);
            SET outHex = CONCAT(outHex, secondHex);
            SET idx = idx + 4;
        ELSEIF prefixHex = 'C3' AND idx + 3 <= CHAR_LENGTH(hexText) THEN
            SET secondHex = SUBSTRING(hexText, idx + 2, 2);
            SET secondVal = CONV(secondHex, 16, 10) + 64;
            SET outHex = CONCAT(outHex, LPAD(HEX(secondVal), 2, '0'));
            SET idx = idx + 4;
        ELSE
            SET outHex = CONCAT(outHex, prefixHex);
            SET idx = idx + 2;
        END IF;
    END WHILE;

    RETURN CONVERT(UNHEX(outHex) USING utf8mb4);
END //

DELIMITER ;

CREATE TABLE IF NOT EXISTS `backup_saved_itinerary_mojibake_20260418` LIKE `saved_itinerary`;
INSERT INTO `backup_saved_itinerary_mojibake_20260418`
SELECT s.*
FROM `saved_itinerary` s
WHERE s.`itinerary_json` IS NOT NULL
  AND s.`itinerary_json` <> repair_mojibake_utf8(s.`itinerary_json`)
  AND NOT EXISTS (
      SELECT 1
      FROM `backup_saved_itinerary_mojibake_20260418` b
      WHERE b.`id` = s.`id`
  );

CREATE TABLE IF NOT EXISTS `backup_route_node_fact_mojibake_20260418` LIKE `route_node_fact`;
INSERT INTO `backup_route_node_fact_mojibake_20260418`
SELECT r.*
FROM `route_node_fact` r
WHERE r.`sys_reason` IS NOT NULL
  AND r.`sys_reason` <> repair_mojibake_utf8(r.`sys_reason`)
  AND NOT EXISTS (
      SELECT 1
      FROM `backup_route_node_fact_mojibake_20260418` b
      WHERE b.`id` = r.`id`
  );

UPDATE `saved_itinerary`
SET `itinerary_json` = repair_mojibake_utf8(`itinerary_json`)
WHERE `itinerary_json` IS NOT NULL
  AND `itinerary_json` <> repair_mojibake_utf8(`itinerary_json`);
SET @saved_itinerary_repaired = ROW_COUNT();

UPDATE `route_node_fact`
SET `sys_reason` = repair_mojibake_utf8(`sys_reason`)
WHERE `sys_reason` IS NOT NULL
  AND `sys_reason` <> repair_mojibake_utf8(`sys_reason`);
SET @route_node_reason_repaired = ROW_COUNT();

SELECT 'saved_itinerary' AS target_table,
       @saved_itinerary_repaired AS repaired_rows,
       (SELECT COUNT(*) FROM `backup_saved_itinerary_mojibake_20260418`) AS backup_rows
UNION ALL
SELECT 'route_node_fact' AS target_table,
       @route_node_reason_repaired AS repaired_rows,
       (SELECT COUNT(*) FROM `backup_route_node_fact_mojibake_20260418`) AS backup_rows;

DROP FUNCTION IF EXISTS repair_mojibake_utf8;
