---
name: apboa-skill-creator
description: Create a new agent skill from user requirements and upload it to the Apboa platform. Use when the user wants to build a custom skill that doesn't exist in the market — the skill analyzes requirements, designs SKILL.md with proper frontmatter and instructions, and deploys it to the Apboa platform.
---

# Apboa Skill Creator

Create a new skill from user requirements and upload it to the Apboa platform.

## Inputs To Collect

- What the skill should do (user's description of desired functionality).
- Skill name (suggest one if not provided).
- Category for the skill on the platform.

Platform authentication is handled automatically. Do not ask for the platform URL, username, or password.

## ⚠️ Critical: UTF-8 Encoding Rules

All file operations and API calls with Chinese content must follow UTF-8 encoding rules. See `apboa-agent-deployer` skill for full encoding trap details. Key rules:

- **JSON bodies**: Write UTF-8 file, send with `curl.exe -d "@file.json"`
- **Multipart upload**: Use Python `requests.post`, NOT `curl.exe -F`
- **File copy**: Use Python `shutil.copytree`, NOT PowerShell `Copy-Item`
- **GET calls**: Use `curl.exe` to preserve Chinese in responses

## Workflow

### Step 1: Analyze User Requirements

When the user describes what they want a skill to do:

1. Clarify the skill's purpose and scope
2. Identify the target domain and use cases
3. Determine what the skill needs: workflows, scripts, reference docs, or just instructions
4. Suggest a concise skill name (lowercase, hyphenated, e.g., `pdf-editor`, `contract-review`)

Ask the user to confirm before proceeding. Present:
- Proposed skill name
- Brief description of what it will do
- Whether it needs scripts, references, or assets

### Step 2: Create the Skill Structure

Create the skill directory and files locally:

```
<skill-name>/
├── SKILL.md (required)
├── scripts/ (optional — if deterministic code is needed)
├── references/ (optional — if domain docs are needed)
└── assets/ (optional — if templates or files are needed)
```

### Step 3: Write SKILL.md

#### Frontmatter (required)

```yaml
---
name: <skill-name>
description: <what the skill does and when to use it>
---
```

The `description` field is critical — it determines when the skill triggers. Include:
- What the skill does
- Specific triggers/contexts for when to use it
- Example user requests that should activate it

#### Body (required)

Write clear, concise instructions for the skill. Follow these principles:

- **Imperative form**: "Run the script" not "You should run the script"
- **Concise**: Only include information Claude doesn't already know
- **Progressive disclosure**: Keep SKILL.md under 500 lines; move details to reference files
- **No extraneous files**: No README.md, CHANGELOG.md, or setup docs

Structure the body with:
1. Brief introduction (1-2 sentences)
2. Inputs to collect (if any)
3. Step-by-step workflow
4. Output format (if applicable)

### Step 4: Create Supporting Resources (if needed)

**Scripts** (`scripts/`): For deterministic, repeated operations.
- Test scripts by running them before finalizing.

**References** (`references/`): For domain knowledge, schemas, API docs.
- Keep files focused; add table of contents for files >100 lines.

**Assets** (`assets/`): For templates, images, or files used in output.

### Step 5: Package and Upload to Apboa

#### Stage the skill with Python:

```python
import shutil, zipfile, os

staging = os.path.join(os.environ["TEMP"], "skill-staging")
if os.path.exists(staging): shutil.rmtree(staging)
os.makedirs(os.path.join(staging, "skills"))

src = skill_path  # path to created skill directory
dst = os.path.join(staging, "skills", skill_name)
shutil.copytree(src, dst)

zip_path = os.path.join(os.environ["TEMP"], "skills.zip")
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(os.path.join(staging, "skills")):
        for f in files:
            fp = os.path.join(root, f)
            arcname = os.path.join("skills", os.path.relpath(fp, staging))
            zf.write(fp, arcname)
```

#### Log in to Apboa platform:

```powershell
$resp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"<username>","password":"<password>"}'
$token = $resp.data.accessToken
```

#### Upload via API (use Python for multipart to avoid encoding issues):

```python
import requests
with open(zip_path, "rb") as f:
    resp = requests.post(
        f"{BASE_URL}/api/skill/import/upload",
        headers={"Authorization": f"Bearer {token}"},
        files={"file": ("skills.zip", f, "application/zip")},
        data={"category": "<user-chosen-category>", "cover": "true"}
    )
```

Expected: `code=200`, `importedCount` = 1.

### Step 6: Verify Upload

Use `curl.exe` to verify the skill appears on the platform:

```bash
curl.exe -s "<baseUrl>/api/skill/page?page=1&size=1000&enabled=true" -H "Authorization: Bearer <token>"
```

Check:
- Skill `name` matches the intended name
- `description` is non-empty and readable (no garbled characters)
- `category` is correct
- Skill is enabled

### Step 7: Report

Report to the user:
- Skill name and ID on the platform
- Category and enabled status
- Confirmation that the skill is ready to be bound to an agent
- Do NOT include tokens or passwords

## Skill Design Guidelines

### Good Skill Names
- `contract-review` — clear, specific
- `pdf-editor` — action-oriented
- `chinese-standard-writer` — domain-specific

### Bad Skill Names
- `my-skill` — too generic
- `tool1` — meaningless
- `skill-for-helping-users-with-documents` — too long

### Description Quality

**Good**: "Review contracts for legal risks, clause completeness, and regulatory compliance. Use when the user uploads a contract document and asks for review, analysis, or risk assessment."

**Bad**: "A skill for documents." — too vague, won't trigger correctly.
