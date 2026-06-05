-- 智能体定义表增加头像字段
ALTER TABLE `agent_definition`
ADD COLUMN `avatar` varchar(100) NULL DEFAULT NULL COMMENT '头像文件ID' AFTER `tag`;
