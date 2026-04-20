USE `city_trip_db`;

-- Refresh selected Chengdu POIs with web-verified baseline fields.
-- Sources are aligned with seed_poi_web_20260408.sql.

UPDATE `poi`
SET
  `category` = '科教文化',
  `district` = '青羊区',
  `address` = '成都市浣花南路251号',
  `latitude` = 30.663017,
  `longitude` = 104.031558,
  `open_time` = '09:00:00',
  `close_time` = '17:00:00',
  `closed_weekdays` = '周一',
  `temporarily_closed` = 0,
  `status_note` = '法定节假日及特别展期开放时间可能调整，请以四川博物院当日公告为准。',
  `status_source` = 'web_seed_2026_04_08',
  `status_updated_at` = NOW(),
  `avg_cost` = 0.00,
  `stay_duration` = 180,
  `indoor` = 1,
  `night_available` = 0,
  `rain_friendly` = 1,
  `walking_level` = '低',
  `tags` = '文化,历史,室内,休闲',
  `suitable_for` = '独自,朋友,情侣,亲子',
  `description` = '西南地区重要的综合性博物院，巴蜀青铜器、张大千书画及汉代画像砖陶塑藏品突出。',
  `priority_score` = 4.80
WHERE `name` = '四川博物院';

UPDATE `poi`
SET
  `category` = '科教文化',
  `district` = '青羊区',
  `address` = '成都市小河街1号',
  `latitude` = 30.659667,
  `longitude` = 104.061180,
  `open_time` = '09:00:00',
  `close_time` = '17:00:00',
  `closed_weekdays` = '周一',
  `temporarily_closed` = 0,
  `status_note` = '周五、周六通常延时至20:30，节假日开放时间可能延长，请以成都博物馆当日公告为准。',
  `status_source` = 'web_seed_2026_04_08',
  `status_updated_at` = NOW(),
  `avg_cost` = 0.00,
  `stay_duration` = 180,
  `indoor` = 1,
  `night_available` = 0,
  `rain_friendly` = 1,
  `walking_level` = '低',
  `tags` = '文化,历史,室内,休闲',
  `suitable_for` = '独自,朋友,情侣,亲子',
  `description` = '成都市规模较大的综合型博物馆，常设成都历史文化陈列、皮影木偶展等展览。',
  `priority_score` = 4.70
WHERE `name` = '成都博物馆';

UPDATE `poi`
SET
  `category` = '科教文化',
  `district` = '青羊区',
  `address` = '成都市人民中路一段16号',
  `latitude` = 30.662533,
  `longitude` = 104.063294,
  `open_time` = '09:00:00',
  `close_time` = '17:00:00',
  `closed_weekdays` = '周一',
  `temporarily_closed` = 0,
  `status_note` = '4F未来学院等区域开放时间可能不同，请以四川科技馆预约页面及现场公告为准。',
  `status_source` = 'web_seed_2026_04_08',
  `status_updated_at` = NOW(),
  `avg_cost` = 0.00,
  `stay_duration` = 180,
  `indoor` = 1,
  `night_available` = 0,
  `rain_friendly` = 1,
  `walking_level` = '中',
  `tags` = '文化,室内,亲子,科普,休闲',
  `suitable_for` = '亲子,朋友,独自',
  `description` = '位于天府广场北侧的综合科技馆，适合亲子科普、互动体验和雨天行程。',
  `priority_score` = 4.50
WHERE `name` = '四川科技馆';

UPDATE `poi`
SET
  `category` = '科教文化',
  `district` = '成华区',
  `address` = '成都市成华区成华大道十里店路88号',
  `latitude` = 30.680708,
  `longitude` = 104.141892,
  `open_time` = '09:00:00',
  `close_time` = '17:00:00',
  `closed_weekdays` = '周一',
  `temporarily_closed` = 0,
  `status_note` = '法定节假日和特别活动期间可能延时开放，请以成都自然博物馆当日公告为准。',
  `status_source` = 'web_seed_2026_04_08',
  `status_updated_at` = NOW(),
  `avg_cost` = 20.00,
  `stay_duration` = 180,
  `indoor` = 1,
  `night_available` = 0,
  `rain_friendly` = 1,
  `walking_level` = '中',
  `tags` = '自然,文化,室内,亲子,科普',
  `suitable_for` = '亲子,朋友,独自',
  `description` = '以恐龙化石、矿物岩石和生命演化展陈见长的自然博物馆，适合亲子和科普型游览。',
  `priority_score` = 4.70
WHERE `name` = '成都自然博物馆';

UPDATE `poi`
SET
  `category` = '公园休闲',
  `district` = '青羊区',
  `address` = '成都市青羊区祠堂街9号',
  `latitude` = 30.659640,
  `longitude` = 104.054646,
  `open_time` = '06:30:00',
  `close_time` = '22:00:00',
  `closed_weekdays` = NULL,
  `temporarily_closed` = 0,
  `status_note` = '夏季通常会延长开放至22:30，实际开放时间以公园当日公告为准。',
  `status_source` = 'web_seed_2026_04_08',
  `status_updated_at` = NOW(),
  `avg_cost` = 20.00,
  `stay_duration` = 90,
  `indoor` = 0,
  `night_available` = 1,
  `rain_friendly` = 0,
  `walking_level` = '低',
  `tags` = '休闲,自然,本地生活,散步',
  `suitable_for` = '独自,朋友,情侣,亲子',
  `description` = '成都最具代表性的城市公园之一，适合喝茶、散步、体验本地慢生活。',
  `priority_score` = 4.40
WHERE `name` = '成都市人民公园';

UPDATE `poi`
SET
  `category` = '自然生态',
  `district` = '成华区',
  `address` = '成都市昭觉寺南路234号',
  `latitude` = 30.711000,
  `longitude` = 104.104000,
  `open_time` = '08:00:00',
  `close_time` = '17:00:00',
  `closed_weekdays` = NULL,
  `temporarily_closed` = 0,
  `status_note` = '夏冬季开园时间存在细微差异，实际以成都动物园当日公告为准。',
  `status_source` = 'web_seed_2026_04_08',
  `status_updated_at` = NOW(),
  `avg_cost` = 20.00,
  `stay_duration` = 180,
  `indoor` = 0,
  `night_available` = 0,
  `rain_friendly` = 0,
  `walking_level` = '中',
  `tags` = '自然,亲子,动物,休闲',
  `suitable_for` = '亲子,朋友,情侣',
  `description` = '成都本地经典亲子动物园，常年展出多种兽类、鸟类与爬行动物。',
  `priority_score` = 4.40
WHERE `name` = '成都动物园';

UPDATE `poi`
SET
  `category` = '自然风光',
  `district` = '都江堰市',
  `address` = '成都市都江堰市公园路',
  `latitude` = 31.003729,
  `longitude` = 103.610945,
  `open_time` = '08:00:00',
  `close_time` = '18:00:00',
  `closed_weekdays` = NULL,
  `temporarily_closed` = 0,
  `status_note` = '冬春季闭园时间可能提前，节假日客流高峰建议提前预约。',
  `status_source` = 'web_seed_2026_04_08',
  `status_updated_at` = NOW(),
  `avg_cost` = 80.00,
  `stay_duration` = 240,
  `indoor` = 0,
  `night_available` = 0,
  `rain_friendly` = 0,
  `walking_level` = '高',
  `tags` = '自然,文化,历史,休闲',
  `suitable_for` = '朋友,情侣,亲子',
  `description` = '世界文化遗产核心景区，以鱼嘴、飞沙堰、宝瓶口等古代水利工程闻名。',
  `priority_score` = 4.90
WHERE `name` = '都江堰景区';
