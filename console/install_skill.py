import subprocess
import os
import shutil

output_lines = []
def log(msg):
    output_lines.append(msg)

skill_id = "vivy-yi/xiaohongshu-skills/personal-branding"

# Try npx skills add with full output capture
log(f"Installing skill: {skill_id}")

try:
    result = subprocess.run(
        ["npx", "skills", "add", skill_id, "-g", "-y"],
        capture_output=True, text=True, timeout=120
    )
    log(f"Exit code: {result.returncode}")
    log(f"Stdout: {result.stdout}")
    log(f"Stderr: {result.stderr}")
except Exception as e:
    log(f"Error: {e}")

# Check common installation paths
possible_paths = [
    os.path.expanduser("~/.claude/skills/personal-branding"),
    os.path.expanduser("~/.agents/skills/personal-branding"),
    os.path.expanduser("~/.claude/skills/vivy-yi-xiaohongshu-skills-personal-branding"),
    os.path.expanduser("~/.agents/skills/vivy-yi-xiaohongshu-skills-personal-branding"),
    os.path.join(os.environ.get("APPDATA", ""), ".claude", "skills", "personal-branding"),
    os.path.join(os.environ.get("LOCALAPPDATA", ""), ".claude", "skills", "personal-branding"),
]

# Also search for any skills directories
home = os.path.expanduser("~")
log(f"\nSearching for skills in {home}...")
for root, dirs, files in os.walk(home):
    # Skip certain directories
    skip = ['node_modules', '.git', 'AppData\\Local\\Temp', 'target']
    if any(s in root for s in skip):
        continue
    if 'skills' in root.lower() and ('personal' in root.lower() or 'branding' in root.lower()):
        log(f"Found: {root}")
        for f in files:
            log(f"  - {f}")
    # Limit depth
    depth = root.replace(home, '').count(os.sep)
    if depth > 5:
        dirs.clear()

# Check if skill was installed
for path in possible_paths:
    if os.path.exists(path):
        log(f"\nFound skill at: {path}")
        for f in os.listdir(path):
            log(f"  - {f}")
        skill_md = os.path.join(path, "SKILL.md")
        if os.path.exists(skill_md):
            with open(skill_md, 'r', encoding='utf-8') as f:
                content = f.read()
            log(f"\nSKILL.md content ({len(content)} chars):")
            log(content[:3000])

with open("install_result.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(output_lines))

print("Done!")
