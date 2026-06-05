---
name: json-formatter
description: Format and beautify JSON data with various options including indentation, key sorting, minification, and validation. Use when Claude needs to work with JSON data for: (1) Formatting or prettifying JSON strings, (2) Minifying or compressing JSON, (3) Validating JSON syntax, (4) Sorting JSON keys, (5) Converting between compact and readable JSON formats.
---

# JSON Formatter

Format, beautify, and validate JSON data with various options.

## Quick Start

### Format JSON with default settings (2-space indent)

```python
import json
data = {"name": "John", "age": 30, "city": "New York"}
formatted = json.dumps(data, indent=2, ensure_ascii=False)
print(formatted)
```

### Format JSON with custom options

```python
import json
data = {"name": "John", "age": 30, "city": "New York"}
formatted = json.dumps(
    data,
    indent=4,  # 4-space indent
    sort_keys=True,  # Sort keys alphabetically
    ensure_ascii=False  # Allow non-ASCII characters
)
print(formatted)
```

## Using the Format Script

The `scripts/format_json.py` script provides command-line JSON formatting:

### Basic formatting

```bash
# Format from stdin
echo '{"name":"John","age":30}' | python scripts/format_json.py

# Format from file
python scripts/format_json.py input.json

# Format to output file
python scripts/format_json.py input.json -o formatted.json
```

### Formatting options

```bash
# Custom indentation
python scripts/format_json.py input.json -i 4

# Sort keys alphabetically
python scripts/format_json.py input.json -s

# Compact output (minify)
python scripts/format_json.py input.json -c

# Minify JSON
python scripts/format_json.py input.json -m

# Prettify JSON
python scripts/format_json.py input.json -p

# Validate JSON
python scripts/format_json.py input.json -v
```

## Common Patterns

### 1. Format JSON string

```python
import json

def format_json_string(json_string: str, indent: int = 2) -> str:
    """Format a JSON string with specified indentation."""
    try:
        data = json.loads(json_string)
        return json.dumps(data, indent=indent, ensure_ascii=False)
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON: {e}")

# Usage
json_str = '{"name":"John","age":30,"city":"New York"}'
formatted = format_json_string(json_str)
print(formatted)
```

### 2. Minify JSON string

```python
import json

def minify_json(json_string: str) -> str:
    """Remove unnecessary whitespace from JSON string."""
    try:
        data = json.loads(json_string)
        return json.dumps(data, separators=(',', ':'))
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON: {e}")

# Usage
json_str = '''
{
    "name": "John",
    "age": 30,
    "city": "New York"
}
'''
minified = minify_json(json_str)
print(minified)
```

### 3. Validate JSON

```python
import json

def is_valid_json(json_string: str) -> bool:
    """Check if a string is valid JSON."""
    try:
        json.loads(json_string)
        return True
    except json.JSONDecodeError:
        return False

# Usage
json_str = '{"name": "John", "age": 30}'
print(is_valid_json(json_str))  # True

invalid_str = '{"name": "John", "age": 30,}'  # Trailing comma
print(is_valid_json(invalid_str))  # False
```

### 4. Sort JSON keys

```python
import json

def sort_json_keys(json_string: str) -> str:
    """Sort JSON keys alphabetically."""
    try:
        data = json.loads(json_string)
        return json.dumps(data, indent=2, sort_keys=True, ensure_ascii=False)
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON: {e}")

# Usage
json_str = '{"name":"John","age":30,"city":"New York"}'
sorted_json = sort_json_keys(json_str)
print(sorted_json)
```

## Advanced Features

### Pretty print nested JSON

```python
import json

def pretty_print_json(data: Any, indent: int = 2) -> str:
    """Pretty print JSON data with proper formatting."""
    return json.dumps(data, indent=indent, ensure_ascii=False, default=str)

# Usage with complex nested data
data = {
    "users": [
        {"name": "John", "age": 30, "address": {"city": "New York", "zip": "10001"}},
        {"name": "Jane", "age": 25, "address": {"city": "Los Angeles", "zip": "90001"}}
    ],
    "count": 2
}

print(pretty_print_json(data))
```

### Convert JSON to different formats

```python
import json

def json_to_compact(json_string: str) -> str:
    """Convert JSON to compact format (no whitespace)."""
    data = json.loads(json_string)
    return json.dumps(data, separators=(',', ':'))

def json_to_indented(json_string: str, indent: int = 2) -> str:
    """Convert JSON to indented format."""
    data = json.loads(json_string)
    return json.dumps(data, indent=indent, ensure_ascii=False)

# Usage
json_str = '{"name":"John","age":30}'
print("Compact:", json_to_compact(json_str))
print("Indented:", json_to_indented(json_str))
```

## Error Handling

Always handle JSON parsing errors gracefully:

```python
import json

def safe_format_json(json_string: str, indent: int = 2) -> tuple[bool, str]:
    """
    Safely format JSON string.
    
    Returns:
        Tuple of (success: bool, result: str)
        If success is False, result contains error message
    """
    try:
        data = json.loads(json_string)
        formatted = json.dumps(data, indent=indent, ensure_ascii=False)
        return True, formatted
    except json.JSONDecodeError as e:
        return False, f"Invalid JSON: {e}"
    except Exception as e:
        return False, f"Error: {e}"

# Usage
json_str = '{"name": "John", "age": 30}'
success, result = safe_format_json(json_str)

if success:
    print("Formatted JSON:")
    print(result)
else:
    print(f"Error: {result}")
```

## Tips

1. **Use `ensure_ascii=False`** for non-ASCII characters (e.g., Chinese, Japanese)
2. **Use `sort_keys=True`** for consistent key ordering
3. **Use `separators=(',', ':')`** for compact output
4. **Always validate JSON** before formatting to avoid errors
5. **Use `default=str`** to handle non-serializable objects (like datetime)

## References

- Python `json` module documentation: https://docs.python.org/3/library/json.html
- JSON specification: https://www.json.org/json-en.html