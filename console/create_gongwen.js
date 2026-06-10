const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType, 
        ShadingType, PageNumber, PageBreak } = require('docx');
const fs = require('fs');

// ========== 公文格式常量 ==========
const DXA_PER_CM = 567;
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;
const MARGIN_TOP = Math.round(3.7 * DXA_PER_CM);
const MARGIN_BOTTOM = Math.round(3.5 * DXA_PER_CM);
const MARGIN_LEFT = Math.round(2.8 * DXA_PER_CM);
const MARGIN_RIGHT = Math.round(2.6 * DXA_PER_CM);
const CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

// 字号（半磅单位）
const ER_HAO = 44;
const SAN_HAO = 32;
const SI_HAO = 28;
const XIAO_SI = 24;

// 行距28磅
const LINE_SPACING = 560;

// 边框样式
const thinBorder = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const borders = { top: thinBorder, bottom: thinBorder, left: thinBorder, right: thinBorder };

// ========== 辅助函数 ==========
function bodyPara(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        indent: { firstLine: 640 },
        children: [new TextRun({ text, font: "仿宋", size: SAN_HAO })]
    });
}

function heading1(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        children: [new TextRun({ text, font: "黑体", size: SAN_HAO, bold: true })]
    });
}

function heading2(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_2,
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        indent: { firstLine: 640 },
        children: [new TextRun({ text, font: "楷体", size: SAN_HAO, bold: true })]
    });
}

function emptyLine() {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        children: []
    });
}

function makeCell(text, opts = {}) {
    const { bold, shading, width } = opts;
    return new TableCell({
        borders,
        width: width ? { size: width, type: WidthType.DXA } : undefined,
        shading: shading ? { fill: shading, type: ShadingType.CLEAR } : undefined,
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        children: [new Paragraph({
            spacing: { line: LINE_SPACING, lineRule: "exact" },
            children: [new TextRun({ text, font: "仿宋", size: SAN_HAO, bold: !!bold })]
        })]
    });
}

// ========== 构建文档内容 ==========
const children = [];

// 标题
children.push(new Paragraph({
    spacing: { before: 400, after: 200, line: 780, lineRule: "exact" },
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "弘毅直播销冠教练", font: "黑体", size: ER_HAO, bold: true })]
}));

children.push(new Paragraph({
    spacing: { before: 0, after: 400, line: 780, lineRule: "exact" },
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "安装使用说明书", font: "黑体", size: ER_HAO, bold: true })]
}));

// 副标题
children.push(new Paragraph({
    spacing: { before: 200, after: 100, line: LINE_SPACING, lineRule: "exact" },
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "蒸馏自35位保险直播大咖实战逐字稿", font: "楷体", size: SAN_HAO, bold: true })]
}));

children.push(new Paragraph({
    spacing: { before: 0, after: 400, line: LINE_SPACING, lineRule: "exact" },
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "覆盖直播获客全流程七大阶段 · 道法术器完整体系", font: "楷体", size: SAN_HAO, bold: true })]
}));

// 版本信息表
const colW = Math.round(CONTENT_WIDTH / 4);
children.push(new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [colW, colW, colW, colW],
    rows: [
        new TableRow({ children: [
            makeCell("版本", { bold: true, shading: "E8E8E8", width: colW }),
            makeCell("v2.2", { width: colW }),
            makeCell("更新日期", { bold: true, shading: "E8E8E8", width: colW }),
            makeCell("2026-05-09", { width: colW })
        ]}),
        new TableRow({ children: [
            makeCell("团队", { bold: true, shading: "E8E8E8", width: colW }),
            makeCell("明亚保险经纪 · 弘毅团队", { width: colW }),
            makeCell("机密等级", { bold: true, shading: "E8E8E8", width: colW }),
            makeCell("团队内部使用", { width: colW })
        ]})
    ]
}));

children.push(emptyLine());

// ========== 目录页 ==========
children.push(new Paragraph({
    spacing: { before: 200, after: 300, line: LINE_SPACING, lineRule: "exact" },
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "目  录", font: "黑体", size: ER_HAO, bold: true })]
}));

const tocItems = [
    "第一章  技能介绍 ...................................... 3",
    "第二章  安装指南 ...................................... 4",
    "第三章  使用方法 ...................................... 6",
    "第四章  自我进化机制 .................................. 8",
    "第五章  常见问题 ...................................... 10",
    "附录A   文件清单 ...................................... 11",
    "附录B   蒸馏标准 ...................................... 12"
];

tocItems.forEach(item => {
    children.push(new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "exact" },
        indent: { firstLine: 640 },
        children: [new TextRun({ text: item, font: "仿宋", size: SAN_HAO })]
    }));
});

children.push(new PageBreak());

// ========== 第一章 技能介绍 ==========
children.push(heading1("第一章  技能介绍"));

children.push(heading2("1.1 什么是弘毅直播销冠教练"));
children.push(bodyPara("弘毅直播销冠教练是一款AI智能教练技能，由弘毅团队从35位保险直播大咖的实战逐字稿中深度蒸馏而成。它不是一个简单的问答工具，而是一位24小时在线的直播教练，覆盖从开播准备到成交裂变的全流程。"));

children.push(heading2("1.2 核心价值"));

// 核心价值表格
const coreCol1 = Math.round(CONTENT_WIDTH * 0.3);
const coreCol2 = Math.round(CONTENT_WIDTH * 0.7);
children.push(new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [coreCol1, coreCol2],
    rows: [
        new TableRow({ children: [
            makeCell("维度", { bold: true, shading: "D5E8F0", width: coreCol1 }),
            makeCell("说明", { bold: true, shading: "D5E8F0", width: coreCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("道法术器体系", { width: coreCol1 }),
            makeCell("完整涵盖直播获客的核心理念（道）、七阶段全链路（法）、实战话术库（术）、知识库索引（器）", { width: coreCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("35位大咖经验", { width: coreCol1 }),
            makeCell("HY的295场突破、SWL的TOT之路、SXY的淡季策略等真实案例", { width: coreCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("七阶段全流程", { width: coreCol1 }),
            makeCell("从直播前准备、内容策划、直播间执行、私域承接、成交交付、售后裂变到复盘优化", { width: coreCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("自我进化能力", { width: coreCol1 }),
            makeCell("支持伙伴投喂资料后自动蒸馏，持续增强知识库", { width: coreCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("跨机器即装即用", { width: coreCol1 }),
            makeCell("安装后立即可用，无需额外配置", { width: coreCol2 })
        ]})
    ]
}));

children.push(heading2("1.3 技能文件构成"));
children.push(bodyPara("SKILL.md — 主技能文件（约600行，包含道法术器完整体系）"));
children.push(bodyPara("knowledge/ — 知识库目录（8个文件，覆盖七大阶段+心态能量）"));
children.push(bodyPara("安装说明.md — 简版安装说明"));

children.push(new PageBreak());

// ========== 第二章 安装指南 ==========
children.push(heading1("第二章  安装指南"));

children.push(heading2("2.1 前置条件"));

// 前置条件表格
const preCol1 = Math.round(CONTENT_WIDTH * 0.25);
const preCol2 = Math.round(CONTENT_WIDTH * 0.35);
const preCol3 = Math.round(CONTENT_WIDTH * 0.4);
children.push(new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [preCol1, preCol2, preCol3],
    rows: [
        new TableRow({ children: [
            makeCell("条件", { bold: true, shading: "D5E8F0", width: preCol1 }),
            makeCell("要求", { bold: true, shading: "D5E8F0", width: preCol2 }),
            makeCell("说明", { bold: true, shading: "D5E8F0", width: preCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("WorkBuddy", { width: preCol1 }),
            makeCell("已安装并登录", { width: preCol2 }),
            makeCell("本技能运行在WorkBuddy平台上", { width: preCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("操作系统", { width: preCol1 }),
            makeCell("Windows / macOS", { width: preCol2 }),
            makeCell("两个系统均可安装", { width: preCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("网络连接", { width: preCol1 }),
            makeCell("需要联网", { width: preCol2 }),
            makeCell("AI对话需要网络连接", { width: preCol3 })
        ]})
    ]
}));

children.push(heading2("2.2 Windows 系统安装步骤"));

children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    indent: { firstLine: 640 },
    children: [
        new TextRun({ text: "步骤一：找到安装目录", font: "楷体", size: SAN_HAO, bold: true })
    ]
}));
children.push(bodyPara("在Windows资源管理器中，导航到以下路径："));
children.push(bodyPara("C:\\Users\\你的用户名\\.workbuddy\\skills\\"));
children.push(bodyPara("⚠ .workbuddy 是隐藏文件夹。如果看不到，请在文件夹选项中勾选「显示隐藏的项目」，或在地址栏直接输入路径。"));

children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    indent: { firstLine: 640 },
    children: [
        new TextRun({ text: "步骤二：复制技能文件夹", font: "楷体", size: SAN_HAO, bold: true })
    ]
}));
children.push(bodyPara("将「弘毅直播销冠教练」整个文件夹复制到上面的 skills 目录中。"));

children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    indent: { firstLine: 640 },
    children: [
        new TextRun({ text: "步骤三：重启WorkBuddy", font: "楷体", size: SAN_HAO, bold: true })
    ]
}));
children.push(bodyPara("关闭 WorkBuddy，然后重新打开。技能即自动加载，无需手动激活。"));

children.push(heading2("2.3 macOS 系统安装步骤"));
children.push(bodyPara("在 Finder 中，使用快捷键 Command+Shift+G，输入以下路径："));
children.push(bodyPara("/Users/你的用户名/.workbuddy/skills/"));
children.push(bodyPara("⚠ .workbuddy 是隐藏文件夹。在 Finder 中按 Command+Shift+. 可显示隐藏文件。"));

children.push(heading2("2.4 安装验证"));
children.push(bodyPara("安装成功后，在 WorkBuddy 对话框中输入以下内容进行验证："));
children.push(bodyPara("你好，你是谁？"));
children.push(bodyPara("如果AI回复中提到「弘毅直播销冠教练」相关内容，说明安装成功。"));

children.push(new PageBreak());

// ========== 第三章 使用方法 ==========
children.push(heading1("第三章  使用方法"));

children.push(heading2("3.1 如何触发教练"));
children.push(bodyPara("安装后，直接在 WorkBuddy 对话框中说出你的问题即可，AI会自动识别并调用弘毅直播销冠教练。无需输入任何特殊命令。"));

children.push(heading2("3.2 常用场景与触发话术"));

// 场景表格
const sceneCol1 = Math.round(CONTENT_WIDTH * 0.35);
const sceneCol2 = Math.round(CONTENT_WIDTH * 0.65);
children.push(new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [sceneCol1, sceneCol2],
    rows: [
        new TableRow({ children: [
            makeCell("场景", { bold: true, shading: "D5E8F0", width: sceneCol1 }),
            makeCell("你可以这样说", { bold: true, shading: "D5E8F0", width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("不知道播什么", { width: sceneCol1 }),
            makeCell("我刚开播，不知道讲什么内容，帮我规划一下", { width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("直播间没人", { width: sceneCol1 }),
            makeCell("直播间留不住人怎么办？有什么办法提升停留时长？", { width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("加微不回复", { width: sceneCol1 }),
            makeCell("客户加了微信不回复，怎么跟进？", { width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("不知道促单", { width: sceneCol1 }),
            makeCell("客户说再考虑考虑，我该怎么处理？", { width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("售后转介绍", { width: sceneCol1 }),
            makeCell("成交后怎么做服务？怎么让客户转介绍？", { width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("想放弃", { width: sceneCol1 }),
            makeCell("直播快坚持不下去了，每天都很焦虑", { width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("分红险讲解", { width: sceneCol1 }),
            makeCell("分红险怎么讲？客户问收益怎么回答？", { width: sceneCol2 })
        ]}),
        new TableRow({ children: [
            makeCell("直播复盘", { width: sceneCol1 }),
            makeCell("今天播了一场，帮我复盘一下，数据如下...", { width: sceneCol2 })
        ]})
    ]
}));

children.push(heading2("3.3 投喂资料（让教练为你进化）"));
children.push(bodyPara("这是本技能最强大的功能之一。你可以通过投喂资料让教练变得更强大："));
children.push(bodyPara("【直接粘贴】在对话框中直接粘贴直播逐字稿、客户异议、成功案例等文字内容。"));
children.push(bodyPara("【描述场景】如「我今天遇到一个客户说...」，AI会自动提炼并存入知识库。"));
children.push(bodyPara("【上传文件】将培训笔记、行业政策文档等通过WorkBuddy发送给AI。"));

children.push(new PageBreak());

// ========== 第四章 自我进化机制 ==========
children.push(heading1("第四章  自我进化机制"));

children.push(heading2("4.1 什么是自我进化"));
children.push(bodyPara("弘毅直播销冠教练不是一个静态的工具。它具备自我进化能力——通过伙伴们投喂的资料，持续蒸馏、持续增强。无论在哪台电脑上安装，都可以通过投喂让知识库不断壮大。"));

children.push(heading2("4.2 蒸馏标准"));
children.push(bodyPara("AI在蒸馏资料时，会按以下标准筛选："));

// 蒸馏标准表格
const distCol1 = Math.round(CONTENT_WIDTH * 0.2);
const distCol2 = Math.round(CONTENT_WIDTH * 0.4);
const distCol3 = Math.round(CONTENT_WIDTH * 0.4);
children.push(new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [distCol1, distCol2, distCol3],
    rows: [
        new TableRow({ children: [
            makeCell("类型", { bold: true, shading: "D5E8F0", width: distCol1 }),
            makeCell("保留内容", { bold: true, shading: "D5E8F0", width: distCol2 }),
            makeCell("舍弃内容", { bold: true, shading: "D5E8F0", width: distCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("实战话术", { width: distCol1 }),
            makeCell("可直接读出来用的话术原文", { width: distCol2 }),
            makeCell("修饰性废话、过度客套", { width: distCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("数据案例", { width: distCol1 }),
            makeCell("真实数据、关键转折点", { width: distCol2 }),
            makeCell("夸大宣传、无来源数据", { width: distCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("方法论", { width: distCol1 }),
            makeCell("可复制的SOP流程、决策树", { width: distCol2 }),
            makeCell("模糊的经验描述", { width: distCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("客户洞察", { width: distCol1 }),
            makeCell("真实异议及处理方式", { width: distCol2 }),
            makeCell("理论推演、没有实战验证的", { width: distCol3 })
        ]})
    ]
}));

children.push(new PageBreak());

// ========== 第五章 常见问题 ==========
children.push(heading1("第五章  常见问题"));

children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    indent: { firstLine: 640 },
    children: [
        new TextRun({ text: "Q1：安装后AI没有调用教练怎么办？", font: "楷体", size: SAN_HAO, bold: true })
    ]
}));
children.push(bodyPara("请确认：文件夹名称是否为「弘毅直播销冠教练」，不要有多余的后缀；SKILL.md 是否在文件夹根目录下；是否重启了WorkBuddy；knowledge/ 目录下的8个文件是否完整。"));

children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    indent: { firstLine: 640 },
    children: [
        new TextRun({ text: "Q2：投喂资料后，其他伙伴电脑上也会更新吗？", font: "楷体", size: SAN_HAO, bold: true })
    ]
}));
children.push(bodyPara("不会自动同步。每个伙伴电脑上的技能是独立的。建议由团队统一维护最新版本，定期更新分发。"));

children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    indent: { firstLine: 640 },
    children: [
        new TextRun({ text: "Q3：投喂的资料会丢失吗？", font: "楷体", size: SAN_HAO, bold: true })
    ]
}));
children.push(bodyPara("投喂的内容会写入 knowledge/ 目录下的文件中。只要不删除这些文件，知识会永久保留。建议定期备份 knowledge/ 目录。"));

children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    indent: { firstLine: 640 },
    children: [
        new TextRun({ text: "Q4：可以同时安装多个教练技能吗？", font: "楷体", size: SAN_HAO, bold: true })
    ]
}));
children.push(bodyPara("可以。弘毅直播销冠教练、弘毅招募教练、弘毅视频教练、史海建AI分身可以同时安装。AI会根据你的问题自动选择最合适的教练。"));

children.push(new PageBreak());

// ========== 附录A 文件清单 ==========
children.push(heading1("附录A  文件清单"));

children.push(bodyPara("本技能包含以下文件，安装前请确认全部完整："));

// 文件清单表格
const fileCol1 = Math.round(CONTENT_WIDTH * 0.45);
const fileCol2 = Math.round(CONTENT_WIDTH * 0.15);
const fileCol3 = Math.round(CONTENT_WIDTH * 0.4);
children.push(new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [fileCol1, fileCol2, fileCol3],
    rows: [
        new TableRow({ children: [
            makeCell("文件路径", { bold: true, shading: "D5E8F0", width: fileCol1 }),
            makeCell("大小", { bold: true, shading: "D5E8F0", width: fileCol2 }),
            makeCell("说明", { bold: true, shading: "D5E8F0", width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("SKILL.md", { width: fileCol1 }),
            makeCell("~21KB", { width: fileCol2 }),
            makeCell("主技能文件", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("安装说明.md", { width: fileCol1 }),
            makeCell("~2KB", { width: fileCol2 }),
            makeCell("简版安装说明", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/阶段零_准备.md", { width: fileCol1 }),
            makeCell("~4KB", { width: fileCol2 }),
            makeCell("直播前准备知识库", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/阶段一_策划.md", { width: fileCol1 }),
            makeCell("~8KB", { width: fileCol2 }),
            makeCell("内容策划知识库", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/阶段二_执行.md", { width: fileCol1 }),
            makeCell("~6KB", { width: fileCol2 }),
            makeCell("直播间执行知识库", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/阶段三_承接.md", { width: fileCol1 }),
            makeCell("~8KB", { width: fileCol2 }),
            makeCell("私域承接知识库", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/阶段四_成交.md", { width: fileCol1 }),
            makeCell("~8KB", { width: fileCol2 }),
            makeCell("成交与交付知识库", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/阶段五_售后.md", { width: fileCol1 }),
            makeCell("~8KB", { width: fileCol2 }),
            makeCell("售后与裂变知识库", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/阶段六_复盘.md", { width: fileCol1 }),
            makeCell("~8KB", { width: fileCol2 }),
            makeCell("复盘优化知识库", { width: fileCol3 })
        ]}),
        new TableRow({ children: [
            makeCell("knowledge/心态能量.md", { width: fileCol1 }),
            makeCell("~7KB", { width: fileCol2 }),
            makeCell("心态能量知识库", { width: fileCol3 })
        ]})
    ]
}));

children.push(new PageBreak());

// ========== 附录B 蒸馏标准 ==========
children.push(heading1("附录B  蒸馏标准详细说明"));
children.push(bodyPara("本附录详细说明AI蒸馏资料时的标准，帮助伙伴们了解什么样的资料最有价值。"));

children.push(heading2("B.1 高价值资料（优先蒸馏）"));
children.push(bodyPara("带有具体数据的直播逐字稿（如：在线人数、加微率、转化率）"));
children.push(bodyPara("真实客户异议及处理过程（完整的对话记录）"));
children.push(bodyPara("从0到1的成长故事（含具体数字、时间节点）"));
children.push(bodyPara("可直接复用的话术模板（开场/促单/异议处理/转介绍）"));
children.push(bodyPara("标准化SOP流程（步骤清晰、可执行）"));

children.push(heading2("B.2 中等价值资料（会蒸馏但需加工）"));
children.push(bodyPara("培训课程笔记（需要提炼核心方法论）"));
children.push(bodyPara("行业政策解读（需要转化为实战话术）"));
children.push(bodyPara("直播复盘总结（需要提炼通用改进方法）"));

children.push(heading2("B.3 低价值资料（建议补充后再投喂）"));
children.push(bodyPara("纯理论文章（缺乏实战验证）"));
children.push(bodyPara("模糊的经验描述（如「感觉还不错」、「大概还行」）"));
children.push(bodyPara("无来源的网络文章（无法验证真实性）"));

children.push(emptyLine());
children.push(emptyLine());

// 落款
children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    alignment: AlignmentType.RIGHT,
    children: [new TextRun({ text: "弘毅团队", font: "仿宋", size: SAN_HAO })]
}));
children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    alignment: AlignmentType.RIGHT,
    children: [new TextRun({ text: "让每个家庭拥有专属经纪人", font: "仿宋", size: SAN_HAO })]
}));
children.push(new Paragraph({
    spacing: { line: LINE_SPACING, lineRule: "exact" },
    alignment: AlignmentType.RIGHT,
    children: [new TextRun({ text: "2026年5月9日", font: "仿宋", size: SAN_HAO })]
}));

// ========== 创建文档 ==========
const doc = new Document({
    styles: {
        default: {
            document: { run: { font: "仿宋", size: SAN_HAO } }
        },
        paragraphStyles: [
            {
                id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { size: SAN_HAO, bold: true, font: "黑体" },
                paragraph: { spacing: { before: 120, after: 120, line: LINE_SPACING, lineRule: "exact" }, outlineLevel: 0 }
            },
            {
                id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { size: SAN_HAO, bold: true, font: "楷体" },
                paragraph: { spacing: { before: 80, after: 80, line: LINE_SPACING, lineRule: "exact" }, indent: { firstLine: 640 }, outlineLevel: 1 }
            }
        ]
    },
    sections: [{
        properties: {
            page: {
                size: { width: PAGE_WIDTH, height: PAGE_HEIGHT },
                margin: { top: MARGIN_TOP, bottom: MARGIN_BOTTOM, left: MARGIN_LEFT, right: MARGIN_RIGHT }
            }
        },
        headers: {
            default: new Header({
                children: [new Paragraph({
                    alignment: AlignmentType.CENTER,
                    border: { bottom: { style: BorderStyle.SINGLE, size: 12, color: "FF0000", space: 1 } },
                    children: [new TextRun({ text: "明亚保险经纪 · 弘毅团队", font: "仿宋", size: XIAO_SI, color: "FF0000" })]
                })]
            })
        },
        footers: {
            default: new Footer({
                children: [new Paragraph({
                    alignment: AlignmentType.CENTER,
                    children: [
                        new TextRun({ text: "— ", font: "仿宋", size: SI_HAO }),
                        new TextRun({ children: [PageNumber.CURRENT], font: "Times New Roman", size: SI_HAO }),
                        new TextRun({ text: " —", font: "仿宋", size: SI_HAO })
                    ]
                })]
            })
        },
        children
    }]
});

// 生成文档
Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("弘毅直播销冠教练安装使用说明书_公文版.docx", buffer);
    console.log("文档生成成功：弘毅直播销冠教练安装使用说明书_公文版.docx");
});
