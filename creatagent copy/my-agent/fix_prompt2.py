#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys

# Set stdout encoding to utf-8
sys.stdout.reconfigure(encoding='utf-8')

# Read the file
with open('src/main/java/com/myagent/ChatServer.java', 'r', encoding='utf-8') as f:
    content = f.read()

# The file has literal \n in Java strings (two characters: backslash + n)
# Let's find and replace the workflow sections with proper Java string format

# Workflow B - replace step 5 and add steps 6
old_b5 = '"5. **打包上传** — 将技能目录打包为 .zip 并上传到平台\\n"'
new_b5 = '"5. **打包上传（必须执行）** — 技能安装后，必须执行以下操作将技能上传到平台：\\n"' + \
         ' +\\n            ' + \
         '"   a. 用 PowerShell 打包为 .zip：\\n"' + \
         ' +\\n            ' + \
         '"      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n"' + \
         ' +\\n            ' + \
         '"   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n"' + \
         ' +\\n            ' + \
         '"   c. 告知用户上传结果（成功/失败）\\n"' + \
         ' +\\n            ' + \
         '"6. **后续** — 询问用户是否需要将该技能关联到某个智能体\\n"'

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

# Workflow C - replace step 3 and add step 4
old_c3 = '"3. **打包上传** — 将技能目录打包为 .zip 并上传到平台\\n"'
new_c3 = '"3. **打包上传（必须执行）** — 技能创建后，必须执行以下操作将技能上传到平台：\\n"' + \
         ' +\\n            ' + \
         '"   a. 用 PowerShell 打包为 .zip：\\n"' + \
         ' +\\n            ' + \
         '"      `powershell.exe -Command \\"Compress-Archive -Path \'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\*\' -DestinationPath \'C:\\\\Users\\\\14420\\\\<skill-name>.zip\' -Force\\"`\\n"' + \
         ' +\\n            ' + \
         '"   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n"' + \
         ' +\\n            ' + \
         '"   c. 告知用户上传结果（成功/失败）\\n"' + \
         ' +\\n            ' + \
         '"4. **后续** — 询问用户是否需要将该技能关联到某个智能体\\n"'

if old_c3 in content:
    content = content.replace(old_c3, new_c3)
    print('Workflow C step 3 replaced')
else:
    print('Workflow C step 3 NOT found')

# Write back
with open('src/main/java/com/myagent/ChatServer.java', 'w', encoding='utf-8') as f:
    f.write(content)

print('File saved')
