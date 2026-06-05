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
 * 同步调用测试 — 避免流式 tool_calls 分片问题
 */
public class SkillTestSync {

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

        // ========== 测试: 触发 find-skills skill ==========
        System.out.println("========================================");
        System.out.println("测试: 触发 find-skills-wzr-999 skill (同步调用)");
        System.out.println("========================================");

        MemoryPrompt prompt = new MemoryPrompt();
        prompt.setSystemMessage("你是一个强大的 AI 助手。当用户的请求匹配某个 skill 时，必须优先使用该 skill 来完成任务。回答使用中文。");
        prompt.addTool(skillsTool);
        prompt.addTools(CommonTools.getAllCommonsTools());

        prompt.addUserMessage("我想找一个能帮我自动部署项目的 skill，帮我搜索一下有没有现成的");

        System.out.println("用户: 我想找一个能帮我自动部署项目的 skill，帮我搜索一下有没有现成的");
        System.out.println();

        // 同步调用循环（处理 tool_calls）
        int maxRounds = 5;
        for (int round = 0; round < maxRounds; round++) {
            AiMessageResponse response = chatModel.chat(prompt);

            String content = response.getMessage().getContent();
            if (content != null) {
                System.out.println("AI: " + content);
            }

            // 检查是否有工具调用
            List<ToolCall> toolCalls = response.getMessage().getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                System.out.println("\n[对话结束，无更多工具调用]");
                break;
            }

            // 保存 AI 消息
            prompt.addMessage(response.getMessage());

            // 执行工具调用
            System.out.println("\n--- 工具调用 ---");
            for (ToolCall tc : toolCalls) {
                System.out.println("  工具: " + tc.getName());
                System.out.println("  参数: " + tc.getArguments());
            }

            List<ToolMessage> toolMessages = response.executeToolCallsAndGetToolMessages();
            for (ToolMessage tm : toolMessages) {
                String tc = tm.getContent();
                if (tc != null && tc.length() > 300) {
                    System.out.println("  返回: " + tc.substring(0, 300) + "...");
                } else {
                    System.out.println("  返回: " + tc);
                }
            }

            prompt.addMessages(toolMessages);
            System.out.println("\n--- 继续对话 (轮次 " + (round + 2) + ") ---");
        }

        System.out.println("\n========================================");
        System.out.println("测试完成");
        System.out.println("========================================");
    }
}
