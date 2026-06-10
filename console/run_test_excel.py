import sys
import traceback

try:
    exec(open('test_excel.py').read())
    print("脚本执行成功")
except Exception as e:
    print(f"错误: {e}")
    traceback.print_exc()