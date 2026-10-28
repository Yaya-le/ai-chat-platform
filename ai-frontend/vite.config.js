import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// import path from 'path' // 如果需要 @ 别名可开启

export default defineConfig({
  plugins: [vue()], // 必须配置，否则无法解析 .vue 文件
  resolve: {
    alias: {
      '@': '/src' // 建议配置路径别名，方便后续引入
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 替换为你的后端地址
        changeOrigin: true
        //rewrite: (p) => p.replace(/^\/api/, '')
      }
    }
  }
})