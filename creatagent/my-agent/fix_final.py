#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys

# Read the file
with open('src/main/java/com/myagent/ChatServer.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Find and fix Workflow B (line 72) and Workflow C (line 78)
# The problem is that the replacement put everything on one line

# We need to replace lines 72 and 78 with properly formatted multi-line strings

# First, let's find the exact line numbers
new_lines = []
i = 0
while i < len(lines):
    line = lines[i]

    # Fix Workflow B step 5 (line 72)
    if '"5. **打包上传（必须执行）**' in line and '技能安装后' in line:
        # Replace with properly formatted multi-line strings
        new_lines.append('            "5. **打包上传（必须执行）** — 技能安装后，必须执行以下操作将技能上传到平台：\\n" +\n')
        new_lines.append('            "   a. 用 PowerShell 打包为 .zip：\\n" +\n')
        new_lines.append('            "      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n" +\n')
        new_lines.append('            "   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n" +\n')
        new_lines.append('            "   c. 告知用户上传结果（成功/失败）\\n" +\n')
        new_lines.append('            "6. **后续** — 询问用户是否需要将该技能关联到某个智能体\\n" +\n')
        i += 1  # Skip the old line
        print(f'Fixed Workflow B at line {i}')
        continue

    # Fix Workflow C step 3 (line 78)
    if '"3. **打包上传（必须执行）**' in line and '技能创建后' in line:
        # Replace with properly formatted multi-line strings
        new_lines.append('            "3. **打包上传（必须执行）** — 技能创建后，必须执行以下操作将技能上传到平台：\\n" +\n')
        new_lines.append('            "   a. 用 PowerShell 打包为 .zip：\\n" +\n')
        new_lines.append('            "      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n" +\n')
        new_lines.append('            "   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n" +\n')
        new_lines.append('            "   c. 告知用户上传结果（成功/失败）\\n" +\n')
        new_lines.append('            "4. **后续** — 询问用户是否需要将该技能关联到某个智能体\\n" +\n')
        i += 1  # Skip the old line
        print(f'Fixed Workflow C at line {i}')
        continue

    new_lines.append(line)
    i += 1

# Write back
with open('src/main/java/com/myagent/ChatServer.java', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print('File saved')
