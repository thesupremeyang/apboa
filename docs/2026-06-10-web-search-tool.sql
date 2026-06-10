-- 联网搜索工具关联 SQL
-- 执行此 SQL 前，请确保系统已启动一次，让 WebSearchTool 自动注册到数据库

-- 1. 查找 web_search 工具的 ID
-- SELECT id, name, description FROM tool_config WHERE name = 'web_search';

-- 2. 查找 online-search 技能的 ID
-- SELECT id, name, description FROM skill_config WHERE name = 'online-search';

-- 3. 关联工具到技能（请将下面的 ID 替换为实际查询到的值）
-- INSERT INTO skill_tool (id, skill_id, tool_id) VALUES (1, <online-search技能ID>, <web_search工具ID>);

-- 或者使用以下一步完成的 SQL（假设 online-search 技能 ID 为 2061281494814261250）：
-- INSERT INTO skill_tool (id, skill_id, tool_id)
-- SELECT 1, 2061281494814261250, id FROM tool_config WHERE name = 'web_search';

-- 如果需要为特定智能体直接绑定搜索工具，可以使用 agent_tool 表：
-- INSERT INTO agent_tool (id, agent_definition_id, tool_id)
-- SELECT 1, <智能体ID>, id FROM tool_config WHERE name = 'web_search';
