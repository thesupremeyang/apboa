import requests
import time

with open("platform_check.txt", "w", encoding="utf-8") as f:
    url = "http://117.72.185.237:3000"
    for i in range(5):
        try:
            r = requests.get(url, timeout=15)
            f.write(f"Attempt {i+1}: status={r.status_code}, len={len(r.text)}\n")
            if r.status_code == 200:
                f.write("Platform is UP!\n")
                
                # Try login
                f.write("\nTrying login...\n")
                login_r = requests.post(
                    f"{url}/api/auth/login",
                    json={"username": "admin", "password": "Admin@123.com"},
                    headers={"Content-Type": "application/json"},
                    timeout=15
                )
                f.write(f"Login status: {login_r.status_code}\n")
                f.write(f"Login body: {login_r.text[:300]}\n")
                break
        except Exception as e:
            f.write(f"Attempt {i+1}: error={e}\n")
        if i < 4:
            time.sleep(5)

print("Done")
