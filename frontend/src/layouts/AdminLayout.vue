<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataBoard,
  Document,
  Monitor,
  SwitchButton,
  UserFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/admin', label: '首页概览', icon: DataBoard },
  { path: '/admin/work-orders', label: '工单管理', icon: Document, disabled: true },
  { path: '/admin/dashboard', label: '生产看板', icon: Monitor, disabled: true },
]

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <el-container class="page-shell admin-layout">
    <el-aside width="240px" class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-badge">MES</span>
        <div>
          <strong>管理端</strong>
          <p class="muted-text">Admin Console</p>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="sidebar-menu">
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
          :disabled="item.disabled"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <el-tag v-if="item.disabled" size="small" type="info" class="soon-tag">D12</el-tag>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h2>管理端工作台</h2>
          <p class="muted-text">工单、派工、看板将在此区域逐步接入</p>
        </div>

        <div class="topbar-actions">
          <el-tag type="info" effect="plain">
            <el-icon><UserFilled /></el-icon>
            {{ authStore.username }}
          </el-tag>
          <el-button class="cursor-pointer" text type="primary" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  background: var(--color-background);
}

.sidebar {
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
  padding: 24px 16px;
}

.sidebar-brand {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 0 8px 24px;
}

.brand-badge {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--color-primary);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.75rem;
  font-weight: 700;
}

.sidebar-brand strong {
  display: block;
}

.sidebar-brand p {
  margin: 2px 0 0;
  font-size: 0.75rem;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.soon-tag {
  margin-left: auto;
}

.topbar {
  height: auto;
  padding: 20px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.topbar h2 {
  margin: 0;
  font-size: 1.25rem;
}

.topbar p {
  margin: 4px 0 0;
  font-size: 0.875rem;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.main-content {
  padding: 24px 28px 32px;
}

@media (max-width: 768px) {
  .admin-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }
}
</style>
