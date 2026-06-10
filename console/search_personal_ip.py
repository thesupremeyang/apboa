import requests
import json

api_url = "https://skills.sh/api/search"
queries = ["personal brand", "personal branding", "content creation", "social media marketing", "influencer", "reputation", "marketing strategy", "copywriting", "seo", "newsletter"]

all_skills = {}

with open("search_results_final.txt", "w", encoding="utf-8") as f:
    for query in queries:
        try:
            resp = requests.get(api_url, params={"q": query}, timeout=15, headers={"User-Agent": "Mozilla/5.0", "Accept": "application/json"})
            if resp.status_code == 200:
                data = resp.json()
                skills = data.get("skills", [])
                f.write(f"\n{'='*60}\n")
                f.write(f"🔍 Search: '{query}' - Found {len(skills)} skills\n")
                f.write(f"{'='*60}\n")
                for s in skills:
                    skill_id = s.get("id", "")
                    name = s.get("name", "unknown")
                    installs = s.get("installs", 0)
                    source = s.get("source", "")
                    desc = s.get("description", s.get("shortDescription", ""))
                    
                    if skill_id not in all_skills:
                        all_skills[skill_id] = s
                    
                    f.write(f"\n  📦 {name}\n")
                    f.write(f"     ID: {skill_id}\n")
                    f.write(f"     Installs: {installs:,}\n")
                    f.write(f"     Source: {source}\n")
                    if desc:
                        f.write(f"     Description: {desc[:150]}\n")
            else:
                f.write(f"\nSearch '{query}' failed: HTTP {resp.status_code}\n")
        except Exception as e:
            f.write(f"\nError searching '{query}': {e}\n")
    
    # Write summary
    f.write(f"\n\n{'='*60}\n")
    f.write(f"📊 SUMMARY: {len(all_skills)} unique skills found\n")
    f.write(f"{'='*60}\n\n")
    
    # Sort by installs
    sorted_skills = sorted(all_skills.values(), key=lambda x: x.get("installs", 0), reverse=True)
    for i, s in enumerate(sorted_skills, 1):
        name = s.get("name", "unknown")
        installs = s.get("installs", 0)
        skill_id = s.get("id", "")
        desc = s.get("description", s.get("shortDescription", ""))
        f.write(f"{i}. {name} ({installs:,} installs)\n")
        f.write(f"   ID: {skill_id}\n")
        if desc:
            f.write(f"   {desc[:120]}\n")
        f.write(f"\n")

print("Done!")
