-- 创建代码执行配置
INSERT INTO code_execution_config (id, config_name, enable_shell, enable_read, enable_write, command, enabled, created_at, updated_at, created_by) 
VALUES (1, 'default-full-access', 1, 1, 1, '["python","node","pip","npm","npx","cd","dir","mkdir","copy","type","echo"]', 1, NOW(), NOW(), 1);

-- 关联到小红书笔记智能体 (ID: 2060620658504470530)
INSERT INTO agent_code_execution (agent_definition_id, code_execution_id)
VALUES (2060620658504470530, 1);
