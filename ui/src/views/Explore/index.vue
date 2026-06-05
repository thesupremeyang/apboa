<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import * as agentApi from '@/api/agent'
import ExploreAgentCard from '@/components/explore/ExploreAgentCard.vue'
import ChatFloat from '@/components/chat/ChatFloat.vue'

const list = ref<any[]>([])
const loading = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)

const keyword = ref('')
const selectedTag = ref<string | null>(null)
const tags = ref<string[]>([])

const total = ref(0)
const stats = computed(() => [
  { label: '智能体', value: total.value, suffix: '+' },
  { label: '分类', value: tags.value.length, suffix: '+' },
  { label: '即开即用', value: '100', suffix: '%' },
])

const features = [
  {
    icon: '🚀',
    title: '开箱即用',
    desc: '所有智能体创建完成后即可使用，无需复杂配置，快速集成到您的业务场景中。',
  },
  {
    icon: '🎯',
    title: '场景丰富',
    desc: '覆盖文档智能、数据分析、客服交互等多个领域，满足不同行业的业务需求。',
  },
  {
    icon: '🛡️',
    title: '技术领先',
    desc: '基于最新的 RAG、知识图谱、大模型等技术栈，保持技术领先优势。',
  },
  {
    icon: '💡',
    title: '灵活定制',
    desc: '支持工具、技能、知识库等多种能力扩展，根据需求定制专属智能体。',
  },
  {
    icon: '🔄',
    title: '持续更新',
    desc: '定期引入新技术和能力，持续优化智能体性能，保持方案竞争力。',
  },
  {
    icon: '🤝',
    title: '专业支持',
    desc: '提供全面的技术文档和示例，帮助您快速上手并充分发挥智能体能力。',
  },
]

async function loadAgents(page: number = 1, append: boolean = false) {
  if (loading.value) return
  loading.value = true
  try {
    const params: any = { page, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (selectedTag.value) params.tag = selectedTag.value
    const response = await agentApi.page(params)
    const data = response.data.data
    if (append) {
      list.value = [...list.value, ...data.records]
    } else {
      list.value = data.records || []
    }
    total.value = data.total || 0
    hasMore.value = data.current < data.pages
    currentPage.value = page
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    const response = await agentApi.listTags()
    tags.value = response.data.data || []
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

function handleSearch() {
  loadAgents(1)
}

function handleTagChange(tag: string | null) {
  selectedTag.value = tag
  loadAgents(1)
}

function handleLoadMore() {
  if (hasMore.value && !loading.value) {
    loadAgents(currentPage.value + 1, true)
  }
}

function handleTryAgent(id: string) {
  const hash = `#/chat/${encodeURIComponent(id)}`
  const url = `${window.location.origin}${window.location.pathname}${hash}`
  window.open(url, '_blank')
}

let observer: IntersectionObserver | null = null

function initScrollObserver() {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible')
        observer?.unobserve(entry.target)
      }
    })
  }, { threshold: 0.1 })

  requestAnimationFrame(() => {
    document.querySelectorAll('.fade-in-section').forEach(el => {
      observer?.observe(el)
    })
  })
}

onMounted(() => {
  loadTags()
  loadAgents(1)
  initScrollObserver()
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<template>
  <div class="explore-page">

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-inner">
        <div class="hero-text">
          <h1 class="hero-title">智能体广场</h1>
          <p class="hero-desc">
            发现并体验各种智能体，选择适合您需求的 AI 智能体助手
          </p>
          <div class="hero-stats">
            <div v-for="stat in stats" :key="stat.label" class="stat-item">
              <span class="stat-value">{{ stat.value }}<span class="stat-suffix">{{ stat.suffix }}</span></span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
          </div>
        </div>
        <div class="hero-visual">
          <div class="visual-card">
            <div class="visual-grid">
              <div class="visual-cell">📄</div>
              <div class="visual-cell">🎨</div>
              <div class="visual-cell">📊</div>
              <div class="visual-cell">🎙️</div>
              <div class="visual-cell">🤖</div>
              <div class="visual-cell">🧠</div>
            </div>
          </div>
          <div class="floating-badge badge-1">
            <div class="badge-icon" style="background: #e8f5e9">
              <svg viewBox="0 0 24 24" fill="none" stroke="#4CAF50" stroke-width="2.5">
                <path d="M5 13l4 4L19 7"/>
              </svg>
            </div>
            <span class="badge-text">即刻使用</span>
          </div>
          <div class="floating-badge badge-2">
            <div class="badge-icon" style="background: #e3f2fd">
              <svg viewBox="0 0 24 24" fill="none" stroke="#2196F3" stroke-width="2.5">
                <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
              </svg>
            </div>
            <span class="badge-text">开箱即用</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Agents Section -->
    <section class="main-section">
      <div class="toolbar fade-in-section">
        <div class="filter-group">
          <button
            :class="['filter-btn', { active: selectedTag === null }]"
            @click="handleTagChange(null)"
          >
            全部
          </button>
          <button
            v-for="tag in tags"
            :key="tag"
            :class="['filter-btn', { active: selectedTag === tag }]"
            @click="handleTagChange(tag)"
          >
            {{ tag }}
          </button>
        </div>
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
          </svg>
          <input
            v-model="keyword"
            placeholder="搜索智能体..."
            @keyup.enter="handleSearch"
          />
        </div>
      </div>

      <div class="content-area">
        <div v-if="loading && list.length === 0" class="loading-state">
          <div class="loader"></div>
          <p>正在加载智能体...</p>
        </div>

        <div v-else-if="!loading && list.length === 0" class="empty-state">
          <div class="empty-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 9h6M9 13h6M9 17h4"/>
            </svg>
          </div>
          <h3>暂无智能体</h3>
          <p>请联系管理员创建智能体</p>
        </div>

        <div v-else class="card-grid">
          <ExploreAgentCard
            v-for="item in list"
            :key="item.id"
            :data="{
              id: item.id,
              name: item.name,
              description: item.description,
              agentType: item.agentType || 'CUSTOM',
              tag: item.tag,
              toolCount: item.tool?.length || 0,
              skillCount: item.skill?.length || 0,
              knowledgeCount: item.knowledgeBase?.length || 0,
              mcpCount: item.mcp?.length || 0,
              trialCount: Math.floor(Math.random() * 901) + 100,
              avatar: item.avatar && item.avatar.startsWith('http') ? item.avatar : `/avatars/${item.name}.png`,
            }"
            @try="handleTryAgent"
          />
        </div>

        <div v-if="hasMore && list.length > 0" class="load-more">
          <button :class="['load-more-btn', { loading }]" @click="handleLoadMore">
            <span v-if="loading" class="btn-loader"></span>
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>

        <div v-if="!hasMore && list.length > 0" class="end-line">
          <span class="end-line-icon">✓</span>
          已展示全部 {{ total }} 个智能体
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="features-section">
      <div class="features-header fade-in-section">
        <span class="section-badge">核心优势</span>
        <h2 class="section-title">为什么选择我们</h2>
        <p class="section-desc">专业、高效、可靠的 AI 智能体解决方案</p>
      </div>
      <div class="features-grid">
        <div
          v-for="(f, i) in features"
          :key="f.title"
          :id="'feature-' + i"
          class="feature-card fade-in-section"
          :class="'delay-' + (i * 100)"
        >
          <div class="feature-icon">
            <span class="feature-emoji">{{ f.icon }}</span>
          </div>
          <h3 class="feature-title">{{ f.title }}</h3>
          <p class="feature-desc">{{ f.desc }}</p>
        </div>
      </div>
    </section>
  </div>

  <!-- 悬浮对话窗口 -->
  <ChatFloat />
</template>

<style scoped lang="scss">
.explore-page {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  background: #f8f9fb;
}

/* ============= Hero Section ============= */
.hero-section {
  position: relative;
  z-index: 1;
  padding: 48px 24px 56px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;

  @media (max-width: 968px) {
    grid-template-columns: 1fr;
    gap: 40px;
  }
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.2;
  color: #1a1a2e;
  margin-bottom: 12px;

  @media (max-width: 768px) {
    font-size: 30px;
  }
}

.hero-desc {
  font-size: 16px;
  color: #666;
  max-width: 480px;
  line-height: 1.8;
  margin-bottom: 28px;
}

.hero-stats {
  display: flex;
  gap: 40px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;

  .stat-item {
    text-align: center;

    .stat-value {
      display: block;
      font-size: 30px;
      font-weight: 800;
      color: #1a1a2e;
      line-height: 1;
      font-variant-numeric: tabular-nums;
    }

    .stat-suffix {
      font-size: 18px;
      color: #666;
      margin-left: 2px;
    }

    .stat-label {
      font-size: 13px;
      color: #999;
      margin-top: 4px;
      display: block;
    }
  }
}

/* Hero Visual */
.hero-visual {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;

  @media (max-width: 968px) {
    display: none;
  }
}

.visual-card {
  position: relative;
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  transform: rotate(2deg);
  transition: transform 0.5s ease;
  border: 1px solid #f0f0f0;

  &:hover {
    transform: rotate(0deg);
  }
}

.visual-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;

  .visual-cell {
    width: 72px;
    height: 72px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    background: #f5f5f5;
    transition: transform 0.3s;

    &:hover {
      transform: scale(1.08);
    }
  }
}

.floating-badge {
  position: absolute;
  background: #fff;
  border-radius: 10px;
  padding: 8px 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 8px;
  animation: float 6s ease-in-out infinite;
  border: 1px solid #f0f0f0;

  .badge-icon {
    width: 28px; height: 28px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;

    svg { width: 16px; height: 16px; }
  }

  .badge-text {
    font-size: 12px;
    font-weight: 600;
    color: #333;
    white-space: nowrap;
  }

  &.badge-1 {
    top: -8px;
    right: -16px;
    animation-delay: 0.2s;
  }

  &.badge-2 {
    bottom: -8px;
    left: -16px;
    animation-delay: 0.6s;
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
}

/* ============= Main Section ============= */
.main-section {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;

  .filter-group {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    .filter-btn {
      padding: 7px 20px;
      border-radius: 6px;
      border: 1px solid #e0e0e0;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
      background: #fff;
      color: #666;

      &:hover {
        border-color: #bbb;
        color: #333;
      }

      &.active {
        background: #333;
        color: #fff;
        border-color: #333;
      }
    }
  }

  .search-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 7px 16px;
    border-radius: 6px;
    background: #fff;
    border: 1px solid #e0e0e0;
    transition: all 0.2s;
    min-width: 220px;

    &:focus-within {
      border-color: #999;
      box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.04);
    }

    .search-icon {
      width: 16px; height: 16px; color: #999; flex-shrink: 0;
    }

    input {
      border: none; outline: none; background: transparent;
      font-size: 13px; color: #333; width: 100%;
      &::placeholder { color: #bbb; }
    }
  }
}

.content-area {
  .card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;

    @media (max-width: 768px) { grid-template-columns: 1fr; }
    @media (min-width: 769px) and (max-width: 1024px) { grid-template-columns: repeat(2, 1fr); }
    @media (min-width: 1400px) { grid-template-columns: repeat(4, 1fr); }
  }

  .loading-state {
    text-align: center; padding: 100px 20px;

    .loader {
      width: 36px; height: 36px;
      border: 3px solid #eee;
      border-top-color: #999;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 16px;
    }

    p { color: #999; font-size: 14px; }
  }

  .empty-state {
    text-align: center; padding: 100px 20px;

    .empty-icon-wrap {
      width: 64px; height: 64px;
      margin: 0 auto 16px;
      border-radius: 16px;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;

      svg { width: 32px; height: 32px; color: #ccc; }
    }

    h3 { font-size: 16px; color: #666; margin-bottom: 6px; }
    p { font-size: 13px; color: #999; }
  }
}

.load-more {
  text-align: center; padding: 32px 20px;

  .load-more-btn {
    padding: 10px 40px;
    border-radius: 6px;
    border: 1px solid #ddd;
    background: #fff;
    color: #555;
    font-size: 13px; font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    display: inline-flex; align-items: center; gap: 8px;

    &:hover {
      border-color: #999;
      color: #333;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }

    &.loading { opacity: 0.6; cursor: not-allowed; }

    .btn-loader {
      width: 14px; height: 14px;
      border: 2px solid #eee;
      border-top-color: #999;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
  }
}

.end-line {
  text-align: center;
  padding: 32px 20px;
  color: #bbb;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  .end-line-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px; height: 18px;
    border-radius: 50%;
    background: #f0f0f0;
    color: #999;
    font-size: 10px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ============= Features Section ============= */
.features-section {
  position: relative;
  z-index: 1;
  padding: 28px 24px 80px;
  max-width: 1200px;
  margin: 0 auto;
}

.features-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 14px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 14px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 10px;
  line-height: 1.3;

  @media (max-width: 768px) {
    font-size: 22px;
  }
}

.section-desc {
  font-size: 14px;
  color: #999;
  max-width: 500px;
  margin: 0 auto;
  line-height: 1.7;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
  @media (min-width: 769px) and (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.feature-card {
  position: relative;
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  border: 1px solid #eee;
  transition: all 0.3s ease;

  &:hover {
    border-color: #ddd;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  }
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  background: #f5f5f5;

  .feature-emoji {
    font-size: 22px;
    line-height: 1;
  }
}

.feature-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 13px;
  color: #888;
  line-height: 1.7;
  margin: 0;
}

/* ============= Animations ============= */
.fade-in-section {
  opacity: 0;
  transform: translateY(16px);
  transition: opacity 0.5s ease-out, transform 0.5s ease-out;

  &.visible {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
