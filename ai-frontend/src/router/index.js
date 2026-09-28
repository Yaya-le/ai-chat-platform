import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import Register from '@/views/Register.vue'

const routes = [
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { public: true }  // ✅ 加了这行
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    // 聊天页：已登录的主工作区
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue'),
    meta: {
      requiresAuth: true,
      title: 'Nexus AI 对话'
    }
  },
  // 临时注释掉 Dashboard，避免 Vite 扫描报错
  // {
  //   path: '/dashboard',
  //   name: 'Dashboard',
  //   component: () => import('@/views/dashboard/index.vue'),
  //   meta: { requiresAuth: true }
  // },
  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// router.beforeEach((to, from, next) => {
//   const token = getToken()

//   // 公开页面直接放行
//   if (to.meta.public) {
//     return next()
//   }

//   // 需要登录但未登录 → 跳登录
//   if (!token) {
//     return next('/login')
//   }

//   next()
// })

export default router