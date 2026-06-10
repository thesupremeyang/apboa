-- 添加文档类型支持参数
-- 执行此 SQL 以支持文档上传功能

-- 添加文档文件类型参数
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`)
VALUES (8, '支持的文档文件类型', 'ALLOW_DOCUMENT_FILE_TYPE', 'doc,docx,xls,xlsx,pdf,ppt,pptx,html,htm,txt,csv,md')
ON DUPLICATE KEY UPDATE `param_value` = 'doc,docx,xls,xlsx,pdf,ppt,pptx,html,htm,txt,csv,md';

-- 更新系统常量（可选，用于代码中的默认值）
-- 在 SysConst.java 中添加：
-- public static final String ALLOW_DOCUMENT_FILE_TYPE = "doc,docx,xls,xlsx,pdf,ppt,pptx,html,htm,txt,csv,md";

-- 说明：
-- doc: Word 97-2003 文档
-- docx: Word 文档
-- xls: Excel 97-2003 表格
-- xlsx: Excel 表格
-- pdf: PDF 文档
-- ppt: PowerPoint 97-2003 演示文稿
-- pptx: PowerPoint 演示文稿
-- html/htm: HTML 网页
-- txt: 纯文本文件
-- csv: CSV 文件
-- md: Markdown 文件
