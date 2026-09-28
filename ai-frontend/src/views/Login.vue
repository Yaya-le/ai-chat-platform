<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { User, Lock, View, Hide, ChatDotRound, MagicStick, Cpu } from '@element-plus/icons-vue'
import { setToken } from '@/utils/auth'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const passwordVisible = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true

    try {
      // request.js 的 baseURL 已是 '/api'，这里写相对路径即可
      // 实际请求：POST /api/auth/login → 由 vite proxy / nginx 转发到后端
      const data = await request.post('/auth/login', {
        username: form.username,
        password: form.password
      })
      // if (!data.token) {
      //   console.error('[login] 未取到 token，后端返回：', data)
      //   throw new Error('登录成功，但接口未返回 token')
      // }

      //setToken(token) // 写入 localStorage['ADMIN_TOKEN']
      if (data === "success"){
        ElMessage.success('登录成功')
        router.push('/chat')
      }        
      else
        ElMessage.error('登录失败')
      // 回跳来源页，无来源则进首页
      //const redirect = router.currentRoute.value.query.redirect
      
    } catch (e) {
      // 业务错误 / 网络错误：request.js 拦截器已统一 ElMessage 弹窗，
      // 这里仅打日志，避免重复提示
      console.warn('[login]', e?.message || e)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="ai-login">
    <!-- 背景粒子/光效层 -->
    <div class="bg-grid" />
    <div class="glow glow-a" />
    <div class="glow glow-b" />
    <div class="glow glow-c" />

    <!-- 左侧：产品感知 -->
    <div class="hero">
      <div class="hero-inner">
        <!-- Logo -->
        <div class="hero-logo">
          <div class="logo-mark">
            <el-icon :size="28">
              <MagicStick />
            </el-icon>
          </div>
          <span class="logo-name">Nexus AI</span>
        </div>

        <h1 class="hero-title">
          与智能对话<br />让答案触手可及
        </h1>
        <p class="hero-sub">
          基于大语言模型的智能问答平台 · 知识检索 · 多轮对话 · 私有化部署
        </p>

        <!-- 浮动对话卡片 -->
        <div class="chat-preview">
          <div class="bubble bubble-user">
            <el-icon>
              <User />
            </el-icon>
            <span>帮我总结这份 50 页的 PDF 报告</span>
          </div>
          <div class="bubble bubble-ai">
            <el-icon>
              <Cpu />
            </el-icon>
            <span>已阅读全部内容，核心结论有三点：①市场增速 23%…</span>
          </div>
          <div class="bubble bubble-user">
            <el-icon>
              <ChatDotRound />
            </el-icon>
            <span>展开第二点，结合 Q3 数据再分析</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：登录卡片 -->
    <div class="login-side">
      <div class="glass-card" @keyup.enter="handleLogin">
        <div class="card-head">
          <h2>欢迎回来</h2>
          <p>登录后继续与 AI 对话</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号 / 邮箱" :prefix-icon="User" clearable />
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="form.password" :type="passwordVisible ? 'text' : 'password'" placeholder="密码"
              :prefix-icon="Lock">
              <template #suffix>
                <el-icon class="pointer" @click="passwordVisible = !passwordVisible">
                  <View v-if="passwordVisible" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="row-between">
            <el-checkbox>保持登录</el-checkbox>
            <a class="link" href="javascript:;">忘记密码？</a>
          </div>

          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            {{ loading ? '鉴权中…' : '开始对话' }}
          </el-button>

          <div class="divider">
            <span>或</span>
          </div>

          <div class="oauth-row">
            <button class="oauth-btn" type="button" @click="ElMessage.info('SSO 登录待接入')">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path fill="currentColor"
                  d="M21.35 11.1H12v2.8h5.35c-.23 1.4-1.68 4.1-5.35 4.1-3.22 0-5.85-2.66-5.85-5.95S8.78 6.1 12 6.1c1.85 0 3.1.79 3.81 1.47l2.6-2.5C17.05 3.79 14.7 2.5 12 2.5 6.76 2.5 2.5 6.76 2.5 12S6.76 21.5 12 21.5c6.17 0 10.27-4.34 10.27-10.44 0-.7-.07-1.23-.17-1.96z" />
              </svg>
              SSO
            </button>
            <button class="oauth-btn" type="button" @click="ElMessage.info('GitHub 登录待接入')">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path fill="currentColor"
                  d="M12 .5C5.37.5 0 5.78 0 12.292c0 5.21 3.438 9.63 8.205 11.188.6.11.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.604-4.042-1.604-.546-1.38-1.333-1.748-1.333-1.748-1.09-.744.083-.729.083-.729 1.205.083 1.838 1.236 1.838 1.236 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.303-5.467-1.332-5.467-5.93 0-1.31.468-2.38 1.236-3.22-.124-.303-.536-1.523.117-3.176 0 0 1.008-.322 3.3 1.23A11.5 11.5 0 0112 6.844c1.02.005 2.047.138 3.006.404 2.28-1.552 3.285-1.23 3.285-1.23.655 1.653.243 2.873.12 3.176.77.84 1.235 1.91 1.235 3.22 0 4.61-2.807 5.624-5.48 5.92.432.37.809 1.096.809 2.21 0 1.596-.015 2.882-.015 3.276 0 .32.216.694.825.576C20.565 21.917 24 17.497 24 12.292 24 5.78 18.627.5 12 .5z" />
              </svg>
              GitHub
            </button>
          </div>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <el-link type="primary" :underline="false" @click="$router.push('/register')">
            立即注册
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 全局 ===== */
.ai-login {
  min-height: 100vh;
  position: relative;
  display: flex;
  overflow: hidden;
  background: #0a0e1a;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* ===== 背景网格 + 光效 ===== */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, .03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, .03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 50%, #000 40%, transparent 100%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: .45;
  pointer-events: none;
}

.glow-a {
  width: 520px;
  height: 520px;
  background: #6366f1;
  top: -160px;
  left: -120px;
}

.glow-b {
  width: 420px;
  height: 420px;
  background: #06b6d4;
  bottom: -100px;
  left: 30%;
}

.glow-c {
  width: 360px;
  height: 360px;
  background: #8b5cf6;
  top: 20%;
  right: -80px;
  opacity: .25;
}

/* ===== 左侧 Hero ===== */
.hero {
  flex: 0 0 54%;
  display: flex;
  align-items: center;
  padding: 60px 64px;
  position: relative;
  z-index: 1;
}

.hero-inner {
  max-width: 520px;
}

.hero-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 56px;
}

.logo-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(99, 102, 241, .45);
}

.logo-name {
  font-size: 20px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: .3px;
}

.hero-title {
  font-size: 44px;
  font-weight: 700;
  line-height: 1.15;
  margin: 0 0 20px;
  background: linear-gradient(135deg, #f1f5f9 30%, #818cf8 70%, #22d3ee);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-sub {
  font-size: 15px;
  line-height: 1.7;
  color: #94a3b8;
  margin: 0 0 48px;
}

/* 对话预览 */
.chat-preview {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bubble {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 88%;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.55;
  animation: slideUp .6s ease both;
}

.bubble .el-icon {
  flex-shrink: 0;
  margin-top: 1px;
}

.bubble-user {
  align-self: flex-end;
  background: rgba(99, 102, 241, .18);
  border: 1px solid rgba(99, 102, 241, .3);
  color: #e0e7ff;
  border-bottom-right-radius: 4px;
}

.bubble-user .el-icon {
  color: #a5b4fc;
}

.bubble-ai {
  align-self: flex-start;
  background: rgba(255, 255, 255, .05);
  border: 1px solid rgba(255, 255, 255, .1);
  color: #cbd5e1;
  border-bottom-left-radius: 4px;
  backdrop-filter: blur(12px);
}

.bubble-ai .el-icon {
  color: #22d3ee;
}

.bubble:nth-child(2) {
  animation-delay: .1s;
}

.bubble:nth-child(3) {
  animation-delay: .22s;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 右侧登录 ===== */
.login-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
  position: relative;
  z-index: 1;
}

.glass-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .1);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 20px;
  padding: 44px 36px 32px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, .4), inset 0 1px 0 rgba(255, 255, 255, .08);
}

.card-head {
  margin-bottom: 32px;
  text-align: center;
}

.card-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  color: #f1f5f9;
}

.card-head p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

/* 输入框暗色覆写 */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, .05);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 12px;
  box-shadow: none;
  padding: 6px 14px;
  transition: all .2s;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(129, 140, 248, .5);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, .18);
  background: rgba(255, 255, 255, .07);
}

:deep(.el-input__inner) {
  color: #e2e8f0;
}

:deep(.el-input__inner::placeholder) {
  color: #475569;
}

:deep(.el-input__prefix-inner .el-icon) {
  color: #64748b;
}

/* checkbox 暗色 */
:deep(.el-checkbox__label) {
  color: #94a3b8;
  font-size: 13px;
}

:deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, .08);
  border-color: rgba(255, 255, 255, .2);
}

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -2px 0 24px;
}

.link {
  font-size: 13px;
  color: #818cf8;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
  color: #a5b4fc;
}

.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  border: none;
  transition: all .25s;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 28px rgba(99, 102, 241, .45);
}

/* 分割线 */
.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0 18px;
  color: #475569;
  font-size: 12px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(255, 255, 255, .08);
}

.divider span {
  padding: 0 4px;
}

/* OAuth */
.oauth-row {
  display: flex;
  gap: 12px;
}

.oauth-btn {
  flex: 1;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 10px;
  color: #cbd5e1;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
}

.oauth-btn:hover {
  background: rgba(255, 255, 255, .08);
  border-color: rgba(255, 255, 255, .22);
}

.card-foot {
  text-align: center;
  margin: 24px 0 0;
  font-size: 13px;
  color: #64748b;
}

.pointer {
  cursor: pointer;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .ai-login {
    flex-direction: column;
  }

  .hero {
    flex: none;
    padding: 48px 32px 24px;
  }

  .hero-title {
    font-size: 32px;
  }

  .chat-preview {
    display: none;
  }

  .login-side {
    padding: 24px 24px 48px;
  }

  .glass-card {
    max-width: 440px;
  }
}
</style>
