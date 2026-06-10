# 文档上传功能实现方案

## 1. 需求概述

当前平台只支持上传图片、音频、视频等多媒体文件，不支持上传办公文档（Word、Excel、PDF、PPT等）。

**目标**：让平台支持上传常见办公文档格式，并将文档内容解析为文本传递给 AI 智能体。

## 2. 支持的文件格式

| 格式 | 扩展名 | 解析库 |
|------|--------|--------|
| Word 文档 | `.doc`, `.docx` | Apache POI |
| Excel 表格 | `.xls`, `.xlsx` | Apache POI |
| PDF 文件 | `.pdf` | Apache POI (5.x) |
| PowerPoint | `.ppt`, `.pptx` | Apache POI |
| 纯文本 | `.txt`, `.csv`, `.md` | Java IO |
| HTML | `.html`, `.htm` | Jsoup |

## 3. 技术方案

### 3.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        前端上传                              │
│   用户选择文件 → 调用 /api/attach/upload → 获取文件 ID       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     后端处理流程                             │
│                                                             │
│  1. 接收文件 ID 和消息                                       │
│  2. 判断文件类型（图片/音频/视频/文档）                       │
│  3. 如果是文档 → 调用 DocumentParserService 解析内容         │
│  4. 将解析后的文本作为 TextBlock 添加到消息中                │
│  5. 发送给 AI 模型                                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      AI 模型处理                            │
│   接收包含文档内容的文本消息 → 生成回复                      │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 消息格式

当用户上传文档时，发送给 AI 的消息格式：

```
[文档内容开始]
以下是用户上传的文档《报告.docx》的内容：

（此处为解析后的文档文本内容）

[文档内容结束]

用户的问题：请帮我总结这份报告的要点
```

## 4. 实现步骤

### 4.1 添加 Maven 依赖

**文件**：`biz/resource/pom.xml`

```xml
<dependencies>
    <!-- 现有依赖... -->

    <!-- Apache POI (Word, Excel, PowerPoint) -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi</artifactId>
        <version>5.2.5</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>

    <!-- PDF 解析 -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>3.0.1</version>
    </dependency>

    <!-- HTML 解析 -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.17.2</version>
    </dependency>
</dependencies>
```

### 4.2 创建文档解析服务

**新建文件**：`biz/resource/src/main/java/com/hxh/apboa/resource/service/DocumentParserService.java`

```java
package com.hxh.apboa.resource.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务
 * 支持 Word、Excel、PDF、HTML 等常见办公文档格式
 *
 * @author Claude
 */
@Slf4j
@Service
public class DocumentParserService {

    /** 文档内容最大长度限制（字符数） */
    private static final int MAX_CONTENT_LENGTH = 100000;

    /**
     * 解析文档内容
     *
     * @param inputStream 文件输入流
     * @param extension   文件扩展名
     * @param fileName    文件名（用于提示信息）
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String extension, String fileName) {
        try {
            String content = switch (extension.toLowerCase()) {
                case "doc" -> parseDoc(inputStream);
                case "docx" -> parseDocx(inputStream);
                case "xls" -> parseXls(inputStream);
                case "xlsx" -> parseXlsx(inputStream);
                case "pdf" -> parsePdf(inputStream);
                case "ppt", "pptx" -> parsePptx(inputStream);
                case "html", "htm" -> parseHtml(inputStream);
                case "txt", "csv", "md", "json", "xml", "yaml", "yml" -> parseText(inputStream);
                default -> throw new UnsupportedOperationException("不支持的文件格式: " + extension);
            };

            // 截断过长的内容
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH) + "\n\n... [文档内容过长，已截断] ...";
            }

            return content;
        } catch (Exception e) {
            log.error("解析文档失败: {}", fileName, e);
            return "[文档解析失败: " + e.getMessage() + "]";
        }
    }

    /**
     * 解析 .doc 文件 (旧版 Word)
     */
    private String parseDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 解析 .docx 文件 (新版 Word)
     */
    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }
            return content.toString();
        }
    }

    /**
     * 解析 .xls 文件 (旧版 Excel)
     */
    private String parseXls(InputStream inputStream) throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {
            return parseWorkbook(workbook);
        }
    }

    /**
     * 解析 .xlsx 文件 (新版 Excel)
     */
    private String parseXlsx(InputStream inputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            return parseWorkbook(workbook);
        }
    }

    /**
     * 解析 Excel 工作簿
     */
    private String parseWorkbook(Workbook workbook) {
        StringBuilder content = new StringBuilder();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            content.append("=== Sheet: ").append(sheet.getSheetName()).append(" ===\n");

            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    cells.add(getCellValue(cell));
                }
                content.append(String.join("\t", cells)).append("\n");
            }
            content.append("\n");
        }

        return content.toString();
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        yield String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        yield cell.getCellFormula();
                    }
                }
            }
            case BLANK -> "";
            default -> cell.toString();
        };
    }

    /**
     * 解析 PDF 文件
     */
    private String parsePdf(InputStream inputStream) throws IOException {
        // PDFBox 3.x 需要使用 Loader 加载
        try (PDDocument document = Loader.loadPDF(readAllBytes(inputStream))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 PPT/PPTX 文件
     * 注意：Apache POI 对 PPT 的支持有限，这里提供基础实现
     */
    private String parsePptx(InputStream inputStream) throws IOException {
        try (org.apache.poi.xslf.usermodel.XMLSlideShow slideshow =
                     new org.apache.poi.xslf.usermodel.XMLSlideShow(inputStream)) {
            StringBuilder content = new StringBuilder();

            for (org.apache.poi.xslf.usermodel.XSLFSlide slide : slideshow.getSlides()) {
                content.append("--- 幻灯片 ").append(slides.indexOf(slide) + 1).append(" ---\n");

                for (org.apache.poi.xslf.usermodel.XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape) {
                        content.append(textShape.getText()).append("\n");
                    }
                }
                content.append("\n");
            }

            return content.toString();
        }
    }

    /**
     * 解析 HTML 文件
     */
    private String parseHtml(InputStream inputStream) throws IOException {
        String html = new String(readAllBytes(inputStream), StandardCharsets.UTF_8);
        return Jsoup.parse(html).text();
    }

    /**
     * 解析纯文本文件
     */
    private String parseText(InputStream inputStream) throws IOException {
        return new String(readAllBytes(inputStream), StandardCharsets.UTF_8);
    }

    /**
     * 读取所有字节（兼容 Java 8）
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }

    /**
     * 判断是否是文档类型
     */
    public static boolean isDocumentType(String extension) {
        if (extension == null) {
            return false;
        }
        return switch (extension.toLowerCase()) {
            case "doc", "docx", "xls", "xlsx", "pdf", "ppt", "pptx",
                 "txt", "csv", "md", "html", "htm", "json", "xml", "yaml", "yml" -> true;
            default -> false;
        };
    }
}
```

### 4.3 修改 FileBase64Wrapper 类

**文件**：`common/src/main/java/com/hxh/apboa/common/wrapper/FileBase64Wrapper.java`

添加新字段：

```java
@Getter
@Setter
public class FileBase64Wrapper {
    private ModelType modelType;
    private String mediaType;
    private String base64;

    // 新增字段：文档文本内容
    private String textContent;

    // 新增字段：是否是文档类型
    private boolean document;
}
```

### 4.4 修改 AttachServiceImpl

**文件**：`biz/resource/src/main/java/com/hxh/apboa/resource/service/AttachServiceImpl.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachServiceImpl extends ServiceImpl<AttachMapper, Attach> implements AttachService {
    private final ParamsAdapter paramsAdapter;
    private final StorageProtocolService storageProtocolService;
    private final AttachLogService attachLogService;
    private final AttachChunkMapper attachChunkMapper;

    // 注入文档解析服务
    private final DocumentParserService documentParserService;

    // ... 其他方法保持不变 ...

    @Override
    public FileBase64Wrapper getFileBase64(Long fileId) {
        Attach attach = getById(fileId);
        if (attach == null) {
            return null;
        }

        FileBase64Wrapper wrapper = new FileBase64Wrapper();
        String extension = attach.getExtension();

        // 判断文件类型
        String resultType = switch (extension) {
            case String ext when paramsAdapter.getValue("ALLOW_IMAGE_FILE_TYPE").contains(ext) -> "IMAGE";
            case String ext when paramsAdapter.getValue("ALLOW_AUDIO_FILE_TYPE").contains(ext) -> "AUDIO";
            case String ext when paramsAdapter.getValue("ALLOW_VIDEO_FILE_TYPE").contains(ext) -> "VIDEO";
            default -> null;
        };

        // 获取文件存储服务
        FileStorageService storageService = storageProtocolService.getStorageService();
        if (!storageService.getProtocol().equals(attach.getProtocol())) {
            log.warn("当前文件存储协议为{}，与当前启用的存储配置不匹配", attach.getProtocol());
            return null;
        }

        try (InputStream inputStream = storageService.load(genStoragePath(attach))) {
            if (resultType != null) {
                // 多媒体文件处理
                wrapper.setModelType(ModelType.valueOf(resultType));
                String mediaType = switch (resultType) {
                    case "IMAGE" -> "image/" + extension;
                    case "AUDIO" -> "audio/" + extension;
                    case "VIDEO" -> "video/" + extension;
                    default -> null;
                };
                wrapper.setMediaType(mediaType);
                byte[] bytes = IOUtils.toByteArray(inputStream);
                wrapper.setBase64(Base64.getEncoder().encodeToString(bytes));
            } else if (DocumentParserService.isDocumentType(extension)) {
                // 文档文件处理
                wrapper.setDocument(true);
                String textContent = documentParserService.parse(inputStream, extension, attach.getOriginalName());
                wrapper.setTextContent(textContent);
                wrapper.setModelType(ModelType.DOCUMENT);
            } else {
                // 不支持的文件类型
                return null;
            }
        } catch (IOException e) {
            log.error("读取文件失败: {}", fileId, e);
            return null;
        }

        return wrapper;
    }
}
```

### 4.5 添加 DOCUMENT 枚举值

**文件**：`common/src/main/java/com/hxh/apboa/common/enums/ModelType.java`

```java
@Getter
@AllArgsConstructor
public enum ModelType {
    CHAT("文本"),
    IMAGE("图像"),
    VIDEO("视频"),
    AUDIO("音频"),
    DOCUMENT("文档");  // 新增

    private final String description;
}
```

### 4.6 修改 AguiMessageConverter

**文件**：`console/src/main/java/io/agentscope/core/agui/converter/AguiMessageConverter.java`

```java
private void fullMultimodalMsg(List<Msg> message) {
    // 获取服务
    if (attachService == null) {
        try {
            attachService = BeanUtils.getBean(AttachService.class);
        } catch (BeansException e) {
            log.error(e.getMessage(), e);
        }
        return;
    }

    // 获取附件ID
    AgentContext agentContext = AgentContext.get();
    List<String> fileIds = agentContext.getFileIds();
    if (fileIds != null && !fileIds.isEmpty()) {

        // 基于附件构建多模态 ContentBlock
        List<ContentBlock> blocks = new LinkedList<>();
        StringBuilder documentContents = new StringBuilder();

        fileIds.forEach(fileId -> {
            FileBase64Wrapper wrapper = attachService.getFileBase64(Long.valueOf(fileId));
            if (wrapper != null) {
                if (wrapper.isDocument()) {
                    // 文档类型：收集文本内容
                    documentContents.append(wrapper.getTextContent()).append("\n\n");
                } else {
                    // 多媒体类型：构建对应的 ContentBlock
                    ContentBlock block = switch (wrapper.getModelType()) {
                        case IMAGE -> ImageBlock.builder()
                                .source(Base64Source.builder()
                                        .data(wrapper.getBase64())
                                        .mediaType(wrapper.getMediaType())
                                        .build())
                                .build();
                        case VIDEO -> VideoBlock.builder()
                                .source(Base64Source.builder()
                                        .data(wrapper.getBase64())
                                        .mediaType(wrapper.getMediaType())
                                        .build())
                                .build();
                        case AUDIO -> AudioBlock.builder()
                                .source(Base64Source.builder()
                                        .data(wrapper.getBase64())
                                        .mediaType(wrapper.getMediaType())
                                        .build())
                                .build();
                        default -> null;
                    };

                    if (block != null) {
                        blocks.add(block);
                    }
                }
            }
        });

        // 移除 message 中最后一条消息，并构建新的文本 ContentBlock
        String content = message.removeLast().getTextContent();
        if (content != null && !content.isEmpty()) {
            // 去除文件前缀标记
            String[] split = content.split("@==##::::##==@", 2);
            String result = split.length > 1 ? split[1] : split[0];

            // 如果有文档内容，添加到消息中
            if (documentContents.length() > 0) {
                String docContent = documentContents.toString().trim();
                result = "[文档内容开始]\n" + docContent + "\n[文档内容结束]\n\n" + result;
            }

            blocks.add(TextBlock.builder().text(result).build());
        }

        // 构建复合消息
        Msg multiMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(blocks)
                .build();

        // 追加到末尾
        message.add(multiMsg);
    }
}
```

### 4.7 更新数据库参数

**执行 SQL**：

```sql
-- 更新支持的文档文件类型参数（如果不存在则插入）
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`)
VALUES (8, '支持的文档文件类型', 'ALLOW_DOCUMENT_FILE_TYPE', 'doc,docx,xls,xlsx,pdf,ppt,pptx,txt,csv,md,html,htm')
ON DUPLICATE KEY UPDATE `param_value` = 'doc,docx,xls,xlsx,pdf,ppt,pptx,txt,csv,md,html,htm';
```

### 4.8 更新 SysConst 常量

**文件**：`common/src/main/java/com/hxh/apboa/common/consts/SysConst.java`

```java
public static final String ALLOW_DOCUMENT_FILE_TYPE = "doc,docx,xls,xlsx,pdf,ppt,pptx,txt,csv,md,html,htm";
```

## 5. 前端修改

### 5.1 更新文件类型提示

**文件**：`ui/src/components/chat/ChatInputToolbar.vue`

在 tooltip 中显示支持的文档类型：

```vue
<ATooltip placement="bottom">
  <template #title>
    <span v-if="allowUploadFileType && allowUploadFileType.length > 0">
      点击上传文件（仅支持: {{ allowUploadFileType.join('、') }}）
    </span>
    <span v-else>点击上传文件（支持文档、图片等）</span>
  </template>
  <button
    :disabled="allowUploadFileType === undefined || allowUploadFileType === null"
    type="button"
    class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
    style="margin-right: 15px"
    @click="emit('pickFile')"
  >
    <PaperClipOutlined />
  </button>
</ATooltip>
```

## 6. 测试步骤

### 6.1 单元测试

创建测试类 `DocumentParserServiceTest.java`：

```java
@SpringBootTest
class DocumentParserServiceTest {

    @Autowired
    private DocumentParserService documentParserService;

    @Test
    void testParseDocx() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test.docx");
        String content = documentParserService.parse(is, "docx", "test.docx");
        assertNotNull(content);
        assertFalse(content.isEmpty());
        System.out.println("Parsed content: " + content);
    }

    @Test
    void testParsePdf() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test.pdf");
        String content = documentParserService.parse(is, "pdf", "test.pdf");
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    @Test
    void testParseXlsx() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test.xlsx");
        String content = documentParserService.parse(is, "xlsx", "test.xlsx");
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }
}
```

### 6.2 集成测试

1. 启动后端服务
2. 登录平台
3. 进入聊天页面
4. 上传一个 Word 文档
5. 发送消息："请帮我总结这份文档的内容"
6. 验证智能体能否正确读取并分析文档内容

### 6.3 测试用例

| 测试场景 | 输入文件 | 预期结果 |
|---------|---------|---------|
| 上传 Word 文档 | report.docx | 智能体能读取文档内容并回复 |
| 上传 Excel 表格 | data.xlsx | 智能体能读取表格数据并回复 |
| 上传 PDF 文件 | manual.pdf | 智能体能读取 PDF 内容并回复 |
| 上传纯文本 | readme.txt | 智能体能读取文本内容并回复 |
| 上传 HTML 页面 | page.html | 智能体能读取页面内容并回复 |
| 上传大文件 | large.pdf (>5MB) | 提示文件过大，拒绝上传 |
| 上传不支持的格式 | file.zip | 提示不支持该格式 |

## 7. 注意事项

### 7.1 性能考虑

- **大文件解析**：解析大文档可能需要较长时间，建议：
  - 设置文档内容最大长度限制（如 100KB）
  - 对于超长文档，截断并提示用户
  - 考虑异步解析，避免阻塞请求

### 7.2 安全考虑

- **文件类型验证**：严格验证文件扩展名和 MIME 类型
- **文件大小限制**：防止上传超大文件导致内存溢出
- **内容过滤**：解析后的内容可能包含恶意脚本，需要进行清理

### 7.3 兼容性

- **旧版 Office 格式**：`.doc`、`.xls` 需要使用 HWPF/HSSF 解析
- **新版 Office 格式**：`.docx`、`.xlsx` 需要使用 XWPF/XSSF 解析
- **PDF 版本**：PDFBox 3.x 支持 PDF 1.0-2.0

### 7.4 错误处理

- 解析失败时，返回友好的错误提示
- 记录详细的错误日志，便于排查问题
- 对于损坏的文件，提示用户重新上传

## 8. 依赖版本

| 依赖 | 版本 | 用途 |
|------|------|------|
| Apache POI | 5.2.5 | Word、Excel、PowerPoint 解析 |
| PDFBox | 3.0.1 | PDF 解析 |
| Jsoup | 1.17.2 | HTML 解析 |

## 9. 后续优化

1. **支持更多格式**：如 RTF、ODT、Numbers 等
2. **表格识别**：智能识别表格结构，转换为 Markdown 格式
3. **图片提取**：从文档中提取图片，作为多模态内容传递
4. **OCR 支持**：对于扫描版 PDF，使用 OCR 提取文字
5. **流式解析**：对于大文件，支持流式解析，减少内存占用

---

**文档版本**：v1.0  
**创建日期**：2026-06-06  
**作者**：Claude Code Assistant
