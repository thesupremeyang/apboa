---
name: apboa-cli
description: Apboa 智能体平台 CLI 管理工具。当用户需要管理 Apboa 平台上的智能体、技能、工具、知识库、MCP、钩子、敏感词、模型、提示词等资源时使用。触发词：apboa、智能体、agent、技能上传、skill、工具管理、知识库、MCP服务器、钩子hook、敏感词、模型配置、提示词模板。
---

# Apboa CLI 管理工具

通过 `opencli apboa` 命令管理 Apboa 智能体平台的所有资源。如果 `opencli` 命令不可用，使用 `npx opencli apboa ...` 作为等价替代。

## 前置条件

CLI 已安装并登录，token 保存在 `~/.apboa/token`。如未登录，先执行登录。

## 命令总览

### 认证
```bash
opencli apboa login <username> --password <pwd>
```

### 智能体 (Agent)
```bash
opencli apboa agent-list [-f json|table|csv]          # 列出所有智能体
opencli apboa agent-get <id>                           # 获取详情
opencli apboa agent-create --name <n> --code <c> --description <d> --systemPrompt <sp> [--skillIds <ids>] [--modelConfigId <mid>]
opencli apboa agent-update <id> [--name <n>] [--systemPrompt <sp>] [--skillIds <ids>]
```

### 技能 (Skill)
```bash
opencli apboa skill-list [--size N]                    # 列出技能
opencli apboa skill-upload <path> [--category <cat>]   # 上传技能包 (.zip)，上传后用 skill-list 获取 ID
```

### 工具 (Tool)
```bash
opencli apboa tool-list                                # 列出工具
opencli apboa tool-get <id>                            # 获取详情
opencli apboa tool-categories                          # 列出分类
opencli apboa tool-create --name <n> --description <d> --category <c> --inputSchema <json>
opencli apboa tool-update <id> [--name <n>] [--description <d>]
opencli apboa tool-delete <id>
```

### 知识库 (Knowledge)
```bash
opencli apboa knowledge-list                           # 列出知识库
opencli apboa knowledge-get <id>                       # 获取详情
opencli apboa knowledge-create --name <n> --description <d> [--kbType LOCAL] [--ragMode AGENTIC]
opencli apboa knowledge-update <id> [--name <n>] [--description <d>]
opencli apboa knowledge-delete <id>
```

### MCP 服务器
```bash
opencli apboa mcp-list                                 # 列出 MCP 配置
opencli apboa mcp-get <id>                             # 获取详情
opencli apboa mcp-create --name <n> --protocol <HTTP|SSE|STDIO> --url <u>
opencli apboa mcp-update <id> [--name <n>] [--enabled true|false]
opencli apboa mcp-delete <id>
```

### 钩子 (Hook)
```bash
opencli apboa hook-list                                # 列出钩子
opencli apboa hook-get <id>                            # 获取详情
opencli apboa hook-create --name <n> --hookType <t> --description <d>
opencli apboa hook-update <id> [--name <n>] [--enabled true|false]
opencli apboa hook-delete <id>
```

### 敏感词 (Sensitive)
```bash
opencli apboa sensitive-list                           # 列出敏感词配置
opencli apboa sensitive-get <id>                       # 获取详情
opencli apboa sensitive-categories                     # 列出分类
opencli apboa sensitive-create --name <n> --category <c> --action <BLOCK|REPLACE> --words <json-array>
opencli apboa sensitive-update <id> [--name <n>] [--words <json-array>]
opencli apboa sensitive-delete <id>
```

### 模型 & 提示词
```bash
opencli apboa model-list                               # 列出模型配置
opencli apboa prompt-list                              # 列出提示词模板
```

## 通用选项

- `-f json|table|csv|yaml|md` — 输出格式（默认 table）
- `--size N` — 分页大小
- `-v` — 调试输出

## 使用规则

1. **先查后操作** — 修改/删除前先用 list/get 确认资源存在
2. **JSON 输出** — 需要解析数据时用 `-f json`
3. **创建智能体** — 需要先获取 skillIds（skill-list）和 modelConfigId（model-list），并为目标智能体生成完整 `systemPrompt`
4. **上传技能** — 需要 .zip 格式的技能包
5. **systemPrompt 支持文件** — 长提示词或多行提示词优先写入文件，用 `@file:C:\完整路径\prompt.txt` 从文件加载，避免命令行转义问题
6. **自定义提示词验证** — 创建后用 `agent-get <id> -f json` 确认 `systemPromptLength > 0` 且 `followTemplate = false`

## 典型工作流

### 创建一个完整的智能体
```
1. opencli apboa model-list -f json          → 获取 modelConfigId
2. opencli apboa skill-list -f json          → 选择需要的 skillIds
3. 根据用户需求和已选技能生成目标智能体的系统提示词，并保存到 temp_prompts\<agent-code>.txt
4. opencli apboa agent-create --name "xxx" --code "xxx" --description "xxx" --systemPrompt "@file:C:\Users\14420\Desktop\creatagent\my-agent\temp_prompts\xxx.txt" --skillIds "id1,id2" --modelConfigId "mid" -f json
5. opencli apboa agent-get <agentId> -f json  → 验证 systemPromptLength > 0 且 followTemplate=false
```

### 上传并关联技能
```
1. 打包技能目录为 .zip
2. opencli apboa skill-upload ./my-skill.zip --category custom -f json
3. opencli apboa skill-list -f json           → 获取新技能 ID
4. opencli apboa agent-update <agentId> --skillIds "existing,new-skill-id"
```
