-- 启用 WriteFileTool 工具配置
-- 执行此 SQL 后需要重启后端服务

-- 1. 添加代码执行环境配置（如果不存在）
INSERT INTO `code_execution_config` (`id`, `config_name`, `enable_write`, `enable_shell`, `command`, `enabled`)
VALUES (1, '默认代码执行环境', 1, 1, 'python3,bash', 1)
ON DUPLICATE KEY UPDATE `enable_write` = 1, `enabled` = 1;

-- 2. 将智能体关联到代码执行配置（替换 agent_definition_id 为你的智能体 ID）
-- 你可以通过管理后台查看智能体 ID
-- INSERT INTO `agent_code_execution` (`id`, `agent_definition_id`, `code_execution_id`)
-- VALUES (1, <你的智能体ID>, 1);

-- 查看现有智能体列表
SELECT id, name, agent_code FROM agent_definition WHERE enabled = 1;
