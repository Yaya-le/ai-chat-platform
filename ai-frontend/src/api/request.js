// src/api/request.js — 统一 axios 实例（业务接口层）
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截
request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

// 响应拦截
request.interceptors.response.use(
  (res) => res.data,
  (error) => {
    ElMessage.error(error.response?.data?.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
