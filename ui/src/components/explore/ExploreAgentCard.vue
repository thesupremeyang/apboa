<script setup lang="ts">
import { computed, ref } from 'vue'

interface ExploreAgent {
  id: string
  name: string
  description: string
  agentType: 'CUSTOM' | 'A2A'
  tag?: string
  toolCount?: number
  skillCount?: number
  knowledgeCount?: number
  mcpCount?: number
  trialCount?: number
  avatar?: string
  updatedAt?: string
}

const props = defineProps<{ data: ExploreAgent }>()

const emit = defineEmits<{ try: [id: string] }>()

const avatarError = ref(false)

const formattedTime = computed(() => {
  if (!props.data.updatedAt) return ''
  const date = new Date(props.data.updatedAt)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / 86400000)
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
})

function handleTry() {
  emit('try', props.data.id)
}

const tagColors: Record<string, { bg: string; color: string; gradient: string }> = {
  doc: { bg: 'rgba(91, 107, 127, 0.1)', color: '#5B6B7F', gradient: 'linear-gradient(135deg, #5B6B7F, #748FFC)' },
  multimodal: { bg: 'rgba(139, 107, 175, 0.1)', color: '#8B6BAF', gradient: 'linear-gradient(135deg, #8B6BAF, #B794F6)' },
  data: { bg: 'rgba(74, 139, 127, 0.1)', color: '#4A8B7F', gradient: 'linear-gradient(135deg, #4A8B7F, #38D9A9)' },
  biz: { bg: 'rgba(139, 115, 85, 0.1)', color: '#8B7355', gradient: 'linear-gradient(135deg, #8B7355, #FFB347)' },
  service: { bg: 'rgba(91, 139, 107, 0.1)', color: '#5B8B6B', gradient: 'linear-gradient(135deg, #5B8B6B, #07CA6B)' },
  industry: { bg: 'rgba(107, 107, 139, 0.1)', color: '#6B6B8B', gradient: 'linear-gradient(135deg, #6B6B8B, #6C63FF)' },
}

const getTagStyle = computed(() => {
  const t = props.data.tag
  if (!t) return {}
  const config = tagColors[t.toLowerCase()] || { bg: 'rgba(91, 107, 127, 0.1)', color: '#5B6B7F', gradient: 'linear-gradient(135deg, #5B6B7F, #748FFC)' }
  return { background: config.bg, color: config.color }
})

const getAvatarGradient = computed(() => {
  if (props.data.agentType === 'A2A') {
    return 'linear-gradient(135deg, #1856FF 0%, #6C63FF 100%)'
  }
  const t = props.data.tag?.toLowerCase()
  const config = tagColors[t || '']
  return config?.gradient || 'linear-gradient(135deg, #5B6B7F 0%, #748FFC 100%)'
})

const capItems = computed(() => {
  const items = []
  if (props.data.toolCount) items.push({ icon: 'tool', count: props.data.toolCount, label: '工具' })
  if (props.data.skillCount) items.push({ icon: 'skill', count: props.data.skillCount, label: '技能' })
  if (props.data.knowledgeCount) items.push({ icon: 'knowledge', count: props.data.knowledgeCount, label: '知识库' })
  if (props.data.mcpCount) items.push({ icon: 'mcp', count: props.data.mcpCount, label: 'MCP' })
  return items
})
</script>

<template>
  <div class="agent-card glass-card" @click="handleTry">
    <!-- Card Glow Effect -->
    <div class="card-glow" :style="{ background: getAvatarGradient }"></div>

    <div class="card-header">
      <div class="card-avatar" :style="{ background: getAvatarGradient }">
        <img v-if="data.avatar && !avatarError" :src="data.avatar" alt="avatar" class="avatar-img" @error="avatarError = true" />
        <svg v-else-if="data.agentType === 'A2A'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 12H4M12 4v16M8 8l4-4 4 4M8 16l4 4 4-4"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="5" r="2"/><path d="M12 7v4"/>
        </svg>
      </div>
      <div class="card-info">
        <div class="card-name">{{ data.name }}</div>
        <div class="card-meta">
          <span v-if="data.tag" :style="getTagStyle" class="meta-tag">{{ data.tag }}</span>
          <span class="meta-type">{{ data.agentType === 'CUSTOM' ? '自定义' : 'A2A' }}</span>
        </div>
      </div>
    </div>

    <p class="card-desc">{{ data.description || '暂无描述' }}</p>

    <div v-if="capItems.length > 0" class="card-caps">
      <span v-for="cap in capItems" :key="cap.icon" class="cap">
        <svg v-if="cap.icon === 'tool'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>
        <svg v-if="cap.icon === 'skill'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 9h6M9 13h6M9 17h4"/></svg>
        <svg v-if="cap.icon === 'knowledge'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        <svg v-if="cap.icon === 'mcp'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="8" height="8" rx="1"/><rect x="14" y="2" width="8" height="8" rx="1"/><rect x="2" y="14" width="8" height="8" rx="1"/><rect x="14" y="14" width="8" height="8" rx="1"/></svg>
        {{ cap.count }}
      </span>
    </div>

    <div class="card-footer">
      <div class="footer-left">
        <span v-if="data.trialCount" class="card-trial">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          {{ data.trialCount }}
        </span>
        <span v-if="formattedTime" class="card-time">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          {{ formattedTime }}
        </span>
      </div>
      <button class="try-btn" @click.stop="handleTry">
        立即体验
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.agent-card {
  position: relative;
  border-radius: 20px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.23, 1, 0.32, 1);
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 20px 40px rgba(24, 86, 255, 0.15);

    .card-glow {
      opacity: 0.08;
    }

    .try-btn {
      background: linear-gradient(135deg, #1856FF 0%, #6C63FF 100%);
      color: #fff;
      border-color: transparent;
      box-shadow: 0 4px 16px rgba(24, 86, 255, 0.3);
    }
  }

  .card-glow {
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    opacity: 0;
    transition: opacity 0.4s ease;
    filter: blur(60px);
    z-index: -1;
    pointer-events: none;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.card-avatar {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

  svg { width: 28px; height: 28px; color: white; }

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-name {
  font-size: 16px;
  font-weight: 700;
  color: #141414;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
  font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
}

.card-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;

  .meta-tag {
    padding: 3px 10px;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.02em;
  }

  .meta-type {
    padding: 3px 10px;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 600;
    background: rgba(91, 107, 127, 0.1);
    color: #5B6B7F;
  }
}

.card-desc {
  font-size: 13px;
  color: #5B6B7F;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  margin: 0;
  min-height: 42px;
}

.card-caps {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .cap {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
    background: rgba(24, 86, 255, 0.06);
    color: #1856FF;
    transition: all 0.2s ease;

    svg { width: 14px; height: 14px; stroke: #1856FF; }

    &:hover {
      background: rgba(24, 86, 255, 0.12);
    }
  }
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);

  .footer-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .card-trial,
  .card-time {
    font-size: 12px;
    color: #5B6B7F;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-weight: 500;

    svg { width: 14px; height: 14px; opacity: 0.6; }
  }

  .try-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 18px;
    border-radius: 10px;
    border: 1px solid rgba(24, 86, 255, 0.2);
    background: rgba(24, 86, 255, 0.06);
    color: #1856FF;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.23, 1, 0.32, 1);
    font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;

    svg { width: 16px; height: 16px; }

    &:hover {
      background: linear-gradient(135deg, #1856FF 0%, #6C63FF 100%);
      color: #fff;
      border-color: transparent;
      box-shadow: 0 4px 16px rgba(24, 86, 255, 0.3);
      transform: translateX(2px);
    }
  }
}
</style>
