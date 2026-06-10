import sys
import traceback

output_lines = []
def log(msg):
    output_lines.append(str(msg))
    print(msg, flush=True)

try:
    import requests
    import os
    import shutil
    import zipfile
    import json

    BASE_URL = "http://117.72.185.237:3000"
    USERNAME = "admin"
    PASSWORD = "Admin@123.com"

    # Step 1: Login
    log("Step 1: Login...")
    login_resp = requests.post(
        f"{BASE_URL}/api/auth/login",
        json={"username": USERNAME, "password": PASSWORD},
        headers={"Content-Type": "application/json"},
        timeout=30
    )
    log(f"Login status: {login_resp.status_code}")
    login_data = login_resp.json()
    log(f"Login code: {login_data.get('code')}")

    if login_data.get("code") != 200:
        log(f"Login failed: {login_data}")
        raise Exception("Login failed")

    token = login_data["data"]["accessToken"]
    log(f"Token: {token[:20]}...")

    # Step 2: Prepare zip
    log("\nStep 2: Prepare zip...")
    skill_dir = os.path.join(os.environ.get("TEMP", "/tmp"), "downloaded-skills", "personal-branding")
    log(f"Skill dir: {skill_dir}")
    log(f"Exists: {os.path.exists(skill_dir)}")

    if os.path.exists(skill_dir):
        for f in os.listdir(skill_dir):
            log(f"  File: {f}")

    staging = os.path.join(os.environ.get("TEMP", "/tmp"), "skills-staging")
    if os.path.exists(staging):
        shutil.rmtree(staging)
    os.makedirs(os.path.join(staging, "skills"), exist_ok=True)

    dst = os.path.join(staging, "skills", "personal-branding")
    if os.path.exists(dst):
        shutil.rmtree(dst)
    shutil.copytree(skill_dir, dst)
    log(f"Copied to: {dst}")

    # Create zip
    zip_path = os.path.join(os.environ.get("TEMP", "/tmp"), "skills_upload.zip")
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(os.path.join(staging, "skills")):
            for f in files:
                fp = os.path.join(root, f)
                arcname = os.path.join("skills", os.path.relpath(fp, staging))
                zf.write(fp, arcname)
                log(f"  Zip: {arcname}")

    zip_size = os.path.getsize(zip_path)
    log(f"Zip: {zip_path} ({zip_size} bytes)")

    # Step 3: Upload
    log("\nStep 3: Upload...")
    with open(zip_path, "rb") as f:
        upload_resp = requests.post(
            f"{BASE_URL}/api/skill/import/upload",
            headers={"Authorization": f"Bearer {token}"},
            files={"file": ("skills.zip", f, "application/zip")},
            data={"category": "个人品牌", "cover": "true"},
            timeout=60
        )

    log(f"Upload status: {upload_resp.status_code}")
    try:
        upload_data = upload_resp.json()
        log(f"Upload response: {json.dumps(upload_data, ensure_ascii=False)[:500]}")
    except:
        log(f"Upload text: {upload_resp.text[:500]}")

    # Step 4: Verify
    log("\nStep 4: Verify...")
    verify_resp = requests.get(
        f"{BASE_URL}/api/skill/page?page=1&size=100&enabled=true",
        headers={"Authorization": f"Bearer {token}"},
        timeout=30
    )
    verify_data = verify_resp.json()

    if verify_data.get("code") == 200:
        skills = verify_data.get("data", {}).get("records", [])
        log(f"Total skills: {len(skills)}")
        for s in skills:
            name = s.get("name", "")
            if "personal" in name.lower() or "品牌" in name or "branding" in name.lower():
                log(f"\n⭐ Found: {name}")
                log(f"  ID: {s.get('id')}")
                log(f"  Category: {s.get('category')}")
                log(f"  Enabled: {s.get('enabled')}")

    # Cleanup
    shutil.rmtree(staging, ignore_errors=True)
    if os.path.exists(zip_path):
        os.remove(zip_path)

    log("\n✅ Upload complete!")

except Exception as e:
    log(f"\n❌ Error: {e}")
    log(traceback.format_exc())

finally:
    with open("upload_result.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(output_lines))
