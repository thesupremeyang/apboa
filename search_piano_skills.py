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

with open("piano_search.txt", "w", encoding="utf-8") as f:
    f.write("Searching for piano-related skills...\n")
    
    try:
        response = requests.get(url, headers=headers, timeout=30)
        
        if response.status_code == 200:
            data = response.json()
            
            if data.get("code") == 200 and data.get("success"):
                skills = data.get("data", {}).get("records", [])
                f.write(f"Total skills on platform: {len(skills)}\n\n")
                
                # Search for piano-related skills
                piano_keywords = ["piano", "钢琴", "音乐", "music", "乐器", "instrument", "教学", "teaching", "学习", "learning"]
                found_skills = []
                
                for skill in skills:
                    name = skill.get("name", "").lower()
                    desc = skill.get("description", "").lower()
                    
                    for keyword in piano_keywords:
                        if keyword in name or keyword in desc:
                            found_skills.append(skill)
                            break
                
                if found_skills:
                    f.write(f"Found {len(found_skills)} piano/music-related skills:\n\n")
                    for i, skill in enumerate(found_skills, 1):
                        f.write(f"{i}. {skill.get('name')}\n")
                        f.write(f"   ID: {skill.get('id')}\n")
                        f.write(f"   Description: {skill.get('description', '')[:200]}...\n")
                        f.write(f"   Category: {skill.get('category', 'N/A')}\n\n")
                else:
                    f.write("No piano/music-related skills found on platform.\n")
                    f.write("\nWe need to create custom skills for piano teaching.\n")
                
                # Also check for education-related skills
                f.write("\n--- Education-related skills ---\n")
                edu_keywords = ["教育", "education", "教学", "teaching", "学习", "learning", "培训", "training", "课程", "course"]
                edu_skills = []
                
                for skill in skills:
                    name = skill.get("name", "").lower()
                    desc = skill.get("description", "").lower()
                    
                    for keyword in edu_keywords:
                        if keyword in name or keyword in desc:
                            edu_skills.append(skill)
                            break
                
                if edu_skills:
                    f.write(f"Found {len(edu_skills)} education-related skills:\n\n")
                    for i, skill in enumerate(edu_skills[:10], 1):  # Show first 10
                        f.write(f"{i}. {skill.get('name')}\n")
                        f.write(f"   ID: {skill.get('id')}\n")
                        f.write(f"   Category: {skill.get('category', 'N/A')}\n\n")
            else:
                f.write(f"API error: {data}\n")
        else:
            f.write(f"HTTP error: {response.text}\n")
            
    except Exception as e:
        f.write(f"Error: {str(e)}\n")
    
    f.write("\nSearch completed.\n")