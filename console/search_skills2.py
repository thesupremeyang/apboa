import requests
import json
import sys

# Redirect output to file
with open("search_results.txt", "w", encoding="utf-8") as f:
    queries = ["personal branding", "personal brand", "brand", "social media", "content creation", "marketing", "influencer"]

    for query in queries:
        try:
            url = f"https://skills.sh/search?q={query.replace(' ', '+')}"
            resp = requests.get(url, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
            f.write(f"\n=== Search: '{query}' ===\n")
            f.write(f"Status: {resp.status_code}\n")
            f.write(f"Content length: {len(resp.text)}\n")
            
            content = resp.text
            import re
            
            # Look for NEXT_DATA which contains the page props
            data_start = content.find('__NEXT_DATA__')
            if data_start > -1:
                json_start = content.find('{', data_start)
                json_end = content.find('</script>', json_start)
                if json_start > -1 and json_end > -1:
                    json_str = content[json_start:json_end]
                    try:
                        data = json.loads(json_str)
                        # Try to extract skills from page props
                        props = data.get("props", {}).get("pageProps", {})
                        if props:
                            f.write(f"Page props keys: {list(props.keys())}\n")
                            # Look for skills data
                            skills_data = props.get("skills", props.get("results", props.get("data", [])))
                            if skills_data:
                                if isinstance(skills_data, list):
                                    for s in skills_data[:20]:
                                        if isinstance(s, dict):
                                            name = s.get("name", s.get("slug", "unknown"))
                                            desc = s.get("description", s.get("shortDescription", "No desc"))
                                            f.write(f"  - {name}: {desc[:120]}\n")
                                        else:
                                            f.write(f"  - {s}\n")
                                elif isinstance(skills_data, dict):
                                    for k, v in skills_data.items():
                                        f.write(f"  - {k}: {v}\n")
                            else:
                                f.write(f"No skills data found in props\n")
                                f.write(f"Props content: {json.dumps(props, ensure_ascii=False)[:2000]}\n")
                        else:
                            f.write(f"Full data keys: {list(data.keys())}\n")
                            f.write(f"Data: {json.dumps(data, ensure_ascii=False)[:2000]}\n")
                    except Exception as e:
                        f.write(f"JSON parse error: {e}\n")
            else:
                f.write("No NEXT_DATA found\n")
                
        except Exception as e:
            f.write(f"Error searching '{query}': {e}\n")

    # Also try the npx registry API
    f.write("\n=== Trying NPM Registry ===\n")
    try:
        resp = requests.get("https://registry.npmjs.org/-/v1/search?text=skills+personal+branding&size=10", timeout=15)
        f.write(f"NPM search status: {resp.status_code}\n")
        if resp.status_code == 200:
            data = resp.json()
            for obj in data.get("objects", []):
                pkg = obj.get("package", {})
                f.write(f"- {pkg.get('name')}: {pkg.get('description', 'No description')[:120]}\n")
    except Exception as e:
        f.write(f"NPM search error: {e}\n")

    # Try skills.sh API endpoints
    f.write("\n=== Trying skills.sh API ===\n")
    api_endpoints = [
        "https://skills.sh/api/search?q=brand",
        "https://skills.sh/api/skills?q=brand", 
        "https://skills.sh/api/v1/skills?q=brand",
        "https://api.skills.sh/search?q=brand",
        "https://registry.skills.sh/api/search?q=brand",
    ]
    for endpoint in api_endpoints:
        try:
            resp = requests.get(endpoint, timeout=10, headers={"User-Agent": "Mozilla/5.0", "Accept": "application/json"})
            f.write(f"\n{endpoint}\n")
            f.write(f"Status: {resp.status_code}\n")
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    f.write(f"Response: {json.dumps(data, ensure_ascii=False)[:2000]}\n")
                except:
                    f.write(f"Response (text): {resp.text[:500]}\n")
        except Exception as e:
            f.write(f"Error: {e}\n")

print("Done! Check search_results.txt")
