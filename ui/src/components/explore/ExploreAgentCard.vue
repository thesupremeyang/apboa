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

const tagColors: Record<string, string> = {
  doc: '#5B6B7F',
  multimodal: '#8B6BAF',
  data: '#4A8B7F',
  biz: '#8B7355',
  service: '#5B8B6B',
  industry: '#6B6B8B',
}

const getTagStyle = computed(() => {
  const t = props.data.tag
  if (!t) return {}
  const color = tagColors[t.toLowerCase()] || '#888'
  return { background: `${color}10`, color }
})
</script>

<template>
  <div class="agent-card" @click="handleTry">
    <div class="card-header">
      <div class="card-avatar" :class="data.agentType === 'A2A' ? 'avatar-a2a' : 'avatar-custom'">
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

    <div v-if="data.toolCount || data.skillCount || data.knowledgeCount || data.mcpCount" class="card-caps">
      <span v-if="data.toolCount" class="cap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>
        {{ data.toolCount }}
      </span>
      <span v-if="data.skillCount" class="cap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 9h6M9 13h6M9 17h4"/></svg>
        {{ data.skillCount }}
      </span>
      <span v-if="data.knowledgeCount" class="cap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        {{ data.knowledgeCount }}
      </span>
      <span v-if="data.mcpCount" class="cap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="8" height="8" rx="1"/><rect x="14" y="2" width="8" height="8" rx="1"/><rect x="2" y="14" width="8" height="8" rx="1"/><rect x="14" y="14" width="8" height="8" rx="1"/></svg>
        {{ data.mcpCount }}
      </span>
    </div>

    <div class="card-footer">
      <span v-if="data.trialCount" class="card-trial">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
        {{ data.trialCount }}
      </span>
      <span v-if="formattedTime" class="card-time">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
        {{ formattedTime }}
      </span>
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
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid #eee;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &:hover {
    border-color: #ddd;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-avatar {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;

  svg { width: 26px; height: 26px; }

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &.avatar-custom {
    background: #f0f0f0;
    color: #888;
  }
  &.avatar-a2a {
    background: #e8f0fe;
    color: #5B7BAF;
  }
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 3px;
}

.card-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;

  .meta-tag {
    padding: 1px 8px;
    border-radius: 3px;
    font-size: 11px;
    font-weight: 500;
  }

  .meta-type {
    padding: 1px 8px;
    border-radius: 3px;
    font-size: 11px;
    font-weight: 500;
    background: #f5f5f5;
    color: #999;
  }
}

.card-desc {
  font-size: 12px;
  color: #888;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  margin: 0;
}

.card-caps {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  .cap {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 3px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 500;
    background: #f5f5f5;
    color: #888;

    svg { width: 12px; height: 12px; stroke: #aaa; }
  }
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;

  .card-trial,
  .card-time {
    font-size: 11px;
    color: #bbb;
    display: inline-flex;
    align-items: center;
    gap: 3px;

    svg { width: 12px; height: 12px; }
  }

  .try-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 5px 14px;
    border-radius: 5px;
    border: 1px solid #e0e0e0;
    background: #fff;
    color: #555;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    margin-left: auto;

    svg { width: 14px; height: 14px; }

    &:hover {
      background: #333;
      color: #fff;
      border-color: #333;
    }
  }
}
</style>
