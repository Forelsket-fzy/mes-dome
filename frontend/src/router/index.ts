import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canAccessAdmin, canAccessTerminal, resolveHomePath } from '@/utils/redirect'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: () => {
        const authStore = useAuthStore()
        return authStore.isAuthenticated
          ? resolveHomePath(authStore.username)
          : '/login'
      },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, portal: 'admin' },
      children: [
        {
          path: '',
          name: 'admin-home',
          component: () => import('@/views/admin/AdminHomeView.vue'),
          meta: { title: '管理端首页' },
        },
      ],
    },
    {
      path: '/terminal',
      component: () => import('@/layouts/TerminalLayout.vue'),
      meta: { requiresAuth: true, portal: 'terminal' },
      children: [
        {
          path: '',
          name: 'terminal-home',
          component: () => import('@/views/terminal/TerminalHomeView.vue'),
          meta: { title: '工人端首页' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const isPublic = Boolean(to.meta.public)

  if (!isPublic && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path === '/login' && authStore.isAuthenticated) {
    return resolveHomePath(authStore.username)
  }

  if (to.meta.portal === 'admin' && !canAccessAdmin(authStore.username)) {
    return '/terminal'
  }

  if (to.meta.portal === 'terminal' && !canAccessTerminal(authStore.username)) {
    return '/admin'
  }

  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · MES-dome` : 'MES-dome 车间执行系统'
  return true
})

export default router
