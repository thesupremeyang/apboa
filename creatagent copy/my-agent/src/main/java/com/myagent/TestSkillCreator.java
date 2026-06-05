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
import java.util.List;

/**
 * 测试 skill-creator skill 的执行
 */
public class TestSkillCreator {

    static final String BASE_URL = "https://token-plan-sgp.xiaomimimo.com";
    static final String API_KEY = "tp-sefnsoeah0svii3iz9lkvhxdtxhjdafbr9hobixbsgvr0b0j";
    static final String MODEL = "mimo-v2.5-pro";

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

        System.out.println("========================================");
        System.out.println("测试: 触发 skill-creator skill");
        System.out.println("========================================");
        System.out.println();

        MemoryPrompt prompt = new MemoryPrompt();
        prompt.setSystemMessage(AgentPrompts.SYSTEM_PROMPT);
        prompt.addTool(skillsTool);
        prompt.addTools(CommonTools.getAllCommonsTools());

        // 这个请求应该触发 skill-creator
        prompt.addUserMessage("帮我创建一个名为 'json-formatter' 的 skill，功能是格式化和美化 JSON 数据");

        System.out.println("用户: 帮我创建一个名为 'json-formatter' 的 skill，功能是格式化和美化 JSON 数据");
        System.out.println();

        // 同步调用循环
        int maxRounds = 20;
        for (int round = 0; round < maxRounds; round++) {
            System.out.println("--- 轮次 " + (round + 1) + " ---");

            AiMessageResponse response = chatModel.chat(prompt);

            String content = response.getMessage().getContent();
            if (content != null && !content.isEmpty()) {
                System.out.println("AI: " + content);
            }

            List<ToolCall> toolCalls = response.getMessage().getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                System.out.println("\n[对话结束]");
                break;
            }

            prompt.addMessage(response.getMessage());

            System.out.println("\n工具调用:");
            for (ToolCall tc : toolCalls) {
                String arguments = tc.getArguments();
                if (arguments != null && arguments.length() > 200) {
                    System.out.println("  → " + tc.getName() + ": " + arguments.substring(0, 200) + "...");
                } else {
                    System.out.println("  → " + tc.getName() + ": " + arguments);
                }
            }

            List<ToolMessage> toolMessages;
            try {
                toolMessages = response.executeToolCallsAndGetToolMessages();
            } catch (Exception e) {
                System.out.println("  ⚠ 工具执行异常: " + e.getMessage());
                // 手动生成错误的 ToolMessage 让对话继续
                toolMessages = new java.util.ArrayList<>();
                for (ToolCall tc : toolCalls) {
                    ToolMessage errMsg = new ToolMessage();
                    errMsg.setToolCallId(tc.getId());
                    errMsg.setContent("工具执行失败: " + e.getMessage());
                    toolMessages.add(errMsg);
                }
            }
            for (ToolMessage tm : toolMessages) {
                String tc = tm.getContent();
                if (tc != null && tc.length() > 300) {
                    System.out.println("  返回: " + tc.substring(0, 300) + "...");
                } else {
                    System.out.println("  返回: " + tc);
                }
            }

            prompt.addMessages(toolMessages);
            System.out.println();
        }

        // 检查生成的 skill 文件
        System.out.println("========================================");
        System.out.println("检查生成的 skill 文件:");
        System.out.println("========================================");
        File generatedSkill = new File("skills/json-formatter");
        if (generatedSkill.exists()) {
            System.out.println("目录存在: " + generatedSkill.getAbsolutePath());
            for (File f : generatedSkill.listFiles()) {
                System.out.println("  文件: " + f.getName() + " (" + f.length() + " bytes)");
            }
            // 读取 SKILL.md 内容
            File skillMd = new File(generatedSkill, "SKILL.md");
            if (skillMd.exists()) {
                try {
                    String md = new String(java.nio.file.Files.readAllBytes(skillMd.toPath()));
                    System.out.println("\n--- SKILL.md 内容 ---");
                    System.out.println(md);
                } catch (Exception e) {
                    System.out.println("读取失败: " + e.getMessage());
                }
            }
        } else {
            System.out.println("目录不存在，skill 未被创建");
        }
    }
}
