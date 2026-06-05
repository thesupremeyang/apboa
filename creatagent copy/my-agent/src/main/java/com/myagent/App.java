package com.myagent;

import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.message.ToolCall;
import com.agentsflex.core.message.ToolMessage;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.model.chat.openai.OpenAIChatConfig;
import com.agentsflex.model.chat.openai.OpenAIChatModel;
import com.agentsflex.skill.SkillsTool;
import com.agentsflex.tool.commons.CommonTools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Apboa 智能体平台 AI 助手
 *
 * 核心功能：
 * A. 创建智能体 — 搜索/创建技能 → 上传 → 创建 Agent
 * B. 搜索并上传技能 — 从开源社区搜索 → 安装 → 上传平台
 * C. 创建技能 — 使用 skill-creator 创建 → 上传平台
 */
public class App {

    // ========== 配置 ==========
    static final String BASE_URL = "https://token-plan-sgp.xiaomimimo.com";
    static final String API_KEY = "tp-sefnsoeah0svii3iz9lkvhxdtxhjdafbr9hobixbsgvr0b0j";
    static final String MODEL = "mimo-v2.5-pro";

    static final String SYSTEM_PROMPT =
            "你是 Apboa 智能体平台的 AI 助手。你的核心职责是帮助用户通过自然语言管理平台上的智能体和技能。\n" +
            "\n" +
            "## 你拥有的能力\n" +
            "\n" +
            "### 1. Apboa 平台 CLI 工具\n" +
            "通过 `opencli apboa` 命令管理平台资源（智能体、技能、工具、知识库、MCP、钩子、敏感词等）。\n" +
            "当需要操作平台时，使用 Bash 工具执行 CLI 命令。需要 JSON 输出时用 `-f json`。\n" +
            "\n" +
            "### 2. 技能搜索 (find-skills-wzr-999)\n" +
            "当需要从开源社区搜索技能时，触发 `find-skills-wzr-999` skill，然后按其指引执行 `npx skills find <keyword>` 搜索。\n" +
            "\n" +
            "### 3. 技能创建 (skill-creator)\n" +
            "当没有合适的现有技能时，触发 `skill-creator` skill 从零创建新技能。\n" +
            "\n" +
            "## 核心工作流程\n" +
            "\n" +
            "### 工作流 A：创建智能体\n" +
            "当用户想要创建一个智能体时，按以下步骤执行：\n" +
            "1. **理解需求** — 分析用户想要什么样的智能体（名称、功能、描述、系统提示词）\n" +
            "2. **并行查询** — 同时执行：\n" +
            "   - `opencli apboa skill-list -f json` 查看平台已有技能\n" +
            "   - `opencli apboa model-list -f json` 获取 modelConfigId\n" +
            "3. **匹配技能** — 从已有技能中挑选匹配的 skillIds\n" +
            "4. **如果缺少技能** — 触发 `find-skills-wzr-999` skill，执行 `npx skills find <keyword>` 从社区搜索\n" +
            "5. **安装并上传** — 执行 `npx skills add <package> -g -y` 安装，然后打包为 .zip 并 `opencli apboa skill-upload <path> --category <cat>`\n" +
            "6. **如果社区也没有** — 触发 `skill-creator` 创建新技能，然后打包上传\n" +
            "7. **创建智能体** — 执行 `opencli apboa agent-create --name <n> --code <c> --description <d> --systemPrompt <sp> --skillIds <ids> --modelConfigId <mid>`\n" +
            "8. **验证** — 执行 `opencli apboa agent-list -f json` 确认创建成功\n" +
            "\n" +
            "### 工作流 B：搜索并上传技能\n" +
            "当用户想要搜索某个技能时：\n" +
            "1. **理解需求** — 明确用户需要什么功能的技能\n" +
            "2. **社区搜索** — 触发 `find-skills-wzr-999` skill，执行 `npx skills find <keyword>`\n" +
            "3. **展示结果** — 将搜索结果以表格形式展示给用户\n" +
            "4. **用户确认后安装** — 执行 `npx skills add <package> -g -y`\n" +
            "5. **打包上传** — 将技能目录打包为 .zip 并上传到平台\n" +
            "\n" +
            "### 工作流 C：创建技能\n" +
            "当用户想要创建一个新技能时：\n" +
            "1. **理解需求** — 分析技能的功能、触发条件、使用场景\n" +
            "2. **创建技能** — 触发 `skill-creator` skill，按其指引创建 SKILL.md 和相关资源\n" +
            "3. **打包上传** — 将技能目录打包为 .zip 并上传到平台\n" +
            "\n" +
            "## 重要规则\n" +
            "1. **每一步都要向用户汇报进度** — 告诉用户当前在做什么\n" +
            "2. **遇到问题要询问用户** — 不要自己猜测不确定的信息\n" +
            "3. **使用中文回答**\n" +
            "4. **CLI 命令需要 JSON 输出时用 `-f json`**\n" +
            "5. **打包 .zip 用 PowerShell** — 必须用完整 Windows 路径，示例：\n" +
            "   `powershell.exe -Command \"Compress-Archive -Path 'C:\\Users\\14420\\.agents\\skills\\skill-name\\*' -DestinationPath 'C:\\Users\\14420\\skill-name.zip' -Force\"`\n" +
            "   注意：不要用 $HOME 或 ~，必须用 `C:\\Users\\14420` 这样的完整路径\n" +
            "6. **创建智能体需要 skillIds 和 modelConfigId** — 必须先查询获取\n" +
            "7. **npx skills 安装路径** — `C:\\Users\\14420\\.agents\\skills\\<name>/`\n" +
            "8. **并行执行** — 能并行的操作尽量并行\n" +
            "9. **不要过度验证** — 上传成功后直接进行下一步\n" +
            "10. **创建智能体时** — 如果 skillIds 为空，可以不传该参数";

    public static void main(String[] args) {
        String skillsDir = new File("skills").getAbsolutePath();

        OpenAIChatConfig config = new OpenAIChatConfig();
        config.setEndpoint(BASE_URL);
        config.setApiKey(API_KEY);
        config.setModel(MODEL);
        config.setLogEnabled(false);

        ChatModel chatModel = new OpenAIChatModel(config);

        Tool skillsTool = SkillsTool.builder()
                .addSkillsDirectory(skillsDir)
                .build();

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║       Apboa 智能体平台 AI 助手                    ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║  我可以帮你：                                      ║");
        System.out.println("║  1. 创建智能体 — 告诉我你想要什么功能               ║");
        System.out.println("║  2. 搜索技能   — 从开源社区找到合适的 skill        ║");
        System.out.println("║  3. 创建技能   — 按你的需求从零创建 skill          ║");
        System.out.println("║  4. 管理平台   — 查看/管理平台上的各种资源         ║");
        System.out.println("║                                                    ║");
        System.out.println("║  输入 'quit' 退出                                  ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("你: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;
            if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                System.out.println("再见！");
                break;
            }

            System.out.println();

            // 创建新的对话 prompt
            MemoryPrompt prompt = new MemoryPrompt();
            prompt.setSystemMessage(AgentPrompts.SYSTEM_PROMPT);
            prompt.addTool(skillsTool);
            prompt.addTools(CommonTools.getAllCommonsTools());
            prompt.addUserMessage(input);

            // 同步对话循环
            chatLoop(chatModel, prompt);

            System.out.println();
        }

        scanner.close();
    }

    private static void chatLoop(ChatModel chatModel, MemoryPrompt prompt) {
        int maxRounds = 20;
        for (int round = 0; round < maxRounds; round++) {
            AiMessageResponse response = chatModel.chat(prompt);

            // 输出 AI 文本
            String content = response.getMessage().getContent();
            if (content != null && !content.isEmpty()) {
                System.out.println("AI: " + content);
            }

            // 检查工具调用
            List<ToolCall> toolCalls = response.getMessage().getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                return;
            }

            // 保存 AI 消息
            prompt.addMessage(response.getMessage());

            // 执行工具
            List<ToolMessage> toolMessages;
            try {
                toolMessages = response.executeToolCallsAndGetToolMessages();
            } catch (Exception e) {
                System.out.println("  ⚠ 工具执行异常: " + e.getMessage());
                toolMessages = new ArrayList<>();
                for (ToolCall tc : toolCalls) {
                    ToolMessage errMsg = new ToolMessage();
                    errMsg.setToolCallId(tc.getId());
                    errMsg.setContent("工具执行失败: " + e.getMessage());
                    toolMessages.add(errMsg);
                }
            }

            prompt.addMessages(toolMessages);
        }
    }
}
