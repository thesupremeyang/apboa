import requests
import json

try:
    # 尝试连接到平台
    response = requests.get("http://117.72.185.237:3000", timeout=5)
    print(f"连接成功，状态码: {response.status_code}")
    print(f"响应内容: {response.text[:200]}...")
except requests.exceptions.ConnectionError as e:
    print(f"连接错误: {e}")
except requests.exceptions.Timeout as e:
    print(f"连接超时: {e}")
except Exception as e:
    print(f"其他错误: {e}")