-- 社区精品化重构升级脚本
-- 目标：补齐社区治理字段，支持软删除、全站置顶与作者置顶评论。

ALTER TABLE `saved_itinerary`
  ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '社区帖子是否已软删除',
  ADD COLUMN `deleted_at` DATETIME NULL COMMENT '删帖时间',
  ADD COLUMN `deleted_by` BIGINT NULL COMMENT '删帖执行人',
  ADD COLUMN `is_global_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否全站置顶',
  ADD COLUMN `global_pinned_at` DATETIME NULL COMMENT '全站置顶时间',
  ADD COLUMN `global_pinned_by` BIGINT NULL COMMENT '全站置顶执行人',
  ADD COLUMN `pinned_comment_id` BIGINT NULL COMMENT '作者置顶评论 ID';

ALTER TABLE `saved_itinerary`
  ADD INDEX `idx_saved_itinerary_public_deleted_updated` (`is_public`, `is_deleted`, `update_time`),
  ADD INDEX `idx_saved_itinerary_global_pinned` (`is_global_pinned`, `global_pinned_at`);