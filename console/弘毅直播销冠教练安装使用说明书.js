const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        Header, Footer, AlignmentType, LevelFormat, 
        TableOfContents, HeadingLevel, BorderStyle, WidthType, ShadingType,
        PageNumber, PageBreak, TabStopType, TabStopPosition } = require('docx');
const fs = require('fs');

// 公文格式常量
const DXA_PER_CM = 567; // 1cm ≈ 567 DXA

// A4纸张尺寸
const PAGE_WIDTH = 11906; // A4宽度
const PAGE_HEIGHT = 16838; // A4高度

// 公文页边距（cm转DXA）
const MARGIN_TOP = Math.round(3.7 * DXA_PER_CM); // 上3.7cm
const MARGIN_BOTTOM = Math.round(3.5 * DXA_PER_CM); // 下3.5cm
const MARGIN_LEFT = Math.round(2.8 * DXA_PER_CM); // 左2.8cm
const MARGIN_RIGHT = Math.round(2.6 * DXA_PER_CM); // 右2.6cm

// 内容宽度
const CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

// 公文字号（半磅单位）
const ER_HAO = 44; // 二号 = 22pt
const SAN_HAO = 32; // 三号 = 16pt
const SI_HAO = 28; // 四号 = 14pt
const XIAO_SI = 24; // 小四 = 12pt

// 公文行距（28磅固定值）
const LINE_SPACING = 560; // 28磅 = 560 twips

// 定义边框样式
const border = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const borders = { top: border, bottom: border, left: border, right: border };
const headerBorder = { style: BorderStyle.SINGLE, size: 12, color: "FF0000" }; // 红色页眉线

// 创建辅助函数：正文段落
function createBodyParagraph(text, options = {}) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        indent: { firstLine: SAN_HAO * 20 }, // 首行缩进2字符（三号字×2）
        ...options,
        children: [
            new TextRun({
                text: text,
                font: { name: "仿宋" },
                size: SAN_HAO,
            })
        ]
    });
}

// 创建辅助函数：带多段文字的段落
function createBodyParagraphWithRuns(runs, options = {}) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        indent: { firstLine: SAN_HAO * 20 },
        ...options,
        children: runs.map(run => new TextRun({
            font: { name: "仿宋" },
            size: SAN_HAO,
            ...run
        }))
    });
}

// 创建辅助函数：无缩进的正文段落
function createNoIndentParagraph(text, options = {}) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        ...options,
        children: [
            new TextRun({
                text: text,
                font: { name: "仿宋" },
                size: SAN_HAO,
            })
        ]
    });
}

// 创建辅助函数：表格单元格
function createTableCell(text, options = {}) {
    return new TableCell({
        borders,
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        ...options,
        children: [
            new Paragraph({
                spacing: { line: LINE_SPACING, lineRule: "exact" },
                children: [
                    new TextRun({
                        text: text,
                        font: { name: "仿宋" },
                        size: SAN_HAO,
                        ...(options.bold ? { bold: true } : {}),
                    })
                ]
            })
        ]
    });
}

// 构建文档
const doc = new Document({
    styles: {
        default: {
            document: {
                run: {
                    font: "仿宋",
                    size: SAN_HAO
                }
            }
        },
        paragraphStyles: [
            // 公文标题样式（方正小标宋体，实际用黑体替代）
            {
                id: "Heading1",
                name: "Heading 1",
                basedOn: "Normal",
                next: "Normal",
                quickFormat: true,
                run: { size: ER_HAO, bold: true, font: { name: "黑体" } },
                paragraph: { 
                    spacing: { before: 0, after: 0, line: 780, lineRule: "exact" }, 
                    alignment: AlignmentType.CENTER,
                    outlineLevel: 0 
                }
            },
            // 一级标题样式（黑体，三号）
            {
                id: "Heading2",
                name: "Heading 2",
                basedOn: "Normal",
                next: "Normal",
                quickFormat: true,
                run: { size: SAN_HAO, bold: true, font: { name: "黑体" } },
                paragraph: { 
                    spacing: { before: 120, after: 120, line: LINE_SPACING, lineRule: "exact" }, 
                    outlineLevel: 1 
                }
            },
            // 二级标题样式（楷体，三号，加粗）
            {
                id: "Heading3",
                name: "Heading 3",
                basedOn: "Normal",
                next: "Normal",
                quickFormat: true,
                run: { size: SAN_HAO, bold: true, font: { name: "楷体" } },
                paragraph: { 
                    spacing: { before: 80, after: 80, line: LINE_SPACING, lineRule: "exact" },
                    indent: { firstLine: SAN_HAO * 20 },
                    outlineLevel: 2 
                }
            },
        ]
    },
    numbering: {
        config: [
            {
                reference: "bullets",
                levels: [{
                    level: 0,
                    format: LevelFormat.BULLET,
                    text: "\u2022",
                    alignment: AlignmentType.LEFT,
                    style: { paragraph: { indent: { left: 840, hanging: 420 } } }
                }]
            },
        ]
    },
    sections: [{
        properties: {
            page: {
                size: { width: PAGE_WIDTH, height: PAGE_HEIGHT },
                margin: { 
                    top: MARGIN_TOP, 
                    bottom: MARGIN_BOTTOM, 
                    left: MARGIN_LEFT, 
                    right: MARGIN_RIGHT 
                }
            }
        },
        headers: {
            default: new Header({
                children: [
                    new Paragraph({
                        alignment: AlignmentType.CENTER,
                        spacing: { after: 0 },
                        children: [
                            new TextRun({
                                text: "明亚保险经纪 · 弘毅团队",
                                font: { name: "仿宋" },
                                size: XIAO_SI,
                                color: "FF0000"
                            })
                        ],
                        border: {
                            bottom: { style: BorderStyle.SINGLE, size: 12, color: "FF0000", space: 1 }
                        }
                    })
                ]
            })
        },
        footers: {
            default: new Footer({
                children: [
                    new Paragraph({
                        alignment: AlignmentType.CENTER,
                        children: [
                            new TextRun({
                                text: "— ",
                                font: { name: "仿宋" },
                                size: SI_HAO
                            }),
                            new TextRun({
                                children: [PageNumber.CURRENT],
                                font: { name: "Times New Roman" },
                                size: SI_HAO
                            }),
                            new TextRun({
                                text: " —",
                                font: { name: "仿宋" },
                                size: SI_HAO
                            })
                        ]
                    })
                ]
            })
        },
        children: [
            // ============ 文档标题 ============
            new Paragraph({
                spacing: { before: 0, after: 200, line: 780, lineRule: "exact" },
                alignment: AlignmentType.CENTER,
                children: [
                    new TextRun({
                        text: "弘毅直播销冠教练",
                        font: { name: "黑体" },
                        size: ER_HAO,
                        bold: true
                    })
                ]
            }),
            new Paragraph({
                spacing: { before: 0, after: 400, line: 780, lineRule: "exact" },
                alignment: AlignmentType.CENTER,
                children: [
                    new TextRun({
                        text: "安装使用说明书",
                        font: { name: "黑体" },
                        size: ER_HAO,
                        bold: true
                    })
                ]
            }),
            
            // 副标题
            new Paragraph({
                spacing: { before: 200, after: 100, line: LINE_SPACING, lineRule: "exact" },
                alignment: AlignmentType.CENTER,
                children: [
                    new TextRun({
                        text: "蒸馏自35位保险直播大咖实战逐字稿",
                        font: { name: "楷体" },
                        size: SAN_HAO,
                        bold: true
                    })
                ]
            }),
            new Paragraph({
                spacing: { before: 0, after: 400, line: LINE_SPACING, lineRule: "exact" },
                alignment: AlignmentType.CENTER,
                children: [
                    new TextRun({
                        text: "覆盖直播获客全流程七大阶段 · 道法术器完整体系",
                        font: { name: "楷体" },
                        size: SAN_HAO,
                        bold: true
                    })
                ]
            }),

            // ============ 版本信息表 ============
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [
                    Math.round(CONTENT_WIDTH * 0.25), 
                    Math.round(CONTENT_WIDTH * 0.25),
                    Math.round(CONTENT_WIDTH * 0.25),
                    Math.round(CONTENT_WIDTH * 0.25)
                ],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("版本", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("v2.2", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } 
                            }),
                            createTableCell("更新日期", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("2026-05-09", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } 
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("团队", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("明亚保险经纪 · 弘毅团队", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA },
                                columnSpan: 3
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("机密等级", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("团队内部使用", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA },
                                columnSpan: 3
                            }),
                        ]
                    })
                ]
            }),
            
            // 空行
            new Paragraph({ spacing: { before: 200, after: 200 } }),
            
            // ============ 目录标题 ============
            new Paragraph({
                spacing: { before: 200, after: 200, line: LINE_SPACING, lineRule: "exact" },
                alignment: AlignmentType.CENTER,
                children: [
                    new TextRun({
                        text: "目    录",
                        font: { name: "黑体" },
                        size: ER_HAO,
                        bold: true
                    })
                ]
            }),
            
            // 目录内容
            new TableOfContents("", {
                hyperlink: true,
                headingStyleRange: "1-3"
            }),
            
            // 分页
            new Paragraph({ children: [new PageBreak()] }),
            
            // ============ 第一章 技能介绍 ============
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [new TextRun("第一章  技能介绍")]
            }),
            
            // 1.1 什么是弘毅直播销冠教练
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("1.1 什么是弘毅直播销冠教练")]
            }),
            createBodyParagraph("弘毅直播销冠教练是一款AI智能教练技能，由弘毅团队从35位保险直播大咖的实战逐字稿中深度蒸馏而成。它不是一个简单的问答工具，而是一位24小时在线的直播教练，覆盖从开播准备到成交裂变的全流程。"),
            
            // 1.2 核心价值
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("1.2 核心价值")]
            }),
            
            // 核心价值表格
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [Math.round(CONTENT_WIDTH * 0.25), Math.round(CONTENT_WIDTH * 0.75)],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("维度", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("说明", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("道法术器体系", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } 
                            }),
                            createTableCell("完整涵盖直播获客的核心理念（道）、七阶段全链路（法）、实战话术库（术）、知识库索引（器）", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } 
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("35位大咖经验", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } 
                            }),
                            createTableCell("HY的295场突破、SWL的TOT之路、SXY的淡季策略等真实案例", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } 
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("七阶段全流程", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } 
                            }),
                            createTableCell("从直播前准备、内容策划、直播间执行、私域承接、成交交付、售后裂变到复盘优化", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } 
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("自我进化能力", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } 
                            }),
                            createTableCell("支持伙伴投喂资料后自动蒸馏，持续增强知识库", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } 
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("跨机器即装即用", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } 
                            }),
                            createTableCell("安装后立即可用，无需额外配置", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } 
                            }),
                        ]
                    })
                ]
            }),
            
            // 1.3 技能文件构成
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("1.3 技能文件构成")]
            }),
            createBodyParagraphWithRuns([
                { text: "SKILL.md", bold: true },
                { text: " — 主技能文件（约600行，包含道法术器完整体系）" }
            ]),
            createBodyParagraphWithRuns([
                { text: "knowledge/", bold: true },
                { text: " — 知识库目录（8个文件，覆盖七大阶段+心态能量）" }
            ]),
            createBodyParagraphWithRuns([
                { text: "安装说明.md", bold: true },
                { text: " — 简版安装说明" }
            ]),
            
            // 分页
            new Paragraph({ children: [new PageBreak()] }),
            
            // ============ 第二章 安装指南 ============
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [new TextRun("第二章  安装指南")]
            }),
            
            // 2.1 前置条件
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("2.1 前置条件")]
            }),
            createBodyParagraph("安装本技能前，请确保您已满足以下条件："),
            
            // 前置条件表格
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [
                    Math.round(CONTENT_WIDTH * 0.2), 
                    Math.round(CONTENT_WIDTH * 0.3),
                    Math.round(CONTENT_WIDTH * 0.5)
                ],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("条件", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("要求", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("说明", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("WorkBuddy", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } 
                            }),
                            createTableCell("已安装并登录", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } 
                            }),
                            createTableCell("本技能运行在WorkBuddy平台上", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } 
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("操作系统", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } 
                            }),
                            createTableCell("Windows / macOS", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } 
                            }),
                            createTableCell("两个系统均可安装", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } 
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("网络连接", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } 
                            }),
                            createTableCell("需要联网", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } 
                            }),
                            createTableCell("AI对话需要网络连接", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } 
                            }),
                        ]
                    })
                ]
            }),
            
            // 2.2 Windows 系统安装步骤
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("2.2 Windows 系统安装步骤")]
            }),
            
            // 步骤一
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [new TextRun("步骤一：找到安装目录")]
            }),
            createBodyParagraph("在Windows资源管理器中，导航到以下路径："),
            createNoIndentParagraph("C:\\Users\\你的用户名\\.workbuddy\\skills\\", {
                indent: { left: 840 },
                spacing: { line: LINE_SPACING, lineRule: "exact" }
            }),
            createBodyParagraphWithRuns([
                { text: "⚠ ", color: "FF0000" },
                { text: ".workbuddy 是隐藏文件夹。如果看不到，请在文件夹选项中勾选「显示隐藏的项目」，或在地址栏直接输入路径。" }
            ]),
            
            // 步骤二
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [new TextRun("步骤二：复制技能文件夹")]
            }),
            createBodyParagraph("将「弘毅直播销冠教练」整个文件夹复制到上面的 skills 目录中。"),
            createBodyParagraph("复制完成后，目录结构应为："),
            
            // 目录结构
            createNoIndentParagraph(".workbuddy/", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("  └── skills/", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("      └── 弘毅直播销冠教练/", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("          ├── SKILL.md", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("          ├── 安装说明.md", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("          └── knowledge/", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("              ├── 阶段零_准备.md", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("              ├── 阶段一_策划.md", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            createNoIndentParagraph("              └── ...", {
                indent: { left: 840 },
                spacing: { line: 400, lineRule: "exact" }
            }),
            
            // 步骤三
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [new TextRun("步骤三：重启WorkBuddy")]
            }),
            createBodyParagraph("关闭 WorkBuddy，然后重新打开。技能即自动加载，无需手动激活。"),
            
            // 2.3 macOS 系统安装步骤
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("2.3 macOS 系统安装步骤")]
            }),
            
            // macOS步骤一
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [new TextRun("步骤一：找到安装目录")]
            }),
            createBodyParagraph("在 Finder 中，使用快捷键 Command+Shift+G，输入以下路径："),
            createNoIndentParagraph("/Users/你的用户名/.workbuddy/skills/", {
                indent: { left: 840 },
                spacing: { line: LINE_SPACING, lineRule: "exact" }
            }),
            createBodyParagraphWithRuns([
                { text: "⚠ ", color: "FF0000" },
                { text: ".workbuddy 是隐藏文件夹。在 Finder 中按 Command+Shift+. 可显示隐藏文件。" }
            ]),
            
            // macOS步骤二
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [new TextRun("步骤二：复制技能文件夹")]
            }),
            createBodyParagraph("将「弘毅直播销冠教练」整个文件夹复制到上面的 skills 目录中。目录结构与Windows一致。"),
            
            // macOS步骤三
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [new TextRun("步骤三：重启WorkBuddy")]
            }),
            createBodyParagraph("关闭并重新打开 WorkBuddy，技能自动生效。"),
            
            // 2.4 安装验证
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("2.4 安装验证")]
            }),
            createBodyParagraph("安装成功后，在 WorkBuddy 对话框中输入以下内容进行验证："),
            createNoIndentParagraph("你好，你是谁？", {
                indent: { left: 840 },
                spacing: { line: LINE_SPACING, lineRule: "exact" },
                children: [
                    new TextRun({
                        text: "你好，你是谁？",
                        font: { name: "楷体" },
                        size: SAN_HAO,
                        bold: true
                    })
                ]
            }),
            createBodyParagraph("如果AI回复中提到「弘毅直播销冠教练」相关内容，说明安装成功。"),
            
            // 分页
            new Paragraph({ children: [new PageBreak()] }),
            
            // ============ 第三章 使用方法 ============
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [new TextRun("第三章  使用方法")]
            }),
            
            // 3.1 如何触发教练
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("3.1 如何触发教练")]
            }),
            createBodyParagraph("安装后，直接在 WorkBuddy 对话框中说出你的问题即可，AI会自动识别并调用弘毅直播销冠教练。无需输入任何特殊命令。"),
            
            // 3.2 常用场景与触发话术
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("3.2 常用场景与触发话术")]
            }),
            createBodyParagraph("以下是伙伴们最常用的场景和对应的触发话术："),
            
            // 场景表格
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [Math.round(CONTENT_WIDTH * 0.25), Math.round(CONTENT_WIDTH * 0.75)],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("场景", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("你可以这样说", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("不知道播什么", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("我刚开播，不知道讲什么内容，帮我规划一下", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("直播间没人", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("直播间留不住人怎么办？有什么办法提升停留时长？", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("加微不回复", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("客户加了微信不回复，怎么跟进？", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("不知道促单", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("客户说再考虑考虑，我该怎么处理？", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("售后转介绍", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("成交后怎么做服务？怎么让客户转介绍？", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("想放弃", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("直播快坚持不下去了，每天都很焦虑", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("分红险讲解", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("分红险怎么讲？客户问收益怎么回答？", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("直播复盘", { width: { size: Math.round(CONTENT_WIDTH * 0.25), type: WidthType.DXA } }),
                            createTableCell("今天播了一场，帮我复盘一下，数据如下...", { width: { size: Math.round(CONTENT_WIDTH * 0.75), type: WidthType.DXA } }),
                        ]
                    })
                ]
            }),
            
            // 3.3 投喂资料
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("3.3 投喂资料（让教练为你进化）")]
            }),
            createBodyParagraph("这是本技能最强大的功能之一。你可以通过投喂资料让教练变得更强大："),
            createBodyParagraphWithRuns([
                { text: "【直接粘贴】", bold: true },
                { text: "在对话框中直接粘贴直播逐字稿、客户异议、成功案例等文字内容。" }
            ]),
            createBodyParagraphWithRuns([
                { text: "【描述场景】", bold: true },
                { text: "如"我今天遇到一个客户说..."，AI会自动提炼并存入知识库。" }
            ]),
            createBodyParagraphWithRuns([
                { text: "【上传文件】", bold: true },
                { text: "将培训笔记、行业政策文档等通过WorkBuddy发送给AI。" }
            ]),
            createBodyParagraph("投喂后，AI会自动执行："),
            
            // 投喂流程表
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [Math.round(CONTENT_WIDTH * 0.2), Math.round(CONTENT_WIDTH * 0.35), Math.round(CONTENT_WIDTH * 0.45)],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("阶段", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("动作", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.35), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("产出", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("基础版", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("安装技能文件", { width: { size: Math.round(CONTENT_WIDTH * 0.35), type: WidthType.DXA } }),
                            createTableCell("七阶段全流程+道法术器体系+案例库", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("投喂资料", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("伙伴粘贴/上传新资料", { width: { size: Math.round(CONTENT_WIDTH * 0.35), type: WidthType.DXA } }),
                            createTableCell("AI自动识别内容类型", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("蒸馏提炼", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("深度研读+提取精华", { width: { size: Math.round(CONTENT_WIDTH * 0.35), type: WidthType.DXA } }),
                            createTableCell("保留话术/方法论/案例，去除水分", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("知识沉淀", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("写入knowledge/目录", { width: { size: Math.round(CONTENT_WIDTH * 0.35), type: WidthType.DXA } }),
                            createTableCell("知识库自动增强，版本号递增", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("持续生长", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("每次对话都是进化机会", { width: { size: Math.round(CONTENT_WIDTH * 0.35), type: WidthType.DXA } }),
                            createTableCell("教练越用越好，越来越懂弘毅团队", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                        ]
                    })
                ]
            }),
            
            // 3.4 对话技巧
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("3.4 对话技巧")]
            }),
            createBodyParagraphWithRuns([
                { text: "【说具体问题】", bold: true }
            ]),
            createBodyParagraphWithRuns([
                { text: "✔ ", color: "008000" },
                { text: ""我直播间平均停留时长只有30秒，怎么提升？"" }
            ], { indent: { left: 840 } }),
            createBodyParagraphWithRuns([
                { text: "✘ ", color: "FF0000" },
                { text: ""直播怎么做？"（太笼统，AI无法给出精准建议）" }
            ], { indent: { left: 840 } }),
            createBodyParagraphWithRuns([
                { text: "【提供数据】", bold: true }
            ]),
            createBodyParagraphWithRuns([
                { text: "✔ ", color: "008000" },
                { text: ""今天直播2小时，最高在线30人，加微5个，怎么提升？"" }
            ], { indent: { left: 840 } }),
            createBodyParagraphWithRuns([
                { text: "【说你的阶段】", bold: true }
            ]),
            createBodyParagraphWithRuns([
                { text: "✔ ", color: "008000" },
                { text: ""我刚播了10场，直播间一直没什么人..."" }
            ], { indent: { left: 840 } }),
            createBodyParagraphWithRuns([
                { text: "✔ ", color: "008000" },
                { text: ""我已经播了200场了，该成交的都成交了，怎么突破？"" }
            ], { indent: { left: 840 } }),
            
            // 分页
            new Paragraph({ children: [new PageBreak()] }),
            
            // ============ 第四章 自我进化机制 ============
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [new TextRun("第四章  自我进化机制")]
            }),
            
            // 4.1 什么是自我进化
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("4.1 什么是自我进化")]
            }),
            createBodyParagraph("弘毅直播销冠教练不是一个静态的工具。它具备自我进化能力——通过伙伴们投喂的资料，持续蒸馏、持续增强。无论在哪台电脑上安装，都可以通过投喂让知识库不断壮大。"),
            
            // 4.2 进化原理
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("4.2 进化原理")]
            }),
            
            // 4.3 蒸馏标准
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("4.3 蒸馏标准")]
            }),
            createBodyParagraph("AI在蒸馏资料时，会按以下标准筛选："),
            
            // 蒸馏标准表
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [Math.round(CONTENT_WIDTH * 0.2), Math.round(CONTENT_WIDTH * 0.4), Math.round(CONTENT_WIDTH * 0.4)],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("类型", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("保留内容", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("舍弃内容", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("实战话术", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("可直接读出来用的话术原文", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                            createTableCell("修饰性废话、过度客套", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("数据案例", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("真实数据、关键转折点", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                            createTableCell("夸大宣传、无来源数据", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("方法论", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("可复制的SOP流程、决策树", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                            createTableCell("模糊的经验描述、"我觉得"", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("客户洞察", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("真实异议及处理方式", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                            createTableCell("理论推演、没有实战验证的", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("心态故事", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("真实经历、具体场景", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                            createTableCell("空洞鸡汤、没有细节的励志", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    })
                ]
            }),
            
            // 4.4 伙伴可以投喂什么
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("4.4 伙伴可以投喂什么")]
            }),
            
            // 投喂类型表
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [
                    Math.round(CONTENT_WIDTH * 0.2), 
                    Math.round(CONTENT_WIDTH * 0.3),
                    Math.round(CONTENT_WIDTH * 0.5)
                ],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("资料类型", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("投喂方式", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("蒸馏方向", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("直播逐字稿", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("直接粘贴文字", { width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } }),
                            createTableCell("提炼话术、互动技巧、成交信号", { width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("客户异议", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell(""我遇到一个客户说..."", { width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } }),
                            createTableCell("更新异议处理话术库", { width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("成功案例", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell(""我用这个方法成交了..."", { width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } }),
                            createTableCell("写入案例库，提炼可复制方法", { width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("失败复盘", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell(""今天播的效果不好..."", { width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } }),
                            createTableCell("诊断问题，给出改进方案", { width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("培训笔记", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("上传文档或粘贴内容", { width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } }),
                            createTableCell("提炼方法论，更新知识体系", { width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("行业政策", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell(""新规出来了..."", { width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } }),
                            createTableCell("更新合规提醒和应对话术", { width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("直播数据", { width: { size: Math.round(CONTENT_WIDTH * 0.2), type: WidthType.DXA } }),
                            createTableCell("提供截图或数据", { width: { size: Math.round(CONTENT_WIDTH * 0.3), type: WidthType.DXA } }),
                            createTableCell("数据诊断，给出优化建议", { width: { size: Math.round(CONTENT_WIDTH * 0.5), type: WidthType.DXA } }),
                        ]
                    })
                ]
            }),
            
            // 4.5 进化记录
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("4.5 进化记录")]
            }),
            createBodyParagraph("每次进化都会在 SKILL.md 末尾记录，包括："),
            createBodyParagraph("进化日期和版本号", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("新增/修改的内容说明", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("跨Skill知识同步情况", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("任何人都可以查看进化记录，了解教练的成长轨迹。"),
            
            // 分页
            new Paragraph({ children: [new PageBreak()] }),
            
            // ============ 第五章 常见问题 ============
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [new TextRun("第五章  常见问题")]
            }),
            
            // Q1
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("Q1：安装后AI没有调用教练怎么办？")]
            }),
            createBodyParagraph("请确认："),
            createBodyParagraph("文件夹名称是否为「弘毅直播销冠教练」，不要有多余的后缀", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("SKILL.md 是否在文件夹根目录下，不要放在子目录中", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("是否重启了WorkBuddy", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("knowledge/ 目录下的8个文件是否完整", { indent: { left: 1200, firstLine: 0 } }),
            
            // Q2
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("Q2：投喂资料后，其他伙伴电脑上也会更新吗？")]
            }),
            createBodyParagraph("不会自动同步。每个伙伴电脑上的技能是独立的。但可以通过以下方式同步："),
            createBodyParagraph("将更新后的文件夹重新分发给伙伴", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("使用版本管理工具（如Git）统一管理", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("建议由团队统一维护最新版本，定期更新分发。"),
            
            // Q3
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("Q3：投喂的资料会丢失吗？")]
            }),
            createBodyParagraph("投喂的内容会写入 knowledge/ 目录下的文件中。只要不删除这些文件，知识会永久保留。建议定期备份 knowledge/ 目录。"),
            
            // Q4
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("Q4：技能更新后需要重新安装吗？")]
            }),
            createBodyParagraph("如果收到新版本的技能文件包，只需要："),
            createBodyParagraph("关闭WorkBuddy", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("用新文件夹替换旧文件夹（建议先备份 knowledge/ 目录）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("重新打开WorkBuddy", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraphWithRuns([
                { text: "注意：", bold: true },
                { text: "如果旧版本中有投喂的内容，建议先将 knowledge/ 文件夹备份出来，合并到新版本中。" }
            ]),
            
            // Q5
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("Q5：可以同时安装多个教练技能吗？")]
            }),
            createBodyParagraph("可以。弘毅直播销冠教练、弘毅招募教练、弘毅视频教练、史海建AI分身可以同时安装。AI会根据你的问题自动选择最合适的教练。"),
            
            // Q6
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("Q6：技能会消耗更多AI额度吗？")]
            }),
            createBodyParagraph("不会额外消耗。技能只是为AI提供了更丰富的知识背景，不会增加API调用次数或token消耗。"),
            
            // 分页
            new Paragraph({ children: [new PageBreak()] }),
            
            // ============ 附录A 文件清单 ============
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [new TextRun("附录A  文件清单")]
            }),
            createBodyParagraph("本技能包含以下文件，安装前请确认全部完整："),
            
            // 文件清单表
            new Table({
                width: { size: CONTENT_WIDTH, type: WidthType.DXA },
                columnWidths: [
                    Math.round(CONTENT_WIDTH * 0.45), 
                    Math.round(CONTENT_WIDTH * 0.15),
                    Math.round(CONTENT_WIDTH * 0.4)
                ],
                rows: [
                    new TableRow({
                        children: [
                            createTableCell("文件路径", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("大小", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                            createTableCell("说明", { 
                                width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA },
                                shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
                                bold: true
                            }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("SKILL.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~21KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("主技能文件", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("安装说明.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~2KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("简版安装说明", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/阶段零_准备.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~4KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("直播前准备知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/阶段一_策划.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~8KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("内容策划知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/阶段二_执行.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~6KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("直播间执行知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/阶段三_承接.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~8KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("私域承接知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/阶段四_成交.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~8KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("成交与交付知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/阶段五_售后.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~8KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("售后与裂变知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/阶段六_复盘.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~8KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("复盘优化知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    }),
                    new TableRow({
                        children: [
                            createTableCell("knowledge/心态能量.md", { width: { size: Math.round(CONTENT_WIDTH * 0.45), type: WidthType.DXA } }),
                            createTableCell("~7KB", { width: { size: Math.round(CONTENT_WIDTH * 0.15), type: WidthType.DXA } }),
                            createTableCell("心态能量知识库", { width: { size: Math.round(CONTENT_WIDTH * 0.4), type: WidthType.DXA } }),
                        ]
                    })
                ]
            }),
            
            createBodyParagraphWithRuns([
                { text: "⚠ ", color: "FF0000" },
                { text: "如果发现文件缺失或大小异常，请联系团队获取完整包。" }
            ]),
            
            // 分页
            new Paragraph({ children: [new PageBreak()] }),
            
            // ============ 附录B 蒸馏标准 ============
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [new TextRun("附录B  蒸馏标准详细说明")]
            }),
            createBodyParagraph("本附录详细说明AI蒸馏资料时的标准，帮助伙伴们了解什么样的资料最有价值。"),
            
            // B.1 高价值资料
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("B.1 高价值资料（优先蒸馏）")]
            }),
            createBodyParagraph("带有具体数据的直播逐字稿（如：在线人数、加微率、转化率）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("真实客户异议及处理过程（完整的对话记录）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("从0到1的成长故事（含具体数字、时间节点）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("可直接复用的话术模板（开场/促单/异议处理/转介绍）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("标准化SOP流程（步骤清晰、可执行）", { indent: { left: 1200, firstLine: 0 } }),
            
            // B.2 中等价值资料
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("B.2 中等价值资料（会蒸馏但需加工）")]
            }),
            createBodyParagraph("培训课程笔记（需要提炼核心方法论）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("行业政策解读（需要转化为实战话术）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("直播复盘总结（需要提炼通用改进方法）", { indent: { left: 1200, firstLine: 0 } }),
            
            // B.3 低价值资料
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [new TextRun("B.3 低价值资料（建议补充后再投喂）")]
            }),
            createBodyParagraph("纯理论文章（缺乏实战验证）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("模糊的经验描述（如"感觉还不错"、"大概还行"）", { indent: { left: 1200, firstLine: 0 } }),
            createBodyParagraph("无来源的网络文章（无法验证真实性）", { indent: { left: 1200, firstLine: 0 } }),
            
            // 空行
            new Paragraph({ spacing: { before: 400, after: 200 } }),
            
            // 落款
            new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { line: LINE_SPACING, lineRule: "exact" },
                children: [
                    new TextRun({
                        text: "弘毅团队 · 让每个家庭拥有专属经纪人",
                        font: { name: "楷体" },
                        size: SAN_HAO,
                        bold: true
                    })
                ]
            }),
            new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { line: LINE_SPACING, lineRule: "exact" },
                children: [
                    new TextRun({
                        text: "本说明书由弘毅团队AI系统自动生成 · v2.2 · 2026-05-09",
                        font: { name: "仿宋" },
                        size: SI_HAO,
                        color: "808080"
                    })
                ]
            })
        ]
    }]
});

// 生成文档
Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("弘毅直播销冠教练安装使用说明书_公文版.docx", buffer);
    console.log("文档已生成：弘毅直播销冠教练安装使用说明书_公文版.docx");
});
