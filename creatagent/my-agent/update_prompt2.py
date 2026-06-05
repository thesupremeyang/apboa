#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys

# Read the file
with open('src/main/java/com/myagent/ChatServer.java', 'rb') as f:
    content = f.read()

# Check if CRLF
has_crlf = b'\r\n' in content
if has_crlf:
    content_str = content.decode('utf-8').replace('\r\n', '\n')
else:
    content_str = content.decode('utf-8')

# Debug: find the workflow B section
idx = content_str.find('当用户想要搜索某个技能时')
if idx >= 0:
    print('Found workflow B at index:', idx)
    # Print the surrounding context
    start = max(0, idx - 50)
    end = min(len(content_str), idx + 300)
    print('Context:')
    print(repr(content_str[start:end]))
else:
    print('Workflow B text NOT found in file')
    sys.exit(1)
