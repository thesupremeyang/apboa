#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys

# Set stdout encoding to utf-8
sys.stdout.reconfigure(encoding='utf-8')

# Read the file
with open('src/main/java/com/myagent/ChatServer.java', 'r', encoding='utf-8') as f:
    content = f.read()

# The issue is that the previous replacement created invalid Java syntax
# We need to fix the format: each new line should be a separate string concatenation

# Fix Workflow B - the bad replacement
old_bad_b = '"5. **打包上传（必须执行）** — 技能安装后，必须执行以下操作将技能上传到平台：\\n" +\\n            "   a. 用 PowerShell 打包为 .zip：\\n" +\\n            "      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n" +\\n            "   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n" +\\n            "   c. 告知用户上传结果（成功/失败）\\n"'

new_good_b = '"5. **打包上传（必须执行）** — 技能安装后，必须执行以下操作将技能上传到平台：\\n"' + \
             ' +\\n            ' + \
             '"   a. 用 PowerShell 打包为 .zip：\\n"' + \
             ' +\\n            ' + \
             '"      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n"' + \
             ' +\\n            ' + \
             '"   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n"' + \
             ' +\\n            ' + \
             '"   c. 告知用户上传结果（成功/失败）\\n"'

if old_bad_b in content:
    content = content.replace(old_bad_b, new_good_b)
    print('Workflow B fixed')
else:
    print('Workflow B bad pattern NOT found')

# Fix Workflow C - the bad replacement
old_bad_c = '"3. **打包上传（必须执行）** — 技能创建后，必须执行以下操作将技能上传到平台：\\n" +\\n            "   a. 用 PowerShell 打包为 .zip：\\n" +\\n            "      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n" +\\n            "   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n" +\\n            "   c. 告知用户上传结果（成功/失败）\\n"'

new_good_c = '"3. **打包上传（必须执行）** — 技能创建后，必须执行以下操作将技能上传到平台：\\n"' + \
             ' +\\n            ' + \
             '"   a. 用 PowerShell 打包为 .zip：\\n"' + \
             ' +\\n            ' + \
             '"      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n"' + \
             ' +\\n            ' + \
             '"   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n"' + \
             ' +\\n            ' + \
             '"   c. 告知用户上传结果（成功/失败）\\n"'

if old_bad_c in content:
    content = content.replace(old_bad_c, new_good_c)
    print('Workflow C fixed')
else:
    print('Workflow C bad pattern NOT found')

# Write back
with open('src/main/java/com/myagent/ChatServer.java', 'w', encoding='utf-8') as f:
    f.write(content)

print('File saved')
