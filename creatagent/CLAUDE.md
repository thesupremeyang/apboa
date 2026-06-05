# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This repository contains the **Apboa Agent 助手** — a natural-language assistant that helps users create intelligent agents and manage skills on the Apboa platform. It is NOT the Apboa platform itself.

### Two Distinct Systems

| | Apboa 平台 (Platform) | Agent 助手 (This Repo) |
|---|---|---|
| **定位** | 智能体全生命周期管理平台 | 平台的自然语言辅助工具 |
| **地址** | `http://117.72.185.237:3000` | `http://localhost:8081` |
| **功能** | 管理智能体、技能、模型、工具、知识库、MCP、Hook、敏感词等资源 | 用自然语言对话来创建智能体、搜索/创建技能 |
| **技术栈** | Spring Boot + Vue 3 前端控制台 | Java (Agents-Flex) + Node.js CLI 适配器 |
| **用户交互** | 图形化 Web 控制台，表单操作 | 对话窗口，自然语言输入 |
| **关系** | 被操作的目标平台 | 通过 REST API 调用平台完成操作 |

**简单来说**：Apboa 平台是"管理后台"，Agent 助手是"对话式快捷入口"。用户可以在平台前端手动创建智能体，也可以通过 Agent 助手用一句话描述需求来自动完成。

### Agent 助手的三个工作流

- **工作流 A（创建智能体）**: 用户描述需求 → 解析需求 → 查询平台现有技能/模型 → 搜索技能市场或创建自定义技能 → 上传技能 → 创建智能体并绑定
- **工作流 B（搜索并上传技能）**: 用户指定搜索方向 → 从技能市场搜索 → 安装 → 打包上传到平台
- **工作流 C（创建自定义技能）**: 用户描述技能功能 → 创建 SKILL.md 和支撑资源 → 打包上传到平台

The README and primary documentation are in Chinese, targeting a Chinese-speaking development/operations audience.

## Build & Run Commands

### Java (my-agent/) — Primary Application
From `my-agent/`:
```bash
mvn package                          # Build uber-jar (Maven Shade Plugin)
mvn test                             # Run tests
mvn exec:java -Dexec.mainClass=com.myagent.ChatServer   # Run the web server (port 8081)
java -jar target/my-agent-1.0-SNAPSHOT.jar               # Run the packaged uber-jar
```
Main class: `com.myagent.ChatServer`

### Node.js (opencli-apboa/) — CLI Adapter
From `opencli-apboa/`:
```bash
npm install                          # Install dependencies
npm install -g @jackwener/opencli    # Install CLI framework globally
opencli apboa login <username> --password <pwd>   # Authenticate
```

## Architecture

### Agent 助手在系统中的位置
```
用户 (浏览器)
  │
  ├─ 直接访问平台 ──→ [Apboa 平台前端 (Vue 3)] ──→ [Apboa 平台后端 (Spring Boot)]
  │                                                    http://117.72.185.237:3000
  │
  └─ 对话式操作 ────→ [Agent 助手 chat.html]
                           │
                           ▼
                      [ChatServer.java]  ←──→  [MiMo v2.5 Pro LLM]
                      (JDK HttpServer,              │
                       port 8081)                   │ tool calls
                           │                        ▼
                           └──→ [opencli apboa CLI] ──→ [Apboa 平台 REST API]
                                                     http://117.72.185.237:3000
```

**数据流向**：Agent 助手本身不存储任何业务数据，它只是一个"翻译层"——把用户的自然语言翻译成平台 API 调用，所有数据最终都落在 Apboa 平台上。

### Java Application (my-agent/) — Agent 助手后端

Built on [Agents-Flex](https://github.com/agents-flex/agents-flex) v2.1.4 framework. Uses JDK built-in `com.sun.net.httpserver.HttpServer` (no Spring, no Jetty). This is the Agent 助手's own backend, separate from the Apboa platform's Spring Boot backend.

**Key source files** (`my-agent/src/main/java/com/myagent/`):

| Class | Role |
|-------|------|
| `ChatServer.java` | **Primary entry point** — Web server with `/api/chat` and `/api/clear` endpoints. Contains intent detection and server-side agent-creation workflow (`executeCreateAgentWorkflow`) that bypasses the LLM for direct CLI execution. Multi-round tool-calling loop (up to 15 rounds). |
| `App.java` | CLI/terminal version — interactive REPL with Scanner |
| `AgentPrompts.java` | System prompt constant defining agent capabilities and three workflows |

**Request flow**:
1. `POST /api/chat` → intent detection (`detectIntent`)
2. "create_agent" intent → `executeCreateAgentWorkflow` directly (parses requirements via LLM, queries platform skills/models, creates agent via CLI)
3. Otherwise → multi-round tool-calling loop with LLM, where the model can invoke skills and common tools
4. If too many tool calls or no text response → forces a summary round

**Skills** are loaded at runtime from `my-agent/skills/` as SKILL.md files (Markdown with YAML frontmatter). Key skills: `apboa-agent-builder`, `apboa-finding-skill`, `apboa-skill-creator`, `apboa-cli`.

### Node.js CLI Adapter (opencli-apboa/) — Agent 助手的"手和脚"

ES modules (`"type": "module"`), built on `@jackwener/opencli` framework. This is the bridge between the Agent 助手 and the Apboa platform — the LLM doesn't call platform APIs directly, it invokes these CLI commands, which in turn call the platform REST API.

Located at `opencli-apboa/clis/apboa/` — 37 JS command modules covering CRUD operations for agents, skills, tools, knowledge, MCP, hooks, and sensitive words on the Apboa platform.

Shared utilities in `utils.js` handle token persistence (`~/.apboa/token`) and authenticated API fetch.

### Skills (skills/ and my-agent/skills/) — Agent 助手的"技能包"

These are the Agent 助手's own skills (SKILL.md files), NOT skills on the Apboa platform. They define how the Agent 助手 should execute each workflow. The LLM reads these skill definitions to know what steps to follow.

- `skills/` — Top-level skill definitions (3 skills: `apboa-builder`, `apboa-finding-skill`, `apboa-skill-creator`)
- `my-agent/skills/` — Runtime skills loaded by Agents-Flex's SkillsTool (includes the 3 core skills plus demo/utility skills like `code-review`, `translate`, `json-formatter`, etc.)

## Key Configuration

| Config | Value | Notes |
|--------|-------|-------|
| LLM endpoint | `https://token-plan-sgp.xiaomimimo.com` | OpenAI-compatible API |
| LLM model | `mimo-v2.5-pro` | Hardcoded in Java source |
| Web server port | `8081` | In `ChatServer.java` (README says 8080 — discrepancy) |
| Apboa platform API | `http://117.72.185.237:3000` | Hardcoded in opencli-apboa JS files |
| Auth token storage | `~/.apboa/token` | Created by `opencli apboa login` |

**Note**: API keys and endpoints are currently hardcoded in Java source files. The README notes these should be moved to environment variables.

## Tech Stack

- **Java**: JDK 8+ (recommended 17), Maven 3.8+, Agents-Flex 2.1.4, fastjson2
- **Node.js**: 18+, npm 9+, `@jackwener/opencli` ^1.8.1
- **Infrastructure**: Ubuntu 22.04, Nginx (reverse proxy), systemd (service management)
