import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeToken } from '@/utils/auth'

const service = axios.create({
  baseURL: '/api', // 代理前缀，生产环境走 nginx 转发
  timeout: 10000
})

// 标记位：避免 401 时重复弹窗 + 防止路由守卫触发无限重定向循环
let isRedirecting = false

// 请求拦截
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截
service.interceptors.response.use(
  (res) => {
    // 兼容：后端可能返回 { code, message, data }，也可能 data 就是业务对象
    const body = res.data
    const code = body && typeof body === 'object' ? body.code : undefined
    const message = body && typeof body === 'object' ? body.message : undefined

    // 统一返回结构：{ code, message, data }
    if (code === 0 || code === 200) {
      return body.data
    }

    // 裸对象（无 code 包裹）：约定 data 字段即为业务数据，直接放行
    // 例如后端直接返回 { token: 'xxx' } 或 { data: {...}, token: 'xxx' }
    if (code === undefined) {
      return body
    }

    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    const status = error.response?.status
    const msg =
      error.response?.data?.message ||
      error.response?.data?.msg ||
      error.message

    if (status === 401) {
      // 只在首次进入时清理 + 跳转，避免路由守卫反复触发形成死循环
      if (!isRedirecting) {
        isRedirecting = true
        ElMessage.error('登录已过期，请重新登录')
        removeToken()

        // 用 location.href 直接刷新页面跳登录，彻底跳出 Vue Router 导航循环；
        // 若坚持用 router.push，路由守卫读 getToken() 为空会反复重定向。
        const loginUrl =
          router.currentRoute.value.path !== '/login' ? '/login' : '/login'
        window.location.href = loginUrl

        // 下一个事件循环重置标记，保证后续新会话能正常响应
        setTimeout(() => {
          isRedirecting = false
        }, 0)
      }
    } else {
      ElMessage.error(msg || '网络异常，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default service
