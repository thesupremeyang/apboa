<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as agentApi from '@/api/agent'

const router = useRouter()

// ========== 响应式状态 ==========
const query = ref('')
const category = ref('全部智能体')
const tab = ref('综合推荐')
const view = ref<'grid' | 'list'>('grid')
const enterprise = ref(false)
const selectedAgent = ref<any>(null)
const toastText = ref('')
const agents = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const tags = ref<string[]>([])
const showMoreCategories = ref(false)

let searchTimer: ReturnType<typeof setTimeout> | null = null
let toastTimer: ReturnType<typeof setTimeout> | null = null

// ========== 静态数据（未对接后端，保留） ==========
const categories = [
  { name: '全部智能体', count: '128', icon: 'grid' },
  { name: '文档智能', count: '24', icon: 'file' },
  { name: '数据分析', count: '28', icon: 'chart' },
  { name: '内容运营', count: '32', icon: 'sparkle' },
  { name: '办公效率', count: '27', icon: 'window' },
]

// 固定分类名称集合，用于过滤后端标签中已有的分类
const fixedCategoryNames = new Set(categories.map(c => c.name))

// 后端标签中不属于固定分类的额外分类
const extraCategories = computed(() => {
  return tags.value.filter(t => !fixedCategoryNames.has(t))
})

// 额外分类的图标映射（按关键词匹配）
const tagIconMap: Record<string, string> = {
  'A2A': 'link',
  '多模态': 'multimodal',
  '创意': 'sparkle',
  '创作': 'sparkle',
  '写作': 'sparkle',
  '翻译': 'globe',
  '编程': 'code',
  '代码': 'code',
  '客服': 'service',
  '营销': 'chart',
  '教育': 'book',
  '医疗': 'heart',
  '金融': 'chart',
  '法律': 'file',
  '人力资源': 'people',
  'HR': 'people',
  '项目': 'window',
  '安全': 'shield',
  '测试': 'check',
}

function getTagIcon(tag: string): string {
  for (const [keyword, icon] of Object.entries(tagIconMap)) {
    if (tag.includes(keyword)) return icon
  }
  return 'tag'
}

const stats = [
  { label: '智能体总数', value: '128', delta: '较上周  +8', icon: 'cube' },
  { label: '使用总次数', value: '32,847', delta: '较上周  +12.5%', icon: 'trend' },
  { label: '解决任务数', value: '18,629', delta: '较上周  +9.3%', icon: 'check' },
  { label: '用户满意度', value: '4.8/5', delta: '较上周  +0.2', icon: 'star' },
]

const hotCapabilities = [
  { name: '数据处理', pct: 32.1 },
  { name: '内容创作', pct: 26.7 },
  { name: '分析洞察', pct: 18.3 },
  { name: '文档处理', pct: 13.2 },
  { name: '流程自动化', pct: 9.7 },
]

const latestPublished = [
  { name: '合同审查助手', tag: '文档智能', time: '1小时前' },
  { name: '财务报表分析师', tag: '数据分析', time: '3小时前' },
  { name: '招聘需求分析师', tag: '办公效率', time: '5小时前' },
  { name: '市场竞品分析师', tag: '数据分析', time: '昨天' },
]

const trendDates = ['05-11', '05-13', '05-15', '05-17']

// ========== 工具函数 ==========
function notify(text: string) {
  toastText.value = text
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => { toastText.value = '' }, 2200)
}

function stableHash(str: string): number {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = ((h << 5) - h + str.charCodeAt(i)) | 0
  }
  return Math.abs(h)
}

function formatNumber(n: number): string {
  if (n >= 10000) return (n / 1000).toFixed(1) + 'k'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

function computeRating(a: any): number {
  const caps = (a.tool?.length || 0) + (a.skill?.length || 0) + (a.knowledgeBase?.length || 0) + (a.mcp?.length || 0)
  return Math.min(5, 4.2 + caps * 0.08)
}

function computeUses(a: any): number {
  return (stableHash(String(a.id)) % 15000) + 500
}

function computeScore(a: any): number {
  const caps = (a.tool?.length || 0) + (a.skill?.length || 0) + (a.knowledgeBase?.length || 0) + (a.mcp?.length || 0)
  return Math.min(99, 82 + caps * 2)
}

const ownerMap: Record<string, string> = {
  '文档智能': '文档科技',
  '数据分析': '数据工场团队',
  '内容运营': '新媒体研究所',
  '办公效率': '效率办公团队',
}

function toCardData(a: any) {
  const caps = (a.tool?.length || 0) + (a.skill?.length || 0) + (a.knowledgeBase?.length || 0) + (a.mcp?.length || 0)
  return {
    id: a.id,
    name: a.name,
    category: a.tag || (a.agentType === 'A2A' ? 'A2A智能体' : '未分类'),
    rating: computeRating(a),
    uses: formatNumber(computeUses(a)),
    score: computeScore(a) + '%',
    owner: ownerMap[a.tag || ''] || (a.agentType === 'A2A' ? 'A2A生态' : '平台官方'),
    image: a.avatar && a.avatar.startsWith('http') ? a.avatar : '',
    desc: a.description || '暂无描述',
    _raw: a,
  }
}

// ========== 已对接后端：筛选与排序 ==========
const filteredAgents = computed(() => {
  let list = agents.value

  // 分类筛选（所有分类名称都可能是有效的筛选条件）
  if (category.value !== '全部智能体') {
    list = list.filter(a => (a.tag || '') === category.value)
  }

  // 搜索筛选
  if (query.value) {
    const q = query.value.toLowerCase()
    list = list.filter(a =>
      `${a.name}${a.tag || ''}${a.description || ''}`.toLowerCase().includes(q)
    )
  }

  // 排序
  if (tab.value === '最高评分') {
    list = [...list].sort((a, b) => computeRating(b) - computeRating(a))
  } else if (tab.value === '最多使用') {
    list = [...list].sort((a, b) => computeUses(b) - computeUses(a))
  } else if (tab.value === '最新发布') {
    list = [...list].sort((a, b) => {
      const ta = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
      const tb = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
      return tb - ta
    })
  }

  return list
})

const cardAgents = computed(() => filteredAgents.value.map(toCardData))

// ========== 已对接后端：API 调用 ==========
async function loadAgents(page = 1, append = false) {
  if (loading.value) return
  loading.value = true
  try {
    const params: any = { page, size: pageSize.value, enabled: true }
    // 服务端搜索：当有搜索关键词时传给后端
    if (query.value) params.name = query.value
    // 服务端标签筛选
    if (category.value !== '全部智能体') {
      params.tag = category.value
    }
    const res = await agentApi.page(params)
    const data = res.data?.data
    if (data) {
      if (append) {
        agents.value = [...agents.value, ...(data.records || [])]
      } else {
        agents.value = data.records || []
      }
      total.value = data.total || 0
      hasMore.value = data.current < data.pages
      currentPage.value = page
    }
  } catch (e) {
    console.error('加载智能体失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    const res = await agentApi.listTags()
    tags.value = res.data?.data || []
  } catch (e) {
    console.error('加载标签失败:', e)
  }
}

function debounceSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    // 搜索时同时用服务端 name 筛选 + 客户端二次过滤
    loadAgents(1)
  }, 300)
}

function handleLoadMore() {
  if (hasMore.value && !loading.value) {
    loadAgents(currentPage.value + 1, true)
  }
}

// ========== 已对接后端：智能体体验 ==========
function handleTryAgent(cardData: any) {
  const id = cardData.id || cardData._raw?.id
  if (!id) {
    notify('智能体 ID 无效')
    return
  }
  const hash = `#/chat/${encodeURIComponent(id)}`
  const url = `${window.location.origin}${window.location.pathname}${hash}`
  window.open(url, '_blank')
}

function handleOpenDetail(cardData: any) {
  selectedAgent.value = cardData
}

// ========== 导航（静态保留） ==========
function handleNav(item: string) {
  if (item === '智能体市场') return
  notify(`${item}功能即将开放`)
}

function handlePublish() {
  notify('发布智能体入口已打开')
}

function handleSubmitRequest() {
  notify('需求提交入口已打开')
}

function handleCustomAgent() {
  notify('定制顾问将尽快联系你')
}

// ========== 生命周期 ==========
onMounted(() => {
  loadTags()
  loadAgents(1)
})

onUnmounted(() => {
  if (searchTimer) clearTimeout(searchTimer)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<template>
  <div class="agent-market-page">
    <!-- 工作区：左侧栏 + 主内容 + 右侧栏 -->
    <div class="workspace">
      <!-- ========== 左侧栏 ========== -->
      <aside class="left-panel panel">
        <h2>智能体市场</h2>
        <div class="category-list">
          <button
            v-for="cat in categories"
            :key="cat.name"
            :class="{ selected: category === cat.name }"
            @click="category = cat.name"
          >
            <!-- 图标 -->
            <svg v-if="cat.icon === 'grid'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
            <svg v-else-if="cat.icon === 'file'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <svg v-else-if="cat.icon === 'chart'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
            <svg v-else-if="cat.icon === 'sparkle'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l2.4 7.2L22 12l-7.6 2.8L12 22l-2.4-7.2L2 12l7.6-2.8z"/></svg>
            <svg v-else-if="cat.icon === 'window'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
            <span>{{ cat.name }}</span>
            <em v-if="cat.count">{{ cat.count }}</em>
          </button>

          <!-- 更多分类：展开/收起按钮 -->
          <button
            v-if="extraCategories.length > 0"
            class="more-categories-btn"
            @click="showMoreCategories = !showMoreCategories"
          >
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg>
            <span>更多分类</span>
            <svg
              width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              :style="{ transform: showMoreCategories ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform 0.25s' }"
            ><polyline points="6 9 12 15 18 9"/></svg>
          </button>
        </div>

        <!-- 更多分类展开区域 -->
        <div v-if="showMoreCategories && extraCategories.length > 0" class="extra-categories">
          <button
            v-for="tag in extraCategories"
            :key="tag"
            :class="{ selected: category === tag }"
            @click="category = category === tag ? '全部智能体' : tag"
          >
            <svg v-if="getTagIcon(tag) === 'link'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'multimodal'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'sparkle'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l2.4 7.2L22 12l-7.6 2.8L12 22l-2.4-7.2L2 12l7.6-2.8z"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'globe'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'code'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'heart'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'book'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'shield'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'people'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <svg v-else-if="getTagIcon(tag) === 'check'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
            <span>{{ tag }}</span>
          </button>
        </div>

        <!-- 静态筛选条件（保留） -->
        <div class="filter-title">
          <strong>筛选条件</strong>
          <button @click="category = '全部智能体'; enterprise = false; showMoreCategories = false">清空</button>
        </div>
        <div class="filter-group">
          <div class="filter-group-header">
            <strong>场景</strong>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          <label v-for="(item, i) in ['全部场景', '数据处理', '内容创作', '分析洞察', '流程自动化', '知识管理']" :key="item">
            <input type="checkbox" :checked="i === 0" readonly />
            <span>{{ item }}</span>
          </label>
        </div>
        <div v-for="item in ['能力标签', '适用行业', '连接应用']" :key="item" class="filter-group">
          <div class="filter-group-header">
            <strong>{{ item }}</strong>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          <button class="select-btn">
            请选择{{ item }}
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
        </div>
        <div class="enterprise-toggle">
          <span>仅看企业可用 <i>i</i></span>
          <button :class="{ on: enterprise }" @click="enterprise = !enterprise">
            <svg v-if="enterprise" width="30" height="30" viewBox="0 0 24 24" fill="currentColor"><rect x="1" y="5" width="22" height="14" rx="7" fill="#1768f2"/><circle cx="17" cy="12" r="4" fill="#fff"/></svg>
            <svg v-else width="30" height="30" viewBox="0 0 24 24" fill="currentColor"><rect x="1" y="5" width="22" height="14" rx="7" fill="#aeb7c5"/><circle cx="7" cy="12" r="4" fill="#fff"/></svg>
          </button>
        </div>
      </aside>

      <!-- ========== 主内容区 ========== -->
      <main class="main-content">
        <!-- 搜索栏 -->
        <div class="search-row">
          <label class="search-box">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
            <input
              v-model="query"
              placeholder="搜索智能体，例如：Excel数据处理、文档总结、市场分析"
              @input="debounceSearch"
            />
            <kbd>⌘ K</kbd>
          </label>
          <button class="publish-btn" @click="handlePublish">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            发布智能体
          </button>
        </div>

        <!-- 仪表盘统计（静态保留） -->
        <section class="stats-row">
          <div v-for="s in stats" :key="s.label" class="stat-card">
            <div>
              <span>{{ s.label }}</span>
              <strong>{{ s.value }}</strong>
              <small>{{ s.delta }} ↑</small>
            </div>
            <i>
              <svg v-if="s.icon === 'cube'" width="31" height="31" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
              <svg v-else-if="s.icon === 'trend'" width="31" height="31" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
              <svg v-else-if="s.icon === 'check'" width="31" height="31" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <svg v-else width="31" height="31" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            </i>
          </div>
        </section>

        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="tabs">
            <button
              v-for="t in ['综合推荐', '最新发布', '最多使用', '最高评分']"
              :key="t"
              :class="{ active: tab === t }"
              @click="tab = t"
            >{{ t }}</button>
          </div>
          <div class="view-controls">
            <button :class="{ active: view === 'grid' }" @click="view = 'grid'">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
            </button>
            <button :class="{ active: view === 'list' }" @click="view = 'list'">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
            </button>
            <button class="sort-btn">
              默认排序
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading && agents.length === 0" class="loading-state">
          <div class="loader"></div>
          <p>正在加载智能体...</p>
        </div>

        <!-- 智能体网格（已对接后端） -->
        <section v-else :class="['agent-grid', view]">
          <article
            v-for="card in cardAgents"
            :key="card.id"
            class="agent-card"
            @click="handleOpenDetail(card)"
          >
            <div class="agent-head">
              <div class="agent-avatar" :style="{ background: `hsl(${stableHash(card.name) % 360}, 60%, 55%)` }">
                <span>{{ card.name.slice(0, 1) }}</span>
              </div>
              <div>
                <h3>{{ card.name }}</h3>
                <p>
                  <span>{{ card.category }}</span>
                  <b>★ {{ card.rating.toFixed(1) }}</b>
                </p>
              </div>
            </div>
            <p class="desc">{{ card.desc }}</p>
            <div class="badges">
              <span>企业认证</span>
              <em>热门</em>
            </div>
            <div class="metrics">
              <span>♧ {{ card.uses }}</span>
              <span>⊙ {{ card.score }}</span>
            </div>
            <div class="owner">
              <span>{{ card.owner.slice(0, 1) }}</span>
              <strong>{{ card.owner }}</strong>
              <button @click.stop="handleTryAgent(card)">立即体验</button>
            </div>
          </article>

          <!-- 提交需求卡片（静态保留） -->
          <button v-if="filteredAgents.length > 0" class="request-card" @click="handleSubmitRequest">
            <span>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </span>
            <strong>提交需求</strong>
            <small>告诉我们你需要的智能体</small>
            <em>去提交</em>
          </button>

          <!-- 空状态 -->
          <div v-if="filteredAgents.length === 0 && !loading" class="empty-state">
            <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
            <strong>没有找到匹配的智能体</strong>
            <span>试试其他关键词或清除筛选条件</span>
          </div>
        </section>

        <!-- 加载更多（已对接后端） -->
        <div v-if="hasMore && agents.length > 0" class="load-more">
          <button :class="{ loading }" @click="handleLoadMore">
            <span v-if="loading" class="btn-loader"></span>
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
        <div v-if="!hasMore && agents.length > 0" class="end-line">
          ✓ 已展示全部 {{ total }} 个智能体
        </div>
      </main>

      <!-- ========== 右侧栏（静态保留） ========== -->
      <aside class="right-panel">
        <!-- 市场洞察 -->
        <section class="panel insight">
          <div class="section-heading">
            <h2>市场洞察</h2>
            <button>更多 <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></button>
          </div>
          <h3>热门能力</h3>
          <div v-for="(item, i) in hotCapabilities" :key="item.name" class="rank">
            <b :class="{ gold: i < 2, orange: i === 2 }">{{ i + 1 }}</b>
            <span>
              {{ item.name }}
              <i><em :style="{ width: (item.pct * 2.7) + '%' }"></em></i>
            </span>
            <small>{{ item.pct }}%</small>
          </div>
        </section>

        <!-- 使用趋势 -->
        <section class="panel trend-card">
          <h3>使用趋势（近7天）</h3>
          <div class="chart">
            <span>10k</span><span>8k</span><span>6k</span><span>4k</span><span>2k</span>
            <svg viewBox="0 0 260 135" preserveAspectRatio="none">
              <path class="area" d="M0 128 C20 55 38 76 58 49 S92 67 112 42 S145 25 164 58 S198 72 218 42 S244 31 260 29 L260 135 L0 135 Z"/>
              <path class="line" d="M0 128 C20 55 38 76 58 49 S92 67 112 42 S145 25 164 58 S198 72 218 42 S244 31 260 29"/>
            </svg>
            <div class="dates">
              <i v-for="d in trendDates" :key="d">{{ d }}</i>
            </div>
          </div>
        </section>

        <!-- 最新发布 -->
        <section class="panel latest">
          <div class="section-heading">
            <h3>最新发布</h3>
            <button>更多 <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></button>
          </div>
          <div v-for="(item, i) in latestPublished" :key="item.name" class="latest-row">
            <i :class="['mini', `c${i}`]">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/></svg>
            </i>
            <strong>{{ item.name }}</strong>
            <em>{{ item.tag }}</em>
            <small>{{ item.time }}</small>
          </div>
        </section>

        <!-- 定制智能体 -->
        <section class="panel custom-cta">
          <div>
            <h3>定制智能体</h3>
            <p>满足个性化业务需求</p>
            <button @click="handleCustomAgent">立即定制</button>
          </div>
          <div class="custom-cube">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
          </div>
        </section>
      </aside>
    </div>

    <!-- ========== 智能体详情弹窗 ========== -->
    <div v-if="selectedAgent" class="modal-backdrop" @click="selectedAgent = null">
      <div class="modal" @click.stop>
        <button class="close-btn" @click="selectedAgent = null">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
        <div class="modal-avatar" :style="{ background: `hsl(${stableHash(selectedAgent.name) % 360}, 60%, 55%)` }">
          <span>{{ selectedAgent.name.slice(0, 1) }}</span>
        </div>
        <div>
          <span class="modal-tag">{{ selectedAgent.category }}</span>
          <h2>{{ selectedAgent.name }}</h2>
          <p>{{ selectedAgent.desc }}</p>
          <div class="modal-meta">
            <b>★ {{ selectedAgent.rating.toFixed(1) }}</b>
            <b>{{ selectedAgent.uses }} 次使用</b>
            <b>{{ selectedAgent.score }} 满意度</b>
          </div>
          <button class="modal-try-btn" @click="handleTryAgent(selectedAgent)">立即体验</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div v-if="toastText" class="toast">
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
      {{ toastText }}
    </div>
  </div>
</template>

<style scoped>
/* ========================================
   变量与基础
   ======================================== */
.agent-market-page {
  --blue: #1768f2;
  --line: #e5ebf5;
  --muted: #7b879d;
  --shadow: 0 6px 22px rgba(49, 93, 170, .08);
  font-family: "Noto Sans SC", "Microsoft YaHei", sans-serif;
  color: #182033;
  background: linear-gradient(150deg, #fff 0%, #f8fbff 35%, #f5f9ff 100%);
  min-height: 100%;
  font-size: 13px;
}

.agent-market-page button {
  cursor: pointer;
  font: inherit;
  color: inherit;
  border: 0;
  background: none;
}

/* ========================================
   工作区布局
   ======================================== */
.workspace {
  display: grid;
  grid-template-columns: 218px minmax(600px, 1fr) 274px;
  gap: 14px;
  padding: 16px;
  height: 100%;
  min-height: calc(100vh - 120px);
}

.panel {
  background: rgba(255, 255, 255, .91);
  border: 1px solid var(--line);
  border-radius: 15px;
  box-shadow: 0 3px 15px rgba(45, 83, 142, .035);
}

/* ========================================
   左侧栏
   ======================================== */
.left-panel {
  padding: 18px 10px;
  overflow: auto;
}

.left-panel h2 {
  font-size: 17px;
  margin: 0 8px 10px;
}

.category-list {
  display: grid;
  gap: 3px;
}

.category-list button {
  height: 41px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 10px;
}

.category-list button span {
  flex: 1;
  text-align: left;
}

.category-list button em {
  font-style: normal;
  font-size: 11px;
  color: #6e7d94;
}

.category-list button.selected {
  background: #edf4ff;
  color: #1261e9;
  font-weight: 600;
}

.category-list button.selected em {
  background: #dbeaff;
  color: #1261e9;
  border-radius: 8px;
  padding: 1px 5px;
}

.category-list .more-categories-btn {
  height: 41px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 10px;
  color: #657087;
}

.category-list .more-categories-btn:hover {
  background: #f4f6f9;
  color: var(--blue);
}

.extra-categories {
  display: grid;
  gap: 2px;
  padding: 4px 0 8px;
  margin: 0 0 4px;
  border-top: 1px dashed var(--line);
  animation: fadeInDown 0.25s ease;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.extra-categories button {
  height: 38px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px 0 14px;
  font-size: 13px;
  color: #4a5568;
  transition: all 0.15s;
}

.extra-categories button:hover {
  background: #f4f6f9;
  color: var(--blue);
}

.extra-categories button.selected {
  background: #edf4ff;
  color: #1261e9;
  font-weight: 600;
}

.extra-categories button span {
  flex: 1;
  text-align: left;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding: 0 7px 10px;
}

.tag-list button {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  background: #f4f6f9;
  color: #657087;
  transition: all 0.2s;
}

.tag-list button:hover {
  background: #e8f0ff;
  color: var(--blue);
}

.tag-list button.selected {
  background: #edf4ff;
  color: var(--blue);
  font-weight: 600;
}

.filter-title {
  margin: 8px 7px 0;
  border-top: 1px solid var(--line);
  padding: 14px 0 8px;
  display: flex;
  justify-content: space-between;
}

.filter-title button {
  color: #8b97aa;
}

.filter-group {
  border-bottom: 1px solid #edf1f7;
  padding: 7px;
}

.filter-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.filter-group label {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #657087;
  margin: 10px 0;
}

.filter-group input[type="checkbox"] {
  appearance: none;
  width: 15px;
  height: 15px;
  border: 1px solid #d4ddea;
  border-radius: 4px;
  margin: 0;
}

.filter-group input[type="checkbox"]:checked {
  background: var(--blue);
  border-color: var(--blue);
  box-shadow: inset 0 0 0 3px #fff;
}

.select-btn {
  width: 100%;
  height: 32px;
  border: 1px solid #dce4f0;
  background: #fff;
  border-radius: 6px;
  color: #96a0b2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 9px;
}

.enterprise-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 7px;
}

.enterprise-toggle span {
  font-weight: 600;
}

.enterprise-toggle i {
  display: inline-grid;
  place-items: center;
  border: 1px solid #9aa6b8;
  color: #8b97a9;
  font-style: normal;
  border-radius: 50%;
  font-size: 9px;
  width: 13px;
  height: 13px;
}

.enterprise-toggle button {
  color: #aeb7c5;
}

.enterprise-toggle button.on {
  color: var(--blue);
}

/* ========================================
   主内容区
   ======================================== */
.main-content {
  min-width: 0;
  overflow: auto;
  padding: 4px 5px;
  scrollbar-width: none;
}

.main-content::-webkit-scrollbar {
  display: none;
}

.search-row {
  display: flex;
  gap: 30px;
  margin-bottom: 26px;
}

.search-box {
  height: 43px;
  border: 1px solid #d9e4f3;
  box-shadow: 0 7px 19px rgba(40, 100, 190, .1);
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 13px;
  flex: 1;
  color: #65738a;
}

.search-box input {
  border: 0;
  outline: 0;
  flex: 1;
  color: #273247;
  font: inherit;
}

.search-box input::placeholder {
  color: #9aa6b8;
}

.search-box kbd {
  border: 1px solid #d9e1ed;
  background: #fbfcfe;
  border-radius: 5px;
  padding: 2px 7px;
  color: #79869a;
  font-size: 11px;
}

.publish-btn {
  width: 150px;
  border: 1px solid #cbdcf8;
  background: #f9fbff;
  color: var(--blue);
  border-radius: 9px;
  box-shadow: 0 6px 20px rgba(41, 105, 211, .15);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-weight: 600;
}

/* 仪表盘统计 */
.stats-row {
  height: 119px;
  background: rgba(255, 255, 255, .9);
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow);
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-bottom: 21px;
}

.stat-card {
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-right: 1px solid var(--line);
}

.stat-card:last-child {
  border: 0;
}

.stat-card div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.stat-card span {
  color: #768298;
}

.stat-card strong {
  font-size: 25px;
  line-height: 1.1;
  color: #111827;
}

.stat-card small {
  color: #158f5f;
}

.stat-card i {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #f4f9ff, #e8f3ff);
  border-radius: 50%;
  color: #2384f4;
  font-style: normal;
}

/* 工具栏 */
.toolbar {
  height: 53px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tabs {
  display: flex;
  gap: 25px;
}

.tabs button {
  padding: 7px 12px;
  font-weight: 500;
}

.tabs button.active {
  background: #edf4ff;
  color: var(--blue);
  border-radius: 8px;
}

.view-controls {
  display: flex;
  gap: 9px;
}

.view-controls button {
  height: 31px;
  min-width: 48px;
  border: 1px solid #dae4f2;
  background: #fff;
  border-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #68758a;
}

.view-controls button.active {
  color: var(--blue);
  background: #eef5ff;
}

.sort-btn {
  width: 132px;
  justify-content: space-between;
  padding: 0 12px;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 100px 20px;
}

.loader {
  width: 36px;
  height: 36px;
  border: 3px solid #e5ebf5;
  border-top-color: var(--blue);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  color: #7b879d;
  font-size: 14px;
}

/* ========================================
   智能体卡片网格
   ======================================== */
.agent-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(225px, 1fr));
  gap: 12px;
  padding-bottom: 12px;
}

.agent-grid.list {
  grid-template-columns: 1fr;
}

.agent-card {
  height: 222px;
  border: 1px solid #dfe7f2;
  border-radius: 13px;
  background: rgba(255, 255, 255, .88);
  padding: 15px;
  display: flex;
  flex-direction: column;
  transition: .2s;
  overflow: hidden;
  cursor: pointer;
}

.agent-card:hover {
  transform: translateY(-2px);
  border-color: #b8cff4;
  box-shadow: var(--shadow);
}

.agent-head {
  display: flex;
  gap: 13px;
  align-items: center;
}

.agent-avatar {
  width: 45px;
  height: 45px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.agent-head h3 {
  font-size: 15px;
  margin: 0 0 6px;
  color: #131b29;
}

.agent-head p {
  margin: 0;
  display: flex;
  gap: 18px;
}

.agent-head p span {
  font-size: 10px;
  color: #7f8b9f;
  background: #f4f6f9;
  padding: 2px 5px;
  border-radius: 3px;
}

.agent-head p b {
  font-size: 11px;
  color: #3e4656;
  font-weight: 500;
}

.agent-head p b:first-letter {
  color: #ff9f16;
}

.desc {
  color: #687489;
  line-height: 1.55;
  height: 42px;
  margin: 11px 0 7px;
  overflow: hidden;
  font-size: 12px;
}

.badges {
  display: flex;
  gap: 7px;
}

.badges span,
.badges em {
  font-style: normal;
  font-size: 10px;
  border-radius: 4px;
  padding: 2px 6px;
}

.badges span {
  background: #edf5ff;
  color: #2c7bf0;
}

.badges em {
  background: #fff0ec;
  color: #f07b61;
}

.metrics {
  display: flex;
  gap: 22px;
  color: #7d899e;
  font-size: 11px;
  margin: 8px 0;
}

.owner {
  border-top: 1px solid #edf1f6;
  margin-top: auto;
  padding-top: 8px;
  display: flex;
  align-items: center;
  gap: 7px;
}

.owner > span {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #e7f0ff;
  color: #236de1;
  font-size: 10px;
  display: grid;
  place-items: center;
}

.owner strong {
  font-size: 11px;
  color: #68758a;
  font-weight: 500;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.owner button {
  border: 1px solid #cfe0f9;
  color: var(--blue);
  background: #f6f9ff;
  border-radius: 5px;
  font-size: 11px;
  padding: 4px 13px;
}

.owner button:hover {
  background: var(--blue);
  color: #fff;
}

/* 提交需求卡片 */
.request-card {
  height: 222px;
  border: 1px dashed #d4deec;
  border-radius: 13px;
  background: rgba(255, 255, 255, .5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #40506a;
}

.request-card span {
  width: 32px;
  height: 32px;
  border: 2px solid #aab8cc;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #8190a5;
}

.request-card small {
  color: #9aa5b6;
}

.request-card em {
  font-style: normal;
  color: var(--blue);
  background: #edf4ff;
  border: 1px solid #d3e2f9;
  border-radius: 5px;
  padding: 5px 25px;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #9aa6b8;
}

.empty-state strong {
  color: #56647a;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 20px;
}

.load-more button {
  padding: 10px 40px;
  border: 1px solid #d9e4f3;
  border-radius: 8px;
  background: #fff;
  color: #273247;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.load-more button:hover {
  border-color: var(--blue);
  color: var(--blue);
}

.load-more button.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-loader {
  width: 14px;
  height: 14px;
  border: 2px solid #e5ebf5;
  border-top-color: var(--blue);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

.end-line {
  text-align: center;
  padding: 20px;
  color: #7b879d;
  font-size: 12px;
}

/* ========================================
   右侧栏
   ======================================== */
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: auto;
}

.right-panel .panel {
  padding: 16px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-heading h2,
.section-heading h3 {
  margin: 0;
}

.section-heading button {
  color: #8390a3;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

/* 市场洞察 */
.insight {
  height: 310px;
}

.insight h2 {
  font-size: 17px;
}

.insight h3,
.trend-card h3,
.latest h3,
.custom-cta h3 {
  font-size: 14px;
  margin: 22px 0 14px;
}

.rank {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 11px 0;
}

.rank > b {
  width: 17px;
  height: 17px;
  border-radius: 5px;
  background: #f0f2f6;
  display: grid;
  place-items: center;
  font-size: 10px;
  color: #707d91;
}

.rank > b.gold {
  background: #fff1d9;
  color: #f19c25;
}

.rank > b.orange {
  background: #ffebe7;
  color: #eb775f;
}

.rank span {
  flex: 1;
  color: #4c596e;
  font-size: 12px;
}

.rank span i {
  height: 3px;
  background: #eef2f7;
  display: block;
  margin-top: 6px;
}

.rank span em {
  height: 100%;
  display: block;
  background: #4d88ff;
}

.rank small {
  color: #4c596e;
  font-size: 11px;
}

/* 使用趋势 */
.trend-card {
  height: 226px;
}

.trend-card h3 {
  margin: 0 0 10px;
}

.chart {
  height: 165px;
  border-left: 1px solid #edf1f6;
  border-bottom: 1px solid #edf1f6;
  position: relative;
  margin: 0 4px 0 25px;
  background: repeating-linear-gradient(to bottom, transparent 0, transparent 32px, #edf1f6 33px);
}

.chart > span {
  position: absolute;
  left: -27px;
  color: #8490a2;
  font-size: 9px;
}

.chart > span:nth-child(1) { top: -3px; }
.chart > span:nth-child(2) { top: 30px; }
.chart > span:nth-child(3) { top: 63px; }
.chart > span:nth-child(4) { top: 96px; }
.chart > span:nth-child(5) { top: 129px; }

.chart svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
}

.chart .area {
  fill: #e7f1ff;
}

.chart .line {
  stroke: #1768f2;
  fill: none;
  stroke-width: 2;
}

.dates {
  position: absolute;
  bottom: -20px;
  left: -6px;
  right: -6px;
  display: flex;
  justify-content: space-between;
}

.dates i {
  font-size: 9px;
  font-style: normal;
  color: #7d899b;
}

/* 最新发布 */
.latest {
  min-height: 200px;
}

.latest h3 {
  margin: 0;
}

.latest-row {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 13px 0;
}

.latest-row .mini {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  border-radius: 4px;
  background: #746ef5;
  font-style: normal;
}

.latest-row .c1 { background: #27afd2; }
.latest-row .c2 { background: #efb443; }
.latest-row .c3 { background: #f15d57; }

.latest-row strong {
  font-size: 11px;
  flex: 1;
}

.latest-row em {
  font-size: 9px;
  font-style: normal;
  background: #eef4ff;
  color: #4b86e8;
  padding: 2px 4px;
  border-radius: 3px;
}

.latest-row small {
  font-size: 9px;
  color: #9aa4b4;
}

/* 定制智能体 */
.custom-cta {
  height: 165px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
}

.custom-cta h3 {
  margin: 0 0 7px;
}

.custom-cta p {
  margin: 0 0 14px;
  color: #8b97aa;
}

.custom-cta button {
  border: 1px solid #bcd2f7;
  background: #eef5ff;
  color: var(--blue);
  padding: 6px 14px;
  border-radius: 6px;
}

.custom-cube {
  color: #4a9cf5;
  background: #edf6ff;
  width: 105px;
  height: 105px;
  display: grid;
  place-items: center;
  border-radius: 28px;
  transform: rotate(-8deg);
}

/* ========================================
   弹窗
   ======================================== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(21, 31, 49, .26);
  display: grid;
  place-items: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
}

.modal {
  width: 560px;
  background: #fff;
  border-radius: 18px;
  padding: 28px;
  display: flex;
  gap: 22px;
  position: relative;
  box-shadow: 0 25px 80px rgba(27, 55, 99, .25);
}

.modal-avatar {
  width: 112px;
  height: 112px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 42px;
  font-weight: 700;
  flex-shrink: 0;
}

.close-btn {
  position: absolute;
  right: 15px;
  top: 15px;
  background: #f4f6f9;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
}

.modal-tag {
  color: var(--blue);
  background: #edf4ff;
  padding: 4px 8px;
  border-radius: 5px;
  font-size: 12px;
}

.modal h2 {
  margin: 8px 0;
  font-size: 20px;
}

.modal p {
  color: #657187;
  line-height: 1.7;
  font-size: 13px;
}

.modal-meta {
  display: flex;
  gap: 18px;
  color: #7c899d;
  font-size: 11px;
  margin: 18px 0;
}

.modal-try-btn {
  background: var(--blue);
  color: #fff;
  border-radius: 8px;
  padding: 9px 28px;
  font-weight: 600;
}

.modal-try-btn:hover {
  opacity: 0.9;
}

/* Toast */
.toast {
  position: fixed;
  top: 82px;
  left: 50%;
  transform: translateX(-50%);
  background: #17233a;
  color: #fff;
  border-radius: 8px;
  padding: 10px 17px;
  display: flex;
  gap: 8px;
  align-items: center;
  z-index: 2000;
  box-shadow: var(--shadow);
  font-size: 13px;
}

/* ========================================
   响应式
   ======================================== */
@media (max-width: 1350px) {
  .workspace {
    grid-template-columns: 200px minmax(500px, 1fr);
  }
  .right-panel {
    display: none;
  }
  .agent-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1050px) {
  .workspace {
    grid-template-columns: 1fr;
    height: auto;
  }
  .left-panel {
    display: none;
  }
  .agent-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-content {
    overflow: visible;
  }
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
    height: auto;
  }
  .stat-card:nth-child(2) {
    border-right: 0;
  }
}

@media (min-width: 1351px) {
  .agent-grid:not(.list) {
    grid-template-columns: repeat(4, minmax(170px, 1fr));
  }
  .agent-card {
    padding: 13px;
  }
  .agent-head {
    gap: 10px;
  }
  .agent-avatar {
    width: 54px;
    height: 54px;
  }
}

@media (max-width: 680px) {
  .workspace {
    padding: 10px;
  }
  .search-row {
    gap: 8px;
  }
  .publish-btn {
    width: 48px;
    font-size: 0;
  }
  .agent-grid {
    grid-template-columns: 1fr;
  }
  .stats-row {
    grid-template-columns: 1fr;
  }
  .stat-card {
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }
  .tabs {
    gap: 0;
  }
  .view-controls {
    display: none;
  }
}
</style>
