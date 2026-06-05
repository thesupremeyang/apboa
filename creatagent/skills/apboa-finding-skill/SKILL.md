---
name: apboa-finding-skill
description: Search the open agent skills ecosystem (skills.sh) for suitable skills and upload selected ones to the Apboa platform. Use when the user wants to find, browse, or install skills from the skill market and deploy them to their Apboa platform.
---

# Apboa Finding Skill

Search the skill market for suitable skills and upload user-selected skills to the Apboa platform.

## Inputs To Collect

- Search keywords or skill category the user is looking for.
- Which skills the user wants to upload (from search results).

Platform authentication is handled automatically. Do not ask for the platform URL, username, or password.

## ⚠️ Critical: UTF-8 Encoding Rules

All file operations and API calls with Chinese content must follow UTF-8 encoding rules. See `apboa-agent-deployer` skill for full encoding trap details. Key rules:

- **JSON bodies**: Write UTF-8 file, send with `curl.exe -d "@file.json"`
- **Multipart upload**: Use Python `requests.post`, NOT `curl.exe -F`
- **File copy**: Use Python `shutil.copytree`, NOT PowerShell `Copy-Item`
- **GET calls**: Use `curl.exe` to preserve Chinese in responses

## Workflow

### Step 1: Search the Skill Market

Use `npx skills find` to search for skills matching the user's needs.

```bash
npx skills find [query]
```

Search tips:
- Use specific keywords: "pdf processing" is better than just "pdf"
- Try alternative terms: if "deploy" doesn't work, try "deployment" or "ci-cd"
- Browse the leaderboard at https://skills.sh/ for popular skills

Present results to the user with:
1. Skill name and description
2. Install count and source reputation
3. Install command

### Step 2: Install Selected Skills

After the user picks skills, install them locally:

```bash
npx skills add <owner/repo@skill> -g -y
```

The `-g` flag installs globally, `-y` skips confirmation.

### Step 3: Read Installed Skills

Read the installed skill's `SKILL.md` from the local skills directory (typically `~/.claude/skills/<skill-name>/` or `~/.agents/skills/<skill-name>/`).

Confirm the skill has valid YAML frontmatter with `name` and `description`. If missing, create frontmatter from the skill content before uploading.

### Stage and Upload to Apboa

Use Python for all file operations to preserve UTF-8:

```python
import shutil, zipfile, os

# Stage
staging = os.path.join(os.environ["TEMP"], "skills-staging")
if os.path.exists(staging): shutil.rmtree(staging)
os.makedirs(os.path.join(staging, "skills"))

for skill_folder in selected_skills:
    src = skill_folder  # path to installed skill
    dst = os.path.join(staging, "skills", os.path.basename(skill_folder))
    shutil.copytree(src, dst)

# Zip
zip_path = os.path.join(os.environ["TEMP"], "skills.zip")
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(os.path.join(staging, "skills")):
        for f in files:
            fp = os.path.join(root, f)
            arcname = os.path.join("skills", os.path.relpath(fp, staging))
            zf.write(fp, arcname)
```

### Step 4: Log in to Apboa Platform

```powershell
$resp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"<username>","password":"<password>"}'
$token = $resp.data.accessToken
```

### Step 5: Upload Skills via API

Use Python `requests` for multipart upload (curl.exe -F corrupts Chinese fields):

```python
import requests
with open(zip_path, "rb") as f:
    resp = requests.post(
        f"{BASE_URL}/api/skill/import/upload",
        headers={"Authorization": f"Bearer {token}"},
        files={"file": ("skills.zip", f, "application/zip")},
        data={"category": "通用工具", "cover": "true"}
    )
```

Expected: `code=200`, `importedCount + skippedCount` = expected skill count.

### Step 6: Verify Upload

Use `curl.exe` for GET calls to verify:

```bash
curl.exe -s "<baseUrl>/api/skill/page?page=1&size=1000&enabled=true" -H "Authorization: Bearer <token>"
```

Check each uploaded skill:
- `category` displays correctly (not `??????`)
- `description` is non-empty and Chinese displays correctly
- Skill is enabled

### Step 7: Report

Report to the user:
- Total skills uploaded
- Each skill: name, ID, category, enabled status
- Any issues found during verification
- Do NOT include tokens or passwords
