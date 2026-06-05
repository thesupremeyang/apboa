import requests
import json

BASE_URL = "http://117.72.185.237:3000"

# Read token
with open("token.txt", "r") as f:
    token = f.read().strip()

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# Read agent ID
with open("agent_id.txt", "r") as f:
    agent_id = f.read().strip()

# Read IDs
with open("platform_ids.json", "r") as f:
    ids = json.load(f)

# Read agent body
with open("agent_body.json", "r", encoding="utf-8") as f:
    agent_data = json.load(f)

# Ensure skill is a list
agent_data["skill"] = [ids["skill_id"]]

print(f"Updating agent {agent_id} with skill {ids['skill_id']}...")

# Update agent
resp = requests.put(
    f"{BASE_URL}/api/agent/definition/{agent_id}",
    headers=headers,
    json=agent_data
)

result = resp.json()
print(f"Status: {resp.status_code}")
print(f"Response: {json.dumps(result, ensure_ascii=False, indent=2)}")

# Verify update
verify_resp = requests.get(f"{BASE_URL}/api/agent/definition/{agent_id}", headers=headers)
verify_data = verify_resp.json()
if verify_data.get("success"):
    agent = verify_data["data"]
    print(f"\n=== Updated Agent Info ===")
    print(f"Name: {agent['name']}")
    print(f"Skills: {agent.get('skill', [])}")
    
    # Save result
    with open("update_result.txt", "w", encoding="utf-8") as f:
        f.write(json.dumps(verify_data, ensure_ascii=False, indent=2))

print("\nDone!")