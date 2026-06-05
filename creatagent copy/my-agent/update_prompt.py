import re

# Read as binary to handle CRLF
with open('src/main/java/com/myagent/ChatServer.java', 'rb') as f:
    content = f.read()

# Normalize to LF for replacement
content_str = content.decode('utf-8').replace('\\r\\n', '\\n')

# Workflow B replacement
old_b = '当用户想要搜索某个技能时：\\n' + \
        '1. **理解需求** — 明确用户需要什么功能的技能\\n' + \
        '2. **社区搜索** — 触发 `find-skills-wzr-999` skill，执行 `npx skills find <keyword>`\\n' + \
        '3. **展示结果** — 将搜索结果以表格形式展示给用户\\n' + \
        '4. **用户确认后安装** — 执行 `npx skills add <package> -g -y`\\n' + \
        '5. **打包上传** — 将技能目录打包为 .zip 并上传到平台'

new_b = '当用户想要搜索某个技能时，必须完成以下所有步骤（不能跳过上传）：\\n' + \
        '1. **理解需求** — 明确用户需要什么功能的技能\\n' + \
        '2. **社区搜索** — 触发 `find-skills-wzr-999` skill，执行 `npx skills find <keyword>`\\n' + \
        '3. **展示结果** — 将搜索结果以表格形式展示给用户，询问用户想安装哪个\\n' + \
        '4. **用户确认后安装** — 执行 `npx skills add <package> -g -y` 安装技能到本地\\n' + \
        '5. **打包上传（必须执行）** — 技能安装后，必须执行以下操作将技能上传到平台：\\n' + \
        '   a. 用 PowerShell 打包为 .zip：\\n' + \
        '      `powershell.exe -Command "Compress-Archive -Path \\'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\\\*\\' -DestinationPath \\'C:\\\\Users\\\\14420\\\\<skill-name>.zip\\' -Force"`\\n' + \
        '   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n' + \
        '   c. 告知用户上传结果（成功/失败）\\n' + \
        '6. **后续** — 询问用户是否需要将该技能关联到某个智能体'

if old_b in content_str:
    content_str = content_str.replace(old_b, new_b)
    print('Workflow B replaced')
else:
    print('Workflow B NOT found')
    # Debug: print a snippet
    idx = content_str.find('当用户想要搜索某个技能时')
    if idx >= 0:
        print('Found at idx', idx)
        print('Snippet:', repr(content_str[idx:idx+200]))

# Workflow C replacement
old_c = '当用户想要创建一个新技能时：\\n' + \
        '1. **理解需求** — 分析技能的功能、触发条件、使用场景\\n' + \
        '2. **创建技能** — 触发 `skill-creator` skill，按其指引创建 SKILL.md 和相关资源\\n' + \
        '3. **打包上传** — 将技能目录打包为 .zip 并上传到平台'

new_c = '当用户想要创建一个新技能时，必须完成以下所有步骤（不能跳过上传）：\\n' + \
        '1. **理解需求** — 分析技能的功能、触发条件、使用场景\\n' + \
        '2. **创建技能** — 触发 `skill-creator` skill，按其指引创建 SKILL.md 和相关资源\\n' + \
        '3. **打包上传（必须执行）** — 技能创建后，必须执行以下操作将技能上传到平台：\\n' + \
        '   a. 用 PowerShell 打包为 .zip：\\n' + \
        '      `powershell.exe -Command "Compress-Archive -Path \\'C:\\\\Users\\\\14420\\\\.agents\\\\skills\\\\<skill-name>\\\\*\\' -DestinationPath \\'C:\\\\Users\\\\14420\\\\<skill-name>.zip\\' -Force"`\\n' + \
        '   b. 上传到平台：`opencli apboa skill-upload C:\\\\Users\\\\14420\\\\<skill-name>.zip --category <category>`\\n' + \
        '   c. 告知用户上传结果（成功/失败）\\n' + \
        '4. **后续** — 询问用户是否需要将该技能关联到某个智能体'

if old_c in content_str:
    content_str = content_str.replace(old_c, new_c)
    print('Workflow C replaced')
else:
    print('Workflow C NOT found')

# Write back with original line endings
with open('src/main/java/com/myagent/ChatServer.java', 'wb') as f:
    f.write(content_str.encode('utf-8'))

print('File saved')
