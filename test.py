print("Hello from Python!")
import requests
print("requests module loaded")
resp = requests.post("http://117.72.185.237:3000/api/auth/login", 
                    json={"username": "admin", "password": "Admin@123.com"})
print(f"Status code: {resp.status_code}")
data = resp.json()
print(f"Response: {data}")
if data.get("success"):
    token = data["data"]["accessToken"]
    print(f"Token obtained!")
    with open("token.txt", "w") as f:
        f.write(token)
else:
    print("Login failed")