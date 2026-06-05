import requests
import json

BASE_URL = "http://117.72.185.237:3000"
login_url = f"{BASE_URL}/api/auth/login"
credentials = {"username": "admin", "password": "Admin@123.com"}

try:
    resp = requests.post(login_url, json=credentials)
    data = resp.json()
    if data.get("success"):
        token = data["data"]["accessToken"]
        print(f"Login successful!")
        print(f"Token: {token}")
        # Save token for later use
        with open("token.txt", "w") as f:
            f.write(token)
    else:
        print(f"Login failed: {data}")
except Exception as e:
    print(f"Error: {e}")