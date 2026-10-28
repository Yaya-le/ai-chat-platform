<template>
  <div class="login-container">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <div class="brand">
        <div class="logo">
          <el-icon :size="36" color="#6366f1"><Cpu /></el-icon>
        </div>
        <h1 class="brand-title">AI 智能平台</h1>
        <p class="brand-desc">加入我们，开启你的智能对话之旅</p>
      </div>
      <div class="decoration">
        <div class="float-item item-1"><el-icon><ChatDotRound /></el-icon></div>
        <div class="float-item item-2"><el-icon><MagicStick /></el-icon></div>
        <div class="float-item item-3"><el-icon><Cpu /></el-icon></div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-right">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>填写信息，注册你的账户</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @keyup.enter="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="请输入昵称（可选）"
              :prefix-icon="UserFilled"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              :type="passwordVisible ? 'text' : 'password'"
              :suffix-icon="passwordVisible ? View : Hide"
              @click-suffix="passwordVisible = !passwordVisible"
              clearable
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              :type="confirmVisible ? 'text' : 'password'"
              :suffix-icon="confirmVisible ? View : Hide"
              @click-suffix="confirmVisible = !confirmVisible"
              clearable
            />
          </el-form-item>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form>

        <div class="form-footer">
          <span>已有账号？</span>
          <el-link type="primary" :underline="false" @click="goLogin">
            立即登录
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  User, UserFilled, Lock, View, Hide,
  ChatDotRound, MagicStick, Cpu
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const passwordVisible = ref(false)
const confirmVisible = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

// 自定义校验：两次密码一致
const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '长度 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度 6-32 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      // 只传后端需要的字段，nickname 为空就不传
      const payload = {
        username: form.username,
        password: form.password
      }
      if (form.nickname) payload.nickname = form.nickname

      await register(payload)

      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (e) {
      // request 拦截器已统一弹错
      console.warn('[register]', e?.message || e)
    } finally {
      loading.value = false
    }
  })
}

const goLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
/* 与 Chat.vue / Login.vue 一致的浅蓝渐变 + 白色毛玻璃 */
.login-container {
  min-height: 100vh;
  display: flex;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #a8c8e8 0%, #b8c8e8 40%, #8ec8e0 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.login-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 1;
  padding: 40px;
}

.brand { text-align: center; z-index: 1; }
.logo {
  width: 72px; height: 72px; border-radius: 20px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 24px;
  box-shadow: 0 4px 16px rgba(99,102,241,.3);
  color: #fff;
}
.brand-title { font-size: 32px; font-weight: 700; color: #1e293b; margin-bottom: 12px; }
.brand-desc { font-size: 16px; color: #475569; }

.decoration { position: absolute; inset: 0; pointer-events: none; }
.float-item {
  position: absolute; width: 56px; height: 56px; border-radius: 16px;
  background: rgba(255,255,255,.5);
  border: 1px solid rgba(255,255,255,.6);
  backdrop-filter: blur(20px);
  display: flex; align-items: center; justify-content: center;
  color: #6366f1; font-size: 24px;
}
.item-1 { top: 18%; left: 12%; animation: float 6s ease-in-out infinite; }
.item-2 { bottom: 22%; right: 14%; animation: float 7s ease-in-out infinite .8s; }
.item-3 { top: 50%; left: 8%; animation: float 5s ease-in-out infinite 1.5s; }
@keyframes float { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-18px)} }

.login-right {
  width: 480px; display: flex; align-items: center; justify-content: center;
  padding: 40px;
  position: relative; z-index: 1;
}
.form-wrapper {
  width: 100%; max-width: 380px;
  background: rgba(255,255,255,.55);
  border: 1px solid rgba(255,255,255,.6);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border-radius: 20px;
  padding: 40px 36px 28px;
  box-shadow: 0 8px 32px rgba(0,0,0,.08);
}
.form-header { text-align: center; margin-bottom: 32px; }
.form-header h2 { font-size: 24px; font-weight: 600; color: #1e293b; margin-bottom: 8px; }
.form-header p { color: #64748b; font-size: 13px; }

.login-form :deep(.el-input__wrapper) {
  background: rgba(255,255,255,.6);
  border: 1px solid rgba(255,255,255,.7);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,.04);
  padding: 6px 14px;
  transition: all .2s;
}
.login-form :deep(.el-input__wrapper:hover) { border-color: rgba(99,102,241,.4); }
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99,102,241,.15);
  background: rgba(255,255,255,.8);
}
.login-form :deep(.el-input__inner) { color: #1e293b; }
.login-form :deep(.el-input__inner::placeholder) { color: #94a3b8; }
.login-form :deep(.el-input__prefix-inner .el-icon) { color: #64748b; }

.submit-btn {
  width: 100%; height: 46px;
  border-radius: 12px; font-size: 15px; font-weight: 600; letter-spacing: 1px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  border: none;
  box-shadow: 0 4px 16px rgba(99,102,241,.3);
  margin-top: 8px;
}
.submit-btn:hover { transform: translateY(-1px); box-shadow: 0 8px 28px rgba(99,102,241,.45); }

.form-footer { text-align: center; margin-top: 20px; color: #64748b; font-size: 13px; }

@media (max-width: 900px) {
  .login-left { display: none; }
  .login-right { width: 100%; }
}
</style>