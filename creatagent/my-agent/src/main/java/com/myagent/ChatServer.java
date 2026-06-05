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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Apboa Agent Web Server
 * 浏览器打开 http://localhost:8081 即可对话
 * 支持 SSE 流式输出：实时显示 AI 思考过程、工具调用和文本生成
 */
public class ChatServer {

    static final int PORT = 8081;

    // 从配置文件加载（agent-config.properties）
    static String BASE_URL;
    static String API_KEY;
    static String MODEL;
    static String PLATFORM_URL;
    static String PLATFORM_USER;
    static String PLATFORM_PASSWORD;

    static String PROJECT_DIR = System.getProperty("user.dir");

    static ChatModel chatModel;
    static Tool skillsTool;
    static MemoryPrompt currentPrompt;

    /**
     * 加载配置文件 agent-config.properties
     * 搜索顺序：当前目录 → jar 同目录 → classpath
     */
    static Properties loadConfig() throws IOException {
        Properties props = new Properties();

        // 尝试从文件系统加载
        File[] candidates = {
            new File("agent-config.properties"),
            new File(PROJECT_DIR, "agent-config.properties")
        };
        for (File f : candidates) {
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    props.load(fis);
                    System.out.println("已加载配置: " + f.getAbsolutePath());
                    return props;
                }
            }
        }

        // 尝试从 classpath 加载
        InputStream is = ChatServer.class.getResourceAsStream("/agent-config.properties");
        if (is != null) {
            props.load(is);
            is.close();
            System.out.println("已加载配置: classpath");
            return props;
        }

        throw new IOException("未找到 agent-config.properties 配置文件");
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");

        // 加载配置
        Properties config = loadConfig();
        BASE_URL = config.getProperty("llm.endpoint", "");
        API_KEY = config.getProperty("llm.api.key", "");
        MODEL = config.getProperty("llm.model", "mimo-v2.5-pro");
        PLATFORM_URL = config.getProperty("platform.url", "");
        PLATFORM_USER = config.getProperty("platform.admin.user", "");
        PLATFORM_PASSWORD = config.getProperty("platform.admin.password", "");

        // 初始化模型
        OpenAIChatConfig modelConfig = new OpenAIChatConfig();
        modelConfig.setEndpoint(BASE_URL);
        modelConfig.setApiKey(API_KEY);
        modelConfig.setModel(MODEL);
        modelConfig.setLogEnabled(false);
        chatModel = new OpenAIChatModel(modelConfig);

        // 加载 Skills
        String skillsDir = new File("skills").getAbsolutePath();
        skillsTool = SkillsTool.builder()
                .addSkillsDirectory(skillsDir)
                .build();

        // 初始化对话
        currentPrompt = createNewPrompt();

        // 启动 HTTP 服务器
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/chat", new CorsHandler(new StreamChatHandler()));
        server.createContext("/api/clear", new CorsHandler(new ClearHandler()));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║       Apboa Agent Web Server 已启动              ║");
        System.out.println("║                                                    ║");
        System.out.println("║  浏览器打开: http://localhost:" + PORT + "               ║");
        System.out.println("║  按 Ctrl+C 停止                                   ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    static MemoryPrompt createNewPrompt() {
        MemoryPrompt prompt = new MemoryPrompt();
        prompt.setSystemMessage(AgentPrompts.SYSTEM_PROMPT);
        prompt.addTool(skillsTool);
        prompt.addTools(CommonTools.getAllCommonsTools());
        return prompt;
    }

    // ========== 静态文件服务 ==========

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = "/chat.html";

            InputStream is = ChatServer.class.getResourceAsStream("/static" + path);
            if (is == null) {
                String resp = "404 Not Found";
                exchange.sendResponseHeaders(404, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            byte[] bytes = readAllBytes(is);
            is.close();

            String contentType = "text/html";
            if (path.endsWith(".js")) contentType = "application/javascript";
            else if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".json")) contentType = "application/json";

            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }

    // ========== 流式聊天 API — POST /api/chat (SSE) ==========

    /**
     * SSE 事件写入器：每行一个 JSON 对象，格式为 data: {...}\n\n
     */
    static class EventWriter implements Closeable {
        private final OutputStreamWriter writer;

        EventWriter(OutputStream out) {
            this.writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        }

        void sendEvent(String type, JSONObject data) throws IOException {
            JSONObject envelope = new JSONObject();
            envelope.put("type", type);
            envelope.put("data", data);
            writer.write("data: " + envelope.toJSONString() + "\n\n");
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    static class StreamChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = new String(readAllBytes(exchange.getRequestBody()), StandardCharsets.UTF_8);
            String message = extractJsonString(body, "message");
            if (message == null || message.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"message is required\"}");
                return;
            }

            // 设置 SSE 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);

            EventWriter ew = new EventWriter(exchange.getResponseBody());

            try {
                String intent = detectIntent(message);

                if ("create_agent".equals(intent)) {
                    handleCreateAgentWorkflow(message, ew);
                } else {
                    handleChatWithSSE(message, ew);
                }
            } catch (Exception e) {
                try {
                    JSONObject errData = new JSONObject();
                    errData.put("message", e.getMessage());
                    ew.sendEvent("error", errData);
                } catch (IOException ignored) {}
            } finally {
                try { ew.sendEvent("done", new JSONObject()); } catch (IOException ignored) {}
                try { ew.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 普通对话：SSE 流式输出
     */
    static void handleChatWithSSE(String message, EventWriter ew) throws Exception {
        MemoryPrompt prompt = new MemoryPrompt();
        prompt.setSystemMessage(AgentPrompts.SYSTEM_PROMPT);
        prompt.addTool(skillsTool);
        prompt.addTools(CommonTools.getAllCommonsTools());
        prompt.addUserMessage(message);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> future = executor.submit(() -> {
                sseToolLoop(prompt, ew, 0, 15);
            });
            future.get(300, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            try {
                JSONObject data = new JSONObject();
                data.put("text", "抱歉，处理时间过长。请尝试简化您的描述，或稍后再试。");
                ew.sendEvent("content", data);
            } catch (IOException ignored) {}
        } catch (Exception e) {
            try {
                JSONObject errData = new JSONObject();
                errData.put("message", e.getMessage());
                ew.sendEvent("error", errData);
            } catch (IOException ignored) {}
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 工具调用循环（SSE 流式版本）
     * 只推送 thinking 和 content 事件，不暴露工具调用细节
     */
    static void sseToolLoop(MemoryPrompt prompt, EventWriter ew, int round, int maxRounds) {
        if (round >= maxRounds) {
            sendThinking(ew, "正在整理结果...");
            doFinalSummary(prompt, ew);
            return;
        }

        // 发送思考状态（用户友好的描述）
        if (round == 0) {
            sendThinking(ew, "正在思考...");
        } else {
            sendThinking(ew, "正在继续处理...");
        }

        // 调用 LLM
        AiMessageResponse response = chatModel.chat(prompt);

        // 输出文本内容
        String content = response.getMessage().getContent();
        if (content != null && !content.isEmpty()) {
            try {
                JSONObject data = new JSONObject();
                data.put("text", content);
                ew.sendEvent("content", data);
            } catch (IOException ignored) {}
        }

        // 检查是否有工具调用
        List<ToolCall> toolCalls = response.getMessage().getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }

        prompt.addMessage(response.getMessage());

        // 执行工具调用（不向用户暴露工具调用细节）
        List<ToolMessage> toolMessages;
        try {
            toolMessages = response.executeToolCallsAndGetToolMessages();
        } catch (Exception e) {
            toolMessages = new ArrayList<>();
            for (ToolCall tc : toolCalls) {
                ToolMessage errMsg = new ToolMessage();
                errMsg.setToolCallId(tc.getId());
                errMsg.setContent("工具执行失败: " + e.getMessage());
                toolMessages.add(errMsg);
            }
        }

        prompt.addMessages(toolMessages);
        sseToolLoop(prompt, ew, round + 1, maxRounds);

        prompt.addMessages(toolMessages);
        sseToolLoop(prompt, ew, round + 1, maxRounds);
    }

    /**
     * 当工具调用过多但没有文本回复时，强制生成总结
     */
    static void doFinalSummary(MemoryPrompt prompt, EventWriter ew) {
        sendThinking(ew, "正在整理结果...");
        try {
            MemoryPrompt summaryPrompt = new MemoryPrompt();
            summaryPrompt.setSystemMessage("你是一个智能体助手，请用中文总结工具调用结果。用清晰的标题、列表、表格表达状态和结果。");
            summaryPrompt.addUserMessage("请根据之前的对话历史总结最终结果。");

            AiMessageResponse resp = chatModel.chat(summaryPrompt);
            String content = resp.getMessage().getContent();
            if (content != null && !content.isEmpty()) {
                JSONObject data = new JSONObject();
                data.put("text", content);
                ew.sendEvent("content", data);
            }
        } catch (Exception e) {
            try {
                JSONObject data = new JSONObject();
                data.put("text", "总结生成失败，请查看上方工具调用结果。");
                ew.sendEvent("content", data);
            } catch (IOException ignored) {}
        }
    }

    // ========== 创建智能体工作流 ==========

    static void handleCreateAgentWorkflow(String userMessage, EventWriter ew) throws Exception {
        // 步骤1: 解析用户需求
        sendThinking(ew, "正在理解您的需求...");
        String parsed = parseAgentRequest(userMessage);
        String agentName = extractParsedField(parsed, "name");
        String agentCode = extractParsedField(parsed, "code");
        String agentDesc = extractParsedField(parsed, "description");
        String searchKeyword = extractParsedField(parsed, "keyword");
        String systemPrompt = extractParsedField(parsed, "systemPrompt");

        if (agentName == null || agentName.isEmpty()) agentName = "智能体" + System.currentTimeMillis() % 10000;
        if (agentCode == null || agentCode.isEmpty()) agentCode = "agent-" + System.currentTimeMillis() % 10000;
        if (agentDesc == null || agentDesc.isEmpty()) agentDesc = "由 AI 助手创建的智能体";
        if (searchKeyword == null || searchKeyword.isEmpty()) searchKeyword = agentName;

        // 步骤2: 准备创建
        sendThinking(ew, "正在为您准备智能体配置...");
        String skillListJson = executeOpencli("skill-list -f json");
        String matchedSkillId = findMatchingSkill(skillListJson, searchKeyword);

        String modelListJson = executeOpencli("model-list -f json");
        List<String[]> modelInfo = extractAllIdsAndNames(modelListJson);
        String modelConfigId = !modelInfo.isEmpty() ? modelInfo.get(0)[0] : null;

        if (modelConfigId == null) {
            sendContent(ew, "抱歉，当前平台没有可用的 AI 模型，无法创建智能体。请联系管理员配置模型后再试。");
            return;
        }

        // 步骤3: 创建智能体
        sendThinking(ew, "正在创建智能体「" + agentName + "」...");
        String createCmd = "agent-create --name \"" + agentName + "\" --code \"" + agentCode
                + "\" --description \"" + agentDesc + "\" --modelConfigId \"" + modelConfigId + "\"";
        if (matchedSkillId != null) {
            createCmd += " --skillIds \"" + matchedSkillId + "\"";
        }
        createCmd += " -f json";

        String createResult = executeOpencli(createCmd);
        String agentId = extractFirstId(createResult, "agentId");

        // 步骤4: 配置智能体
        if (agentId != null && systemPrompt != null && !systemPrompt.isEmpty()) {
            sendThinking(ew, "正在配置智能体能力...");
            String updateCmd = "agent-update " + agentId + " --systemPrompt \"" + systemPrompt.replace("\"", "\\\"") + "\" -f json";
            executeOpencli(updateCmd);
        }

        // 步骤5: 完成
        if (agentId != null) {
            sendThinking(ew, "正在确认创建结果...");
            String getResult = executeOpencli("agent-get " + agentId + " -f json");

            String formatted = formatWithModel(userMessage,
                    "智能体名称: " + agentName + "\n描述: " + agentDesc
                    + (matchedSkillId != null ? "\n已关联平台技能" : "\n未关联技能")
                    + "\n验证结果: " + getResult);
            sendContent(ew, formatted);
        } else {
            sendContent(ew, "抱歉，智能体创建失败。可能的原因：平台服务暂时不可用或权限不足。请稍后再试，或联系管理员协助处理。");
        }
    }

    // ========== 事件发送辅助方法 ==========

    static void sendThinking(EventWriter ew, String status) {
        try {
            JSONObject data = new JSONObject();
            data.put("status", status);
            ew.sendEvent("thinking", data);
        } catch (IOException ignored) {}
    }

    static void sendContent(EventWriter ew, String text) {
        try {
            JSONObject data = new JSONObject();
            data.put("text", text);
            ew.sendEvent("content", data);
        } catch (IOException ignored) {}
    }

    /**
     * 格式化工具调用参数为可读文本
     */
    static String formatToolArgs(ToolCall tc) {
        try {
            String args = tc.getArguments();
            if (args == null || args.isEmpty()) return "";
            JSONObject obj = JSON.parseObject(args);
            if (obj == null) return args;
            StringBuilder sb = new StringBuilder();
            for (String key : obj.keySet()) {
                String val = obj.getString(key);
                if (val != null && !val.isEmpty()) {
                    if (sb.length() > 0) sb.append("  ");
                    sb.append(key).append(": ").append(val.length() > 80 ? val.substring(0, 80) + "..." : val);
                }
            }
            return sb.length() > 0 ? sb.toString() : args;
        } catch (Exception e) {
            String args = tc.getArguments();
            return args != null ? (args.length() > 100 ? args.substring(0, 100) + "..." : args) : "";
        }
    }

    // ========== 清空对话 — POST /api/clear ==========

    static class ClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            currentPrompt = createNewPrompt();
            sendJson(exchange, 200, "{\"ok\":true}");
        }
    }

    // ========== 意图检测与工具方法（保持不变） ==========

    static String detectIntent(String msg) {
        String m = msg.toLowerCase();

        // 创建类动词（必须是完整的短语，避免单字"做"误匹配）
        boolean hasCreate = m.contains("创建") || m.contains("新建") || m.contains("create")
                || m.contains("帮我建") || m.contains("帮我做个") || m.contains("帮我做一个")
                || m.contains("帮我做一款") || m.contains("帮我搭建") || m.contains("帮我开发")
                || m.contains("帮我造一个") || m.contains("做个智能体") || m.contains("做一个智能体")
                || m.contains("做个ai") || m.contains("做一个ai");

        // 目标对象
        boolean hasAgent = m.contains("智能体") || m.contains("agent") || m.contains("助手")
                || m.contains("机器人");
        // 匹配 "AI" 作为独立词
        if (!hasAgent) {
            int aiIdx = m.indexOf("ai");
            while (aiIdx >= 0) {
                boolean beforeOk = (aiIdx == 0) || !Character.isLetterOrDigit(m.charAt(aiIdx - 1));
                int afterIdx = aiIdx + 2;
                boolean afterOk = (afterIdx >= m.length()) || !Character.isLetterOrDigit(m.charAt(afterIdx));
                if (beforeOk && afterOk) { hasAgent = true; break; }
                aiIdx = m.indexOf("ai", aiIdx + 1);
            }
        }

        if (hasCreate && hasAgent) {
            return "create_agent";
        }
        return "chat";
    }

    static String parseAgentRequest(String userMessage) {
        try {
            MemoryPrompt p = new MemoryPrompt();
            p.setSystemMessage("你是一个JSON提取器。从用户的智能体创建请求中提取以下信息，严格按JSON格式返回，不要添加其他文字：\n" +
                    "{\n" +
                    "  \"name\": \"智能体中文名称\",\n" +
                    "  \"nameEn\": \"英文编码（小写英文加连字符）\",\n" +
                    "  \"description\": \"一句话描述\",\n" +
                    "  \"keyword\": \"用于搜索匹配技能的关键词（英文或中文，简短）\",\n" +
                    "  \"systemPrompt\": \"为该智能体生成的系统提示词（100-300字，中文，包含角色定位、核心职责、输出规范）\"\n" +
                    "}");
            p.addUserMessage(userMessage);
            AiMessageResponse resp = chatModel.chat(p);
            String content = resp.getMessage().getContent();
            return content != null ? content.trim() : "{}";
        } catch (Exception e) {
            return "{}";
        }
    }

    static String extractParsedField(String json, String field) {
        try {
            JSONObject obj = JSON.parseObject(json);
            if (obj != null) return obj.getString(field);
        } catch (Exception ignored) {}
        int start = json.indexOf("{");
        int end = json.lastIndexOf("}");
        if (start >= 0 && end > start) {
            try {
                JSONObject obj = JSON.parseObject(json.substring(start, end + 1));
                if (obj != null) return obj.getString(field);
            } catch (Exception ignored) {}
        }
        return null;
    }

    static String findMatchingSkill(String skillListJson, String keyword) {
        if (keyword == null || keyword.isEmpty()) return null;
        String kw = keyword.toLowerCase();
        try {
            com.alibaba.fastjson2.JSONArray arr = JSON.parseArray(skillListJson);
            if (arr == null) return null;
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                if (name != null && name.toLowerCase().contains(kw)) return obj.getString("id");
            }
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String desc = obj.getString("description");
                if (desc != null && desc.toLowerCase().contains(kw)) return obj.getString("id");
            }
        } catch (Exception ignored) {}
        return null;
    }

    static String executeOpencli(String args) {
        String result = executeCommand("opencli apboa " + args);
        // Token 过期时自动重新登录
        if (result.contains("401") || result.contains("Token无效") || result.contains("Unauthorized")) {
            String loginResult = executeCommand("opencli apboa login \"" + PLATFORM_USER + "\" --password \"" + PLATFORM_PASSWORD + "\"");
            if (loginResult.contains("success: true") || loginResult.contains("tokenSaved")) {
                result = executeCommand("opencli apboa " + args);
            }
        }
        return result;
    }

    static String executeCommand(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
            // 不合并 stderr，避免 npm/node 的警告信息混入 JSON 输出
            pb.environment().put("NODE_TLS_REJECT_UNAUTHORIZED", "0");
            Process p = pb.start();
            String output = readProcessOutput(p, 30000);
            p.waitFor(5, TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            return "命令执行失败: " + e.getMessage();
        }
    }

    static String readProcessOutput(Process p, long timeoutMs) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        long deadline = System.currentTimeMillis() + timeoutMs;
        InputStream is = p.getInputStream();
        while (System.currentTimeMillis() < deadline) {
            int avail = is.available();
            if (avail > 0) {
                int n = is.read(buf, 0, Math.min(avail, buf.length));
                if (n > 0) bos.write(buf, 0, n);
            } else if (!p.isAlive()) {
                while (is.available() > 0) {
                    int n = is.read(buf);
                    if (n > 0) bos.write(buf, 0, n);
                }
                break;
            } else {
                Thread.sleep(100);
            }
        }
        return bos.toString("UTF-8");
    }

    static String extractFirstId(String json, String key) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            com.alibaba.fastjson2.JSONArray arr = JSON.parseArray(json);
            if (arr != null && !arr.isEmpty()) {
                return arr.getJSONObject(0).getString(key);
            }
        } catch (Exception ignored) {}
        try {
            JSONObject obj = JSON.parseObject(json);
            if (obj != null && obj.containsKey(key)) return obj.getString(key);
            if (obj != null && obj.containsKey("data")) {
                Object data = obj.get("data");
                if (data instanceof com.alibaba.fastjson2.JSONArray) {
                    com.alibaba.fastjson2.JSONArray dataArr = (com.alibaba.fastjson2.JSONArray) data;
                    if (!dataArr.isEmpty()) return dataArr.getJSONObject(0).getString(key);
                } else if (data instanceof JSONObject) {
                    return ((JSONObject) data).getString(key);
                }
            }
        } catch (Exception ignored) {}
        if (json.contains("\"" + key + "\"")) {
            int idx = json.indexOf("\"" + key + "\"");
            int colonIdx = json.indexOf(":", idx);
            if (colonIdx > 0) {
                String after = json.substring(colonIdx + 1).trim();
                if (after.startsWith("\"")) {
                    int endQuote = after.indexOf("\"", 1);
                    if (endQuote > 0) return after.substring(1, endQuote);
                }
            }
        }
        return null;
    }

    static int countJsonArrayItems(String json) {
        try {
            com.alibaba.fastjson2.JSONArray arr = JSON.parseArray(json);
            return arr != null ? arr.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    static List<String[]> extractAllIdsAndNames(String json) {
        List<String[]> result = new ArrayList<>();
        try {
            com.alibaba.fastjson2.JSONArray arr = JSON.parseArray(json);
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    if (id != null) result.add(new String[]{id, name != null ? name : ""});
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    static String truncate(String s, int maxLen) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    static String formatWithModel(String userMessage, String workflowResult) {
        try {
            MemoryPrompt p = new MemoryPrompt();
            p.setSystemMessage(
                "你是 Apboa 智能体平台的 AI 助手。请用中文向用户汇报智能体创建结果。\n" +
                "要求：\n" +
                "1. 语气自然友好，像一个专业的产品助手\n" +
                "2. 用简洁的标题和列表展示关键信息\n" +
                "3. 不要展示内部 ID、技术术语、JSON 数据\n" +
                "4. 在最后给出下一步建议（如\"您可以在智能体广场找到这个智能体并开始使用\"\n" +
                "5. 如果有关联技能，简要说明智能体具备的能力\n" +
                "6. 不要使用 emoji"
            );
            p.addUserMessage("用户请求：" + userMessage + "\n\n创建结果：\n" + workflowResult);
            AiMessageResponse resp = chatModel.chat(p);
            String content = resp.getMessage().getContent();
            return content != null && !content.isEmpty() ? content : workflowResult;
        } catch (Exception e) {
            return workflowResult;
        }
    }

    // ========== CORS 处理器 ==========

    static class CorsHandler implements HttpHandler {
        private final HttpHandler delegate;

        CorsHandler(HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.getResponseBody().close();
                return;
            }

            delegate.handle(exchange);
        }
    }

    // ========== 工具方法 ==========

    static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    static String extractJsonString(String json, String key) {
        try {
            return JSON.parseObject(json).getString(key);
        } catch (Exception e) {
            return null;
        }
    }
}
