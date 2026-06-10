<script setup lang="ts">
/**
 * 应用菜单组件
 *
 * @author huxuehao
 */
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import type { Component } from 'vue'
import { bizRoutes } from '@/router/modules'
import { useAccountStore } from '@/stores'
import {
  SafetyCertificateOutlined,
  FileTextOutlined,
  ApiOutlined,
  RobotOutlined,
  ToolOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  DatabaseOutlined,
  LoginOutlined,
  CompassOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

/**
 * 路由路径对应的图标映射
 */
const iconMap: Record<string, Component> = {
  'sensitive': SafetyCertificateOutlined,
  'prompt': FileTextOutlined,
  'model': ApiOutlined,
  'agent': RobotOutlined,
  'explore': CompassOutlined,
  'tool': ToolOutlined,
  'hook': LoginOutlined,
  'skill': AppstoreOutlined,
  'mcp': CloudServerOutlined,
  'knowledge': DatabaseOutlined,
}

/**
 * 从路由配置中提取菜单项
 */
const menuItems = computed(() => {
  const items: Array<{ value: string; label: () => ReturnType<typeof h> }> = []

  // READ_ONLY用户只显示Agent广场
  if (accountStore.isReadOnly) {
    const exploreRoute = bizRoutes[0]?.children?.find(
      (r: RouteRecordRaw) => r.path === 'explore'
    )
    if (exploreRoute?.meta?.title) {
      items.push({
        value: 'explore',
        label: () =>
          h('span', { class: 'menu-item-label' }, [
            h(CompassOutlined, { class: 'menu-item-icon' }),
            h('span', exploreRoute.meta?.title as string),
          ]),
      })
    }
    return items
  }

  bizRoutes[0]?.children?.forEach((routeItem: RouteRecordRaw) => {
    if (routeItem.meta?.hidden !== true && routeItem.meta?.title) {
      const IconComponent = iconMap[routeItem.path]
      const title = routeItem.meta?.title as string
      items.push({
        value: routeItem.path,
        label: () =>
          h('span', { class: 'menu-item-label' }, [
            IconComponent ? h(IconComponent, { class: 'menu-item-icon' }) : null,
            h('span', title),
          ]),
      })
    }
  })

  return items
})

/**
 * 当前激活的菜单项
 */
const selectedValue = computed({
  get: () => route.path.split('/')[1],
  set: (value: string) => {
    router.push('/' + value)
  },
})
</script>

<template>
  <div class="app-menu flex-1 flex justify-center">
    <ASegmented v-model:value="selectedValue" :options="menuItems" />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/components/menu' as *;
</style>
