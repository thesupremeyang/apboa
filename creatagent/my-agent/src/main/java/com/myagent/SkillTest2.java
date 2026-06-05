package com.myagent;

import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.model.client.StreamContext;
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
 * 测试 find-skills 和 skill-creator skill 的触发
 */
public class SkillTest2 {

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

        MemoryPrompt prompt = new MemoryPrompt();
        prompt.setSystemMessage("你是一个强大的 AI 助手。当用户的请求匹配某个 skill 时，必须优先使用该 skill。" +
                "不要自己直接回答，而是通过调用 skill 来完成任务。回答使用中文。");
        prompt.addTool(skillsTool);
        prompt.addTools(CommonTools.getAllCommonsTools());

        // 测试场景：应该触发 find-skills-wzr-999 skill
        String question = "我想找一个能帮我自动部署项目的 skill，帮我搜索一下有没有现成的";
        System.out.println("========================================");
        System.out.println("测试: 触发 find-skills-wzr-999 skill");
        System.out.println("========================================");
        System.out.println("用户: " + question);
        System.out.print("AI: ");

        prompt.addUserMessage(question);

        chatModel.chatStream(prompt, new StreamResponseListener() {
            @Override
            public void onStart(StreamContext context) {}

            @Override
            public void onMessage(StreamContext context, AiMessageResponse response) {
                String content = response.getMessage().getContent();
                if (content != null) {
                    System.out.print(content);
                }

                if (response.getMessage().getToolCalls() != null
                        && !response.getMessage().getToolCalls().isEmpty()) {
                    // 打印工具调用详情
                    System.out.println("\n\n--- 工具调用详情 ---");
                    for (ToolCall tc : response.getMessage().getToolCalls()) {
                        System.out.println("  工具: " + tc.getName());
                        System.out.println("  参数: " + tc.getArguments());
                    }
                    System.out.println("--- 执行工具 ---");

                    prompt.addMessage(response.getMessage());
                    List<ToolMessage> toolMessages = response.executeToolCallsAndGetToolMessages();

                    // 打印工具返回结果
                    for (ToolMessage tm : toolMessages) {
                        String toolContent = tm.getContent();
                        if (toolContent != null && toolContent.length() > 500) {
                            System.out.println("  工具返回: " + toolContent.substring(0, 500) + "...");
                        } else {
                            System.out.println("  工具返回: " + toolContent);
                        }
                    }

                    prompt.addMessages(toolMessages);
                    System.out.println("\n--- AI 继续回答 ---");
                    System.out.print("AI: ");
                    chatModel.chatStream(prompt, this);
                }
            }

            @Override
            public void onStop(StreamContext context) {
                System.out.println("\n\n========================================");
                System.out.println("测试完成");
                System.out.println("========================================");
            }

            @Override
            public void onFailure(StreamContext context, Throwable throwable) {
                System.err.println("\n调用失败: " + throwable.getMessage());
            }
        });
    }
}
