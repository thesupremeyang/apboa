#!/usr/bin/env python
# -*- coding: utf-8 -*-
import re
import sys

# Set stdout encoding to utf-8
sys.stdout.reconfigure(encoding='utf-8')

# Read the file
with open('src/main/java/com/myagent/ChatServer.java', 'r', encoding='utf-8') as f:
    content = f.read()

# The file has literal \n in Java strings
# Let's find and replace the workflow sections

# Workflow B - replace step 5
old_b5 = '"5. **打包上传** — 将技能目录打包为 .zip 并上传到平台\\n"'
new_b5 = '"5. **打包上传（必须执行）** — 技能安装后，必须执行以下操作将技能上传到平台：\\n"' + \
         ' +\\n' + \
         '            "   a. 用 PowerShell 打包为 .zip：\\n"' + \
         ' +\\n' + \
         '            "      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n"' + \
         ' +\\n' + \
         '            "   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n"' + \
         ' +\\n' + \
         '            "   c. 告知用户上传结果（成功/失败）\\n"'

if old_b5 in content:
    content = content.replace(old_b5, new_b5)
    print('Workflow B step 5 replaced')
else:
    print('Workflow B step 5 NOT found')
    # Debug
    idx = content.find('打包上传')
    if idx >= 0:
        print('Found at idx', idx)
        print('Context:', repr(content[idx-10:idx+50]))

# Workflow C - replace step 3
old_c3 = '"3. **打包上传** — 将技能目录打包为 .zip 并上传到平台\\n"'
new_c3 = '"3. **打包上传（必须执行）** — 技能创建后，必须执行以下操作将技能上传到平台：\\n"' + \
         ' +\\n' + \
         '            "   a. 用 PowerShell 打包为 .zip：\\n"' + \
         ' +\\n' + \
         '            "      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n"' + \
         ' +\\n' + \
         '            "   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n"' + \
         ' +\\n' + \
         '            "   c. 告知用户上传结果（成功/失败）\\n"'

if old_c3 in content:
    content = content.replace(old_c3, new_c3)
    print('Workflow C step 3 replaced')
else:
    print('Workflow C step 3 NOT found')

# Also update the workflow B header to emphasize must complete
old_b_header = '"当用户想要搜索某个技能时：\\n"'
new_b_header = '"当用户想要搜索某个技能时，必须完成以下所有步骤（不能跳过上传）：\\n"'

if old_b_header in content:
    content = content.replace(old_b_header, new_b_header)
    print('Workflow B header updated')
else:
    print('Workflow B header NOT found')

# Also update the workflow C header
old_c_header = '"当用户想要创建一个新技能时：\\n"'
new_c_header = '"当用户想要创建一个新技能时，必须完成以下所有步骤（不能跳过上传）：\\n"'

if old_c_header in content:
    content = content.replace(old_c_header, new_c_header)
    print('Workflow C header updated')
else:
    print('Workflow C header NOT found')

# Write back
with open('src/main/java/com/myagent/ChatServer.java', 'w', encoding='utf-8') as f:
    f.write(content)

print('File saved')
