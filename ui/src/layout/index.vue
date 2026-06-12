<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
/**
 * 主布局组件
 *
 * @author huxuehao
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';
const route = useRoute();
import { useAccountStore } from "@/stores";
import { AppLogo, AppMenu, UserSection } from '@/components/layout'
const accountStore = useAccountStore()
const { getRefresh } = accountStore

let isRefresh = computed(() => {
  return getRefresh()
})

/** 路由 meta.hideLogo 为 true 时隐藏Logo */
const showLogo = computed(() => !route.meta.hideLogo)

/** 是否显示导航栏（仅管理员显示） */
const showHeader = computed(() => accountStore.isAdmin)
</script>

<template>
  <div :class="['app-layout', 'flex', 'flex-col', { 'no-header': !showHeader }]">
    <header v-if="showHeader" :class="['layout-header', 'flex', 'items-center', { 'no-logo': !showLogo }]">
      <AppLogo v-if="showLogo" />
      <AppMenu />
      <UserSection />
    </header>

    <main class="layout-content flex-1">
      <router-view
        v-slot="{ Component }"
        :key="route.path + isRefresh"
      >
        <transition name="slide-right" mode="out-in" appear>
          <div style="height: 100%; position: relative;">
            <component :is="Component" />
          </div>
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/layout' as *;

.app-layout.no-header .layout-content {
  margin-top: 0;
  min-height: 100vh;
}
</style>
