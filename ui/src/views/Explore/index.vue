<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as agentApi from '@/api/agent'

const router = useRouter()

// ========== 响应式状态 ==========
const query = ref('')
const category = ref('全部智能体')
const tab = ref('综合推荐')
const view = ref<'grid' | 'list'>('grid')
const showSortDropdown = ref(false)
const SORT_OPTIONS = ['综合推荐', '最新发布', '最多使用', '最高评分']
const selectedAgent = ref<any>(null)
const toastText = ref('')
const agents = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)
const tags = ref<string[]>([])
const overallTotal = ref(0) // 智能体总数（不受筛选影响）

// ========== 伪装筛选状态 ==========
const selectedCapability = ref('')
const selectedIndustry = ref('')
const selectedApp = ref('')
const showIndustryDropdown = ref(false)
const showAppDropdown = ref(false)

// 能力标签选项（映射到标签）
const capabilityOptions = [
  { label: '文本生成', tag: '文案写作' },
  { label: '数据分析', tag: 'AI应用' },
  { label: '内容创作', tag: '内容生成' },
  { label: '营销策划', tag: '营销增长' },
  { label: '教育培训', tag: 'AI教育' },
  { label: '办公自动化', tag: '办公提效' },
  { label: '视频制作', tag: 'AI视频' },
  { label: '客户管理', tag: '私域运营' },
]

// 适用行业选项（映射到标签）
const industryOptions = [
  { label: '教育行业', tag: 'AI教育' },
  { label: '电商行业', tag: '营销增长' },
  { label: '金融行业', tag: 'AI应用' },
  { label: '医疗行业', tag: 'AI应用' },
  { label: '制造行业', tag: '办公提效' },
  { label: '传媒行业', tag: '内容运营' },
  { label: '零售行业', tag: '私域运营' },
  { label: '科技行业', tag: 'AIP' },
]

// 连接应用选项（映射到标签）
const appOptions = [
  { label: '飞书', tag: '办公提效' },
  { label: '钉钉', tag: '办公提效' },
  { label: '企业微信', tag: '私域运营' },
  { label: '抖音', tag: 'AI视频' },
  { label: '小红书', tag: '内容运营' },
  { label: '微信公众号', tag: '内容运营' },
  { label: '淘宝', tag: '营销增长' },
  { label: '京东', tag: '营销增长' },
]

// 筛选处理函数
function handleCapabilitySelect(item: { label: string; tag: string }) {
  if (selectedCapability.value === item.label) {
    selectedCapability.value = ''
    category.value = '全部智能体'
  } else {
    selectedCapability.value = item.label
    category.value = item.tag
  }
}

function handleIndustryClick() {
  showIndustryDropdown.value = !showIndustryDropdown.value
  showAppDropdown.value = false
}

function handleIndustrySelect(item: { label: string; tag: string }) {
  selectedIndustry.value = item.label
  category.value = item.tag
  showIndustryDropdown.value = false
}

function handleAppClick() {
  showAppDropdown.value = !showAppDropdown.value
  showIndustryDropdown.value = false
}

function handleAppSelect(item: { label: string; tag: string }) {
  selectedApp.value = item.label
  category.value = item.tag
  showAppDropdown.value = false
}

function handleMobileCategorySelect(name: string) {
  category.value = name
  selectedCapability.value = ''
  selectedIndustry.value = ''
  selectedApp.value = ''
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
let toastTimer: ReturnType<typeof setTimeout> | null = null

// ========== 动态分类数据（对接后端） ==========
const tagCounts = ref<Record<string, number>>({})

// 固定的10个标签分类
const FIXED_TAGS = [
  'AI应用',
  '文案写作',
  '内容生成',
  'AI教育',
  '内容运营',
  '办公提效',
  '营销增长',
  'AI视频',
  '私域运营',
  'AIP',
]

const categories = computed(() => {
  const fixed = [
    { name: '全部智能体', count: String(overallTotal.value || 0), icon: 'grid' },
  ]
  // 使用固定标签列表，从tagCounts获取数量
  const tagEntries = FIXED_TAGS.map(tag => ({
    name: tag,
    count: String(tagCounts.value[tag] || 0),
    icon: getTagIcon(tag),
  }))
  return [...fixed, ...tagEntries]
})

// 额外分类的图标映射（按关键词匹配）
const tagIconMap: Record<string, string> = {
  'A2A': 'link',
  '多模态': 'multimodal',
  '办公提效': 'window',
  '内容运营': 'chart',
  '文案写作': 'sparkle',
  '内容生成': 'sparkle',
  'AI教育': 'book',
  'AI应用': 'cube',
  'AI视频': 'video',
  '营销增长': 'chart',
  '私域运营': 'people',
  'AIP': 'cube',
  // 兼容旧标签
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

const hotCapabilities = [
  { name: '数据处理', pct: 32.1 },
  { name: '内容创作', pct: 26.7 },
  { name: '分析洞察', pct: 18.3 },
  { name: '文档处理', pct: 13.2 },
  { name: '流程自动化', pct: 9.7 },
]

const latestPublished = ref<any[]>([])

// 加载最新发布的智能体
async function loadLatestPublished() {
  try {
    const res = await agentApi.page({ page: 1, size: 4, enabled: true })
    const data = res.data?.data
    if (data?.records) {
      // 按创建时间倒序排序
      const sorted = [...data.records].sort((a: any, b: any) => {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      })
      latestPublished.value = sorted.slice(0, 4).map((item: any) => ({
        id: item.id,
        name: item.name,
        tag: item.tag || '未分类',
        time: formatTime(item.createdAt),
      }))
    }
  } catch (e) {
    console.error('加载最新发布失败:', e)
    // 使用默认数据
    latestPublished.value = [
      { id: '', name: '合同审查助手', tag: '办公提效', time: '1小时前' },
      { id: '', name: '财务报表分析师', tag: 'AI应用', time: '3小时前' },
      { id: '', name: '招聘需求分析师', tag: '办公提效', time: '5小时前' },
      { id: '', name: '市场竞品分析师', tag: '营销增长', time: '昨天' },
    ]
  }
}

// 格式化时间为相对时间
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days === 1) return '昨天'
  if (days < 30) return `${days}天前`
  return `${Math.floor(days / 30)}个月前`
}

// 跳转到对话页面
function goToChat(agentId: string) {
  if (agentId) {
    router.push(`/chat/${agentId}`)
  }
}

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
  '办公提效': '效率办公团队',
  '文案写作': '内容创作团队',
  '内容生成': 'AI创作团队',
  'AI教育': '教育科技团队',
  'AI应用': 'AI技术团队',
  'AI视频': '视频技术团队',
  '营销增长': '营销增长团队',
  '私域运营': '私域运营团队',
  'AIP': 'AI技术团队',
}

function toCardData(a: any) {
  const caps = (a.tool?.length || 0) + (a.skill?.length || 0) + (a.knowledgeBase?.length || 0) + (a.mcp?.length || 0)
  // 优先使用后端 avatar，否则根据名称匹配本地头像
  let avatarUrl = ''
  if (a.avatar && a.avatar.startsWith('http')) {
    avatarUrl = a.avatar
  } else if (a.name) {
    avatarUrl = `/avatars/${encodeURIComponent(a.name)}.png`
  }
  return {
    id: a.id,
    name: a.name,
    category: a.tag || (a.agentType === 'A2A' ? 'A2A智能体' : '未分类'),
    rating: computeRating(a),
    uses: formatNumber(computeUses(a)),
    score: computeScore(a) + '%',
    owner: ownerMap[a.tag || ''] || (a.agentType === 'A2A' ? 'A2A生态' : '平台官方'),
    image: avatarUrl,
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
async function loadAgents(page = 1) {
  if (loading.value) return
  loading.value = true
  try {
    const params: any = { page, size: pageSize.value, enabled: true }
    if (query.value) params.name = query.value
    if (category.value !== '全部智能体') {
      params.tag = category.value
    }
    const res = await agentApi.page(params)
    const data = res.data?.data
    if (data) {
      agents.value = data.records || []
      total.value = data.total || 0
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

async function loadExploreStats() {
  try {
    const res = await agentApi.getExploreStats()
    const data = res.data?.data
    if (data) {
      overallTotal.value = data.totalAgents || 0
      tagCounts.value = data.tagCounts || {}
    }
  } catch (e) {
    console.error('加载广场统计失败:', e)
  }
}

function debounceSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    // 搜索时同时用服务端 name 筛选 + 客户端二次过滤
    loadAgents(1)
  }, 300)
}

function doSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  loadAgents(1)
}

// 分类切换时重新从后端加载（带 tag 筛选）
watch(category, () => {
  loadAgents(1)
})

// 点击外部关闭排序下拉
function handleSortDropdownOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.sort-wrapper')) {
    showSortDropdown.value = false
  }
}

// 排序下拉显示时监听全局点击
watch(showSortDropdown, (val) => {
  if (val) {
    setTimeout(() => document.addEventListener('mousedown', handleSortDropdownOutside), 0)
  } else {
    document.removeEventListener('mousedown', handleSortDropdownOutside)
  }
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const visiblePages = computed(() => {
  const pages: number[] = []
  const tp = totalPages.value
  const cp = currentPage.value
  let start = Math.max(1, cp - 2)
  let end = Math.min(tp, start + 4)
  if (end - start < 4) start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function goToPage(page: number) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  loadAgents(page)
  document.querySelector('.main-content')?.scrollTo({ top: 0, behavior: 'smooth' })
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

function handleCustomAgent() {
  notify('定制顾问将尽快联系你')
}

// ========== 生命周期 ==========
onMounted(() => {
  loadTags()
  loadAgents(1)
  loadExploreStats()
  loadLatestPublished()
})

onUnmounted(() => {
  if (searchTimer) clearTimeout(searchTimer)
  if (toastTimer) clearTimeout(toastTimer)
  document.removeEventListener('mousedown', handleSortDropdownOutside)
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
            <svg v-else-if="cat.icon === 'video'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
            <svg v-else-if="cat.icon === 'cube'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
            <svg v-else-if="cat.icon === 'book'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
            <svg v-else-if="cat.icon === 'people'" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <span>{{ cat.name }}</span>
            <em :data-count="cat.count">{{ cat.count }}</em>
          </button>
        </div>

        <!-- 伪装筛选条件（映射到标签） -->
        <div class="filter-title">
          <strong>筛选条件</strong>
          <button @click="category = '全部智能体'; selectedCapability = ''; selectedIndustry = ''; selectedApp = ''">清空</button>
        </div>
        <div class="filter-group">
          <div class="filter-group-header">
            <strong>能力标签</strong>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          <div class="capability-tags">
            <button
              v-for="item in capabilityOptions"
              :key="item.label"
              :class="{ selected: selectedCapability === item.label }"
              @click="handleCapabilitySelect(item)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
        <div class="filter-group">
          <div class="filter-group-header">
            <strong>适用行业</strong>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          <button class="select-btn" @click="handleIndustryClick">
            {{ selectedIndustry || '请选择适用行业' }}
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <div v-if="showIndustryDropdown" class="dropdown-list">
            <div
              v-for="item in industryOptions"
              :key="item.label"
              class="dropdown-item"
              @click="handleIndustrySelect(item)"
            >
              {{ item.label }}
            </div>
          </div>
        </div>
        <div class="filter-group">
          <div class="filter-group-header">
            <strong>连接应用</strong>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          <button class="select-btn" @click="handleAppClick">
            {{ selectedApp || '请选择连接应用' }}
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <div v-if="showAppDropdown" class="dropdown-list">
            <div
              v-for="item in appOptions"
              :key="item.label"
              class="dropdown-item"
              @click="handleAppSelect(item)"
            >
              {{ item.label }}
            </div>
          </div>
        </div>
      </aside>

      <!-- ========== 主内容区 ========== -->
      <main class="main-content">
        <!-- 探索 Hero -->
        <section class="market-hero">
          <div class="hero-content">
            <span class="hero-eyebrow">智能体广场</span>
            <h1>探索并使用适合你工作场景的智能体</h1>
            <div class="search-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              <input
                v-model="query"
                placeholder="搜索：文档总结、数据分析、PPT 制作、会议纪要……"
                @input="debounceSearch"
                @keydown.enter="doSearch"
              />
              <button class="search-btn" @click="doSearch" title="搜索">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              </button>
            </div>
          </div>
          <div class="publish-btn custom-cta">
            <div class="custom-cta-text">
              <h3>定制智能体</h3>
              <p>满足个性化业务需求</p>
              <button @click.stop="handleCustomAgent">立即定制</button>
            </div>
            <div class="custom-cube">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
            </div>
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
            <div class="sort-wrapper" style="position:relative">
              <button class="sort-btn" @click.stop="showSortDropdown = !showSortDropdown">
                {{ tab }}
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
              </button>
              <div v-if="showSortDropdown" class="sort-dropdown">
                <div
                  v-for="opt in SORT_OPTIONS"
                  :key="opt"
                  :class="['sort-option', { active: tab === opt }]"
                  @click="tab = opt; showSortDropdown = false"
                >{{ opt }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 移动端快捷筛选 -->
        <div class="mobile-filters" aria-label="智能体分类筛选">
          <span class="mobile-filter-label">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 5h16M7 12h10M10 19h4"/></svg>
            筛选
          </span>
          <div class="mobile-filter-scroll">
            <button
              v-for="cat in categories"
              :key="cat.name"
              :class="{ active: category === cat.name }"
              @click="handleMobileCategorySelect(cat.name)"
            >
              {{ cat.name === '全部智能体' ? '全部' : cat.name }}
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
                <img v-if="card.image" :src="card.image" :alt="card.name" @error="($event.target as HTMLImageElement).style.display='none'" />
                <span v-else>{{ card.name.slice(0, 1) }}</span>
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

          <!-- 空状态 -->
          <div v-if="filteredAgents.length === 0 && !loading" class="empty-state">
            <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
            <strong>没有找到匹配的智能体</strong>
            <span>试试其他关键词或清除筛选条件</span>
          </div>
        </section>

        <!-- 分页控件 -->
        <div v-if="total > pageSize" class="pagination">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
            上一页
          </button>
          <button
            v-for="p in visiblePages"
            :key="p"
            :class="['page-num', { active: p === currentPage }]"
            @click="goToPage(p)"
          >{{ p }}</button>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">
            下一页
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
          <span class="page-info">共 {{ total }} 个智能体</span>
        </div>
      </main>

      <!-- ========== 底部面板（已注释，后期需要可取消注释恢复） ==========
      <div class="bottom-panels">
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
        <section class="panel latest">
          <div class="section-heading">
            <h3>最新发布</h3>
            <button>更多 <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></button>
          </div>
          <div v-for="(item, i) in latestPublished" :key="item.name" class="latest-row" @click="goToChat(item.id)">
            <i :class="['mini', `c${i}`]">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/></svg>
            </i>
            <strong>{{ item.name }}</strong>
            <em>{{ item.tag }}</em>
            <small>{{ item.time }}</small>
          </div>
        </section>
      </div>
      ========== 底部面板结束 ========== -->
    </div>

    <!-- ========== 智能体详情弹窗 ========== -->
    <div v-if="selectedAgent" class="modal-backdrop" @click="selectedAgent = null">
      <div class="modal" @click.stop>
        <button class="close-btn" @click="selectedAgent = null">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
        <div class="modal-avatar" :style="{ background: `hsl(${stableHash(selectedAgent.name) % 360}, 60%, 55%)` }">
          <img v-if="selectedAgent.image" :src="selectedAgent.image" :alt="selectedAgent.name" @error="($event.target as HTMLImageElement).style.display='none'" />
          <span v-else>{{ selectedAgent.name.slice(0, 1) }}</span>
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
  grid-template-columns: 218px minmax(600px, 1fr);
  grid-template-rows: 1fr auto;
  gap: 14px;
  padding: 16px;
  height: 100%;
  min-height: calc(100vh - 120px);
}

.bottom-panels {
  grid-column: 2 / 3;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
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
  grid-row: span 2;
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

.category-list button em:empty {
  display: none;
}

.category-list button em[data-count="0"] {
  color: #b0b8c9;
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
  cursor: pointer;
  position: relative;
}

.select-btn:hover {
  border-color: #1768f2;
  color: #1768f2;
}

.capability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px 0;
}

.capability-tags button {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #dce4f0;
  background: #fff;
  border-radius: 14px;
  font-size: 12px;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s;
}

.capability-tags button:hover {
  border-color: #1768f2;
  color: #1768f2;
  background: #edf4ff;
}

.capability-tags button.selected {
  border-color: #1768f2;
  background: #1768f2;
  color: #fff;
}

.dropdown-list {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #dce4f0;
  border-radius: 6px;
  margin-top: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
  max-height: 200px;
  overflow-y: auto;
}

.dropdown-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.15s;
}

.dropdown-item:hover {
  background: #edf4ff;
  color: #1768f2;
}

.filter-group {
  position: relative;
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

.market-hero {
  position: relative;
  overflow: hidden;
  min-height: 164px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 28px;
  align-items: center;
  margin-bottom: 18px;
  padding: 24px 26px;
  border: 1px solid #dce7f7;
  border-radius: 16px;
  background:
    radial-gradient(circle at 78% 12%, rgba(88, 160, 255, .16), transparent 31%),
    linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%);
  box-shadow: 0 9px 28px rgba(49, 93, 170, .08);
}

.market-hero::after {
  content: "";
  position: absolute;
  right: 252px;
  bottom: -62px;
  width: 168px;
  height: 168px;
  border: 28px solid rgba(75, 139, 242, .07);
  border-radius: 50%;
  pointer-events: none;
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.hero-eyebrow {
  color: var(--blue);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: .08em;
}

.hero-content h1 {
  margin: 0 0 4px;
  color: #17233b;
  font-size: clamp(22px, 2vw, 29px);
  line-height: 1.25;
  letter-spacing: -.02em;
}

.search-box {
  height: 48px;
  width: 100%;
  max-width: 760px;
  border: 1px solid #cfdef3;
  box-shadow: 0 8px 22px rgba(40, 100, 190, .12);
  border-radius: 10px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 13px;
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

.search-box .search-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--blue);
  color: #fff;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.2s;
}

.search-box .search-btn:hover {
  opacity: 0.85;
}

.publish-btn {
  height: 119px;
  width: 180px;
  flex-shrink: 0;
  border: 1.5px solid var(--blue);
  background: #f0f7ff;
  color: var(--blue);
  border-radius: 14px;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.publish-btn:hover {
  background: #e0efff;
}

.publish-btn svg {
  color: var(--blue);
}

.publish-btn span {
  font-size: 15px;
  font-weight: 600;
  color: var(--blue);
}

.publish-btn p {
  margin: 0;
  font-size: 11px;
  color: #6b8ab8;
  text-align: center;
  line-height: 1.4;
  padding: 0 10px;
}

/* 定制智能体（在 publish-btn 蓝框内） */
.custom-cta {
  position: relative;
  z-index: 1;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  padding: 0 20px;
  height: 112px;
  width: 100%;
  min-width: 0;
  background: rgba(255, 255, 255, .64);
  box-shadow: none;
}

.custom-cta-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.custom-cta-text h3 {
  margin: 0;
  font-size: 16px;
  color: var(--blue);
}

.custom-cta-text p {
  margin: 0;
  font-size: 12px;
  color: #6b8ab8;
  text-align: left;
  padding: 0;
}

.custom-cta-text button {
  border: 1px solid #bcd2f7;
  background: #eef5ff;
  color: var(--blue);
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  width: fit-content;
}

.custom-cta-text button:hover {
  background: var(--blue);
  color: #fff;
}

.custom-cube {
  color: #4a9cf5;
  background: #edf6ff;
  width: 90px;
  height: 90px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  transform: rotate(-8deg);
  flex-shrink: 0;
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

.sort-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  min-width: 132px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
  overflow: hidden;
}

.sort-option {
  padding: 8px 14px;
  font-size: 13px;
  color: #475467;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.sort-option:hover {
  background: #f5f8ff;
  color: var(--blue);
}

.sort-option.active {
  color: var(--blue);
  font-weight: 600;
  background: #f0f7ff;
}

.mobile-filters {
  display: none;
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
  overflow: hidden;
}

.agent-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

/* 分页控件 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 24px 0 8px;
}

.page-btn {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d9e4f3;
  border-radius: 6px;
  background: #fff;
  color: #4a5568;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--blue);
  color: var(--blue);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-num {
  width: 32px;
  height: 32px;
  border: 1px solid #d9e4f3;
  border-radius: 6px;
  background: #fff;
  color: #4a5568;
  font-size: 13px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s;
}

.page-num:hover {
  border-color: var(--blue);
  color: var(--blue);
}

.page-num.active {
  background: var(--blue);
  border-color: var(--blue);
  color: #fff;
  font-weight: 600;
}

.page-info {
  margin-left: 10px;
  font-size: 12px;
  color: #9aa6b8;
}

/* ========================================
   底部面板 & 通用
   ======================================== */
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
  min-height: 280px;
}

.insight h2 {
  font-size: 17px;
}

.insight h3,
.trend-card h3,
.latest h3 {
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
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.latest-row:hover {
  background: #f4f6f9;
}

.latest-row:hover strong {
  color: #1768f2;
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
  overflow: hidden;
}

.modal-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
    grid-template-rows: 1fr auto;
  }
  .bottom-panels {
    grid-column: 2 / 3;
  }
  .agent-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1050px) {
  .workspace {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
    height: auto;
  }
  .left-panel {
    display: none;
  }
  .bottom-panels {
    grid-column: 1 / -1;
    grid-template-columns: repeat(3, 1fr);
  }
  .agent-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-content {
    overflow: visible;
  }
  .market-hero {
    grid-template-columns: minmax(0, 1fr) 280px;
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
    padding: 8px;
  }
  .market-hero {
    grid-template-columns: 1fr;
    gap: 12px;
    min-height: auto;
    padding: 16px;
    margin-bottom: 14px;
  }
  .market-hero::after {
    right: -65px;
    bottom: -95px;
  }
  .hero-content {
    gap: 8px;
  }
  .hero-content h1 {
    font-size: 20px;
  }
  .search-box {
    height: 44px;
  }
  .search-box input {
    min-width: 0;
    font-size: 11px;
  }
  .search-box input::placeholder {
    font-size: 11px;
  }
  .market-hero .publish-btn {
    width: 100%;
    height: auto;
    min-height: 82px;
    flex-direction: row;
    padding: 12px 14px 12px 16px;
    border-width: 1px;
    border-radius: 12px;
  }
  .market-hero .custom-cta-text {
    gap: 4px;
  }
  .market-hero .custom-cta-text h3 {
    font-size: 14px;
  }
  .market-hero .custom-cta-text p {
    font-size: 11px;
  }
  .market-hero .custom-cta-text button {
    margin-top: 2px;
    padding: 4px 12px;
  }
  .market-hero .custom-cube {
    width: 54px;
    height: 54px;
    border-radius: 16px;
  }
  .market-hero .custom-cube svg {
    width: 36px;
    height: 36px;
  }
  .agent-grid {
    grid-template-columns: 1fr;
  }
  .tabs {
    gap: 0;
  }
  .toolbar {
    height: 48px;
    width: 100%;
  }
  .tabs {
    width: 100%;
  }
  .tabs button {
    flex: 1;
    min-width: 0;
    padding: 7px 4px;
    font-size: 12px;
    text-align: center;
  }
  .view-controls {
    display: none;
  }
  .mobile-filters {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    min-width: 0;
    margin: 0 0 10px;
    overflow: hidden;
  }
  .mobile-filter-label {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    flex-shrink: 0;
    color: #64748b;
    font-size: 11px;
    font-weight: 600;
  }
  .mobile-filter-scroll {
    flex: 1;
    display: flex;
    gap: 6px;
    width: 0;
    min-width: 0;
    overflow-x: auto;
    padding: 2px 1px 4px;
    scrollbar-width: none;
    overscroll-behavior-x: contain;
  }
  .mobile-filter-scroll::-webkit-scrollbar {
    display: none;
  }
  .mobile-filter-scroll button {
    flex-shrink: 0;
    height: 28px;
    padding: 0 10px;
    border: 1px solid #dce6f3;
    border-radius: 14px;
    background: #fff;
    color: #64748b;
    font-size: 11px;
    white-space: nowrap;
  }
  .mobile-filter-scroll button.active {
    border-color: #b9d3fb;
    background: #edf5ff;
    color: var(--blue);
    font-weight: 600;
  }
  .pagination {
    justify-content: center;
    gap: 4px;
    margin: 12px 0 2px;
    padding: 12px 6px;
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fff;
  }
  .page-btn {
    width: 52px;
    padding: 0 4px;
    justify-content: center;
    white-space: nowrap;
  }
  .page-num {
    width: 30px;
    height: 30px;
  }
  .page-info {
    display: none;
  }
  .bottom-panels {
    grid-template-columns: 1fr;
  }
}
</style>
