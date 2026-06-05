<script setup lang="ts">
import { ref, computed } from 'vue'

const isOpen = ref(false)

const iframeSrc = computed(() => {
  return '/chat.html'
})

function toggleChat() {
  isOpen.value = !isOpen.value
}

function closeChat() {
  isOpen.value = false
}
</script>

<template>
  <div class="chat-float-container">
    <!-- 悬浮按钮 -->
    <button
      v-if="!isOpen"
      class="chat-float-btn"
      @click="toggleChat"
      title="打开对话"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
    </button>

    <!-- 对话窗口 -->
    <div v-if="isOpen" class="chat-window">
      <!-- 关闭按钮 - 悬浮在窗口右上角 -->
      <button
        class="close-float-btn"
        @click="closeChat"
        title="关闭"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M18 6L6 18M6 6l12 12"/>
        </svg>
      </button>

      <!-- 窗口内容 -->
      <div class="chat-window-content">
        <iframe
          :src="iframeSrc"
          class="chat-iframe"
          frameborder="0"
          allow="microphone"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-float-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

.chat-float-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #2f6df6;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(47, 109, 246, 0.4);
  transition: all 0.3s ease;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 24px rgba(47, 109, 246, 0.5);
  }

  &:active {
    transform: scale(0.95);
  }

  svg {
    width: 24px;
    height: 24px;
  }
}

.chat-window {
  width: 380px;
  height: 600px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: visible;
  animation: slideUp 0.3s ease;
  border: 1px solid #e5e7eb;
  position: relative;

  @media (max-width: 480px) {
    width: calc(100vw - 32px);
    height: calc(100vh - 100px);
    position: fixed;
    bottom: 80px;
    right: 16px;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.close-float-btn {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #ff4757;
  color: white;
  border: 3px solid white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(255, 71, 87, 0.4);
  z-index: 10;

  &:hover {
    transform: scale(1.1);
    background: #ff6b81;
    box-shadow: 0 4px 12px rgba(255, 71, 87, 0.5);
  }

  &:active {
    transform: scale(0.95);
  }

  svg {
    width: 14px;
    height: 14px;
  }
}

.chat-window-content {
  flex: 1;
  overflow: hidden;
  border-radius: 12px;
}

.chat-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
