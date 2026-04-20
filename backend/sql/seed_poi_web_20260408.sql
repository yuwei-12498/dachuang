USE `city_trip_db`;

-- Web seed for additional Chengdu POIs.
-- Assumption:
-- 1. This script is intended for the upgraded poi schema that already includes
--    closed_weekdays / temporarily_closed / status_note / status_source / status_updated_at.
-- 2. Coordinates are approximate when sourced from third-party map pages or Wikimedia map metadata.
-- 3. Opening hours use a stable baseline when official sites expose seasonal or weekend-specific variations.

-- 四川博物院
-- Sources:
-- https://www.scmuseum.cn/
-- https://www.scc.org.cn/centredetail.fds?id=309
-- https://www.outdooractive.cn/zh/poi/%E6%88%90%E9%83%BD%E5%B8%82/%E5%9B%9B%E5%B7%9D%E5%8D%9A%E7%89%A9%E9%99%A2/60119346/
INSERT INTO `poi` (
  `name`, `category`, `district`, `address`, `latitude`, `longitude`,
  `open_time`, `close_time`, `closed_weekdays`, `temporarily_closed`,
  `status_note`, `status_source`, `status_updated_at`,
  `avg_cost`, `stay_duration`, `indoor`, `night_available`, `rain_friendly`,
  `walking_level`, `tags`, `suitable_for`, `description`, `priority_score`
)
SELECT
  '四川博物院', '科教文化', '青羊区', '成都市浣花南路251号', 30.663017, 104.031558,
  '09:00:00', '17:00:00', '周一', 0,
  '法定节假日及特别展期开放时间可能调整，请以四川博物院当日公告为准。', 'web_seed_2026_04_08', NOW(),
  0.00, 180, 1, 0, 1,
  '低', '文化,历史,室内,休闲', '独自,朋友,情侣,亲子',
  '西南地区重要的综合性博物馆，巴蜀青铜器、张大千书画及汉代画像砖陶塑藏品突出。', 4.80
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `poi` WHERE `name` = '四川博物院');

-- 成都博物馆
-- Sources:
-- https://www.cdmuseum.com/
-- https://cdmuseum.com/zhinan/
-- https://www.outdooractive.cn/zh/poi/%E6%88%90%E9%83%BD%E5%B8%82/%E6%88%90%E9%83%BD%E5%8D%9A%E7%89%A9%E9%A6%86/60155824/
INSERT INTO `poi` (
  `name`, `category`, `district`, `address`, `latitude`, `longitude`,
  `open_time`, `close_time`, `closed_weekdays`, `temporarily_closed`,
  `status_note`, `status_source`, `status_updated_at`,
  `avg_cost`, `stay_duration`, `indoor`, `night_available`, `rain_friendly`,
  `walking_level`, `tags`, `suitable_for`, `description`, `priority_score`
)
SELECT
  '成都博物馆', '科教文化', '青羊区', '成都市小河街1号', 30.659667, 104.061180,
  '09:00:00', '17:00:00', '周一', 0,
  '周五、周六通常延时至20:30，节假日开放时间可能延长，请以成都博物馆当日公告为准。', 'web_seed_2026_04_08', NOW(),
  0.00, 180, 1, 0, 1,
  '低', '文化,历史,室内,休闲', '独自,朋友,情侣,亲子',
  '成都市规模较大的综合型博物馆，常设成都历史文化陈列、皮影木偶展等展览。', 4.70
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `poi` WHERE `name` = '成都博物馆');

-- 四川科技馆
-- Sources:
-- https://m.cd.bendibao.com/tour/93704.shtm
-- https://m.cd.bendibao.com/tour/125883.shtm
-- https://www.outdooractive.cn/zh/poi/%E6%88%90%E9%83%BD%E5%B8%82/%E5%9B%9B%E5%B7%9D%E7%A7%91%E6%8A%80%E9%A6%86/60282457/
INSERT INTO `poi` (
  `name`, `category`, `district`, `address`, `latitude`, `longitude`,
  `open_time`, `close_time`, `closed_weekdays`, `temporarily_closed`,
  `status_note`, `status_source`, `status_updated_at`,
  `avg_cost`, `stay_duration`, `indoor`, `night_available`, `rain_friendly`,
  `walking_level`, `tags`, `suitable_for`, `description`, `priority_score`
)
SELECT
  '四川科技馆', '科教文化', '青羊区', '成都市人民中路一段16号', 30.662533, 104.063294,
  '09:00:00', '17:00:00', '周一', 0,
  '4F未来学院等区域开放时间可能不同，请以四川科技馆预约页面及现场公告为准。', 'web_seed_2026_04_08', NOW(),
  0.00, 180, 1, 0, 1,
  '中', '文化,室内,亲子,科普,休闲', '亲子,朋友,独自',
  '位于天府广场北侧的综合科技馆，适合亲子科普、互动体验和雨天行程。', 4.50
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `poi` WHERE `name` = '四川科技馆');

-- 成都自然博物馆（成都理工大学博物馆）
-- Sources:
-- https://art.icity.ly/museums/uab2jpy
-- https://m.cd.bendibao.com/tour/148532.shtm
-- https://commons.wikimedia.org/wiki/Category:Chengdu_Natural_History_Museum
INSERT INTO `poi` (
  `name`, `category`, `district`, `address`, `latitude`, `longitude`,
  `open_time`, `close_time`, `closed_weekdays`, `temporarily_closed`,
  `status_note`, `status_source`, `status_updated_at`,
  `avg_cost`, `stay_duration`, `indoor`, `night_available`, `rain_friendly`,
  `walking_level`, `tags`, `suitable_for`, `description`, `priority_score`
)
SELECT
  '成都自然博物馆', '科教文化', '成华区', '成都市成华区成华大道十里店路88号', 30.680708, 104.141892,
  '09:00:00', '17:00:00', '周一', 0,
  '法定节假日和特别活动期间可能延时开放，请以成都自然博物馆当日公告为准。', 'web_seed_2026_04_08', NOW(),
  20.00, 180, 1, 0, 1,
  '中', '自然,文化,室内,亲子,科普', '亲子,朋友,独自',
  '以恐龙化石、矿物岩石和生命演化展陈见长的自然博物馆，适合亲子和科普型游览。', 4.70
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `poi` WHERE `name` = '成都自然博物馆');

-- 成都市人民公园
-- Sources:
-- https://www.cdpeoplespark.cn/
-- https://www.outdooractive.cn/mobile/zh/poi/%E6%88%90%E9%83%BD%E5%B8%82/%E6%88%90%E9%83%BD%E4%BA%BA%E6%B0%91%E5%85%AC%E5%9B%AD/59964981/
INSERT INTO `poi` (
  `name`, `category`, `district`, `address`, `latitude`, `longitude`,
  `open_time`, `close_time`, `closed_weekdays`, `temporarily_closed`,
  `status_note`, `status_source`, `status_updated_at`,
  `avg_cost`, `stay_duration`, `indoor`, `night_available`, `rain_friendly`,
  `walking_level`, `tags`, `suitable_for`, `description`, `priority_score`
)
SELECT
  '成都市人民公园', '公园休闲', '青羊区', '成都市青羊区祠堂街9号', 30.659640, 104.054646,
  '06:30:00', '22:00:00', NULL, 0,
  '夏季通常会延长开放至22:30，实际开放时间以公园当日公告为准。', 'web_seed_2026_04_08', NOW(),
  20.00, 90, 0, 1, 0,
  '低', '休闲,自然,本地生活,散步', '独自,朋友,情侣,亲子',
  '成都最具代表性的城市公园之一，适合喝茶、散步、体验本地慢生活。', 4.40
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `poi` WHERE `name` = '成都市人民公园');

-- 成都动物园
-- Sources:
-- https://www.cdzoo.com.cn/
-- https://www.cdzoo.com.cn/about_us/contact
-- https://commons.wikimedia.org/wiki/Category:Chengdu_Zoo
INSERT INTO `poi` (
  `name`, `category`, `district`, `address`, `latitude`, `longitude`,
  `open_time`, `close_time`, `closed_weekdays`, `temporarily_closed`,
  `status_note`, `status_source`, `status_updated_at`,
  `avg_cost`, `stay_duration`, `indoor`, `night_available`, `rain_friendly`,
  `walking_level`, `tags`, `suitable_for`, `description`, `priority_score`
)
SELECT
  '成都动物园', '自然生态', '成华区', '成都市昭觉寺南路234号', 30.711000, 104.104000,
  '08:00:00', '17:00:00', NULL, 0,
  '夏冬季开园时间存在细微差异，实际以成都动物园当日公告为准。', 'web_seed_2026_04_08', NOW(),
  20.00, 180, 0, 0, 0,
  '中', '自然,亲子,动物,休闲', '亲子,朋友,情侣',
  '成都本地经典亲子动物园，常年展出多种兽类、鸟类与爬行动物。', 4.40
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `poi` WHERE `name` = '成都动物园');

-- 都江堰景区
-- Sources:
-- https://www.djy517.com/online.html?channelCode=mpyd
-- https://www.outdooractive.cn/mobile/zh/poi/%E6%88%90%E9%83%BD%E5%B8%82/%E9%83%BD%E6%B1%9F%E5%A0%B0%E6%B0%B4%E5%88%A9%E9%A3%8E%E6%99%AF%E5%8C%BA/59946426/
INSERT INTO `poi` (
  `name`, `category`, `district`, `address`, `latitude`, `longitude`,
  `open_time`, `close_time`, `closed_weekdays`, `temporarily_closed`,
  `status_note`, `status_source`, `status_updated_at`,
  `avg_cost`, `stay_duration`, `indoor`, `night_available`, `rain_friendly`,
  `walking_level`, `tags`, `suitable_for`, `description`, `priority_score`
)
SELECT
  '都江堰景区', '自然风光', '都江堰市', '成都市都江堰市公园路', 31.003729, 103.610945,
  '08:00:00', '18:00:00', NULL, 0,
  '冬春季闭园时间可能提前，节假日客流高峰建议提前预约。', 'web_seed_2026_04_08', NOW(),
  80.00, 240, 0, 0, 0,
  '高', '自然,文化,历史,休闲', '朋友,情侣,亲子',
  '世界文化遗产核心景区，以鱼嘴、飞沙堰、宝瓶口等古代水利工程闻名。', 4.90
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `poi` WHERE `name` = '都江堰景区');
