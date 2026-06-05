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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Apboa Agent Web Server
 * 浏览器打开 http://localhost:8080 即可对话
 */
public class ChatServer {

    static final String BASE_URL = "https://token-plan-sgp.xiaomimimo.com";
    static final String API_KEY = "tp-sefnsoeah0svii3iz9lkvhxdtxhjdafbr9hobixbsgvr0b0j";
    static final String MODEL = "mimo-v2.5-pro";
    static final int PORT = 8081;

    static String PROJECT_DIR = System.getProperty("user.dir");

    static ChatModel chatModel;
    static Tool skillsTool;
    // 对话历史（单用户）
    static MemoryPrompt currentPrompt;

    public static void main(String[] args) throws Exception {
        // 跳过 SSL 证书吊销检查（Windows Server CRL 检查可能挂起）
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");

        // 初始化模型
        OpenAIChatConfig config = new OpenAIChatConfig();
        config.setEndpoint(BASE_URL);
        config.setApiKey(API_KEY);
        config.setModel(MODEL);
        config.setLogEnabled(false);
        chatModel = new OpenAIChatModel(config);

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
        server.createContext("/api/chat", new CorsHandler(new ChatHandler()));
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

    /**
     * 静态文件服务 — 提供 chat.html
     */
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

    /**
     * 聊天 API — POST /api/chat
     * 请求体: {"message": "..."}
     * 响应体: {"reply": "...", "toolCalls": [...]}
     */
    static class ChatHandler implements HttpHandler {
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

            String intent = detectIntent(message);

            // 创建智能体意图：服务端直接执行工作流
            if ("create_agent".equals(intent)) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    Future<String> future = executor.submit(() -> executeCreateAgentWorkflow(message));
                    String workflowResult = future.get(180, TimeUnit.SECONDS);
                    // 用模型格式化结果
                    String formatted = formatWithModel(message, workflowResult);
                    JSONObject json = new JSONObject();
                    json.put("reply", formatted);
                    json.put("toolCalls", new ArrayList<>());
                    sendJson(exchange, 200, json.toJSONString());
                } catch (TimeoutException e) {
                    sendJson(exchange, 200, "{\"reply\":\"创建智能体超时，请重试。\",\"toolCalls\":[]}");
                } catch (Exception e) {
                    sendJson(exchange, 200, "{\"reply\":\"创建智能体异常: " + e.getMessage() + "\",\"toolCalls\":[]}");
                } finally {
                    executor.shutdownNow();
                }
                return;
            }

            // 其他意图或普通对话：走模型工具调用循环
            MemoryPrompt prompt = new MemoryPrompt();
            prompt.setSystemMessage(AgentPrompts.SYSTEM_PROMPT);
            prompt.addTool(skillsTool);
            prompt.addTools(CommonTools.getAllCommonsTools());
            prompt.addUserMessage(message);

            StringBuilder reply = new StringBuilder();
            List<String> toolCallNames = new ArrayList<>();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            AtomicReference<String> errorRef = new AtomicReference<>(null);

            try {
                Future<?> future = executor.submit(() -> {
                    try {
                        int maxRounds = 15;
                        for (int round = 0; round < maxRounds; round++) {
                            if (Thread.currentThread().isInterrupted()) break;
                            AiMessageResponse response = chatModel.chat(prompt);
                            String content = response.getMessage().getContent();
                            if (content != null && !content.isEmpty()) {
                                reply.append(content);
                            }
                            List<ToolCall> toolCalls = response.getMessage().getToolCalls();
                            if (toolCalls == null || toolCalls.isEmpty()) break;
                            prompt.addMessage(response.getMessage());
                            for (ToolCall tc : toolCalls) toolCallNames.add(tc.getName());
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
                        }
                        // 强制总结
                        if (reply.length() == 0 || toolCallNames.size() > 3) {
                            MemoryPrompt summaryPrompt = new MemoryPrompt();
                            summaryPrompt.setSystemMessage("你是一个智能体助手，请用中文总结工具调用结果。用清晰的标题、列表、表格表达状态和结果。");
                            summaryPrompt.addUserMessage("用户请求：" + message + "\n\n执行了 " + toolCallNames.size() + " 个工具调用。请总结最终结果：");
                            AiMessageResponse finalResponse = chatModel.chat(summaryPrompt);
                            String finalContent = finalResponse.getMessage().getContent();
                            if (finalContent != null && !finalContent.isEmpty()) {
                                reply.setLength(0);
                                reply.append(finalContent);
                            }
                        }
                    } catch (Exception e) {
                        errorRef.set(e.getMessage());
                    }
                });
                future.get(240, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                if (reply.length() == 0) reply.append("请求处理超时，请简化描述或重试。");
                else reply.append("\n\n---\n处理时间超过限制，以上为部分结果。");
            } catch (Exception e) {
                if (reply.length() == 0) reply.append("处理异常: ").append(e.getMessage());
            } finally {
                executor.shutdownNow();
            }

            String errMsg = errorRef.get();
            if (reply.length() == 0 && errMsg != null) reply.append("处理失败: ").append(errMsg);
            if (reply.length() == 0) reply.append("无回复");

            JSONObject json = new JSONObject();
            json.put("reply", reply.toString());
            json.put("toolCalls", toolCallNames);
            sendJson(exchange, 200, json.toJSONString());
        }
    }

    /**
     * 清空对话 — POST /api/clear
     */
    static class ClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            currentPrompt = createNewPrompt();
            sendJson(exchange, 200, "{\"ok\":true}");
        }
    }

    // ========== 意图检测与工作流 ==========

    static String detectIntent(String msg) {
        String m = msg.toLowerCase();
        boolean hasCreate = m.contains("创建") || m.contains("新建") || m.contains("create") || m.contains("帮我创建") || m.contains("帮我建");
        boolean hasAgent = m.contains("智能体") || m.contains("agent") || m.contains("助手") || m.contains("机器人");
        if (hasCreate && hasAgent) {
            return "create_agent";
        }
        return "chat";
    }

    static String executeCreateAgentWorkflow(String userMessage) throws Exception {
        StringBuilder result = new StringBuilder();

        // 步骤0: 用模型解析用户需求
        result.append("## 创建智能体 - 执行结果\n\n");
        result.append("### 步骤0: 解析用户需求\n");
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

        result.append("- 智能体名称: ").append(agentName).append("\n");
        result.append("- 智能体编码: ").append(agentCode).append("\n");
        result.append("- 描述: ").append(agentDesc).append("\n");
        result.append("- 搜索关键词: ").append(searchKeyword).append("\n");
        result.append("- 系统提示词长度: ").append(systemPrompt != null ? systemPrompt.length() : 0).append(" 字\n\n");

        // 步骤1: 查询平台技能
        result.append("### 步骤1: 查询平台技能\n");
        String skillListJson = executeOpencli("skill-list -f json");
        int skillCount = countJsonArrayItems(skillListJson);
        result.append("找到 ").append(skillCount).append(" 个技能\n");

        // 从已有技能中查找匹配的
        String matchedSkillId = findMatchingSkill(skillListJson, searchKeyword);
        if (matchedSkillId != null) {
            result.append("匹配到已有技能: `").append(matchedSkillId).append("`\n\n");
        } else {
            result.append("未找到匹配技能，将不关联技能创建智能体。\n\n");
        }

        // 步骤2: 查询平台模型
        result.append("### 步骤2: 查询平台模型\n");
        String modelListJson = executeOpencli("model-list -f json");
        List<String[]> modelInfo = extractAllIdsAndNames(modelListJson);
        for (String[] info : modelInfo) {
            result.append("- ID: `").append(info[0]).append("`, 名称: ").append(info[1]).append("\n");
        }
        String modelConfigId = !modelInfo.isEmpty() ? modelInfo.get(0)[0] : null;
        if (modelConfigId == null) {
            result.append("**错误**: 未找到可用模型。\n");
            return result.toString();
        }
        result.append("使用模型 ID: `").append(modelConfigId).append("`\n\n");

        // 步骤3: 创建智能体
        result.append("### 步骤3: 创建智能体\n");
        String createCmd = "agent-create --name \"" + agentName + "\" --code \"" + agentCode
                + "\" --description \"" + agentDesc + "\" --modelConfigId \"" + modelConfigId + "\"";
        if (matchedSkillId != null) {
            createCmd += " --skillIds \"" + matchedSkillId + "\"";
            result.append("关联技能 ID: `").append(matchedSkillId).append("`\n");
        }
        createCmd += " -f json";

        String createResult = executeOpencli(createCmd);
        result.append("创建结果: ").append(truncate(createResult, 500)).append("\n\n");

        // 步骤4: 设置系统提示词
        String agentId = extractFirstId(createResult, "agentId");
        if (agentId != null && systemPrompt != null && !systemPrompt.isEmpty()) {
            result.append("### 步骤4: 设置系统提示词\n");
            String updateCmd = "agent-update " + agentId + " --systemPrompt \"" + systemPrompt.replace("\"", "\\\"") + "\" -f json";
            String updateResult = executeOpencli(updateCmd);
            result.append("设置结果: ").append(truncate(updateResult, 300)).append("\n\n");
        }

        // 步骤5: 验证
        if (agentId != null) {
            result.append("### 步骤5: 验证\n");
            String getResult = executeOpencli("agent-get " + agentId + " -f json");
            result.append("验证结果: ").append(truncate(getResult, 500)).append("\n");
        }

        return result.toString();
    }

    /**
     * 用模型解析用户的智能体创建请求，提取名称、编码、描述、搜索关键词和系统提示词
     */
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
        // 尝试从文本中提取 JSON
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

    /**
     * 从技能列表中查找与关键词匹配的技能
     */
    static String findMatchingSkill(String skillListJson, String keyword) {
        if (keyword == null || keyword.isEmpty()) return null;
        String kw = keyword.toLowerCase();
        try {
            com.alibaba.fastjson2.JSONArray arr = JSON.parseArray(skillListJson);
            if (arr == null) return null;
            // 优先精确匹配名称
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                if (name != null && name.toLowerCase().contains(kw)) return obj.getString("id");
            }
            // 其次匹配描述
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
        // 如果 token 过期（401），自动重新登录后重试
        if (result.contains("401") || result.contains("Token无效") || result.contains("Unauthorized")) {
            String loginResult = executeCommand("opencli apboa login admin --password \"Admin@123.com\"");
            if (loginResult.contains("success: true") || loginResult.contains("tokenSaved")) {
                result = executeCommand("opencli apboa " + args);
            }
        }
        return result;
    }

    static String executeCommand(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
            pb.redirectErrorStream(true);
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
                // 读取剩余
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
            // 尝试从 data 字段提取
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
        // 最后尝试直接字符串匹配
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
            p.setSystemMessage("你是一个智能体助手。请用中文简洁地向用户汇报以下执行结果。用清晰的标题和列表。");
            p.addUserMessage("用户请求：" + userMessage + "\n\n执行结果：\n" + workflowResult);
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
            // 设置 CORS 头
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");

            // 处理 OPTIONS 预检请求
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
