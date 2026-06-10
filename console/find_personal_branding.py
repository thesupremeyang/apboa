import os
import shutil

clone_dir = r"C:\Users\Administrator\AppData\Local\Temp\skill-repo"
target_skill = "personal-branding"

output_lines = []
def log(msg):
    output_lines.append(msg)

# Search for personal-branding SKILL.md
found_path = None
for root, dirs, files in os.walk(clone_dir):
    # Check if current directory name matches
    dir_name = os.path.basename(root).lower()
    if target_skill in dir_name or dir_name == target_skill:
        for f in files:
            if f.upper() == "SKILL.MD":
                found_path = os.path.join(root, f)
                log(f"Found by directory name: {found_path}")
                break
    
    # Also check each file
    for f in files:
        if f.upper() == "SKILL.MD":
            full_path = os.path.join(root, f)
            if target_skill in full_path.lower():
                found_path = full_path
                log(f"Found by path: {full_path}")
                break

if found_path:
    with open(found_path, 'r', encoding='utf-8') as sf:
        content = sf.read()
    log(f"\nFile size: {len(content)} chars")
    log(f"\nFull content:\n{content}")
    
    # Copy to skill_dir
    skill_dir = os.path.join(os.environ["TEMP"], "downloaded-skills", "personal-branding")
    if os.path.exists(skill_dir):
        shutil.rmtree(skill_dir)
    os.makedirs(skill_dir, exist_ok=True)
    shutil.copy(found_path, os.path.join(skill_dir, "SKILL.md"))
    log(f"\nSaved to: {skill_dir}/SKILL.md")
else:
    log(f"SKILL.md with '{target_skill}' not found!")
    log("\nListing all SKILL.md files found:")
    count = 0
    for root, dirs, files in os.walk(clone_dir):
        for f in files:
            if f.upper() == "SKILL.MD":
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, clone_dir)
                log(f"  {rel_path}")
                count += 1
    log(f"\nTotal: {count} SKILL.md files")

# Also list the directory structure (top 2 levels)
log("\n\nDirectory structure (top levels):")
for root, dirs, files in os.walk(clone_dir):
    depth = root.replace(clone_dir, '').count(os.sep)
    if depth <= 2:
        indent = '  ' * depth
        log(f"{indent}{os.path.basename(root)}/")
        if depth <= 1:
            for f in files[:5]:
                log(f"{indent}  {f}")

with open("find_result.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(output_lines))

print("Done!")
