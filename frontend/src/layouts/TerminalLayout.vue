<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { List, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/terminal', label: '今日任务', icon: List },
  { path: '/terminal/report', label: '扫码报工', icon: List, disabled: true },
]

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <el-container class="page-shell terminal-layout">
    <el-header class="terminal-header">
      <div class="header-brand">
        <span class="brand-badge">MES</span>
        <div>
          <strong>工人端</strong>
          <p class="muted-text">Shop Floor Terminal</p>
        </div>
      </div>

      <div class="header-actions">
        <el-tag type="warning" effect="plain">
          <el-icon><UserFilled /></el-icon>
          {{ authStore.username }}
        </el-tag>
        <el-button class="cursor-pointer" text type="primary" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside width="220px" class="terminal-sidebar">
        <el-menu :default-active="activeMenu" router class="terminal-menu">
          <el-menu-item
            v-for="item in menuItems"
            :key="item.path"
            :index="item.path"
            :disabled="item.disabled"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
            <el-tag v-if="item.disabled" size="small" type="info" class="soon-tag">D19</el-tag>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="terminal-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.terminal-layout {
  background: var(--color-background);
}

.terminal-header {
  height: auto;
  padding: 18px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #1e293b;
  color: #f8fafc;
}

.header-brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.brand-badge {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--color-cta);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.75rem;
  font-weight: 700;
}

.header-brand p {
  margin: 2px 0 0;
  color: #cbd5e1;
  font-size: 0.75rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.terminal-sidebar {
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  padding-top: 12px;
}

.terminal-menu {
  border-right: none;
}

.soon-tag {
  margin-left: auto;
}

.terminal-main {
  padding: 24px;
}

@media (max-width: 768px) {
  .terminal-sidebar {
    display: none;
  }
}
</style>
