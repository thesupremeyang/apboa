import requests
import json

BASE_URL = "http://117.72.185.237:3000"

# Read token
with open("token.txt", "r") as f:
    token = f.read().strip()

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json; charset=utf-8"
}

# Read IDs
with open("platform_ids.json", "r") as f:
    ids = json.load(f)

# System prompt for piano tutor
system_prompt = """# 角色定义
你是琴琴，一个专业的钢琴教学AI助手。你性格温柔中带着一丝严肃，既亲切又专业靠谱，致力于帮助各种水平的用户学习钢琴演奏。

# 核心能力
1. **水平评估**：根据用户的描述或回答，准确评估其钢琴水平（零基础/初学者/中级/高级），并提供针对性的学习建议。
2. **乐理教学**：用通俗易懂的方式讲解音乐理论知识，包括音符、节拍、音阶、调性、和弦等基础概念，辅以具体示例。
3. **技巧指导**：详细讲解钢琴演奏技巧，包括正确的手型、指法、坐姿、踏板使用等，帮助用户建立良好的演奏习惯。
4. **曲目推荐**：根据用户当前水平，推荐适合练习的曲目，从简单的练习曲到经典名曲，循序渐进。
5. **错误纠正**：分析用户描述的练习问题，诊断常见错误（如节奏不稳、手指力度不均、踏板使用不当等），提供纠正方案。
6. **计划制定**：为用户制定个性化的练琴计划，明确每日练习内容、时长和目标，帮助用户系统化地提升。

# 工作流程
当用户提出需求时，按以下步骤执行：
1. **了解需求**：首先了解用户的钢琴基础、学习目标和可用练习时间。
2. **评估水平**：根据用户描述评估其当前水平，确定学习起点。
3. **制定计划**：根据评估结果，制定阶段性学习计划。
4. **教授内容**：分步骤讲解相关知识和技巧，配合具体示例。
5. **解答疑问**：耐心解答用户在练习中遇到的问题。
6. **进度跟踪**：定期询问练习情况，根据反馈调整学习计划。

# 输出格式
回答时使用以下结构：
1. 简要回应用户的问题或需求
2. 分点列出具体建议或知识点
3. 提供实用的练习方法或技巧
4. 给出下一步行动建议

# 示例对话

用户：我是零基础，想学钢琴，该从哪里开始？
琴琴：欢迎你踏上钢琴学习之旅！作为零基础学习者，我建议你从以下几个方面开始：

**1. 认识键盘**
- 钢琴有88个键，分为白键和黑键
- 黑键以2个和3个为一组交替排列
- 找到中间的C（Do），它是两个黑键左边的白键

**2. 学习基本手型**
- 手指自然弯曲，像握一个小球
- 手腕与键盘平行，不要塌陷
- 用指尖触键，不是指肚

**3. 第一首曲子**
推荐从《小星星》开始，只用右手5个手指，简单易上手。

你想先从哪个部分开始学习呢？

# 约束条件
- 保持专业性：所有建议基于科学的钢琴教学方法
- 适度鼓励：在严肃指导的同时给予适当的鼓励和肯定
- 循序渐进：避免让用户接触超出其水平的内容
- 安全提醒：适时提醒用户注意练琴姿势，避免受伤
- 坦诚边界：对于超出钢琴教学范围的问题，礼貌说明并引导回主题"""

# Agent data
agent_data = {
    "agentType": "CUSTOM",
    "name": "琴琴",
    "agentCode": "piano-tutor",
    "description": "专业的钢琴教学AI助手，性格温柔中带着一丝严肃，为各水平钢琴学习者提供系统化的学习指导，包括乐理知识、演奏技巧、曲目推荐和练琴计划。",
    "tag": "钢琴教学,音乐教育,乐器学习",
    "modelConfigId": ids["model_id"],
    "systemPromptTemplateId": ids["prompt_id"],
    "followTemplate": False,
    "systemPrompt": system_prompt,
    "toolChoiceStrategy": "AUTO",
    "specificToolName": "",
    "skill": [ids["skill_id"]],
    "tool": [],
    "knowledgeBase": [],
    "mcp": [],
    "mcpBindings": [],
    "subAgent": [],
    "hook": [],
    "sensitiveWordConfigId": None,
    "sensitiveFilterEnabled": False,
    "enablePlanning": False,
    "maxIterations": 50,
    "maxSubtasks": 10,
    "requirePlanConfirmation": False,
    "showToolProcess": True,
    "enableMemory": True,
    "enableMemoryCompression": False,
    "memoryCompressionConfig": None,
    "structuredOutputEnabled": False,
    "structuredOutputReminder": "TOOL_CHOICE",
    "structuredOutputSchema": None,
    "studioConfigId": None,
    "codeExecutionConfigId": None,
    "version": "1.0.0",
    "enabled": True
}

# Write JSON to file for curl to use
with open("agent_body.json", "w", encoding="utf-8") as f:
    json.dump(agent_data, f, ensure_ascii=False, indent=2)

print("Agent JSON body written to agent_body.json")

# Check if agent already exists
print("\n=== Checking existing agents ===")
agents_resp = requests.get(f"{BASE_URL}/api/agent/definition/page?page=1&size=1000", headers={
    "Authorization": f"Bearer {token}"
})
agents_data = agents_resp.json()

existing_agent_id = None
if agents_data.get("success"):
    for agent in agents_data["data"]["records"]:
        if agent.get("agentCode") == "piano-tutor" or agent.get("name") == "琴琴":
            existing_agent_id = agent["id"]
            print(f"Found existing agent: {agent['name']} (ID: {existing_agent_id})")
            break

# Create or update agent
if existing_agent_id:
    print(f"\n=== Updating existing agent ===")
    resp = requests.put(
        f"{BASE_URL}/api/agent/definition/{existing_agent_id}",
        headers=headers,
        json=agent_data
    )
else:
    print(f"\n=== Creating new agent ===")
    resp = requests.post(
        f"{BASE_URL}/api/agent/definition",
        headers=headers,
        json=agent_data
    )

result = resp.json()
print(f"Response: {json.dumps(result, ensure_ascii=False, indent=2)}")

if result.get("success"):
    agent_id = result.get("data", {}).get("id", existing_agent_id)
    with open("agent_id.txt", "w") as f:
        f.write(str(agent_id))
    print(f"\nAgent ID saved: {agent_id}")
else:
    print("\nAgent creation/update failed!")

print("\n=== Done! ===")