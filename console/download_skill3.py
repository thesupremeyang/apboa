import requests
import os
import shutil

output_lines = []

def log(msg):
    output_lines.append(msg)

repo = "vivy-yi/xiaohongshu-skills"
skill_name = "personal-branding"
branch = "main"

skill_dir = os.path.join(os.environ["TEMP"], "downloaded-skills", skill_name)
if os.path.exists(skill_dir):
    shutil.rmtree(skill_dir)
os.makedirs(skill_dir, exist_ok=True)

# Try raw.githubusercontent.com directly for common skill file patterns
raw_base = f"https://raw.githubusercontent.com/{repo}/{branch}"

# Try different paths
possible_paths = [
    f"{skill_name}/SKILL.md",
    f"skills/{skill_name}/SKILL.md",
    f"SKILL.md",
    f"{skill_name}/skill.md",
    f"skills/{skill_name}/skill.md",
]

found = False
for path in possible_paths:
    url = f"{raw_base}/{path}"
    log(f"Trying: {url}")
    try:
        resp = requests.get(url, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
        log(f"  Status: {resp.status_code}")
        if resp.status_code == 200:
            content = resp.text
            log(f"  Found! {len(content)} chars")
            
            # Save SKILL.md
            skill_md_path = os.path.join(skill_dir, "SKILL.md")
            with open(skill_md_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            log(f"\nSKILL.md content:")
            log(content[:3000])
            found = True
            break
    except Exception as e:
        log(f"  Error: {e}")

if not found:
    # Try using npx skills with verbose output
    log("\nTrying npx skills with different approaches...")
    
    # Try to list available skills from the repo
    list_url = f"https://skills.sh/api/search?q=xiaohongshu"
    log(f"\nSearching skills.sh for xiaohongshu: {list_url}")
    try:
        resp = requests.get(list_url, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
        log(f"Status: {resp.status_code}")
        if resp.status_code == 200:
            import json
            data = resp.json()
            skills = data.get("skills", [])
            log(f"Found {len(skills)} skills")
            for s in skills:
                sid = s.get("id", "")
                name = s.get("name", "")
                installs = s.get("installs", 0)
                log(f"  - {name} (id: {sid}, installs: {installs})")
    except Exception as e:
        log(f"Error: {e}")

    # Also search for personal-branding specifically
    list_url2 = f"https://skills.sh/api/search?q=personal-branding"
    log(f"\nSearching skills.sh for personal-branding: {list_url2}")
    try:
        resp = requests.get(list_url2, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
        log(f"Status: {resp.status_code}")
        if resp.status_code == 200:
            import json
            data = resp.json()
            skills = data.get("skills", [])
            log(f"Found {len(skills)} skills")
            for s in skills:
                sid = s.get("id", "")
                name = s.get("name", "")
                installs = s.get("installs", 0)
                source = s.get("source", "")
                if "vivy" in sid.lower() or "xiaohongshu" in sid.lower() or "personal-branding" in name:
                    log(f"  ⭐ MATCH: {name} (id: {sid}, installs: {installs}, source: {source})")
    except Exception as e:
        log(f"Error: {e}")

# Write output
with open("download_result2.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(output_lines))

print("Done!")
