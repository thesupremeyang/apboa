import requests
import json

BASE_URL = "http://117.72.185.237:3000"

# Read token
with open("token.txt", "r") as f:
    token = f.read().strip()

headers = {
    "Authorization": f"Bearer {token}"
}

# Get all agents
resp = requests.get(f"{BASE_URL}/api/agent/definition/page?page=1&size=1000", headers=headers)
data = resp.json()

result = []
if data.get("success"):
    agents = data["data"]["records"]
    result.append(f"Total agents: {len(agents)}")
    for agent in agents:
        if "琴琴" in agent.get("name", "") or "piano" in agent.get("agentCode", ""):
            result.append(f"\n=== Found piano tutor agent! ===")
            result.append(f"ID: {agent['id']}")
            result.append(f"Name: {agent['name']}")
            result.append(f"Code: {agent['agentCode']}")
            result.append(f"Description: {agent['description']}")
            result.append(f"Tag: {agent['tag']}")
            result.append(f"Enabled: {agent['enabled']}")
            result.append(f"Skills: {agent.get('skill', [])}")
            
            # Save agent ID
            with open("agent_id.txt", "w") as f:
                f.write(agent['id'])
            break
    else:
        result.append("\nPiano tutor agent not found in agents list")
        
        # Try creating again with different approach
        result.append("\n=== Trying to create agent ===")
        
        with open("agent_body.json", "r", encoding="utf-8") as f:
            agent_data = json.load(f)
        
        create_resp = requests.post(
            f"{BASE_URL}/api/agent/definition",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json"
            },
            json=agent_data
        )
        
        create_result = create_resp.json()
        result.append(f"Create response status: {create_resp.status_code}")
        result.append(f"Create response: {json.dumps(create_result, ensure_ascii=False, indent=2)}")
        
        if create_result.get("success"):
            agent_id = create_result.get("data", {}).get("id")
            if agent_id:
                with open("agent_id.txt", "w") as f:
                    f.write(agent_id)
                result.append(f"Agent created with ID: {agent_id}")

# Write result to file
with open("verify_result.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(result))

print("Verification complete, check verify_result.txt")