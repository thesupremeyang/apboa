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
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务
 * 支持 Word、Excel、PowerPoint 等常见办公文档格式
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
                case "ppt" -> parsePpt(inputStream);
                case "pptx" -> parsePptx(inputStream);
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
     * 解析 .doc 文件 (旧版 Word 97-2003)
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

            // 解析段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            // 解析表格
            for (XWPFTable table : document.getTables()) {
                content.append("\n[表格内容]\n");
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText().trim());
                    }
                    content.append(String.join(" | ", cells)).append("\n");
                }
                content.append("\n");
            }

            return content.toString();
        }
    }

    /**
     * 解析 .xls 文件 (旧版 Excel 97-2003)
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
                for (int j = 0; j <= row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    cells.add(getCellValue(cell));
                }
                // 移除末尾的空单元格
                while (!cells.isEmpty() && cells.get(cells.size() - 1).isEmpty()) {
                    cells.remove(cells.size() - 1);
                }
                if (!cells.isEmpty()) {
                    content.append(String.join("\t", cells)).append("\n");
                }
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
                // 避免科学计数法
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    yield String.valueOf((long) value);
                }
                yield String.valueOf(value);
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
        try (PDDocument document = Loader.loadPDF(readAllBytes(inputStream))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 .ppt 文件 (旧版 PowerPoint 97-2003)
     * 注意：Apache POI 对旧版 PPT 支持有限
     */
    private String parsePpt(InputStream inputStream) throws IOException {
        // 旧版 PPT 格式需要使用 HSLF，但功能有限
        // 这里返回提示信息
        return "[旧版 PPT 格式解析功能有限，建议转换为 PPTX 格式后重新上传]";
    }

    /**
     * 解析 .pptx 文件 (新版 PowerPoint)
     */
    private String parsePptx(InputStream inputStream) throws IOException {
        try (XMLSlideShow slideshow = new XMLSlideShow(inputStream)) {
            StringBuilder content = new StringBuilder();
            List<XSLFSlide> slides = slideshow.getSlides();

            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                content.append("--- 幻灯片 ").append(i + 1).append(" ---\n");

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\n");
                        }
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
     * 读取所有字节
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
                 "html", "htm", "txt", "csv", "md", "json", "xml", "yaml", "yml" -> true;
            default -> false;
        };
    }

    /**
     * 获取文档类型的中文名称
     */
    public static String getDocumentTypeName(String extension) {
        if (extension == null) {
            return "未知格式";
        }
        return switch (extension.toLowerCase()) {
            case "doc" -> "Word 97-2003 文档";
            case "docx" -> "Word 文档";
            case "xls" -> "Excel 97-2003 表格";
            case "xlsx" -> "Excel 表格";
            case "pdf" -> "PDF 文档";
            case "ppt" -> "PowerPoint 97-2003 演示文稿";
            case "pptx" -> "PowerPoint 演示文稿";
            case "html", "htm" -> "HTML 网页";
            case "txt" -> "纯文本文件";
            case "csv" -> "CSV 文件";
            case "md" -> "Markdown 文件";
            case "json" -> "JSON 文件";
            case "xml" -> "XML 文件";
            case "yaml", "yml" -> "YAML 文件";
            default -> "文档";
        };
    }
}
