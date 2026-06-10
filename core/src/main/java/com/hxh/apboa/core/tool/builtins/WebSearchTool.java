package com.hxh.apboa.core.tool.builtins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.core.tool.IAgentTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 联网搜索工具
 * 支持多种搜索 API：Tavily（默认）、DuckDuckGo、SearXNG、SerpAPI、Google Custom Search
 *
 * @author Claude Code
 */
@Slf4j
@Component
public class WebSearchTool implements IAgentTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 搜索 API 类型：tavily、duckduckgo、searxng、serpapi、google
     */
    @Value("${apboa.search.api-type:tavily}")
    private String searchApiType;

    /**
     * Tavily API Key（仅 tavily 类型需要）
     */
    @Value("${apboa.search.tavily-key:}")
    private String tavilyKey;

    /**
     * SearXNG 实例地址（仅 searxng 类型需要）
     */
    @Value("${apboa.search.searxng-url:}")
    private String searxngUrl;

    /**
     * SerpAPI Key（仅 serpapi 类型需要）
     */
    @Value("${apboa.search.serpapi-key:}")
    private String serpapiKey;

    /**
     * Google Custom Search API Key（仅 google 类型需要）
     */
    @Value("${apboa.search.google-api-key:}")
    private String googleApiKey;

    /**
     * Google Custom Search Engine ID（仅 google 类型需要）
     */
    @Value("${apboa.search.google-cx:}")
    private String googleCx;

    @Tool(name = "web_search", description = "在互联网上搜索信息，获取最新的网页内容。适用于查询实时信息、新闻、技术文档等。")
    public Object webSearch(
            @ToolParam(
                    name = "query",
                    description = "搜索关键词",
                    required = true)
            String query,
            @ToolParam(
                    name = "max_results",
                    description = "返回结果数量，默认 5，最大 10",
                    required = false)
            Integer maxResults) {

        if (query == null || query.isBlank()) {
            return R.fail("搜索关键词不能为空");
        }

        if (maxResults == null || maxResults <= 0) {
            maxResults = 5;
        }
        maxResults = Math.min(maxResults, 10);

        try {
            List<SearchResult> results = switch (searchApiType.toLowerCase()) {
                case "tavily" -> searchWithTavily(query, maxResults);
                case "searxng" -> searchWithSearXNG(query, maxResults);
                case "serpapi" -> searchWithSerpAPI(query, maxResults);
                case "google" -> searchWithGoogle(query, maxResults);
                default -> searchWithDuckDuckGo(query, maxResults);
            };

            if (results.isEmpty()) {
                return R.data("未找到相关搜索结果");
            }

            // 格式化搜索结果
            StringBuilder sb = new StringBuilder();
            sb.append("搜索结果：\n\n");
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                sb.append(String.format("[%d] %s\n", i + 1, r.title));
                sb.append(String.format("    链接：%s\n", r.url));
                if (r.snippet != null && !r.snippet.isBlank()) {
                    sb.append(String.format("    摘要：%s\n", r.snippet));
                }
                sb.append("\n");
            }

            return R.data(sb.toString().trim());

        } catch (Exception e) {
            log.error("Web search failed for query: {}", query, e);
            return R.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * Tavily 搜索 API（专为 AI 应用设计，推荐）
     * 需要配置 apboa.search.tavily-key
     * 官网：https://tavily.com
     */
    private List<SearchResult> searchWithTavily(String query, int maxResults) throws Exception {
        if (tavilyKey == null || tavilyKey.isBlank()) {
            throw new IllegalStateException("Tavily API key not configured. Set apboa.search.tavily-key");
        }

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "api_key", tavilyKey,
                "query", query,
                "max_results", maxResults,
                "include_answer", true
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tavily.com/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode resultsNode = root.path("results");

        List<SearchResult> results = new ArrayList<>();
        if (resultsNode.isArray()) {
            for (int i = 0; i < Math.min(resultsNode.size(), maxResults); i++) {
                JsonNode item = resultsNode.get(i);
                String title = item.path("title").asText("");
                String resultUrl = item.path("url").asText("");
                String snippet = item.path("content").asText("");
                results.add(new SearchResult(title, resultUrl, snippet));
            }
        }

        return results;
    }

    /**
     * DuckDuckGo Instant Answer API（免费，无需 API Key）
     * 注意：DuckDuckGo 的 Instant Answer API 主要返回摘要信息，不是完整的搜索结果
     * 如需完整搜索结果，建议使用 SearXNG 或其他 API
     */
    private List<SearchResult> searchWithDuckDuckGo(String query, int maxResults) throws Exception {
        // 使用 DuckDuckGo HTML 搜索
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String html = response.body();

        // 简单解析 HTML 结果
        List<SearchResult> results = new ArrayList<>();
        int index = 0;

        // 解析 DuckDuckGo HTML 结果
        while (results.size() < maxResults) {
            int titleStart = html.indexOf("class=\"result__a\"", index);
            if (titleStart == -1) break;

            int hrefStart = html.indexOf("href=\"", titleStart);
            if (hrefStart == -1) break;
            hrefStart += 6;
            int hrefEnd = html.indexOf("\"", hrefStart);
            if (hrefEnd == -1) break;

            int titleContentStart = html.indexOf(">", hrefEnd) + 1;
            int titleContentEnd = html.indexOf("</a>", titleContentStart);
            if (titleContentEnd == -1) break;

            int snippetStart = html.indexOf("class=\"result__snippet\"", titleContentEnd);
            String snippet = "";
            if (snippetStart != -1) {
                int snippetContentStart = html.indexOf(">", snippetStart) + 1;
                int snippetContentEnd = html.indexOf("</", snippetContentStart);
                if (snippetContentEnd != -1) {
                    snippet = html.substring(snippetContentStart, snippetContentEnd).trim();
                    // 移除 HTML 标签
                    snippet = snippet.replaceAll("<[^>]+>", "").trim();
                }
            }

            String resultUrl = html.substring(hrefStart, hrefEnd);
            // DuckDuckGo 的链接可能是重定向链接，提取实际 URL
            if (resultUrl.contains("uddg=")) {
                int uddgStart = resultUrl.indexOf("uddg=");
                if (uddgStart != -1) {
                    uddgStart += 5;
                    int uddgEnd = resultUrl.indexOf("&", uddgStart);
                    if (uddgEnd == -1) uddgEnd = resultUrl.length();
                    resultUrl = java.net.URLDecoder.decode(
                            resultUrl.substring(uddgStart, uddgEnd), StandardCharsets.UTF_8);
                }
            }

            String title = html.substring(titleContentStart, titleContentEnd)
                    .replaceAll("<[^>]+>", "").trim();

            results.add(new SearchResult(title, resultUrl, snippet));
            index = titleContentEnd;
        }

        return results;
    }

    /**
     * SearXNG 搜索（自托管元搜索引擎，推荐）
     * 需要配置 apboa.search.searxng-url
     */
    private List<SearchResult> searchWithSearXNG(String query, int maxResults) throws Exception {
        if (searxngUrl == null || searxngUrl.isBlank()) {
            throw new IllegalStateException("SearXNG URL not configured. Set apboa.search.searxng-url");
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = searxngUrl + "/search?q=" + encodedQuery + "&format=json&pageno=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode resultsNode = root.path("results");

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(resultsNode.size(), maxResults); i++) {
            JsonNode item = resultsNode.get(i);
            String title = item.path("title").asText("");
            String resultUrl = item.path("url").asText("");
            String snippet = item.path("content").asText("");
            results.add(new SearchResult(title, resultUrl, snippet));
        }

        return results;
    }

    /**
     * SerpAPI 搜索（付费 API，支持 Google、Bing 等多种搜索引擎）
     * 需要配置 apboa.search.serpapi-key
     */
    private List<SearchResult> searchWithSerpAPI(String query, int maxResults) throws Exception {
        if (serpapiKey == null || serpapiKey.isBlank()) {
            throw new IllegalStateException("SerpAPI key not configured. Set apboa.search.serpapi-key");
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://serpapi.com/search?q=" + encodedQuery + "&api_key=" + serpapiKey + "&engine=google";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode resultsNode = root.path("organic_results");

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(resultsNode.size(), maxResults); i++) {
            JsonNode item = resultsNode.get(i);
            String title = item.path("title").asText("");
            String resultUrl = item.path("link").asText("");
            String snippet = item.path("snippet").asText("");
            results.add(new SearchResult(title, resultUrl, snippet));
        }

        return results;
    }

    /**
     * Google Custom Search API
     * 需要配置 apboa.search.google-api-key 和 apboa.search.google-cx
     */
    private List<SearchResult> searchWithGoogle(String query, int maxResults) throws Exception {
        if (googleApiKey == null || googleApiKey.isBlank() || googleCx == null || googleCx.isBlank()) {
            throw new IllegalStateException("Google Search API not configured. Set apboa.search.google-api-key and apboa.search.google-cx");
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://www.googleapis.com/customsearch/v1?key=" + googleApiKey
                + "&cx=" + googleCx + "&q=" + encodedQuery + "&num=" + maxResults;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode resultsNode = root.path("items");

        List<SearchResult> results = new ArrayList<>();
        if (resultsNode.isArray()) {
            for (int i = 0; i < Math.min(resultsNode.size(), maxResults); i++) {
                JsonNode item = resultsNode.get(i);
                String title = item.path("title").asText("");
                String resultUrl = item.path("link").asText("");
                String snippet = item.path("snippet").asText("");
                results.add(new SearchResult(title, resultUrl, snippet));
            }
        }

        return results;
    }

    /**
     * 搜索结果数据类
     */
    private record SearchResult(String title, String url, String snippet) {
    }
}
