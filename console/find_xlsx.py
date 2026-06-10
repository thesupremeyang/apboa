import glob

xlsx_files = glob.glob('*.xlsx')
print("找到的xlsx文件:")
for f in xlsx_files:
    print(f"  {f}")