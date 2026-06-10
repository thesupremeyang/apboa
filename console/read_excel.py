import pandas as pd
import sys

def main():
    print("Script started")
    files = ['simple_test.xlsx', 'formula_test.xlsx']
    found = False
    for f in files:
        try:
            df = pd.read_excel(f)
            found = True
            print(f"Successfully read {f}")
            print(f"Shape: {df.shape}")
            print("Columns:", df.columns.tolist())
            print(df.head())
            print("\n" + "="*30 + "\n")
        except FileNotFoundError:
            print(f"{f} not found.")
        except Exception as e:
            print(f"Error reading {f}: {e}")

    if not found:
        print("No valid Excel files found.")

if __name__ == "__main__":
    main()
