package com.myagent;

import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.model.client.StreamContext;
import com.agentsflex.core.message.ToolMessage;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.model.chat.openai.OpenAIChatConfig;
import com.agentsflex.model.chat.openai.OpenAIChatModel;
import com.agentsflex.skill.SkillsTool;
import com.agentsflex.skill.util.Skills;
import com.agentsflex.skill.Skill;
import com.agentsflex.tool.commons.CommonTools;

import java.io.File;
import java.util.List;

/**
 * 自动测试：验证 Skill 加载和调用
 */
public class SkillTest {

    static final String BASE_URL = "https://token-plan-sgp.xiaomimimo.com";
    static final String API_KEY = "tp-sefnsoeah0svii3iz9lkvhxdtxhjdafbr9hobixbsgvr0b0j";
    static final String MODEL = "mimo-v2.5-pro";

    public static void main(String[] args) throws Exception {
        String skillsDir = new File("skills").getAbsolutePath();

        // ========== 测试1: 验证 Skill 加载 ==========
        System.out.println("========== 测试1: Skill 加载 ==========");
        System.out.println("Skills 目录: " + skillsDir);

        List<Skill> skills = Skills.loadDirectory(skillsDir);
        System.out.println("加载到 " + skills.size() + " 个 Skills:");
        for (Skill skill : skills) {
            System.out.println("  - " + skill.getFrontMatter().get("name") + ": " +
                    skill.getFrontMatter().get("description"));
        }
        System.out.println();

        // ========== 测试2: 验证 SkillsTool 构建 ==========
        System.out.println("========== 测试2: SkillsTool 构建 ==========");
        Tool skillsTool = SkillsTool.builder()
                .addSkillsDirectory(skillsDir)
                .build();
        System.out.println("SkillsTool 构建成功: " + (skillsTool != null));
        System.out.println("工具名称: " + skillsTool.getName());
        System.out.println("工具描述长度: " + skillsTool.getDescription().length() + " 字符");
        System.out.println();

        // ========== 测试3: 验证 CommonTools 加载 ==========
        System.out.println("========== 测试3: CommonTools 加载 ==========");
        List<Tool> commonTools = CommonTools.getAllCommonsTools();
        System.out.println("加载到 " + commonTools.size() + " 个内置工具:");
        for (Tool tool : commonTools) {
            System.out.println("  - " + tool.getName());
        }
        System.out.println();

        // ========== 测试4: 实际调用 - 触发 find-skills skill ==========
        System.out.println("========== 测试4: 调用 AI (触发 find-skills skill) ==========");
        OpenAIChatConfig config = new OpenAIChatConfig();
        config.setEndpoint(BASE_URL);
        config.setApiKey(API_KEY);
        config.setModel(MODEL);
        config.setLogEnabled(false);

        ChatModel chatModel = new OpenAIChatModel(config);

        MemoryPrompt prompt = new MemoryPrompt();
        prompt.setSystemMessage("你是一个强大的 AI 助手。当用户的请求匹配某个 skill 时，优先使用该 skill。回答使用中文。");
        prompt.addTool(skillsTool);
        prompt.addTools(commonTools);

        // 这个问题应该触发 find-skills-wzr-999 skill
        prompt.addUserMessage("我想找一个能帮我做代码审查的 skill，有没有现成的？");

        System.out.println("用户: 我想找一个能帮我做代码审查的 skill，有没有现成的？");
        System.out.print("AI: ");

        chatModel.chatStream(prompt, new StreamResponseListener() {
            @Override
            public void onStart(StreamContext context) {}

            @Override
            public void onMessage(StreamContext context, AiMessageResponse response) {
                String content = response.getMessage().getContent();
                if (content != null) {
                    System.out.print(content);
                }

                // 处理工具调用
                if (response.getMessage().getToolCalls() != null
                        && !response.getMessage().getToolCalls().isEmpty()) {
                    prompt.addMessage(response.getMessage());
                    List<ToolMessage> toolMessages = response.executeToolCallsAndGetToolMessages();
                    prompt.addMessages(toolMessages);

                    System.out.println("\n\n[检测到工具调用，继续对话...]");
                    System.out.print("AI: ");
                    chatModel.chatStream(prompt, this);
                }
            }

            @Override
            public void onStop(StreamContext context) {
                System.out.println("\n\n========== 测试完成 ==========");
            }

            @Override
            public void onFailure(StreamContext context, Throwable throwable) {
                System.err.println("\n调用失败: " + throwable.getMessage());
                throwable.printStackTrace();
            }
        });
    }
}
