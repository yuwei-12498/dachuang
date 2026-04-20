USE `city_trip_db`;

ALTER TABLE `poi`
    ADD COLUMN IF NOT EXISTS `crowd_penalty` DECIMAL(6,2) NULL DEFAULT 0.00 COMMENT '拥挤惩罚系数，值越高越拥挤' AFTER `priority_score`;

UPDATE `poi` SET `crowd_penalty` = 1.20 WHERE `name` = '武侯祠';
UPDATE `poi` SET `crowd_penalty` = 2.40 WHERE `name` = '锦里';
UPDATE `poi` SET `crowd_penalty` = 1.00 WHERE `name` = '杜甫草堂';
UPDATE `poi` SET `crowd_penalty` = 2.80 WHERE `name` = '宽窄巷子';
UPDATE `poi` SET `crowd_penalty` = 1.10 WHERE `name` = '成都博物馆';
UPDATE `poi` SET `crowd_penalty` = 2.60 WHERE `name` = '春熙路';
UPDATE `poi` SET `crowd_penalty` = 2.50 WHERE `name` = '太古里';
UPDATE `poi` SET `crowd_penalty` = 3.00 WHERE `name` = '大熊猫繁育研究基地';
UPDATE `poi` SET `crowd_penalty` = 0.80 WHERE `name` = '文殊院';
UPDATE `poi` SET `crowd_penalty` = 2.10 WHERE `name` = '建设路小吃街';
UPDATE `poi` SET `crowd_penalty` = 2.30 WHERE `name` = '九眼桥酒吧街';
UPDATE `poi` SET `crowd_penalty` = 2.20 WHERE `name` = 'IFS国际金融中心';
UPDATE `poi` SET `crowd_penalty` = 1.60 WHERE `name` = '金沙遗址博物馆';
UPDATE `poi` SET `crowd_penalty` = 1.40 WHERE `name` = '东郊记忆';
