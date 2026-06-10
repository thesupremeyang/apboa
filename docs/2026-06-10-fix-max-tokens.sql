-- 修复 max_tokens 默认值过小导致回复被截断的问题

-- 1. 修改表结构，默认值从 2000 改为 32768
ALTER TABLE `model_config` ALTER COLUMN `max_tokens` SET DEFAULT 32768;

-- 2. 更新所有 max_tokens 为 NULL 或小于 4096 的记录
UPDATE `model_config` SET `max_tokens` = 32768 WHERE `max_tokens` IS NULL OR `max_tokens` < 4096;

-- 3. 验证更新结果
SELECT id, name, max_tokens FROM `model_config`;
