#!/usr/bin/env python3
"""
JSON Formatter Script
Formats and beautifies JSON data with various options.
"""

import json
import sys
import argparse
from typing import Any, Dict, List, Optional, Union


def format_json(
    data: Any,
    indent: int = 2,
    sort_keys: bool = False,
    ensure_ascii: bool = False,
    compact: bool = False
) -> str:
    """
    Format JSON data with specified options.
    
    Args:
        data: JSON data to format
        indent: Number of spaces for indentation (default: 2)
        sort_keys: Sort dictionary keys alphabetically (default: False)
        ensure_ascii: Escape non-ASCII characters (default: False)
        compact: Produce compact output (default: False)
    
    Returns:
        Formatted JSON string
    """
    if compact:
        return json.dumps(
            data,
            separators=(',', ':'),
            ensure_ascii=ensure_ascii,
            sort_keys=sort_keys
        )
    
    return json.dumps(
        data,
        indent=indent,
        ensure_ascii=ensure_ascii,
        sort_keys=sort_keys
    )


def validate_json(json_string: str) -> bool:
    """
    Validate if a string is valid JSON.
    
    Args:
        json_string: String to validate
    
    Returns:
        True if valid JSON, False otherwise
    """
    try:
        json.loads(json_string)
        return True
    except json.JSONDecodeError:
        return False


def minify_json(json_string: str) -> str:
    """
    Minify JSON string by removing unnecessary whitespace.
    
    Args:
        json_string: JSON string to minify
    
    Returns:
        Minified JSON string
    """
    try:
        data = json.loads(json_string)
        return json.dumps(data, separators=(',', ':'))
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON: {e}")


def prettify_json(json_string: str, indent: int = 2) -> str:
    """
    Prettify JSON string with proper indentation.
    
    Args:
        json_string: JSON string to prettify
        indent: Number of spaces for indentation (default: 2)
    
    Returns:
        Prettified JSON string
    """
    try:
        data = json.loads(json_string)
        return json.dumps(data, indent=indent, ensure_ascii=False)
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON: {e}")


def main():
    parser = argparse.ArgumentParser(description='Format and beautify JSON data')
    parser.add_argument('input', nargs='?', help='Input JSON file (default: stdin)')
    parser.add_argument('-o', '--output', help='Output file (default: stdout)')
    parser.add_argument('-i', '--indent', type=int, default=2, help='Indentation spaces (default: 2)')
    parser.add_argument('-s', '--sort-keys', action='store_true', help='Sort keys alphabetically')
    parser.add_argument('-c', '--compact', action='store_true', help='Compact output')
    parser.add_argument('-m', '--minify', action='store_true', help='Minify JSON')
    parser.add_argument('-p', '--prettify', action='store_true', help='Prettify JSON')
    parser.add_argument('-v', '--validate', action='store_true', help='Validate JSON')
    parser.add_argument('--ensure-ascii', action='store_true', help='Escape non-ASCII characters')
    
    args = parser.parse_args()
    
    # Read input
    if args.input:
        try:
            with open(args.input, 'r', encoding='utf-8') as f:
                json_string = f.read()
        except FileNotFoundError:
            print(f"Error: File '{args.input}' not found", file=sys.stderr)
            sys.exit(1)
    else:
        json_string = sys.stdin.read()
    
    # Validate if requested
    if args.validate:
        if validate_json(json_string):
            print("Valid JSON")
            sys.exit(0)
        else:
            print("Invalid JSON", file=sys.stderr)
            sys.exit(1)
    
    # Process JSON
    try:
        if args.minify:
            result = minify_json(json_string)
        elif args.prettify:
            result = prettify_json(json_string, args.indent)
        else:
            data = json.loads(json_string)
            result = format_json(
                data,
                indent=args.indent,
                sort_keys=args.sort_keys,
                ensure_ascii=args.ensure_ascii,
                compact=args.compact
            )
        
        # Output result
        if args.output:
            with open(args.output, 'w', encoding='utf-8') as f:
                f.write(result)
            print(f"Formatted JSON written to '{args.output}'")
        else:
            print(result)
            
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON - {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()