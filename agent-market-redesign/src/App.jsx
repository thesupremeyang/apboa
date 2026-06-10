import { useMemo, useState } from "react";
import {
  AppWindow, Bell, CaretDown, CaretRight, ChartBar, ChartLine, Check,
  CirclesFour, Clock, CloudArrowUp, Cube, DotsThree, File, Funnel,
  GridFour, List, MagnifyingGlass, Plus, SlidersHorizontal, Sparkle,
  SquaresFour, Star, Storefront, SuitcaseSimple, ToggleLeft, ToggleRight,
  TrendUp, Users, X
} from "@phosphor-icons/react";

const categories = [
  ["全部智能体", "128", CirclesFour],
  ["文档智能", "24", File],
  ["数据分析", "28", ChartLine],
  ["内容运营", "32", Sparkle],
  ["办公效率", "27", AppWindow],
  ["更多分类", "", DotsThree],
];

const agents = [
  { name: "Excel数据处理专家", category: "数据分析", rating: 4.8, uses: "12.4k", score: "92%", owner: "数据工场团队", image: "/Excel数据处理专家.png", desc: "擅长Excel数据清洗、处理、分析与可视化，支持复杂公式、透视表和自动化报表生成。" },
  { name: "PDF文档处理专家", category: "文档智能", rating: 4.7, uses: "9.8k", score: "90%", owner: "文档科技", image: "/PDF文档处理专家.png", desc: "支持PDF内容提取、摘要、翻译、格式转换与合并，极速处理各类复杂文档。" },
  { name: "PPT演示制作专家", category: "内容运营", rating: 4.8, uses: "11.6k", score: "94%", owner: "演示设计室", image: "/PPT演示制作专家.png", desc: "根据主题快速生成专业PPT，支持内容策划、版式设计与演讲备注。" },
  { name: "Word文档排版专家", category: "办公效率", rating: 4.7, uses: "8.7k", score: "89%", owner: "效率办公团队", image: "/Word文档排版专家.png", desc: "专业文档排版与格式优化，支持样式统一、目录生成与批量处理。" },
  { name: "高级数据分析师", category: "数据分析", rating: 4.9, uses: "15.3k", score: "95%", owner: "数智分析院", image: "/高级数据分析师.png", desc: "多维数据分析、建模与可视化，提供深度洞察与决策建议。" },
  { name: "会议效率专家", category: "办公效率", rating: 4.6, uses: "7.9k", score: "88%", owner: "协作未来", image: "/会议效率专家.png", desc: "会议纪要生成、待办提炼、议题跟踪，提升团队协作与执行效率。" },
  { name: "小红书爆款操盘手", category: "内容运营", rating: 4.8, uses: "13.2k", score: "93%", owner: "新媒体研究所", image: "/小红书爆款操盘手.png", desc: "小红书内容选题、文案创作与数据分析，助力打造爆款笔记。" },
  { name: "直播短视频电商专家", category: "内容运营", rating: 4.7, uses: "10.1k", score: "91%", owner: "电商增长实验室", image: "/直播短视频电商专家.png", desc: "直播脚本策划、短视频创作与带货策略，提升转化与GMV。" },
];

const stats = [
  ["智能体总数", "128", "较上周  +8", Cube],
  ["使用总次数", "32,847", "较上周  +12.5%", TrendUp],
  ["解决任务数", "18,629", "较上周  +9.3%", Check],
  ["用户满意度", "4.8/5", "较上周  +0.2", Star],
];

function App() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("全部智能体");
  const [tab, setTab] = useState("综合推荐");
  const [view, setView] = useState("grid");
  const [enterprise, setEnterprise] = useState(false);
  const [selected, setSelected] = useState(null);
  const [toast, setToast] = useState("");

  const filtered = useMemo(() => agents.filter((agent) => {
    const categoryMatch = category === "全部智能体" || category === "更多分类" || agent.category === category;
    const queryMatch = `${agent.name}${agent.category}${agent.desc}`.toLowerCase().includes(query.toLowerCase());
    return categoryMatch && queryMatch;
  }).sort((a, b) => tab === "最高评分" ? b.rating - a.rating : tab === "最多使用" ? parseFloat(b.uses) - parseFloat(a.uses) : 0), [query, category, tab]);

  const notify = (text) => {
    setToast(text);
    window.setTimeout(() => setToast(""), 2200);
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand"><div className="brand-mark"><span /><span /></div><strong>Agent广场</strong></div>
        <nav>
          {["智能体市场", "我的智能体", "应用管理", "插件中心", "数据看板", "帮助文档"].map((item, i) =>
            <button key={item} className={i === 0 ? "active" : ""} onClick={() => i !== 0 && notify(`${item}功能即将开放`)}>{item}</button>
          )}
        </nav>
        <div className="top-actions">
          <button className="icon-btn"><Bell size={20} /><b>12</b></button>
          <button className="edition"><Sparkle size={16} />企业版</button>
          <button className="profile"><span>张</span>张小明<CaretDown size={14} /></button>
        </div>
      </header>

      <div className="workspace">
        <aside className="left-panel panel">
          <h2>智能体市场</h2>
          <div className="category-list">
            {categories.map(([name, count, Icon]) => <button className={category === name ? "selected" : ""} onClick={() => setCategory(name)} key={name}><Icon size={17} /><span>{name}</span><em>{count}</em>{name === "更多分类" && <CaretDown size={13} />}</button>)}
          </div>
          <div className="filter-title"><strong>筛选条件</strong><button onClick={() => { setCategory("全部智能体"); setEnterprise(false); }}>清空</button></div>
          <FilterGroup title="场景" open>
            {["全部场景", "数据处理", "内容创作", "分析洞察", "流程自动化", "知识管理"].map((item, i) => <label key={item}><input type="checkbox" checked={i === 0} readOnly /><span>{item}</span></label>)}
          </FilterGroup>
          {["能力标签", "适用行业", "连接应用"].map((item) => <FilterGroup key={item} title={item}><button className="select-btn">请选择{item}<CaretDown size={13} /></button></FilterGroup>)}
          <div className="enterprise-toggle"><span>仅看企业可用 <i>i</i></span><button className={enterprise ? "on" : ""} onClick={() => setEnterprise(!enterprise)}>{enterprise ? <ToggleRight weight="fill" size={30} /> : <ToggleLeft weight="fill" size={30} />}</button></div>
        </aside>

        <main>
          <div className="search-row">
            <label className="search"><MagnifyingGlass size={20} /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="搜索智能体，例如：Excel数据处理、文档总结、市场分析" /><kbd>⌘ K</kbd></label>
            <button className="publish" onClick={() => notify("发布智能体入口已打开")}><CloudArrowUp size={18} />发布智能体</button>
          </div>

          <section className="stats">
            {stats.map(([label, value, delta, Icon]) => <div className="stat" key={label}><div><span>{label}</span><strong>{value}</strong><small>{delta} ↑</small></div><i><Icon size={31} weight="duotone" /></i></div>)}
          </section>

          <div className="toolbar">
            <div className="tabs">{["综合推荐", "最新发布", "最多使用", "最高评分"].map((item) => <button className={tab === item ? "active" : ""} onClick={() => setTab(item)} key={item}>{item}</button>)}</div>
            <div className="view-controls"><button className={view === "grid" ? "active" : ""} onClick={() => setView("grid")}><GridFour size={17} weight="fill" /></button><button className={view === "list" ? "active" : ""} onClick={() => setView("list")}><List size={18} /></button><button className="sort">默认排序<CaretDown size={13} /></button></div>
          </div>

          <section className={`agent-grid ${view}`}>
            {filtered.map((agent) => <AgentCard key={agent.name} agent={agent} onOpen={() => setSelected(agent)} onTry={() => notify(`正在打开 ${agent.name}`)} />)}
            {filtered.length > 0 && <button className="request-card" onClick={() => notify("需求提交入口已打开")}><span><Plus size={24} /></span><strong>提交需求</strong><small>告诉我们你需要的智能体</small><em>去提交</em></button>}
            {filtered.length === 0 && <div className="empty"><MagnifyingGlass size={34} /><strong>没有找到匹配的智能体</strong><span>试试其他关键词或清除筛选条件</span></div>}
          </section>
        </main>

        <aside className="right-panel">
          <section className="panel insight">
            <div className="section-heading"><h2>市场洞察</h2><button>更多 <CaretRight size={13} /></button></div>
            <h3>热门能力</h3>
            {["数据处理|32.1", "内容创作|26.7", "分析洞察|18.3", "文档处理|13.2", "流程自动化|9.7"].map((row, i) => { const [name, n] = row.split("|"); return <div className="rank" key={name}><b>{i + 1}</b><span>{name}<i><em style={{ width: `${Number(n) * 2.7}%` }} /></i></span><small>{n}%</small></div>; })}
          </section>
          <section className="panel trend-card"><h3>使用趋势（近7天）</h3><div className="chart"><span>10k</span><span>8k</span><span>6k</span><span>4k</span><span>2k</span><svg viewBox="0 0 260 135" preserveAspectRatio="none"><path className="area" d="M0 128 C20 55 38 76 58 49 S92 67 112 42 S145 25 164 58 S198 72 218 42 S244 31 260 29 L260 135 L0 135 Z"/><path className="line" d="M0 128 C20 55 38 76 58 49 S92 67 112 42 S145 25 164 58 S198 72 218 42 S244 31 260 29"/></svg><div className="dates"><i>05-11</i><i>05-13</i><i>05-15</i><i>05-17</i></div></div></section>
          <section className="panel latest"><div className="section-heading"><h3>最新发布</h3><button>更多 <CaretRight size={13} /></button></div>{[["合同审查助手", "文档智能", "1小时前"], ["财务报表分析师", "数据分析", "3小时前"], ["招聘需求分析师", "办公效率", "5小时前"], ["市场竞品分析师", "数据分析", "昨天"]].map(([name, tag, time], i) => <div className="latest-row" key={name}><i className={`mini c${i}`}><File size={12} weight="fill" /></i><strong>{name}</strong><em>{tag}</em><small>{time}</small></div>)}</section>
          <section className="panel custom"><div><h3>定制智能体</h3><p>满足个性化业务需求</p><button onClick={() => notify("定制顾问将尽快联系你")}>立即定制</button></div><div className="custom-cube"><Cube size={64} weight="duotone" /></div></section>
        </aside>
      </div>

      {selected && <div className="modal-backdrop" onClick={() => setSelected(null)}><div className="modal" onClick={(e) => e.stopPropagation()}><button className="close" onClick={() => setSelected(null)}><X size={20} /></button><img src={selected.image} /><div><span>{selected.category}</span><h2>{selected.name}</h2><p>{selected.desc}</p><div className="modal-meta"><b>★ {selected.rating}</b><b>{selected.uses} 次使用</b><b>{selected.score} 满意度</b></div><button onClick={() => notify(`正在打开 ${selected.name}`)}>立即体验</button></div></div></div>}
      {toast && <div className="toast"><Check size={17} />{toast}</div>}
    </div>
  );
}

function FilterGroup({ title, open, children }) {
  return <div className="filter-group"><div><strong>{title}</strong><CaretDown size={13} className={open ? "" : "closed"} /></div>{children}</div>;
}

function AgentCard({ agent, onOpen, onTry }) {
  return <article className="agent-card" onClick={onOpen}><div className="agent-head"><img src={agent.image} /><div><h3>{agent.name}</h3><p><span>{agent.category}</span><b>★ {agent.rating}</b></p></div></div><p className="desc">{agent.desc}</p><div className="badges"><span>企业认证</span><em>热门</em></div><div className="metrics"><span>♧ {agent.uses}</span><span>⊙ {agent.score}</span></div><div className="owner"><span>{agent.owner.slice(0, 1)}</span><strong>{agent.owner}</strong><button onClick={(e) => { e.stopPropagation(); onTry(); }}>立即体验</button></div></article>;
}

export default App;
