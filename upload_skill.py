import requests
import json
import zipfile
import os
import shutil

BASE_URL = "http://117.72.185.237:3000"

# Read token
with open("token.txt", "r") as f:
    token = f.read().strip()

headers = {
    "Authorization": f"Bearer {token}"
}

# Step 1: Create zip file
print("=== Step 1: Creating zip file ===")
staging = os.path.join(os.environ.get("TEMP", "/tmp"), "skills-staging")
if os.path.exists(staging):
    shutil.rmtree(staging)
os.makedirs(os.path.join(staging, "skills"))

# Copy skill to staging
src = "piano-teaching"
dst = os.path.join(staging, "skills", "piano-teaching")
shutil.copytree(src, dst)

# Create zip
zip_path = os.path.join(os.environ.get("TEMP", "/tmp"), "skills.zip")
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(os.path.join(staging, "skills")):
        for f in files:
            fp = os.path.join(root, f)
            arcname = os.path.join("skills", os.path.relpath(fp, staging))
            zf.write(fp, arcname)
            print(f"  Added: {arcname}")

print(f"Zip file created: {zip_path}")

# Step 2: Upload to platform
print("\n=== Step 2: Uploading skill ===")
with open(zip_path, "rb") as f:
    files = {"file": ("skills.zip", f, "application/zip")}
    data = {"category": "教育学习", "cover": "true"}
    resp = requests.post(
        f"{BASE_URL}/api/skill/import/upload",
        headers=headers,
        files=files,
        data=data
    )
    upload_data = resp.json()
    print(f"Upload response: {json.dumps(upload_data, ensure_ascii=False, indent=2)}")

# Step 3: Get skills list to find the uploaded skill ID
print("\n=== Step 3: Finding skill ID ===")
skills_resp = requests.get(f"{BASE_URL}/api/skill/page?page=1&size=1000&enabled=true", headers=headers)
skills_data = skills_resp.json()

piano_skill_id = None
if skills_data.get("success"):
    for skill in skills_data["data"]["records"]:
        if "piano" in skill.get("name", "").lower() or "钢琴" in skill.get("description", ""):
            piano_skill_id = skill["id"]
            print(f"Found piano skill: {skill['name']} (ID: {piano_skill_id})")
            break

if piano_skill_id:
    with open("piano_skill_id.txt", "w") as f:
        f.write(piano_skill_id)
    print(f"Skill ID saved: {piano_skill_id}")
else:
    print("Piano skill not found!")

# Step 4: Get model config and prompt template IDs
print("\n=== Step 4: Getting model and prompt IDs ===")
models_resp = requests.get(f"{BASE_URL}/api/model/config/page?page=1&size=1000&enabled=true", headers=headers)
models_data = models_resp.json()
model_id = None
if models_data.get("success") and models_data["data"]["records"]:
    model_id = models_data["data"]["records"][0]["id"]
    print(f"Model ID: {model_id}")

prompts_resp = requests.get(f"{BASE_URL}/api/prompt/template/page?page=1&size=1000&enabled=true", headers=headers)
prompts_data = prompts_resp.json()
prompt_id = None
if prompts_data.get("success") and prompts_data["data"]["records"]:
    # Find the general prompt template
    for p in prompts_data["data"]["records"]:
        if "通用" in p.get("name", ""):
            prompt_id = p["id"]
            break
    if not prompt_id:
        prompt_id = prompts_data["data"]["records"][0]["id"]
    print(f"Prompt Template ID: {prompt_id}")

# Save IDs
with open("platform_ids.json", "w") as f:
    json.dump({
        "skill_id": piano_skill_id,
        "model_id": model_id,
        "prompt_id": prompt_id
    }, f)

print("\n=== Done! ===")