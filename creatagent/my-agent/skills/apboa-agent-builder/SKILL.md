---
name: apboa-agent-builder
description: 根据用户自然语言需求，在 Apboa 平台上从零创建智能体。涵盖需求分析、系统提示词撰写、技能发现（平台现有 → 技能市场 → 自定义创建）、技能上传、智能体创建与验证的完整流程。当用户说"帮我创建一个XX智能体"、"我需要一个能做XX的agent"、"在平台上新建智能体"时使用此技能。
---

# Apboa Agent Builder

根据用户需求，在 Apboa 平台上自动创建完整可用的智能体。

## Inputs To Collect

- 用户对智能体的描述：做什么、面向谁、核心能力

平台认证已自动处理，不要询问用户的平台地址、账号或密码。

## ⚠️ Critical: UTF-8 Encoding Rules

**Three separate encoding traps exist on Windows Chinese systems.** All must be handled correctly or Chinese content becomes garbled (`鏉ユ簮`, `?????`, `鈥?`).

### Trap 1: PowerShell JSON bodies

PowerShell's `Invoke-RestMethod` and `Invoke-WebRequest` serialize request bodies using the system default encoding (GBK/GB2312 on Chinese Windows), not UTF-8.

**Rule:** Any API call that sends Chinese JSON (agent create/update, skill metadata) MUST use `curl.exe` with a UTF-8 file.

```powershell
# Build JSON → write as UTF-8 no BOM → send with curl.exe
$jsonBody = @{ name = "中文名"; description = "描述" } | ConvertTo-Json
$tempJson = "$env:TEMP\request-body.json"
[System.IO.File]::WriteAllText($tempJson, $jsonBody, [System.Text.UTF8Encoding]::new($false))
curl.exe -s -X POST "<url>" -H "Authorization: Bearer $token" -H "Content-Type: application/json; charset=utf-8" -d "@$tempJson"
```

### Trap 2: curl.exe multipart form fields (`-F`)

`curl.exe -F "category=内容制作"` also corrupts Chinese on Windows. The `-F` flag uses the system encoding for field values.

**Rule:** Any multipart upload with Chinese fields MUST use Python's `requests` library, which handles UTF-8 multipart encoding correctly.

```python
import requests
with open(zip_path, "rb") as f:
    files = {"file": ("skills.zip", f, "application/zip")}
    data = {"category": "内容制作", "cover": "true"}
    resp = requests.post(f"{BASE_URL}/api/skill/import/upload", headers=headers, files=files, data=data)
```

### Trap 3: PowerShell file copy (`Copy-Item`)

PowerShell's `Copy-Item` does NOT preserve UTF-8 encoding. On Chinese Windows it reads/writes using the system default (GBK), turning UTF-8 Chinese into garbled text (`鏉ユ簮` instead of `来源`).

**Rule:** Any file copy that preserves Chinese content MUST use Python's `shutil.copytree` or explicit UTF-8 read/write.

```python
import shutil
shutil.copytree(src_path, dst_path)  # preserves bytes, no encoding conversion
```

### Safe operations (no encoding issues)

- `Invoke-RestMethod` for ASCII-only requests (login, GET with no Chinese params) — safe.
- `curl.exe -d "@file.json"` for JSON bodies — safe (reads raw bytes).
- `curl.exe` GET requests — safe for reading responses.
- Python `shutil.copytree` — safe for file operations.

## Workflow

### Step 1: Analyze and Break Down Requirements

收到用户需求后，进行结构化分析：

1. **提取核心信息**
   - 智能体名称：从需求中提炼简洁名称
   - agentCode：英文短码，如 `contract-reviewer`、`data-analyst`
   - 标签：中文标签，如 `合同审查`、`数据分析`
   - 分类：如 `办公效率`、`内容制作`、`开发工具`

2. **拆解功能点**
   - 将用户描述拆解为 3-8 个具体功能点
   - 每个功能点明确：做什么、输入是什么、输出是什么

3. **映射技能需求**
   - 每个功能点对应一个或多个技能需求
   - 标记哪些是核心技能、哪些是辅助技能

输出格式（向用户确认）：

```
智能体名称：XXX
agentCode：xxx
功能拆解：
  1. [功能描述] → 需要：[技能需求]
  2. [功能描述] → 需要：[技能需求]
  ...
```

### Step 2: Write System Prompt

基于需求分析，撰写完整的 systemPrompt。结构如下：

```
# 角色定义
你是[智能体名称]，一个专门[核心能力]的智能助手。

# 核心能力
1. [能力1]：[详细描述]
2. [能力2]：[详细描述]
...

# 工作流程
当用户提出需求时，按以下步骤执行：
1. [步骤1]
2. [步骤2]
...

# 输出格式
[根据智能体类型定义输出格式要求]

# 约束条件
[明确边界和限制]
```

**要求：**
- 使用中文撰写
- 语言简洁明确，避免歧义
- 包含具体的示例（few-shot）以提高一致性
- 总长度控制在 500-2000 字

### Step 3: Platform Login and Skill Discovery

**登录平台**（ASCII-only，安全使用 Invoke-RestMethod）：

```powershell
$resp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"<password>"}'
$token = $resp.data.accessToken
```

**获取平台现有技能**：

```
GET /api/skill/page?page=1&size=1000&enabled=true
```

用 `curl.exe` 执行 GET 请求以正确处理中文响应。

**匹配逻辑：**
- 遍历 Step 1 中的技能需求清单
- 在平台现有技能中按 `name` 和 `description` 模糊匹配
- 记录：已匹配的技能（platform_matched）和未匹配的技能需求（unmatched）

### Step 4: Search Skill Marketplace (find-skills)

对 Step 3 中未匹配的技能需求：

1. **搜索技能市场**
   ```bash
   npx skills find [query]
   ```
   - 为每个未匹配的技能需求生成搜索关键词
   - 优先使用英文关键词（市场技能多为英文）

2. **验证搜索结果**
   - 安装数 > 1K 优先
   - 来源信誉：`vercel-labs`、`anthropics`、`microsoft` 等优先
   - 检查 GitHub stars

3. **安装到本地**
   ```bash
   npx skills add <owner/repo@skill> -g -y
   ```

4. **验证安装**
   - 检查 `~/.claude/skills/` 下是否生成了对应目录
   - 确认 SKILL.md 存在且有正确的 frontmatter

如果市场也没有合适技能，跳转到 Step 5。

### Step 5: Create Custom Skills (skill-creator)

对 Step 4 中市场也未找到的技能需求：

1. **创建技能目录**
   ```
   skill-name/
   ├── SKILL.md
   └── scripts/ (可选)
   ```

2. **编写 SKILL.md**
   - frontmatter：`name` 和 `description`（必须）
   - body：简洁的工作流指令（< 500 行）
   - 使用祈使句/不定式形式

3. **测试脚本**（如有 scripts/ 目录）
   - 实际运行验证无 bug

4. **验证 frontmatter**
   - 确认 `name` 和 `description` 字段存在
   - 没有 frontmatter 的技能会被平台静默跳过

### Step 6: Upload All Skills to Platform

**用 Python 操作文件**（保持 UTF-8 编码）：

```python
import shutil, zipfile, os

# 暂存区
staging = os.path.join(os.environ["TEMP"], "skills-staging")
if os.path.exists(staging): shutil.rmtree(staging)
os.makedirs(os.path.join(staging, "skills"))

for skill_folder in all_skills_to_upload:
    src = skill_folder  # 本地技能路径
    skill_name = os.path.basename(skill_folder)
    dst = os.path.join(staging, "skills", skill_name)
    shutil.copytree(src, dst)  # preserves UTF-8

# 创建 zip
zip_path = os.path.join(os.environ["TEMP"], "skills.zip")
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(os.path.join(staging, "skills")):
        for f in files:
            fp = os.path.join(root, f)
            arcname = os.path.join("skills", os.path.relpath(fp, staging))
            zf.write(fp, arcname)
```

**用 Python requests 上传**（curl.exe -F 会破坏中文）：

```python
import requests
with open(zip_path, "rb") as f:
    resp = requests.post(
        f"{BASE_URL}/api/skill/import/upload",
        headers={"Authorization": f"Bearer {token}"},
        files={"file": ("skills.zip", f, "application/zip")},
        data={"category": "智能体技能", "cover": "true"}
    )
```

Expected: `code=200`, `importedCount + skippedCount` = expected skill count.

### Step 7: Resolve IDs and Create Agent

**解析平台 ID**：

```
GET /api/skill/page?page=1&size=1000&enabled=true
GET /api/model/config/page?page=1&size=1000&enabled=true
GET /api/prompt/template/page?page=1&size=1000&enabled=true
```

- 按 frontmatter `name` 匹配上传的技能，获取 skill ID
- 获取默认模型配置 ID
- 获取默认提示词模板 ID
- 如果模型或提示词模板缺失，停止并报告

**检查是否已有同名智能体**：

```
GET /api/agent/definition/page?page=1&size=1000
```

- 如果存在同 `agentCode` 或 `name` 的智能体，使用 PUT 更新
- 否则使用 POST 创建

**创建智能体**（用 curl.exe + UTF-8 文件）：

```json
{
  "agentType": "CUSTOM",
  "name": "智能体名称",
  "agentCode": "agent-code",
  "description": "描述",
  "tag": "标签",
  "modelConfigId": "model-id",
  "systemPromptTemplateId": "prompt-template-id",
  "followTemplate": false,
  "systemPrompt": "Step 2 中撰写的完整系统提示词",
  "toolChoiceStrategy": "AUTO",
  "specificToolName": "",
  "skill": ["skill-id-1", "skill-id-2"],
  "tool": [], "knowledgeBase": [], "mcp": [], "mcpBindings": [],
  "subAgent": [], "hook": [],
  "sensitiveWordConfigId": null, "sensitiveFilterEnabled": false,
  "enablePlanning": false, "maxIterations": 50, "maxSubtasks": 10,
  "requirePlanConfirmation": false, "showToolProcess": true,
  "enableMemory": true, "enableMemoryCompression": false,
  "memoryCompressionConfig": null, "structuredOutputEnabled": false,
  "structuredOutputReminder": "TOOL_CHOICE", "structuredOutputSchema": null,
  "studioConfigId": null, "codeExecutionConfigId": null,
  "version": "1.0.0", "enabled": true
}
```

### Step 8: Verify and Report

**验证智能体**：
- `enabled=true`
- `name`、`description`、`tag` 中文显示正确（无 `?` 或乱码）
- `systemPrompt` 非空且中文完整
- `skill` 数组包含所有预期技能 ID
- 每个引用的技能 ID 存在且已启用

**编码抽查**：
- 随机检查 2-3 个技能的中文描述前 50 个字符可读

**最终报告**：

```
=== 智能体创建报告 ===
智能体名称：XXX
Agent Code：xxx
Agent ID：xxx
状态：enabled

技能绑定：
  - [技能名1] (来源：平台现有)
  - [技能名2] (来源：技能市场)
  - [技能名3] (来源：自定义创建)

系统提示词长度：XXX 字
验证结果：全部通过
```

Do NOT include tokens or passwords in the report.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Category shows `??????` | Multipart upload used `curl.exe -F` with Chinese field | Re-upload with Python `requests` |
| Description shows `鏉氭簮` (random CJK) | File copied with PowerShell `Copy-Item` (GBK conversion) | Re-copy with Python `shutil.copytree` |
| JSON body shows `?????` | Used `Invoke-RestMethod` for Chinese JSON | Re-send with `curl.exe` + UTF-8 file |
| Skill not imported (skipped) | SKILL.md missing frontmatter `name`/`description` | Add frontmatter and re-upload |
| Agent skills=0 after creation | Skill IDs in wrong format or not in array | Verify `skill` is JSON array of strings |
| npx skills find returns nothing | Keywords too specific | Try broader English terms |
| System prompt too generic | Missing concrete examples | Add few-shot examples in prompt |
