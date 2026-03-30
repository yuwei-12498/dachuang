import { createRouter, createWebHistory } from 'vue-router'
import { initAuthState, useAuthState } from '@/store/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页 - 开始规划' }
  },
  {
    path: '/result',
    name: 'Result',
    component: () => import('@/views/Result.vue'),
    meta: { title: '您的专属行程', requiresAuth: true }
  },
  {
    path: '/auth',
    name: 'Auth',
    component: () => import('@/views/Auth.vue'),
    meta: { title: '登录与注册', hideGlobalChat: true }
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    component: () => import('@/views/Detail.vue'),
    meta: { title: '点位详情与调整', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 行“城”有数`
  }

  if (to.meta.requiresAuth) {
    const authState = useAuthState()
    if (!authState.initialized) {
      await initAuthState()
    }
    if (!authState.user) {
      next({
        path: '/auth',
        query: {
          redirect: to.fullPath
        }
      })
      return
    }
  }

  next()
})

export default router
