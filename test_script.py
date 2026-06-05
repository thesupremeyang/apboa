import requests
import json
import sys

# Test basic Python functionality
print("Python is working!")
print(f"Python version: {sys.version}")

# Test requests library
try:
    import requests
    print("requests library is available")
except ImportError:
    print("requests library is NOT available")

# Test JSON handling
data = {"test": "value", "number": 123}
print(f"JSON test: {json.dumps(data)}")

print("Test completed successfully")