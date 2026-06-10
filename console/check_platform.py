import requests
import time

url = "http://117.72.185.237:3000"
for i in range(3):
    try:
        r = requests.get(url, timeout=15)
        print(f"Attempt {i+1}: status={r.status_code}, len={len(r.text)}")
        if r.status_code == 200:
            print("Platform is UP!")
            break
    except Exception as e:
        print(f"Attempt {i+1}: error={e}")
    if i < 2:
        time.sleep(5)
