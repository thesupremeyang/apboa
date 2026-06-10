import requests
import json
import sys

queries = ["personal branding", "personal brand", "brand", "social media", "content creation", "marketing", "influencer"]

for query in queries:
    try:
        # Try the skills.sh website search page to extract data
        url = f"https://skills.sh/search?q={query.replace(' ', '+')}"
        resp = requests.get(url, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
        print(f"\n=== Search: '{query}' ===")
        print(f"Status: {resp.status_code}")
        print(f"Content length: {len(resp.text)}")
        
        # Look for skill data in the HTML
        content = resp.text
        # Try to find skill-related JSON or data
        import re
        # Look for JSON data in script tags
        json_matches = re.findall(r'__NEXT_DATA__.*?<', content)
        if json_matches:
            print("Found NEXT_DATA in page")
            # Try to extract the data
            data_start = content.find('__NEXT_DATA__')
            if data_start > -1:
                json_start = content.find('{', data_start)
                json_end = content.find('</script>', json_start)
                if json_start > -1 and json_end > -1:
                    json_str = content[json_start:json_end]
                    try:
                        data = json.loads(json_str)
                        print(json.dumps(data, indent=2)[:3000])
                    except:
                        print("Could not parse JSON from NEXT_DATA")
        
    except Exception as e:
        print(f"Error searching '{query}': {e}")

# Also try the npx registry API
print("\n=== Trying NPM Registry ===")
try:
    resp = requests.get("https://registry.npmjs.org/-/v1/search?text=skills+personal+branding&size=10", timeout=15)
    print(f"NPM search status: {resp.status_code}")
    if resp.status_code == 200:
        data = resp.json()
        for obj in data.get("objects", []):
            pkg = obj.get("package", {})
            print(f"- {pkg.get('name')}: {pkg.get('description', 'No description')[:100]}")
except Exception as e:
    print(f"NPM search error: {e}")
