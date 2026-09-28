<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import {
  ArrowDown, Delete, Document, MoreFilled, User, Cpu, Promotion
} from '@element-plus/icons-vue'

/* ============================ 接口层 ============================ */
// 统一封装后端响应：{ code, data, message }
// code === 200 时返回 data，否则抛错交给页面层 catch。
const http = axios.create({
  baseURL: '/api',
  timeout: 150000
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 后端返回 { code, data, message }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      const msg = body.message || '请求失败'
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    // 非标准结构直接返回
    return body
  },
  (err) => {
    const msg = err.response?.data?.message || err.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

// 当前用户写死为 1，登录打通后可改为读取 store / localStorage
const CURRENT_USER_ID = 1

const sessionApi = {
  create: (title) => http.post('/session/create', null, { params: { userId: CURRENT_USER_ID, title } }),
  list: () => http.get('/session/list', { params: { userId: CURRENT_USER_ID } }),
  details: (sessionId) => http.get('/session/details', { params: { sessionId } }),
  delete: (sessionId) => http.get('/session/delete', { params: { sessionId } }),
  update: (sessionId, title) => http.get('/session/update', { params: { sessionId, title } })
}

// 聊天接口：后端字段确认后补充，目前为占位骨架
const chatApi = {
  // 发送消息。接口为 POST /api/chat/send，sessionId 与 message 走 query 参数。
  // 返回结构 { code, data, message }，data 即 AI 回复的文本内容。
  send: ({ sessionId, content }) =>
    http.post('/chat/send', null, { params: { sessionId, message: content } }),
  // 查询某会话的消息列表。接口为 POST /api/chat/list，sessionId 走 query 参数。
  // 返回结构 { code, data: [...], message }。
  listMessages: (sessionId) =>
    http.post('/chat/list', null, { params: { sessionId } })
}

// 字段适配：后端返回结构与此不一致时只改这里，页面逻辑不动。
function adaptSession(item) {
  if (!item) return null
  return {
    id: String(item.id ?? item.sessionId ?? item.session_id),
    title: item.title || '新对话',
    updatedAt: item.updatedAt ?? item.updateTime ?? item.updated_at ?? Date.now(),
    messages: []
  }
}

// 把后端各种 role 写法统一成 'user' | 'assistant'，缺失时按内容兜底为 assistant
// 后端数字枚举：0 = 用户(user)  1 = AI(assistant)
function normRole(role) {
  if (role === 0 || role === '0') return 'user'
  if (role === 1 || role === '1') return 'assistant'
  const r = String(role).toLowerCase().trim()
  if (r === 'user' || r === 'human' || r === 'u') return 'user'
  return 'assistant'
}

function adaptMessage(item) {
  if (!item) return null
  return {
    id: String(item.id ?? item.messageId ?? item.message_id),
    role: normRole(item.role ?? item.type ?? item.sender),
    content: item.content ?? item.text ?? item.message ?? '',
    // 后端返回 ISO 字符串，转时间戳便于排序与复用已有格式化逻辑
    createdAt: item.createdAt ?? item.createTime ?? item.created_at
      ? new Date(item.createdAt ?? item.createTime ?? item.created_at).getTime()
      : Date.now(),
    status: 'done'
  }
}

/* ============================ 常量 ============================ */
const STORAGE_KEY = 'NEXUS_CHAT_HISTORY'
const SESSION_TITLE_LEN = 18

/* ============================ 状态 ============================ */
const scrollbarRef = ref(null)
const textareaRef = ref(null)

const input = ref('')
const autoScroll = ref(true)

const sessions = ref([])
const activeId = ref('')

// 请求状态：loading 为会话列表加载中，sending 为正在等后端回复
const loading = ref(false)
const sending = ref(false)

// 每个会话的消息独立缓存：{ [sessionId]: Message[] }
const messageMap = reactive({})

/* ============================ 派生数据 ============================ */
const activeSession = computed(() =>
  sessions.value.find(s => s.id === activeId.value) || sessions.value[0]
)

const sortedSessions = computed(() =>
  [...sessions.value].sort((a, b) => b.updatedAt - a.updatedAt)
)

// 当前会话的消息从缓存里取，保证切换会话时消息不丢
const activeMessages = computed(() =>
  activeSession.value ? (messageMap[activeSession.value.id] || []) : []
)

const canSend = computed(() => input.value.trim() && !sending.value)

/* ============================ 数据请求 ============================ */
// 拉取会话列表
async function fetchSessions() {
  loading.value = true
  try {
    const list = await sessionApi.list()
    const arr = Array.isArray(list) ? list : (list?.records || list?.list || [])
    sessions.value = arr.map(adaptSession).filter(Boolean)
    if (!sessions.value.length) {
      await createSession('新对话')
      return
    }
    if (!sessions.value.find(s => s.id === activeId.value)) {
      activeId.value = sessions.value[0].id
    }
    // 进入时把当前会话的消息拉一次
    await fetchMessages(activeId.value)
  } catch (e) {
    sessions.value = []
  } finally {
    loading.value = false
  }
}

// 新建会话：后端创建成功后以返回的 id 为准，再拉一次详情补全
async function createSession(title) {
  try {
    const data = await sessionApi.create(title)
    // 后端通常在 data 里返回新建会话的对象或 id
    const created = adaptSession(data) || { id: String(data?.id ?? data), title, updatedAt: Date.now(), messages: [] }
    sessions.value.unshift(created)
    activeId.value = created.id
    messageMap[created.id] = []
    ElMessage.success('已创建新对话')
    focusEditor()
    return created
  } catch (e) {
    return null
  }
}

// 切换会话：若消息未拉取过则先拉一次
async function selectSession(id) {
  if (id === activeId.value) return
  activeId.value = id
  await fetchMessages(id)
  scrollToBottom(true)
  focusEditor()
}

async function removeSession(id) {
  const target = sessions.value.find(s => s.id === id)
  if (!target) return
  try {
    await ElMessageBox.confirm(`确定删除「${target.title}」吗？此操作不可恢复。`, '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }

  try {
    await sessionApi.delete(id)
    delete messageMap[id]
    const idx = sessions.value.findIndex(s => s.id === id)
    sessions.value.splice(idx, 1)

    if (activeId.value === id) {
      if (sessions.value.length) {
        activeId.value = sessions.value[0].id
        await fetchMessages(activeId.value)
      } else {
        await createSession('新对话')
        return
      }
    }
    ElMessage.success('已删除')
  } catch (e) {
    // 拦截器已提示，这里不再重复弹
  }
}

function newSession() {
  createSession('新对话')
}


// 拉取某会话的消息列表，已存在的缓存不重复拉取。
// 后端字段已定：{ id, role, content, createTime, sessionId, deleted }。
// 按 createTime 升序排列，倒序展示时间轴才有意义。
async function fetchMessages(sessionId) {
  if (!sessionId) return
  if (messageMap[sessionId] && messageMap[sessionId].length) return
  try {
    const list = await chatApi.listMessages(sessionId)
    const arr = Array.isArray(list) ? list : (list?.records || list?.list || [])
    const msgs = arr.map(adaptMessage).filter(Boolean)
    msgs.sort((a, b) => a.createdAt - b.createdAt)
    messageMap[sessionId] = msgs
  } catch (e) {
    messageMap[sessionId] = []
  }
  scrollToBottom()
}

/* ============================ 消息发送 ============================ */
// 发送流程：
// 1. 立即把用户输入构造成一条 role=user 的消息写入缓存，零延迟回显
// 2. 清空输入框、滚动到底部，同时把输入置为发送中
// 3. 调用后端聊天接口，把返回的回复一次性追加为 assistant 消息
// 4. 失败时在会话内补一条错误提示，方便排查
async function sendMessage() {
  if (!canSend.value || !activeSession.value) return
  const text = input.value.trim()
  const sid = activeSession.value.id

  if (!messageMap[sid]) messageMap[sid] = []
  const userMsg = {
    id: 'm_' + Date.now().toString(36),
    role: 'user',
    content: text,
    createdAt: Date.now(),
    status: 'done'
  }
  messageMap[sid].push(userMsg)

  input.value = ''
  resetTextareaHeight()

  // 首次发消息时以该消息内容作为会话标题：先改本地显示，再异步通知后端
  const titleUpdated = reflagTitle(text)
  scrollToBottom()

  sending.value = true
  try {
    const reply = await chatApi.send({ sessionId: sid, content: text })
    // data 即为 AI 回复文本，直接落进当前会话
    const replyText = typeof reply === 'string'
      ? reply
      : (reply?.content ?? reply?.data ?? '')
    if (replyText) {
      messageMap[sid].push({
        id: 'm_' + (Date.now() + 1).toString(36),
        role: 'assistant',
        content: replyText,
        createdAt: Date.now(),
        status: 'done'
      })
    }
  } catch (e) {
    messageMap[sid].push({
      id: 'm_err_' + Date.now().toString(36),
      role: 'assistant',
      content: '抱歉，刚才那条消息发送失败了，可以重试。',
      createdAt: Date.now(),
      status: 'error'
    })
  } finally {
    sending.value = false
    scrollToBottom()
    focusEditor()
  }
}

// 首条消息自动作为会话标题：只有标题还是默认值「新对话」时才触发。
// 先更新本地，让用户即时看到变化；再调后端接口落库。
// 接口失败则回滚本地标题，避免前后端不一致。
async function reflagTitle(text) {
  const s = activeSession.value
  if (!s || s.title !== '新对话') return false
  const next = text.replace(/\s+/g, ' ').trim().slice(0, SESSION_TITLE_LEN) || '新对话'
  const prev = s.title
  s.title = next
  try {
    await sessionApi.update(s.id, next)
    return true
  } catch (e) {
    s.title = prev
    return false
  }
}

// 手动重命名走这个方法，与自动命名共用同一接口
async function renameSession(id) {
  const target = sessions.value.find(s => s.id === id)
  if (!target) return
  ElMessageBox.prompt('给这个对话起个名字', '重命名', {
    inputValue: target.title,
    confirmButtonText: '保存',
    cancelButtonText: '取消'
  }).then(async ({ value }) => {
    const v = value?.trim()
    if (!v) return
    const prev = target.title
    const next = v.slice(0, 40)
    target.title = next
    try {
      await sessionApi.update(target.id, next)
    } catch (e) {
      target.title = prev
    }
  }).catch(() => { /* 取消 */ })
}

/* ============================ 输入控件高度自适应 ============================ */
// textarea 是受控组件，必须用 scrollHeight 手动算高度，不能用 auto 高度。
// 顺序很关键：先复位到单行高度，再按 scrollHeight 撑开，否则收缩时高度不会回退。
function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = '26px'
  const next = Math.min(el.scrollHeight, 180)
  el.style.height = next + 'px'
  el.style.overflowY = el.scrollHeight > 180 ? 'auto' : 'hidden'
}

function resetTextareaHeight() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = '26px'
  el.style.overflowY = 'hidden'
}

/* ============================ 滚动控制 ============================ */
function scrollToBottom(smooth = false) {
  nextTick(() => {
    const bar = scrollbarRef.value
    if (!bar || !bar.wrapRef) return
    bar.setScrollTop(bar.wrapRef.scrollHeight)
  })
}

function onScroll() {
  const bar = scrollbarRef.value
  if (!bar || !bar.wrapRef) return
  const { scrollTop, scrollHeight, clientHeight } = bar.wrapRef
  autoScroll.value = scrollHeight - scrollTop - clientHeight < 80
}

const showJumpBtn = computed(() => !autoScroll.value && activeSession.value?.messages.length > 0)

function focusEditor() {
  nextTick(() => textareaRef.value?.focus?.())
}

/* ============================ 生命周期 ============================ */
watch(
  () => activeMessages.value?.length,
  () => {
    if (autoScroll.value) scrollToBottom()
  }
)

onMounted(() => {
  fetchSessions().finally(() => {
    scrollToBottom()
    focusEditor()
  })
})

onUnmounted(() => {
  // 若接入流式接口，在此通过 AbortController 取消未完成的请求
})
</script>

<template>
  <div class="ai-chat">
    <!-- 背景光效，与 Login.vue 保持一致 -->
    <div class="bg-grid" />
    <div class="glow glow-a" />
    <div class="glow glow-b" />
    <div class="glow glow-c" />

    <!-- ===== 左侧：会话列表 ===== -->
    <aside class="chat-aside">
      <div class="aside-head">
        <div class="logo-mark">
          <el-icon :size="20">
            <Cpu />
          </el-icon>
        </div>
        <span class="logo-name">Nexus AI</span>
      </div>

      <button class="new-btn" @click="newSession">
        <el-icon :size="16">
          <Document />
        </el-icon>
        <span>新建对话</span>
      </button>

      <el-scrollbar class="session-list" height="calc(100% - 132px)">
        <div
          v-for="s in sortedSessions"
          :key="s.id"
          class="session-item"
          :class="{ active: s.id === activeId }"
          @click="selectSession(s.id)"
        >
          <div class="session-icon">
            <el-icon :size="15">
              <Document />
            </el-icon>
          </div>
          <div class="session-main">
            <div class="session-title">{{ s.title }}</div>
            <div class="session-preview">
              {{ s.messages[s.messages.length - 1]?.content.replace(/\n+/g, ' ').slice(0, 32) || '暂无消息' }}
            </div>
          </div>
          <el-dropdown
            trigger="click"
            @command="(c) => { if(c==='rename') renameSession(s.id); if(c==='delete') removeSession(s.id) }"
            @click.stop
          >
            <span class="session-more">
              <el-icon :size="14">
                <MoreFilled />
              </el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">
                  <el-icon><Edit /></el-icon>重命名
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <el-icon><Delete /></el-icon>删除会话
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-scrollbar>

      <div class="aside-foot">
        <el-button text class="foot-btn" @click="$router.push('/login')">
          <el-icon :size="15">
            <Document />
          </el-icon>退出登录
        </el-button>
      </div>
    </aside>

    <!-- ===== 右侧：对话区 ===== -->
    <main class="chat-main">
      <header class="chat-head">
        <div class="head-info">
          <h2>{{ activeSession?.title || '新对话' }}</h2>
          <span class="head-tip">{{ sending ? '正在回复…' : '数据来自服务端接口' }}</span>
        </div>
        <div class="head-actions">
          <el-tooltip content="清空会话">
            <el-button
              circle text :icon="Delete"
              @click="activeSession && (activeSession.messages = [])"
            />
          </el-tooltip>
        </div>
      </header>

      <!-- 消息流 -->
      <section class="chat-body">
        <el-scrollbar
          ref="scrollbarRef"
          height="100%"
          always
          @scroll="onScroll"
        >
          <div class="msg-list">
            <template v-if="activeSession">
              <div v-if="!activeMessages.length" class="empty-tip">
                开始你的第一次提问吧
              </div>
              <div
                v-for="m in activeMessages"
                :key="m.id"
                class="msg-row"
                :class="[m.role, { error: m.status === 'error' }]"
              >
                <div class="avatar" :class="m.role">
                  <el-icon :size="16">
                    <User v-if="m.role === 'user'" />
                    <Cpu v-else />
                  </el-icon>
                </div>

                <div class="bubble-wrap">
                  <div class="bubble" :class="m.role">
                    <div class="content">{{ m.content }}</div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </el-scrollbar>

        <transition name="fade">
          <button v-if="showJumpBtn" class="jump-btn" @click="scrollToBottom(true)">
            <el-icon :size="14">
              <ArrowDown />
            </el-icon>
            回到底部
          </button>
        </transition>
      </section>

      <!-- 输入区：去掉上传与语音，只保留一个文本输入框 -->
      <footer class="chat-foot">
        <div class="composer">
          <textarea
            ref="textareaRef"
            v-model="input"
            class="editor"
            rows="1"
            :disabled="sending"
            placeholder="给 Nexus AI 发送消息，Enter 发送 / Shift+Enter 换行"
            @input="autoResize"
            @keydown.enter.exact.prevent="sendMessage"
          />

          <button
            class="send-btn"
            type="button"
            :class="{ disabled: !canSend }"
            :disabled="!canSend"
            @click="sendMessage"
          >
            <el-icon :size="16">
              <Promotion />
            </el-icon>
          </button>
        </div>

        <p class="foot-tip">
          内容由 AI 生成，仅供参考 · 模拟流式响应无需后端即可演示
        </p>
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* ===== 全局 ===== */
.ai-chat {
  height: 100vh;
  display: flex;
  position: relative;
  overflow: hidden;
  background: #0a0e1a;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: #e2e8f0;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, .03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, .03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 50%, #000 40%, transparent 100%);
  pointer-events: none;
}

.glow { position: absolute; border-radius: 50%; filter: blur(120px); opacity: .4; pointer-events: none; }
.glow-a { width: 480px; height: 480px; background: #6366f1; top: -180px; right: -120px; opacity: .28; }
.glow-b { width: 380px; height: 380px; background: #06b6d4; bottom: -140px; left: 30%; opacity: .2; }
.glow-c { width: 340px; height: 340px; background: #8b5cf6; top: 30%; left: -100px; opacity: .2; }

/* ===== 侧边栏 ===== */
.chat-aside {
  width: 268px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 18px 14px;
  background: rgba(255, 255, 255, .02);
  border-right: 1px solid rgba(255, 255, 255, .08);
  position: relative;
  z-index: 1;
}

.aside-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 18px;
}

.logo-mark {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 16px rgba(99, 102, 241, .4);
}

.logo-name { font-size: 17px; font-weight: 700; color: #f1f5f9; letter-spacing: .3px; }

.new-btn {
  width: 100%; height: 42px;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  border: none; border-radius: 11px;
  color: #fff; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all .2s;
  margin-bottom: 14px;
}
.new-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(99, 102, 241, .4); }

.session-list { flex: 1; }

.session-item {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 10px; margin-bottom: 3px;
  border-radius: 10px; cursor: pointer;
  transition: background .15s;
  position: relative;
}
.session-item:hover { background: rgba(255, 255, 255, .05); }
.session-item.active { background: rgba(99, 102, 241, .18); }
.session-item.active::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 22px; border-radius: 0 3px 3px 0; background: #818cf8;
}

.session-icon {
  width: 30px; height: 30px; flex-shrink: 0;
  border-radius: 8px; background: rgba(255, 255, 255, .06);
  display: flex; align-items: center; justify-content: center;
  color: #818cf8;
}
.active .session-icon { background: rgba(99, 102, 241, .3); }

.session-main { flex: 1; min-width: 0; }
.session-title {
  font-size: 13.5px; font-weight: 500; color: #e2e8f0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.session-preview {
  font-size: 12px; color: #64748b;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-top: 2px;
}

.session-more {
  width: 24px; height: 24px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 6px; color: #64748b; opacity: 0;
  transition: opacity .15s;
}
.session-item:hover .session-more { opacity: 1; }
.session-more:hover { background: rgba(255, 255, 255, .08); color: #cbd5e1; }

.aside-foot {
  display: flex; gap: 4px; padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, .06);
}
.foot-btn { color: #94a3b8; font-size: 13px; }
.foot-btn:hover { color: #e2e8f0; background: rgba(255, 255, 255, .05); }

/* ===== 主区 ===== */
.chat-main {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column;
  position: relative; z-index: 1;
}

.chat-head {
  height: 60px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid rgba(255, 255, 255, .06);
}
.head-info h2 { margin: 0; font-size: 16px; font-weight: 600; color: #f1f5f9; }
.head-tip { font-size: 12px; color: #475569; }
.head-actions { display: flex; gap: 4px; }

.chat-body {
  flex: 1; min-height: 0; position: relative;
}

.msg-list {
  max-width: 820px; margin: 0 auto;
  padding: 32px 28px 40px;
  display: flex; flex-direction: column; gap: 26px;
}

.msg-row {
  display: flex; gap: 14px;
  align-items: flex-start;
}
.msg-row.user { flex-direction: row-reverse; }

.avatar {
  width: 34px; height: 34px; flex-shrink: 0;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
}
.avatar.user { background: linear-gradient(135deg, #6366f1, #06b6d4); color: #fff; }
.avatar.assistant { background: rgba(255, 255, 255, .06); color: #22d3ee; border: 1px solid rgba(255, 255, 255, .1); }

.bubble-wrap { max-width: calc(100% - 48px); }

.bubble {
  padding: 13px 16px;
  border-radius: 14px;
  font-size: 14.5px; line-height: 1.75;
  word-break: break-word;
}
.bubble.user {
  background: rgba(99, 102, 241, .16);
  border: 1px solid rgba(99, 102, 241, .28);
  color: #e0e7ff;
  border-bottom-right-radius: 4px;
}
.bubble.assistant {
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .08);
  color: #cbd5e1;
  border-bottom-left-radius: 4px;
}

.content { white-space: pre-wrap; }

.empty-tip {
  text-align: center;
  color: #475569;
  font-size: 13.5px;
  padding: 60px 0;
}

.msg-row.error .bubble {
  border-color: rgba(248, 113, 113, .4);
  background: rgba(248, 113, 113, .1);
  color: #fca5a5;
}

.jump-btn {
  position: absolute; bottom: 24px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 20px;
  background: rgba(99, 102, 241, .9); color: #fff;
  border: none; cursor: pointer; font-size: 13px;
  box-shadow: 0 4px 20px rgba(99, 102, 241, .4);
  backdrop-filter: blur(8px);
}
.jump-btn:hover { background: #6366f1; }

/* ===== 输入区 ===== */
.chat-foot {
  flex-shrink: 0;
  padding: 0 28px 22px;
}

.composer {
  max-width: 820px; margin: 0 auto;
  display: flex; align-items: flex-end; gap: 8px;
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 16px; padding: 8px 8px 8px 14px;
  transition: all .2s;
}
.composer:focus-within {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, .15);
  background: rgba(255, 255, 255, .06);
}

.editor {
  flex: 1; min-height: 26px; max-height: 180px;
  padding: 8px 0;
  font-size: 14.5px; line-height: 1.6;
  font-family: inherit;
  color: #e2e8f0;
  background: transparent;
  border: none; outline: none; resize: none;
  overflow-y: hidden;
}

.editor::placeholder { color: #475569; }
.editor:disabled { cursor: not-allowed; }

.send-btn {
  width: 40px; height: 40px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 12px; cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  color: #fff; transition: all .2s;
}
.send-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99, 102, 241, .4); }
.send-btn.disabled { opacity: .35; cursor: not-allowed; box-shadow: none; transform: none; }

.foot-tip {
  max-width: 820px; margin: 10px auto 0;
  font-size: 11.5px; color: #475569; text-align: center;
}

.fade-enter-active, .fade-leave-active { transition: opacity .2s, transform .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translate(-50%, 8px); }

/* ===== 滚动条美化 ===== */
:deep(.el-scrollbar__thumb) { background: rgba(255, 255, 255, .12); }
:deep(.el-scrollbar__thumb:hover) { background: rgba(255, 255, 255, .22); }

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .chat-aside { position: fixed; left: 0; top: 0; bottom: 0; z-index: 30; transform: translateX(-100%); transition: transform .25s; }
  .chat-aside.open { transform: translateX(0); }
  .chat-head { padding: 0 16px; }
  .msg-list { padding: 20px 16px 32px; }
  .chat-foot { padding: 0 16px 16px; }
}
</style>
