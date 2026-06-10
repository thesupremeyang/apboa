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

## Web Search Tool Configuration

The platform includes a built-in web search tool (`web_search`) that enables agents to search the internet for real-time information.

| Search API | Type | Config |
|-----------|------|--------|
| Tavily | Free tier (default) | `apboa.search.tavily-key` |
| DuckDuckGo | Free | No config needed |
| SearXNG | Free (self-hosted) | `apboa.search.searxng-url` |
| SerpAPI | Paid | `apboa.search.serpapi-key` |
| Google | Paid | `apboa.search.google-api-key` + `apboa.search.google-cx` |

Configuration in `application-dev.yml`:
```yaml
apboa:
  search:
    api-type: tavily      # tavily | duckduckgo | searxng | serpapi | google
    tavily-key:           # Tavily API key
    searxng-url:          # SearXNG instance URL
    serpapi-key:          # SerpAPI key
    google-api-key:       # Google API key
    google-cx:            # Google Custom Search Engine ID
```

Tool source: `core/src/main/java/com/hxh/apboa/core/tool/builtins/WebSearchTool.java`
Guide: `docs/web-search-guide.md`

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

## Document Upload Feature

The platform supports uploading and parsing common office document formats. When a user uploads a document, the system extracts the text content and sends it to the AI agent for analysis.

### Supported Document Formats

| Format | Extension | Parser Library | Description |
|--------|-----------|----------------|-------------|
| Word 97-2003 | `.doc` | Apache POI | Legacy Word format |
| Word Document | `.docx` | Apache POI | Modern Word format |
| Excel 97-2003 | `.xls` | Apache POI | Legacy Excel format |
| Excel Spreadsheet | `.xlsx` | Apache POI | Modern Excel format |
| PDF Document | `.pdf` | PDFBox 3.0.1 | PDF files |
| PowerPoint | `.pptx` | Apache POI | Modern PowerPoint format |
| HTML Webpage | `.html`, `.htm` | Jsoup 1.17.2 | HTML files |
| Plain Text | `.txt` | Java IO | Text files |
| CSV | `.csv` | Java IO | Comma-separated values |
| Markdown | `.md` | Java IO | Markdown files |

### Key Components

- **DocumentParserService**: `biz/resource/src/main/java/com/hxh/apboa/resource/service/DocumentParserService.java`
  - Parses various document formats and extracts text content
  - Supports Word, Excel, PDF, PowerPoint, HTML, and plain text formats
  - Maximum content length: 100,000 characters

- **AttachServiceImpl**: `biz/resource/src/main/java/com/hxh/apboa/resource/service/AttachServiceImpl.java`
  - Modified to support document type detection and parsing
  - Uses `DocumentParserService` to extract text from uploaded documents

- **AguiMessageConverter**: `console/src/main/java/io/agentscope/core/agui/converter/AguiMessageConverter.java`
  - Modified to include document content in messages sent to AI
  - Formats document content with `[文档内容开始]` and `[文档内容结束]` markers

### Dependencies

```xml
<!-- Apache POI for Word, Excel, PowerPoint -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- PDFBox for PDF parsing -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>

<!-- Jsoup for HTML parsing -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
```

### Configuration

Document file types are configured in the `params` database table:
- Key: `ALLOW_DOCUMENT_FILE_TYPE`
- Default value: `doc,docx,xls,xlsx,pdf,ppt,pptx,html,htm,txt,csv,md`

### Database Migration

Run the following SQL to add document type support:
```sql
-- See docs/2026-06-06-document-support.sql
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`)
VALUES (8, '支持的文档文件类型', 'ALLOW_DOCUMENT_FILE_TYPE', 'doc,docx,xls,xlsx,pdf,ppt,pptx,html,htm,txt,csv,md')
ON DUPLICATE KEY UPDATE `param_value` = 'doc,docx,xls,xlsx,pdf,ppt,pptx,html,htm,txt,csv,md';
```

### How It Works

1. User uploads a document file in the chat interface
2. Frontend sends the file to `/api/attach/upload` endpoint
3. Backend saves the file and returns a file ID
4. When sending a message, frontend includes the file ID
5. Backend's `AguiMessageConverter` calls `AttachServiceImpl.getFileBase64()`
6. For document types, `DocumentParserService.parse()` extracts text content
7. Document text is wrapped with markers and included in the message
8. AI agent receives and analyzes the document content

### Related Documentation

- Implementation plan: `docs/document-upload-solution.md`
- Database migration: `docs/2026-06-06-document-support.sql`
