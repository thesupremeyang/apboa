import sys
import traceback
import time

output_lines = []
def log(msg):
    output_lines.append(str(msg))

try:
    import requests
    import os
    import shutil
    import zipfile
    import json

    BASE_URL = "http://117.72.185.237:3000"
    USERNAME = "admin"
    PASSWORD = "Admin@123.com"

    # Step 1: Login with retries
    log("Step 1: Login...")
    token = None
    for attempt in range(5):
        try:
            login_resp = requests.post(
                f"{BASE_URL}/api/auth/login",
                json={"username": USERNAME, "password": PASSWORD},
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            log(f"Attempt {attempt+1}: status={login_resp.status_code}")
            
            if login_resp.status_code == 200:
                login_data = login_resp.json()
                if login_data.get("code") == 200:
                    token = login_data["data"]["accessToken"]
                    log(f"Login success! Token: {token[:20]}...")
                    break
                else:
                    log(f"Login code: {login_data.get('code')}")
            else:
                log(f"Response: {login_resp.text[:200]}")
        except Exception as e:
            log(f"Attempt {attempt+1} error: {e}")
        
        if attempt < 4:
            log(f"Waiting 3 seconds before retry...")
            time.sleep(3)

    if not token:
        raise Exception("Login failed after 5 attempts")

    # Step 2: Prepare zip
    log("\nStep 2: Prepare zip...")
    skill_dir = os.path.join(os.environ.get("TEMP", "/tmp"), "downloaded-skills", "personal-branding")
    log(f"Skill dir exists: {os.path.exists(skill_dir)}")

    staging = os.path.join(os.environ.get("TEMP", "/tmp"), "skills-staging")
    if os.path.exists(staging):
        shutil.rmtree(staging)
    os.makedirs(os.path.join(staging, "skills"), exist_ok=True)

    dst = os.path.join(staging, "skills", "personal-branding")
    if os.path.exists(dst):
        shutil.rmtree(dst)
    shutil.copytree(skill_dir, dst)

    zip_path = os.path.join(os.environ.get("TEMP", "/tmp"), "skills_upload.zip")
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(os.path.join(staging, "skills")):
            for f in files:
                fp = os.path.join(root, f)
                arcname = os.path.join("skills", os.path.relpath(fp, staging))
                zf.write(fp, arcname)
                log(f"Zip: {arcname}")

    log(f"Zip size: {os.path.getsize(zip_path)} bytes")

    # Step 3: Upload with retries
    log("\nStep 3: Upload...")
    for attempt in range(3):
        try:
            with open(zip_path, "rb") as f:
                upload_resp = requests.post(
                    f"{BASE_URL}/api/skill/import/upload",
                    headers={"Authorization": f"Bearer {token}"},
                    files={"file": ("skills.zip", f, "application/zip")},
                    data={"category": "个人品牌", "cover": "true"},
                    timeout=60
                )
            log(f"Upload attempt {attempt+1}: status={upload_resp.status_code}")
            
            if upload_resp.status_code == 200:
                try:
                    upload_data = upload_resp.json()
                    log(f"Response: {json.dumps(upload_data, ensure_ascii=False)[:500]}")
                    if upload_data.get("code") == 200:
                        log("Upload success!")
                        break
                except:
                    log(f"Text: {upload_resp.text[:300]}")
            else:
                log(f"Response: {upload_resp.text[:300]}")
        except Exception as e:
            log(f"Upload attempt {attempt+1} error: {e}")
        
        if attempt < 2:
            time.sleep(3)

    # Step 4: Verify
    log("\nStep 4: Verify...")
    time.sleep(2)
    try:
        verify_resp = requests.get(
            f"{BASE_URL}/api/skill/page?page=1&size=100&enabled=true",
            headers={"Authorization": f"Bearer {token}"},
            timeout=30
        )
        if verify_resp.status_code == 200:
            verify_data = verify_resp.json()
            if verify_data.get("code") == 200:
                skills = verify_data.get("data", {}).get("records", [])
                log(f"Total skills on platform: {len(skills)}")
                for s in skills:
                    name = s.get("name", "")
                    if "personal" in name.lower() or "品牌" in name or "branding" in name.lower():
                        log(f"\n⭐ Found: {name}")
                        log(f"  ID: {s.get('id')}")
                        log(f"  Category: {s.get('category')}")
                        log(f"  Description: {s.get('description', '')[:150]}")
                        log(f"  Enabled: {s.get('enabled')}")
    except Exception as e:
        log(f"Verify error: {e}")

    # Cleanup
    shutil.rmtree(staging, ignore_errors=True)
    if os.path.exists(zip_path):
        os.remove(zip_path)

    log("\n✅ Done!")

except Exception as e:
    log(f"\n❌ Error: {e}")
    log(traceback.format_exc())

finally:
    with open("upload_result.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(output_lines))
