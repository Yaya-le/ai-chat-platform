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
/* 直接复用你 Login.vue 的样式，两份保持一致 */
.login-container {
  min-height: 100vh;
  display: flex;
  background: #0f0f1a;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.brand { text-align: center; z-index: 1; }
.logo {
  width: 72px; height: 72px; border-radius: 20px;
  background: rgba(99,102,241,.15);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 24px;
  backdrop-filter: blur(12px);
}
.brand-title { font-size: 32px; font-weight: 700; color: #fff; margin-bottom: 12px; }
.brand-desc { font-size: 16px; color: rgba(255,255,255,.6); }

.decoration { position: absolute; inset: 0; pointer-events: none; }
.float-item {
  position: absolute; width: 56px; height: 56px; border-radius: 16px;
  background: rgba(99,102,241,.12); display: flex; align-items: center; justify-content: center;
  color: rgba(99,102,241,.7); font-size: 24px;
}
.item-1 { top: 18%; left: 12%; animation: float 6s ease-in-out infinite; }
.item-2 { bottom: 22%; right: 14%; animation: float 7s ease-in-out infinite .8s; }
.item-3 { top: 50%; left: 8%; animation: float 5s ease-in-out infinite 1.5s; }
@keyframes float { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-18px)} }

.login-right {
  width: 480px; display: flex; align-items: center; justify-content: center;
  background: #fff;
}
.form-wrapper { width: 360px; }
.form-header { text-align: center; margin-bottom: 36px; }
.form-header h2 { font-size: 26px; font-weight: 600; color: #1a1a2e; margin-bottom: 8px; }
.form-header p { color: #909399; font-size: 14px; }
.login-form :deep(.el-input__wrapper) { border-radius: 12px; padding: 4px 14px; }
.submit-btn { width: 100%; height: 44px; border-radius: 12px; font-size: 16px; margin-top: 8px; }
.form-footer { text-align: center; margin-top: 20px; color: #909399; font-size: 14px; }

@media (max-width: 900px) {
  .login-left { display: none; }
  .login-right { width: 100%; }
}
</style>