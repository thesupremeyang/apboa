---
name: skill-creator
description: Create or update an Apboa-compatible agent skill from a user's natural-language requirements. Use when the user asks to create a new skill, build a reusable capability, define skill trigger conditions, or package a skill for upload to Apboa.
---

# Skill Creator

Create a minimal, valid skill directory that can be packaged as a zip and uploaded to Apboa.

## Output Location

Create skills under the current project unless the user gives another path:

`C:\Users\14420\Desktop\creatagent\my-agent\skills\<skill-name>`

Use lowercase kebab-case names, for example `sql-query-generator`.

## Workflow

1. Understand the requested capability:
   - skill name
   - trigger situations
   - inputs and outputs
   - whether scripts or references are needed

2. Create the smallest useful skill:
   - always create `SKILL.md`
   - create `scripts/` only when deterministic code is useful
   - create `references/` only when reusable domain notes are needed
   - do not create README, changelog, installation guide, or unrelated docs

3. Write `SKILL.md` with only required frontmatter:

```markdown
---
name: <skill-name>
description: <what the skill does and exactly when to use it>
---

# <Human Title>

<Short operational instructions for the agent.>
```

4. Validate the skill before packaging:
   - `SKILL.md` exists
   - frontmatter has `name` and `description`
   - description includes trigger conditions
   - any script mentioned in `SKILL.md` exists

5. Package the skill as a zip for Apboa. The zip must contain `skills/<skill-name>/SKILL.md`, so stage the upload folder first:

```powershell
New-Item -ItemType Directory -Path 'C:\Users\14420\Desktop\creatagent\my-agent\temp_upload\skills' -Force
Copy-Item -Path 'C:\Users\14420\Desktop\creatagent\my-agent\skills\<skill-name>' -Destination 'C:\Users\14420\Desktop\creatagent\my-agent\temp_upload\skills' -Recurse -Force
Compress-Archive -Path 'C:\Users\14420\Desktop\creatagent\my-agent\temp_upload\skills' -DestinationPath 'C:\Users\14420\Desktop\creatagent\my-agent\temp_zips\<skill-name>.zip' -Force
```

6. Upload it:

```powershell
opencli apboa skill-upload 'C:\Users\14420\Desktop\creatagent\my-agent\temp_zips\<skill-name>.zip' --category '通用' -f json
```

If `opencli` is unavailable, run the same command as:

```powershell
npx opencli apboa skill-upload 'C:\Users\14420\Desktop\creatagent\my-agent\temp_zips\<skill-name>.zip' --category '通用' -f json
```

7. After upload, run `opencli apboa skill-list -f json` and capture the uploaded skill ID when another workflow needs to attach it to an agent.

## Quality Rules

- Keep the skill focused on the user's stated need.
- Prefer concise procedural instructions over broad background explanation.
- Add scripts only when they remove real repeated work.
- Ask the user only when the missing information blocks a safe implementation.
