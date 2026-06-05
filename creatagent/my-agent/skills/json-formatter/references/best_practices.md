# JSON Formatting Best Practices

## Indentation

- Use 2 spaces for indentation (most common convention)
- Use 4 spaces for better readability in complex nested structures
- Never use tabs for JSON indentation

## Key Ordering

- Alphabetical ordering (`sort_keys=True`) for consistent output
- Keep original order for readability when order matters
- Consider semantic grouping (e.g., all metadata together)

## Character Encoding

- Use `ensure_ascii=False` for non-ASCII characters (Chinese, Japanese, etc.)
- Use `ensure_ascii=True` for maximum compatibility

## Compact vs Pretty

- **Compact**: Use for storage, transmission, APIs (minimize size)
- **Pretty**: Use for human readability, debugging, configuration files

## Validation

- Always validate JSON before formatting
- Handle common errors:
  - Trailing commas
  - Single quotes instead of double quotes
  - Unquoted keys
  - Comments (not standard JSON)

## Performance Considerations

- For large JSON files, consider streaming parsers
- Minify JSON for network transmission
- Use appropriate indentation for debugging

## Common Patterns

### API Responses
```json
{
  "status": "success",
  "data": {...},
  "message": "Operation completed"
}
```

### Configuration Files
```json
{
  "database": {
    "host": "localhost",
    "port": 5432,
    "name": "mydb"
  },
  "logging": {
    "level": "INFO",
    "file": "/var/log/app.log"
  }
}
```

### Data Exchange
```json
{
  "version": "1.0",
  "timestamp": "2024-01-01T00:00:00Z",
  "payload": {...}
}
```

## Error Messages

Provide clear error messages when JSON is invalid:
- Line number and column where error occurred
- Type of error (syntax, encoding, etc.)
- Suggested fix when possible

## Tools Integration

### Command Line
```bash
# Pretty print
echo '{"a":1}' | python -m json.tool

# Minify
echo '{"a": 1}' | python -c "import sys,json; print(json.dumps(json.load(sys.stdin), separators=(',',':')))"
```

### Python
```python
import json

# Pretty print
print(json.dumps(data, indent=2))

# Minify
print(json.dumps(data, separators=(',', ':')))
```

### JavaScript
```javascript
// Pretty print
console.log(JSON.stringify(data, null, 2));

// Minify
console.log(JSON.stringify(data));
```