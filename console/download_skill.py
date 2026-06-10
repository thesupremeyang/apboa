import requests
import os
import shutil
import zipfile
import json

# Step 1: Download skill from GitHub
repo = "vivy-yi/xiaohongshu-skills"
skill_name = "personal-branding"
branch = "main"

# Try to find SKILL.md in the repo
github_api = f"https://api.github.com/repos/{repo}/contents/{skill_name}"
print(f"Checking: {github_api}")

resp = requests.get(github_api, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
print(f"Status: {resp.status_code}")

skill_dir = os.path.join(os.environ["TEMP"], "downloaded-skills", skill_name)
if os.path.exists(skill_dir):
    shutil.rmtree(skill_dir)
os.makedirs(skill_dir, exist_ok=True)

if resp.status_code == 200:
    files = resp.json()
    print(f"Found {len(files)} files:")
    for f in files:
        print(f"  - {f['name']} ({f['type']})")
    
    # Download each file
    for f in files:
        if f['type'] == 'file':
            download_url = f.get('download_url')
            if download_url:
                file_resp = requests.get(download_url, timeout=15)
                file_path = os.path.join(skill_dir, f['name'])
                with open(file_path, 'wb') as fp:
                    fp.write(file_resp.content)
                print(f"Downloaded: {f['name']} ({len(file_resp.content)} bytes)")
else:
    # Try root level - maybe skill files are at repo root
    print(f"Trying repo root...")
    github_api2 = f"https://api.github.com/repos/{repo}/contents"
    resp2 = requests.get(github_api2, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
    print(f"Root status: {resp2.status_code}")
    if resp2.status_code == 200:
        for f in resp2.json():
            print(f"  - {f['name']} ({f['type']})")

# Check if we got SKILL.md
skill_md_path = os.path.join(skill_dir, "SKILL.md")
if os.path.exists(skill_md_path):
    with open(skill_md_path, 'r', encoding='utf-8') as f:
        content = f.read()
    print(f"\nSKILL.md content ({len(content)} chars):")
    print(content[:2000])
else:
    print("\nSKILL.md not found, checking what we downloaded...")
    if os.path.exists(skill_dir):
        for root, dirs, files in os.walk(skill_dir):
            for fn in files:
                fp = os.path.join(root, fn)
                print(f"  {fp}")

print("\nDone!")
