try:
    import openpyxl
    print(f"openpyxl版本: {openpyxl.__version__}")
    print("openpyxl导入成功")
except ImportError as e:
    print(f"导入错误: {e}")
    print("需要安装openpyxl")