import requests
import os
import shutil
import json

output_lines = []
def log(msg):
    output_lines.append(msg)

skill_id = "vivy-yi/xiaohongshu-skills/personal-branding"
skill_dir = os.path.join(os.environ["TEMP"], "downloaded-skills", "personal-branding")
if os.path.exists(skill_dir):
    shutil.rmtree(skill_dir)
os.makedirs(skill_dir, exist_ok=True)

# Try skills.sh registry API endpoints
api_patterns = [
    f"https://registry.skills.sh/skills/{skill_id}",
    f"https://skills.sh/api/skills/{skill_id}",
    f"https://skills.sh/api/registry/{skill_id}",
    f"https://registry.skills.sh/api/skills/{skill_id}",
    f"https://registry.skills.sh/{skill_id}/SKILL.md",
]

for url in api_patterns:
    log(f"Trying: {url}")
    try:
        resp = requests.get(url, timeout=15, headers={"User-Agent": "Mozilla/5.0", "Accept": "application/json, text/plain, */*"})
        log(f"  Status: {resp.status_code}, Content-Type: {resp.headers.get('content-type', 'unknown')}")
        if resp.status_code == 200:
            content_type = resp.headers.get('content-type', '')
            if 'json' in content_type:
                data = resp.json()
                log(f"  JSON keys: {list(data.keys()) if isinstance(data, dict) else 'array'}")
                log(f"  Content: {json.dumps(data, ensure_ascii=False)[:2000]}")
            else:
                log(f"  Text ({len(resp.text)} chars): {resp.text[:1000]}")
    except Exception as e:
        log(f"  Error: {e}")

# Try to get the skill content from GitHub with different branch names
log("\n--- Trying GitHub raw URLs ---")
branches = ["main", "master", "gh-pages"]
path_patterns = [
    "personal-branding/SKILL.md",
    "SKILL.md",
]

repo = "vivy-yi/xiaohongshu-skills"
for branch in branches:
    for path in path_patterns:
        url = f"https://raw.githubusercontent.com/{repo}/{branch}/{path}"
        log(f"Trying: {url}")
        try:
            resp = requests.get(url, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
            log(f"  Status: {resp.status_code}")
            if resp.status_code == 200 and len(resp.text) > 50:
                log(f"  SUCCESS! {len(resp.text)} chars")
                # Save it
                with open(os.path.join(skill_dir, "SKILL.md"), 'w', encoding='utf-8') as f:
                    f.write(resp.text)
                log(f"  Saved to {skill_dir}/SKILL.md")
                log(f"\nContent preview:\n{resp.text[:2000]}")
                
                with open("fetch_result.txt", "w", encoding="utf-8") as f:
                    f.write("\n".join(output_lines))
                print("Done - found!")
                exit(0)
        except Exception as e:
            log(f"  Error: {e}")

# Try using git clone
log("\n--- Trying git clone ---")
try:
    import subprocess
    git_url = f"https://github.com/{repo}.git"
    clone_dir = os.path.join(os.environ["TEMP"], "skill-repo")
    if os.path.exists(clone_dir):
        shutil.rmtree(clone_dir)
    
    result = subprocess.run(
        ["git", "clone", "--depth", "1", git_url, clone_dir],
        capture_output=True, text=True, timeout=60
    )
    log(f"Git exit code: {result.returncode}")
    log(f"Git stdout: {result.stdout}")
    log(f"Git stderr: {result.stderr}")
    
    if result.returncode == 0:
        # Look for SKILL.md files
        for root, dirs, files in os.walk(clone_dir):
            for f in files:
                if f.upper() == "SKILL.MD":
                    full_path = os.path.join(root, f)
                    log(f"\nFound SKILL.md at: {full_path}")
                    with open(full_path, 'r', encoding='utf-8') as sf:
                        content = sf.read()
                    log(f"Content ({len(content)} chars):")
                    log(content[:3000])
                    
                    # Copy to skill_dir
                    shutil.copy(full_path, os.path.join(skill_dir, "SKILL.md"))
except Exception as e:
    log(f"Git error: {e}")

with open("fetch_result.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(output_lines))

print("Done!")
