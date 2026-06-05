import requests
import json

BASE_URL = "http://117.72.185.237:3000"

# Step 1: Login
print("=== Step 1: Login ===")
login_resp = requests.post(f"{BASE_URL}/api/auth/login", json={
    "username": "admin",
    "password": "Admin@123.com"
})
login_data = login_resp.json()
if login_data.get("success"):
    token = login_data["data"]["accessToken"]
    print(f"Login successful, token length: {len(token)}")
    with open("token.txt", "w") as f:
        f.write(token)
else:
    print(f"Login failed: {login_data}")
    exit(1)

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# Step 2: Get existing skills
print("\n=== Step 2: Get Skills ===")
skills_resp = requests.get(f"{BASE_URL}/api/skill/page?page=1&size=1000&enabled=true", headers=headers)
skills_data = skills_resp.json()
if skills_data.get("success"):
    skills = skills_data.get("data", {}).get("records", [])
    print(f"Found {len(skills)} skills")
    for s in skills:
        print(f"  - {s.get('name', 'N/A')} (ID: {s.get('id', 'N/A')})")
    with open("skills.json", "w", encoding="utf-8") as f:
        json.dump(skills_data, f, ensure_ascii=False, indent=2)
else:
    print(f"Failed to get skills: {skills_data}")

# Step 3: Get model configs
print("\n=== Step 3: Get Model Configs ===")
models_resp = requests.get(f"{BASE_URL}/api/model/config/page?page=1&size=1000&enabled=true", headers=headers)
models_data = models_resp.json()
if models_data.get("success"):
    models = models_data.get("data", {}).get("records", [])
    print(f"Found {len(models)} model configs")
    for m in models:
        print(f"  - {m.get('name', 'N/A')} (ID: {m.get('id', 'N/A')})")
    with open("models.json", "w", encoding="utf-8") as f:
        json.dump(models_data, f, ensure_ascii=False, indent=2)
else:
    print(f"Failed to get models: {models_data}")

# Step 4: Get prompt templates
print("\n=== Step 4: Get Prompt Templates ===")
prompts_resp = requests.get(f"{BASE_URL}/api/prompt/template/page?page=1&size=1000&enabled=true", headers=headers)
prompts_data = prompts_resp.json()
if prompts_data.get("success"):
    prompts = prompts_data.get("data", {}).get("records", [])
    print(f"Found {len(prompts)} prompt templates")
    for p in prompts:
        print(f"  - {p.get('name', 'N/A')} (ID: {p.get('id', 'N/A')})")
    with open("prompts.json", "w", encoding="utf-8") as f:
        json.dump(prompts_data, f, ensure_ascii=False, indent=2)
else:
    print(f"Failed to get prompts: {prompts_data}")

# Step 5: Get existing agents
print("\n=== Step 5: Get Existing Agents ===")
agents_resp = requests.get(f"{BASE_URL}/api/agent/definition/page?page=1&size=1000", headers=headers)
agents_data = agents_resp.json()
if agents_data.get("success"):
    agents = agents_data.get("data", {}).get("records", [])
    print(f"Found {len(agents)} agents")
    with open("agents.json", "w", encoding="utf-8") as f:
        json.dump(agents_data, f, ensure_ascii=False, indent=2)
else:
    print(f"Failed to get agents: {agents_data}")

print("\n=== All data collected! ===")