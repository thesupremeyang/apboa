-- 添加 Obscura 浏览器自动化 MCP 服务器
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`)
VALUES (
    2026061000000000001,
    'obscura',
    'STDIO',
    1,
    'SYNC',
    30,
    '{"command":"C:\\\\JAVA\\\\apboa\\\\obscura.exe","args":["mcp"],"env":[],"encoding":"UTF-8"}',
    'Obscura 浏览器自动化 MCP 服务器，支持网页导航、点击、表单填写、截图等浏览器操作',
    'UNKNOWN',
    NULL,
    NOW(), NOW(), 1111111111111111111, 1111111111111111111
);
