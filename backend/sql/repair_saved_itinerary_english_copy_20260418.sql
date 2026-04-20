SET NAMES utf8mb4;
USE `city_trip_db`;

CREATE TABLE IF NOT EXISTS `backup_saved_itinerary_english_copy_20260418` LIKE `saved_itinerary`;

INSERT INTO `backup_saved_itinerary_english_copy_20260418`
SELECT s.*
FROM `saved_itinerary` s
WHERE (
          s.`itinerary_json` LIKE '%Balanced%'
       OR s.`itinerary_json` LIKE '%Best overall trade-off for preference match and route smoothness%'
       OR s.`itinerary_json` LIKE '%This route keeps preference match%'
       OR s.`itinerary_json` LIKE '%Starts from %'
      )
  AND NOT EXISTS (
      SELECT 1
      FROM `backup_saved_itinerary_english_copy_20260418` b
      WHERE b.`id` = s.`id`
  );

SET @english_saved_rows := (
    SELECT COUNT(*)
    FROM `backup_saved_itinerary_english_copy_20260418`
);

UPDATE `saved_itinerary` s
JOIN `backup_saved_itinerary_english_copy_20260418` b ON b.`id` = s.`id`
SET s.`itinerary_json` = b.`itinerary_json`;

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Balanced', '均衡方案')
WHERE `itinerary_json` LIKE '%Balanced%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Budget', '省钱方案')
WHERE `itinerary_json` LIKE '%Budget%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Efficient', '高效方案')
WHERE `itinerary_json` LIKE '%Efficient%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Stable', '稳妥方案')
WHERE `itinerary_json` LIKE '%Stable%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Explore', '探索方案')
WHERE `itinerary_json` LIKE '%Explore%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Alternative ', '备选方案')
WHERE `itinerary_json` LIKE '%Alternative %';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Best overall trade-off for preference match and route smoothness', '偏好匹配、路线顺滑与可执行性更均衡')
WHERE `itinerary_json` LIKE '%Best overall trade-off for preference match and route smoothness%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Suitable when cost control matters most', '适合预算控制优先的出行场景')
WHERE `itinerary_json` LIKE '%Suitable when cost control matters most%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Prioritizes lower detour and higher time efficiency', '优先减少绕路，整体时间效率更高')
WHERE `itinerary_json` LIKE '%Prioritizes lower detour and higher time efficiency%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Chooses POIs with lower business-status risk', '优先选择营业状态和时间窗风险更低的点位')
WHERE `itinerary_json` LIKE '%Chooses POIs with lower business-status risk%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Covers more highly scored thematic highlights', '更强调主题覆盖和高分亮点探索')
WHERE `itinerary_json` LIKE '%Covers more highly scored thematic highlights%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Offers a different trade-off for side-by-side comparison', '提供另一种可执行取舍，便于横向比较')
WHERE `itinerary_json` LIKE '%Offers a different trade-off for side-by-side comparison%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'highest overall utility', '综合得分最高')
WHERE `itinerary_json` LIKE '%highest overall utility%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'lowest total cost', '总成本最低')
WHERE `itinerary_json` LIKE '%lowest total cost%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'shortest travel time', '路途耗时最短')
WHERE `itinerary_json` LIKE '%shortest travel time%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'best theme concentration', '主题覆盖最集中')
WHERE `itinerary_json` LIKE '%best theme concentration%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'lowest business risk', '营业风险最低')
WHERE `itinerary_json` LIKE '%lowest business risk%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'a different but still executable trade-off', '另一种仍然可执行的取舍')
WHERE `itinerary_json` LIKE '%a different but still executable trade-off%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Best overall balance', '综合最均衡')
WHERE `itinerary_json` LIKE '%Best overall balance%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Lowest budget pressure', '预算压力最低')
WHERE `itinerary_json` LIKE '%Lowest budget pressure%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Shortest travel time', '路途耗时最短')
WHERE `itinerary_json` LIKE '%Shortest travel time%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Strongest theme coverage', '主题覆盖最强')
WHERE `itinerary_json` LIKE '%Strongest theme coverage%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Lowest business risk', '营业风险最低')
WHERE `itinerary_json` LIKE '%Lowest business risk%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Broader district coverage', '区域覆盖更广')
WHERE `itinerary_json` LIKE '%Broader district coverage%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Higher cost', '成本更高')
WHERE `itinerary_json` LIKE '%Higher cost%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Longer travel time', '路途更长')
WHERE `itinerary_json` LIKE '%Longer travel time%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Needs another business-status check', '需再次确认营业状态')
WHERE `itinerary_json` LIKE '%Needs another business-status check%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Lower stop density', '点位密度较低')
WHERE `itinerary_json` LIKE '%Lower stop density%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'This route keeps preference match, travel smoothness, and feasibility in better balance', '这条路线在偏好匹配、通行顺滑度和可执行性之间更均衡')
WHERE `itinerary_json` LIKE '%This route keeps preference match, travel smoothness, and feasibility in better balance%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Its overall score is the strongest among the candidate plans', '它在候选方案中的综合得分最高')
WHERE `itinerary_json` LIKE '%Its overall score is the strongest among the candidate plans%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Its total cost is lower than the other options', '它的总花费低于其他方案')
WHERE `itinerary_json` LIKE '%Its total cost is lower than the other options%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'It spends less time on the road and avoids more detours', '它在路上的耗时更少，绕路情况也更少')
WHERE `itinerary_json` LIKE '%It spends less time on the road and avoids more detours%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'It covers more POIs aligned with your themes', '它覆盖了更多符合你主题偏好的点位')
WHERE `itinerary_json` LIKE '%It covers more POIs aligned with your themes%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'Its business-status and time-window risk is lower', '它的营业状态与时间窗风险更低')
WHERE `itinerary_json` LIKE '%Its business-status and time-window risk is lower%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'It remains a solid and executable option under the current constraints', '在当前约束下，它依然是一条稳妥且可执行的路线')
WHERE `itinerary_json` LIKE '%It remains a solid and executable option under the current constraints%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'If budget is your top concern, this option costs more', '如果你最看重预算，这条方案花费会更高')
WHERE `itinerary_json` LIKE '%If budget is your top concern, this option costs more%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'If you want fewer detours, this option spends longer on the road', '如果你更在意少绕路，这条方案在路上的时间会更长')
WHERE `itinerary_json` LIKE '%If you want fewer detours, this option spends longer on the road%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'If stability matters most, some POIs still need another business-status check', '如果你最看重稳妥性，部分点位仍建议出发前再次确认营业状态')
WHERE `itinerary_json` LIKE '%If stability matters most, some POIs still need another business-status check%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'If thematic purity matters most, other options are more concentrated', '如果你最看重主题纯度，其他方案的主题聚焦度会更高')
WHERE `itinerary_json` LIKE '%If thematic purity matters most, other options are more concentrated%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, 'It is less suitable for users who optimize only one metric, such as extreme cost-saving or extreme check-in density.', '它不太适合只追求单一指标的用户，例如极致省钱或极致刷点。')
WHERE `itinerary_json` LIKE '%It is less suitable for users who optimize only one metric, such as extreme cost-saving or extreme check-in density.%';

UPDATE `saved_itinerary`
SET `itinerary_json` = REGEXP_REPLACE(
        `itinerary_json`,
        'Starts from ([^"]+) and ends at ([^"]+), offering ([^"]+)\\.',
        '以 $1 为起点，以 $2 收尾，整体特点是：$3。',
        1,
        0,
        'c'
    )
WHERE `itinerary_json` LIKE '%Starts from %';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, '; ', '；')
WHERE `itinerary_json` LIKE '%; %';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, ', ', '、')
WHERE `itinerary_json` LIKE '%, %';

UPDATE `saved_itinerary`
SET `itinerary_json` = REPLACE(`itinerary_json`, '."', '。"')
WHERE `itinerary_json` LIKE '%."%';

SET @remaining_english_rows := (
    SELECT COUNT(*)
    FROM `saved_itinerary`
    WHERE `itinerary_json` LIKE '%"title":"Balanced"%'
       OR `itinerary_json` LIKE '%"subtitle":"Best overall trade-off for preference match and route smoothness"%'
       OR `itinerary_json` LIKE '%"recommendReason":"This route keeps preference match%'
       OR `itinerary_json` LIKE '%"summary":"Starts from %'
);

SET @saved_itinerary_english_repaired := GREATEST(@english_saved_rows - @remaining_english_rows, 0);

SELECT 'saved_itinerary' AS target_table,
       @english_saved_rows AS matched_rows_before_update,
       @saved_itinerary_english_repaired AS repaired_rows,
       (SELECT COUNT(*) FROM `backup_saved_itinerary_english_copy_20260418`) AS backup_rows,
       @remaining_english_rows AS remaining_english_rows;
