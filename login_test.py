import requests
import json
import sys

# Write output to file instead of stdout
with open("output.txt", "w", encoding="utf-8") as f:
    f.write("Starting login test...\n")
    
    try:
        # Login to get token
        login_url = "http://117.72.185.237:3000/api/auth/login"
        login_data = {"username": "admin", "password": "Admin@123.com"}
        
        f.write(f"Attempting login to {login_url}\n")
        response = requests.post(login_url, json=login_data, timeout=30)
        
        f.write(f"Response status: {response.status_code}\n")
        
        if response.status_code == 200:
            resp_data = response.json()
            f.write(f"Response data: {json.dumps(resp_data, ensure_ascii=False)}\n")
            
            if resp_data.get("code") == 200 and resp_data.get("success"):
                token = resp_data.get("data", {}).get("accessToken")
                if token:
                    f.write(f"Token obtained: {token[:20]}...\n")
                    f.write("Login successful!\n")
                    
                    # Save token to file for later use
                    with open("token.txt", "w") as tf:
                        tf.write(token)
                else:
                    f.write("No token in response\n")
            else:
                f.write(f"Login failed: {resp_data}\n")
        else:
            f.write(f"HTTP error: {response.text}\n")
            
    except Exception as e:
        f.write(f"Error: {str(e)}\n")
    
    f.write("Test completed.\n")