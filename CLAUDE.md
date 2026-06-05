# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Apboa is an enterprise AI Agent platform built on [AgentScope](https://github.com/modelscope/agentscope). It provides full lifecycle management for AI agents -- creation, configuration, debugging, and deployment.

## Build & Development Commands

### Backend (Java 21, Maven, Spring Boot 3.4.9)

```bash
# Build all modules (skip tests — no tests exist yet)
mvn clean install -DskipTests

# Build single module
mvn clean install -DskipTests -pl <module-name>

# Run backend (port 3060)
cd console && mvn spring-boot:run

# Or from root
start-backend.bat   # Windows
```

### Frontend (Vue 3 + TypeScript, pnpm required)

```bash
cd ui
pnpm install
pnpm run dev        # Dev server on port 3000 (proxies /api to localhost:3060)
pnpm run build      # Production build (type-check + vite build)
pnpm run lint       # oxlint + eslint
pnpm run format     # prettier
```

Two build targets controlled by `VITE_APP_TARGET`: `main` (the app) and `doc` (built-in documentation site).

### Full Stack Start (Windows)

```bash
start-apboa.bat     # Starts Redis → backend → frontend
stop-apboa.bat      # Stops backend + frontend
status-apboa.bat    # Check system status
```

### Docker Deployment

```bash
cd docker
docker-compose up -d
```

Docker stack includes MySQL 8.0, Redis 7, and optionally pgvector and Elasticsearch. Backend container installs Python 3 + Node.js 22 for sandboxed code execution. See `docker/README.md` for offline deployment and troubleshooting.

## Architecture

### Maven Module Hierarchy

```
apboa (root POM)
  +-- common           Shared layer: entities, DTOs, utils, configs, auth
  +-- cluster          Redis pub/sub for multi-node sync
  +-- websocket        WebSocket real-time communication
  +-- security         (pom)
  |     +-- script-security  AST-based script security checking (ANTLR4, Rhino)
  +-- core             Agent lifecycle orchestration, model/tool/hook/RAG/MCP abstractions
  +-- job              Quartz scheduled tasks
  +-- console          Spring Boot application entry point
  +-- ui               Optional embedded frontend build (frontend-maven-plugin)
  +-- biz              (pom) Business modules aggregator
        +-- model      LLM model management
        +-- prompt     Prompt template management
        +-- tool       Tool management (Java + online Groovy)
        +-- mcp        MCP protocol integration (HTTP/SSE/STDIO)
        +-- skill      Skill package management + script execution
        +-- knowledge  Knowledge base & RAG
        +-- hook       Lifecycle hooks
        +-- sensitive  Sensitive word filtering
        +-- agent      Agent core (integrates all biz sub-modules)
        +-- account    Account & role management
        +-- resource   File storage (S3/FTP/local)
        +-- params     System parameter configuration
        +-- a2a        Agent-to-Agent communication
        +-- studio     Studio integration/debugging
        +-- sk         Skill framework initialization
        +-- rag        RAG retrieval augmentation
```

### Module Dependency Chain

```
console → core → agent → [model, prompt, tool, mcp, skill, knowledge, hook, sensitive, params, a2a, studio]
                    ↑              ↑
                    |              +→ resource → params
                    +→ account → params, websocket
                    +→ job (Quartz)
                    +→ sk, rag
common ← depended on by all modules
cluster ← depended on by common / websocket
security ← depended on by skill / core
```

### Core Design Patterns

- **Factory pattern** everywhere: `IAgentFactory`, `ChatModelFactory`, `ToolkitFactory`, `HooksFactory`, `KnowledgeFactory`, `McpClientFactory`, `SkillBoxFactory`
- **Strategy pattern**: `IChatModel` implementations selected by `ModelProviderType` enum; `VectorStore` implementations selected by config
- **Self-registration via Spring lifecycle**: `IAgentTool` and `IAgentHook` implement `SmartInitializingSingleton`, auto-registering into static registries on startup
- **Dual-mode extension**: All capability modules support both hardcoded Java implementations AND online Groovy script execution (hot-reloadable)
- **AgentScope framework**: Built on `io.agentscope:agentscope-core:1.0.12`, using its `Agent`, `ReActAgent`, `A2aAgent`, `Model`, `Tool`, `Hook` abstractions

### Business Module Standard Structure

Each biz module follows Spring layered architecture:
```
biz/<module>/
  src/main/java/com/hxh/apboa/<module>/
    controller/    REST API controllers
    service/       Service interfaces
    service/impl/  Service implementations
    mapper/        MyBatis-Plus mapper interfaces
  src/main/resources/
    mapper/        XML mapper files
```

## Key Configuration

| Config | Value |
|--------|-------|
| Backend port | 3060 |
| Frontend dev port | 3000 |
| MySQL | `127.0.0.1:3306/apboa` |
| Redis | `127.0.0.1:6379` (db 7) |
| Vector store | pgvector (default), configurable: milvus, qdrant, elasticsearch, weaviate |
| App entry | `console/src/main/java/com/hxh/apboa/Application.java` |
| Main config | `console/src/main/resources/application.yml` |
| Dev config | `console/src/main/resources/application-dev.yml` |

## Database Conventions

- ORM: MyBatis-Plus 3.5.7 with Druid connection pool
- ID generation: Snowflake (`ASSIGN_ID` strategy)
- Logical delete: `delFlag` field (1=deleted, 0=active)
- Audit fields: `createdAt`, `updatedAt`, `createdBy`, `updatedBy` (auto-filled by MyBatis-Plus)
- Migrations: `docs/2026-*.sql` (dated SQL files)
- Initial setup: `docs/once_db_init/db_init.sql`

## Coding Conventions

- **Base package**: `com.hxh.apboa`
- **Lombok**: Used extensively (`@Getter`, `@Setter`, `@RequiredArgsConstructor`)
- **Service pattern**: Interface + `Impl` suffix (e.g., `AgentDefinitionService` / `AgentDefinitionServiceImpl`)
- **Entity base**: All entities extend `BaseEntity` (id, enabled, createdAt, updatedAt, createdBy, updatedBy)
- **Frontend**: Vue 3 Composition API + TypeScript, Pinia stores, Ant Design Vue 4.x

## Frontend Architecture

- Entry: `ui/src/main.ts`
- Router: `ui/src/router/` (hash history, modules: auth, common, biz)
- Stores: `ui/src/stores/` (Pinia with persisted state)
- API: `ui/src/api/` (Axios-based, mirrors backend endpoints)
- Views: `ui/src/views/` (page-level components)
- Components: `ui/src/components/` (reusable, organized by domain)
- Dev proxy: `/api` → `http://127.0.0.1:3060`
- Two build targets: `main` (app) and `doc` (built-in docs site), controlled by `VITE_APP_TARGET`
