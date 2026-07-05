<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '123456',
})

async function handleSubmit() {
  if (!form.username.trim() || !form.password.trim()) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const homePath = await authStore.login(form.username.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : homePath
    await router.push(redirect)
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel page-card">
      <div class="brand">
        <div class="brand-mark">MES</div>
        <div>
          <h1>车间执行系统</h1>
          <p class="muted-text">Manufacturing Execution System</p>
        </div>
      </div>

      <el-form class="login-form" @submit.prevent="handleSubmit">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="admin / planner / worker1"
            size="large"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
            autocomplete="current-password"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-button
          class="submit-btn"
          type="primary"
          size="large"
          :loading="loading"
          native-type="submit"
        >
          登录
        </el-button>
      </el-form>

      <div class="hint muted-text">
        <p>演示账号：admin / planner → 管理端；worker1 → 工人端</p>
        <p>默认密码：123456</p>
      </div>
    </div>

    <aside class="login-aside">
      <h2>生产透明 · 派工可控 · 报工可追溯</h2>
      <ul>
        <li>ERP 推单同步与幂等拦截</li>
        <li>7 态工单状态机自动流转</li>
        <li>Redis 幂等 + MQ 异步报工</li>
      </ul>
    </aside>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(320px, 420px) 1fr;
  gap: 48px;
  align-items: center;
  padding: 48px clamp(24px, 5vw, 80px);
  background:
    radial-gradient(circle at top right, rgb(249 115 22 / 8%), transparent 40%),
    var(--color-background);
}

.login-panel {
  width: 100%;
  box-shadow: 0 10px 30px rgb(15 23 42 / 6%);
}

.brand {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 32px;
}

.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--color-primary);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.brand h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
}

.brand p {
  margin: 4px 0 0;
  font-size: 0.875rem;
}

.login-form {
  display: grid;
  gap: 8px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  background: var(--color-cta);
  border-color: var(--color-cta);
  transition: opacity var(--transition-fast);
}

.submit-btn:hover,
.submit-btn:focus {
  opacity: 0.92;
}

.hint {
  margin-top: 24px;
  font-size: 0.8125rem;
  line-height: 1.6;
}

.hint p {
  margin: 0;
}

.login-aside {
  max-width: 520px;
}

.login-aside h2 {
  margin: 0 0 20px;
  font-size: clamp(1.5rem, 2vw, 2rem);
  line-height: 1.3;
  color: var(--color-text);
}

.login-aside ul {
  margin: 0;
  padding-left: 20px;
  color: var(--color-text-muted);
  line-height: 1.9;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .login-aside {
    display: none;
  }
}
</style>

<style>
.login-form .el-form-item__label {
  color: var(--color-text);
  font-weight: 600;
}

.login-form .el-input__wrapper {
  border-radius: var(--radius-sm);
  transition: box-shadow var(--transition-fast);
}
</style>
