# 联网搜索功能使用指南

## 概述

Apboa 平台现已支持联网搜索功能，智能体可以通过 `web_search` 工具在互联网上搜索实时信息。

## 支持的搜索 API

| API 类型 | 说明 | 是否免费 | 配置项 |
|---------|------|---------|--------|
| Tavily | 默认选项，专为 AI 设计 | ✅ 免费额度 | `apboa.search.tavily-key` |
| DuckDuckGo | 无需 API Key | ✅ 免费 | 无需配置 |
| SearXNG | 自托管元搜索引擎 | ✅ 免费 | `apboa.search.searxng-url` |
| SerpAPI | 支持 Google、Bing 等 | ❌ 付费 | `apboa.search.serpapi-key` |
| Google | Google Custom Search | ❌ 付费 | `apboa.search.google-api-key` + `apboa.search.google-cx` |

## 配置方法

### 1. 使用 Tavily（默认，推荐）

Tavily 是专为 AI 应用设计的搜索 API，提供高质量的搜索结果。

1. 访问 https://tavily.com 注册账号并获取 API Key
2. 修改配置文件 `application-dev.yml`：
```yaml
apboa:
  search:
    api-type: tavily
    tavily-key: your_api_key_here
```

### 2. 使用 DuckDuckGo（免费，无需配置）

无需任何配置，将 `api-type` 改为 `duckduckgo` 即可：
```yaml
apboa:
  search:
    api-type: duckduckgo
```

### 3. 使用 SearXNG（自托管）

SearXNG 是一个自托管的元搜索引擎，聚合多个搜索源。

**部署 SearXNG：**
```bash
# 使用 Docker 快速部署
docker run -d --name searxng -p 8888:8080 searxng/searxng
```

**修改配置文件：**
```yaml
apboa:
  search:
    api-type: searxng
    searxng-url: http://localhost:8888
```

### 4. 使用 SerpAPI

1. 访问 https://serpapi.com 注册账号并获取 API Key
2. 修改配置文件：
```yaml
apboa:
  search:
    api-type: serpapi
    serpapi-key: your_api_key_here
```

### 5. 使用 Google Custom Search

1. 访问 https://console.cloud.google.com 创建项目
2. 启用 Custom Search API 并获取 API Key
3. 访问 https://programmablesearchengine.google.com 创建搜索引擎并获取 CX ID
4. 修改配置文件：
```yaml
apboa:
  search:
    api-type: google
    google-api-key: your_api_key_here
    google-cx: your_cx_here
```

## 为智能体启用搜索功能

### 方法一：通过管理界面绑定工具

1. 登录管理后台
2. 进入「工具管理」页面，找到 `web_search` 工具
3. 进入「智能体管理」页面，选择要配置的智能体
4. 在「工具配置」中添加 `web_search` 工具

### 方法二：通过 SQL 直接绑定

```sql
-- 查找 web_search 工具 ID
SELECT id FROM tool_config WHERE name = 'web_search';

-- 绑定到智能体（将 <agent_id> 和 <tool_id> 替换为实际值）
INSERT INTO agent_tool (id, agent_definition_id, tool_id)
VALUES (1, <agent_id>, <tool_id>);
```

### 方法三：通过技能包绑定

如果智能体已经绑定了 `online-search` 技能包，可以将搜索工具关联到该技能：

```sql
-- 关联工具到技能
INSERT INTO skill_tool (id, skill_id, tool_id)
SELECT 1, id, (SELECT id FROM tool_config WHERE name = 'web_search')
FROM skill_config WHERE name = 'online-search';
```

## 工具参数说明

`web_search` 工具支持以下参数：

| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| query | string | ✅ 是 | 搜索关键词 |
| max_results | integer | ❌ 否 | 返回结果数量，默认 5，最大 10 |

## 使用示例

智能体在对话中可以这样调用搜索工具：

```
用户：帮我搜索一下最新的 AI 新闻
智能体：[调用 web_search 工具，query="最新 AI 新闻 2026"]
```

## 注意事项

1. **DuckDuckGo 限制**：默认的 DuckDuckGo 搜索可能在某些地区受限，生产环境建议使用 SearXNG
2. **API 配额**：SerpAPI 和 Google API 有调用次数限制，请根据实际需求选择合适的套餐
3. **网络环境**：确保服务器能够访问外部网络，否则搜索功能将无法正常工作
4. **重启生效**：修改配置后需要重启后端服务才能生效

## 故障排查

### 搜索无结果

1. 检查网络连接：`curl https://duckduckgo.com`
2. 检查配置是否正确
3. 查看后端日志中的错误信息

### 搜索超时

1. 检查网络延迟
2. 调整 HttpClient 超时配置（默认 10 秒）

### API Key 错误

1. 确认 API Key 是否有效
2. 确认 API Key 是否有足够的配额
