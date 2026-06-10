import requests
import os
import shutil
import zipfile
import json

output_lines = []
def log(msg):
    output_lines.append(msg)

BASE_URL = "http://117.72.185.237:3000"
USERNAME = "admin"
PASSWORD = "Admin@123.com"

# Step 1: Login to Apboa
log("=== Step 1: Login to Apboa ===")
login_resp = requests.post(
    f"{BASE_URL}/api/auth/login",
    json={"username": USERNAME, "password": PASSWORD},
    headers={"Content-Type": "application/json"}
)
log(f"Login status: {login_resp.status_code}")
login_data = login_resp.json()
log(f"Login response: {json.dumps(login_data, ensure_ascii=False)[:500]}")

if login_data.get("code") != 200:
    log("Login failed!")
    with open("upload_result.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(output_lines))
    exit(1)

token = login_data["data"]["accessToken"]
log(f"Token obtained: {token[:20]}...")

# Step 2: Prepare skill zip
log("\n=== Step 2: Prepare skill zip ===")
skill_dir = os.path.join(os.environ["TEMP"], "downloaded-skills", "personal-branding")
staging = os.path.join(os.environ["TEMP"], "skills-staging")
if os.path.exists(staging):
    shutil.rmtree(staging)
os.makedirs(os.path.join(staging, "skills"), exist_ok=True)

# Copy skill to staging
dst = os.path.join(staging, "skills", "personal-branding")
shutil.copytree(skill_dir, dst)
log(f"Copied skill to staging: {dst}")

# Verify SKILL.md exists
skill_md = os.path.join(dst, "SKILL.md")
if os.path.exists(skill_md):
    with open(skill_md, 'r', encoding='utf-8') as f:
        content = f.read()
    log(f"SKILL.md size: {len(content)} chars")
    log(f"SKILL.md preview:\n{content[:500]}")
else:
    log("ERROR: SKILL.md not found in staging!")

# Create zip
zip_path = os.path.join(os.environ["TEMP"], "skills.zip")
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(os.path.join(staging, "skills")):
        for f in files:
            fp = os.path.join(root, f)
            arcname = os.path.join("skills", os.path.relpath(fp, staging))
            zf.write(fp, arcname)
            log(f"Added to zip: {arcname}")

log(f"Zip created: {zip_path} ({os.path.getsize(zip_path)} bytes)")

# Step 3: Upload to Apboa
log("\n=== Step 3: Upload to Apboa ===")
with open(zip_path, "rb") as f:
    upload_resp = requests.post(
        f"{BASE_URL}/api/skill/import/upload",
        headers={"Authorization": f"Bearer {token}"},
        files={"file": ("skills.zip", f, "application/zip")},
        data={"category": "个人品牌", "cover": "true"}
    )

log(f"Upload status: {upload_resp.status_code}")
upload_data = upload_resp.json()
log(f"Upload response: {json.dumps(upload_data, ensure_ascii=False)[:1000]}")

# Step 4: Verify upload
log("\n=== Step 4: Verify upload ===")
verify_resp = requests.get(
    f"{BASE_URL}/api/skill/page?page=1&size=100&enabled=true",
    headers={"Authorization": f"Bearer {token}"}
)
log(f"Verify status: {verify_resp.status_code}")
verify_data = verify_resp.json()

if verify_data.get("code") == 200:
    skills = verify_data.get("data", {}).get("records", [])
    log(f"Total skills on platform: {len(skills)}")
    
    # Find our uploaded skill
    for s in skills:
        name = s.get("name", "")
        if "personal" in name.lower() or "品牌" in name or "branding" in name.lower():
            log(f"\n⭐ Found uploaded skill:")
            log(f"  Name: {s.get('name')}")
            log(f"  ID: {s.get('id')}")
            log(f"  Category: {s.get('category')}")
            log(f"  Description: {s.get('description', '')[:200]}")
            log(f"  Enabled: {s.get('enabled')}")
else:
    log(f"Verify failed: {json.dumps(verify_data, ensure_ascii=False)[:500]}")

# Cleanup
shutil.rmtree(staging, ignore_errors=True)
if os.path.exists(zip_path):
    os.remove(zip_path)

with open("upload_result.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(output_lines))

print("Done!")
