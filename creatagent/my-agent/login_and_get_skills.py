import requests
import json

BASE_URL = "http://117.72.185.237:3000"

# Login
login_data = {
    "username": "admin",
    "password": "Admin@123.com"
}

try:
    # Login
    login_resp = requests.post(f"{BASE_URL}/api/auth/login", json=login_data)
    login_resp.raise_for_status()
    login_result = login_resp.json()
    
    if login_result.get("code") != 200:
        print(f"Login failed: {login_result.get('msg')}")
        exit(1)
    
    token = login_result["data"]["accessToken"]
    print(f"Login successful, got token")
    
    # Save token
    with open("token.txt", "w") as f:
        f.write(token)
    
    # Get skills list
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json; charset=utf-8"
    }
    
    skills_resp = requests.get(f"{BASE_URL}/api/skill/page?page=1&size=1000&enabled=true", headers=headers)
    skills_resp.raise_for_status()
    skills_result = skills_resp.json()
    
    if skills_result.get("code") != 200:
        print(f"Failed to get skills: {skills_result.get('msg')}")
        exit(1)
    
    skills = skills_result.get("data", {}).get("records", [])
    print(f"Got {len(skills)} skills")
    
    # Show skill info
    for i, skill in enumerate(skills[:10]):  # Show first 10
        print(f"{i+1}. {skill.get('name')} - {skill.get('description', 'No description')[:50]}...")
    
    # Save skills list
    with open("platform_skills.json", "w", encoding="utf-8") as f:
        json.dump(skills_result, f, ensure_ascii=False, indent=2)
    
    print("Skills list saved to platform_skills.json")
    
except Exception as e:
    print(f"Error occurred: {e}")
    import traceback
    traceback.print_exc()