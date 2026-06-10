import os

files = os.listdir('.')
xlsx_files = [f for f in files if f.endswith('.xlsx')]
print("当前目录下的xlsx文件:")
for f in xlsx_files:
    print(f"  {f} ({os.path.getsize(f)} bytes)")