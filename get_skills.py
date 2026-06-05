import requests
import json

# Read token from file
with open("token.txt", "r") as f:
    token = f.read().strip()

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# Get skills from platform
url = "http://117.72.185.237:3000/api/skill/page?page=1&size=1000&enabled=true"

with open("skills_output.txt", "w", encoding="utf-8") as f:
    f.write("Fetching skills from platform...\n")
    
    try:
        response = requests.get(url, headers=headers, timeout=30)
        f.write(f"Status: {response.status_code}\n")
        
        if response.status_code == 200:
            data = response.json()
            f.write(f"Response: {json.dumps(data, ensure_ascii=False)}\n")
            
            if data.get("code") == 200 and data.get("success"):
                skills = data.get("data", {}).get("records", [])
                f.write(f"\nFound {len(skills)} skills:\n")
                for i, skill in enumerate(skills[:10], 1):  # Show first 10
                    name = skill.get("name", "Unknown")
                    desc = skill.get("description", "")[:100] + "..." if skill.get("description", "") else ""
                    f.write(f"{i}. {name}: {desc}\n")
            else:
                f.write(f"API error: {data}\n")
        else:
            f.write(f"HTTP error: {response.text}\n")
            
    except Exception as e:
        f.write(f"Error: {str(e)}\n")
    
    f.write("\nCompleted.\n")