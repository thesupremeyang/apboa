import urllib.request
import json

# Step 1: Login
login_url = 'http://117.72.185.237:3000/api/auth/login'
login_data = json.dumps({'username': 'admin', 'password': 'Admin@123.com'}).encode()
login_req = urllib.request.Request(login_url, data=login_data, headers={'Content-Type': 'application/json'})

try:
    login_resp = urllib.request.urlopen(login_req)
    login_result = json.loads(login_resp.read().decode())
    print("LOGIN_RESPONSE:", json.dumps(login_result, ensure_ascii=False))
    
    if login_result.get('code') == 200:
        token = login_result['data'].get('accessToken', '')
        print("TOKEN:", token[:50] + "..." if len(token) > 50 else token)
        
        # Step 2: Get skills
        skills_url = 'http://117.72.185.237:3000/api/skill/page?page=1&size=1000&enabled=true'
        skills_req = urllib.request.Request(skills_url, headers={'Authorization': f'Bearer {token}'})
        skills_resp = urllib.request.urlopen(skills_req)
        skills_result = json.loads(skills_resp.read().decode())
        print("SKILLS_COUNT:", len(skills_result.get('data', {}).get('records', [])))
        print("SKILLS_RESPONSE:", json.dumps(skills_result, ensure_ascii=False)[:3000])
    else:
        print("LOGIN_FAILED")
except Exception as e:
    print("ERROR:", str(e))
