import requests
import os
import shutil
import json

output_lines = []

def log(msg):
    output_lines.append(msg)

# Step 1: Download skill from GitHub
repo = "vivy-yi/xiaohongshu-skills"
skill_name = "personal-branding"

# Try to find the skill folder in the repo
github_api = f"https://api.github.com/repos/{repo}/contents/{skill_name}"
log(f"Checking: {github_api}")

resp = requests.get(github_api, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
log(f"Status: {resp.status_code}")

skill_dir = os.path.join(os.environ["TEMP"], "downloaded-skills", skill_name)
if os.path.exists(skill_dir):
    shutil.rmtree(skill_dir)
os.makedirs(skill_dir, exist_ok=True)

if resp.status_code == 200:
    files = resp.json()
    log(f"Found {len(files)} files:")
    for f in files:
        log(f"  - {f['name']} ({f['type']})")
    
    for f in files:
        if f['type'] == 'file':
            download_url = f.get('download_url')
            if download_url:
                file_resp = requests.get(download_url, timeout=15)
                file_path = os.path.join(skill_dir, f['name'])
                with open(file_path, 'wb') as fp:
                    fp.write(file_resp.content)
                log(f"Downloaded: {f['name']} ({len(file_resp.content)} bytes)")
else:
    log(f"Not found at {skill_name}, trying repo root...")
    github_api2 = f"https://api.github.com/repos/{repo}/contents"
    resp2 = requests.get(github_api2, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
    log(f"Root status: {resp2.status_code}")
    if resp2.status_code == 200:
        for f in resp2.json():
            log(f"  - {f['name']} ({f['type']})")

# Check SKILL.md
skill_md_path = os.path.join(skill_dir, "SKILL.md")
if os.path.exists(skill_md_path):
    with open(skill_md_path, 'r', encoding='utf-8') as f:
        content = f.read()
    log(f"\nSKILL.md found ({len(content)} chars)")
    log(content[:3000])
else:
    log("\nSKILL.md not found in downloaded files")
    if os.path.exists(skill_dir):
        for root, dirs, files in os.walk(skill_dir):
            for fn in files:
                log(f"  File: {os.path.join(root, fn)}")

# Write output
with open("download_result.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(output_lines))

print("Done!")
