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
 * 测试 apboa-cli skill — 验证 Agent 能否调用 Apboa 平台所有功能
 */
public class TestApboaCLI {

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

        // 测试用例列表
        String[] testCases = {
            "帮我查看 Apboa 平台上有哪些智能体",
            "帮我查看 Apboa 平台上有哪些技能",
            "帮我查看 Apboa 平台上有哪些模型配置",
            "帮我查看 Apboa 平台上的工具列表",
            "帮我查看 Apboa 平台上的知识库列表",
            "帮我查看 Apboa 平台上的 MCP 服务器配置",
        };

        for (int i = 0; i < testCases.length; i++) {
            System.out.println("========================================");
            System.out.println("测试 " + (i + 1) + ": " + testCases[i]);
            System.out.println("========================================");

            MemoryPrompt prompt = new MemoryPrompt();
            prompt.setSystemMessage(AgentPrompts.SYSTEM_PROMPT);
            prompt.addTool(skillsTool);
            prompt.addTools(CommonTools.getAllCommonsTools());

            prompt.addUserMessage(testCases[i]);

            // 同步调用循环
            int maxRounds = 5;
            for (int round = 0; round < maxRounds; round++) {
                AiMessageResponse response = chatModel.chat(prompt);

                String content = response.getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    System.out.println("AI: " + content);
                }

                List<ToolCall> toolCalls = response.getMessage().getToolCalls();
                if (toolCalls == null || toolCalls.isEmpty()) {
                    break;
                }

                prompt.addMessage(response.getMessage());

                System.out.println("  [工具调用]");
                for (ToolCall tc : toolCalls) {
                    String arguments = tc.getArguments();
                    if (arguments != null && arguments.length() > 150) {
                        System.out.println("    → " + tc.getName() + ": " + arguments.substring(0, 150) + "...");
                    } else {
                        System.out.println("    → " + tc.getName() + ": " + arguments);
                    }
                }

                List<ToolMessage> toolMessages;
                try {
                    toolMessages = response.executeToolCallsAndGetToolMessages();
                } catch (Exception e) {
                    System.out.println("    ⚠ 工具执行异常: " + e.getMessage());
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
                        System.out.println("    返回: " + tc.substring(0, 300) + "...");
                    } else {
                        System.out.println("    返回: " + tc);
                    }
                }

                prompt.addMessages(toolMessages);
            }

            System.out.println();
        }

        System.out.println("========================================");
        System.out.println("所有测试完成");
        System.out.println("========================================");
    }
}
