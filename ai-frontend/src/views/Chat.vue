<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { marked } from 'marked'
import {
  ArrowDown, Delete, Document, MoreFilled, User, Cpu, Promotion, Edit,
  DocumentCopy, Headset, Pointer, CircleClose, Share
} from '@element-plus/icons-vue'

/* ============================ 接口层 ============================ */
const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ADMIN_TOKEN')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      const msg = body.message || '请求失败'
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    return body
  },
  (err) => {
    if (err.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('ADMIN_TOKEN')
      window.location.href = '/login'
      return Promise.reject(err)
    }
    const msg = err.response?.data?.message || err.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

const sessionApi = {
  create: (title) => http.post('/session/create', null, { params: { title } }),
  list: () => http.get('/session/list'),
  details: (sessionId) => http.get('/session/details', { params: { sessionId } }),
  delete: (sessionId) => http.get('/session/delete', { params: { sessionId } }),
  update: (sessionId, title) => http.get('/session/update', { params: { sessionId, title } })
}

const chatApi = {
  send: ({ sessionId, content }) =>
    http.post('/chat/send', null, { params: { sessionId, message: content } }),
  listMessages: (sessionId) =>
    http.post('/chat/list', null, { params: { sessionId } })
}

function adaptSession(item) {
  if (!item) return null
  return {
    id: String(item.id ?? item.sessionId ?? item.session_id),
    title: item.title || '新对话',
    updatedAt: item.updatedAt ?? item.updateTime ?? item.updated_at ?? Date.now(),
    messages: []
  }
}

function normRole(role) {
  if (role === 0 || role === '0') return 'user'
  if (role === 1 || role === '1') return 'assistant'
  const r = String(role).toLowerCase().trim()
  if (r === 'user' || r === 'human' || r === 'u') return 'user'
  return 'assistant'
}

function adaptMessage(item) {
  if (!item) return null
  let content = item.content ?? item.text ?? item.message ?? ''
  // 清理AI回复外层引号和转义字符
  if (typeof content === 'string') {
    content = content.replace(/^"|"$/g, '').replace(/\\n/g, '\n').replace(/\\"/g, '"')
  }
  return {
    id: String(item.id ?? item.messageId ?? item.message_id),
    role: normRole(item.role ?? item.type ?? item.sender),
    content: content,
    createdAt: item.createdAt ?? item.createTime ?? item.created_at
      ? new Date(item.createdAt ?? item.createTime ?? item.created_at).getTime()
      : Date.now(),
    status: 'done'
  }
}

/* ============================ 状态 ============================ */
const scrollbarRef = ref(null)
const textareaRef = ref(null)
const input = ref('')
const autoScroll = ref(true)
const sessions = ref([])
const activeId = ref('')
const loading = ref(false)
const sending = ref(false)
const messageMap = reactive({})

/* ============================ 派生数据 ============================ */
const activeSession = computed(() =>
  sessions.value.find(s => s.id === activeId.value) || sessions.value[0]
)
const sortedSessions = computed(() =>
  [...sessions.value].sort((a, b) => b.updatedAt - a.updatedAt)
)
const activeMessages = computed(() =>
  activeSession.value ? (messageMap[activeSession.value.id] || []) : []
)
const canSend = computed(() => input.value.trim().length > 0)

/* ============================ 数据请求 ============================ */
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
    await fetchMessages(activeId.value)
  } catch (e) {
    sessions.value = []
  } finally {
    loading.value = false
  }
}

async function createSession(title) {
  try {
    const data = await sessionApi.create(title)
    const created = adaptSession(data) || { id: String(data?.id ?? data), title, updatedAt: Date.now(), messages: [] }
    sessions.value.unshift(created)
    activeId.value = created.id
    messageMap[created.id] = []
    ElMessage.success('已创建新对话')
    focusEditor()
    return created
  } catch (e) { return null }
}

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
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch (e) { return }
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
  } catch (e) {}
}

function newSession() { createSession('新对话') }

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
async function sendMessage() {
  if (!canSend.value || !activeSession.value) return
  const text = input.value.trim()
  const sid = activeSession.value.id
  if (!messageMap[sid]) messageMap[sid] = []
  messageMap[sid].push({
    id: 'm_' + Date.now().toString(36),
    role: 'user', content: text, createdAt: Date.now(), status: 'done'
  })
  input.value = ''
  resetTextareaHeight()
  reflagTitle(text)
  scrollToBottom()
  sending.value = true
  try {
    await chatApi.send({ sessionId: sid, content: text })
    messageMap[sid] = []
    await fetchMessages(sid)
  } catch (e) {
    messageMap[sid].push({
      id: 'm_err_' + Date.now().toString(36),
      role: 'assistant', content: '抱歉，刚才那条消息发送失败了，可以重试。',
      createdAt: Date.now(), status: 'error'
    })
  } finally {
    sending.value = false
    scrollToBottom()
    focusEditor()
  }
}

async function reflagTitle(text) {
  const s = activeSession.value
  if (!s || s.title !== '新对话') return false
  const next = text.replace(/\s+/g, ' ').trim().slice(0, 18) || '新对话'
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

async function renameSession(id) {
  const target = sessions.value.find(s => s.id === id)
  if (!target) return
  ElMessageBox.prompt('给这个对话起个名字', '重命名', {
    inputValue: target.title, confirmButtonText: '保存', cancelButtonText: '取消'
  }).then(async ({ value }) => {
    const v = value?.trim()
    if (!v) return
    const prev = target.title
    target.title = v.slice(0, 40)
    try { await sessionApi.update(target.id, target.title) } catch (e) { target.title = prev }
  }).catch(() => {})
}

/* ============================ 输入控件高度自适应 ============================ */
function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = '26px'
  el.style.height = Math.min(el.scrollHeight, 180) + 'px'
  el.style.overflowY = el.scrollHeight > 180 ? 'auto' : 'hidden'
}
function resetTextareaHeight() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = '26px'
  el.style.overflowY = 'hidden'
}

/* ============================ 滚动控制 ============================ */
function scrollToBottom() {
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
const showJumpBtn = computed(() => !autoScroll.value && activeMessages.value?.length > 0)
function focusEditor() { nextTick(() => textareaRef.value?.focus?.()) }

function renderMarkdown(text) {
  if (!text) return ''
  return marked.parse(text)
}

function copyText(text) {
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制'))
}
function speakText(text) {
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'zh-CN'
  speechSynthesis.speak(u)
}
function likeMsg(m) {
  m.liked = !m.liked
  ElMessage.success(m.liked ? '已点赞' : '已取消')
}
function dislikeMsg(m) {
  m.disliked = !m.disliked
  ElMessage.info(m.disliked ? '已点踩' : '已取消')
}
function shareMsg(text) {
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制，可分享给好友'))
}
function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

/* ============================ 生命周期 ============================ */
watch(() => activeMessages.value?.length, () => { if (autoScroll.value) scrollToBottom() })
onMounted(() => { fetchSessions().finally(() => { scrollToBottom(); focusEditor() }) })
onUnmounted(() => {})
</script>

<template>
  <div class="ai-chat">
    <div class="bg-grid" />
    <div class="glow glow-a" />
    <div class="glow glow-b" />
    <div class="glow glow-c" />

    <aside class="chat-aside">
      <div class="aside-head">
        <div class="logo-mark"><el-icon :size="20"><Cpu /></el-icon></div>
        <span class="logo-name">Echo AI</span>
      </div>
      <button class="new-btn" @click="newSession">
        <el-icon :size="16"><Document /></el-icon><span>新建对话</span>
      </button>
      <el-scrollbar class="session-list" height="calc(100% - 132px)">
        <div v-for="s in sortedSessions" :key="s.id" class="session-item"
          :class="{ active: s.id === activeId }" @click="selectSession(s.id)">
          <div class="session-icon"><el-icon :size="15"><Document /></el-icon></div>
          <div class="session-main">
            <div class="session-title">{{ s.title }}</div>
            <div class="session-preview">暂无消息</div>
          </div>
          <el-dropdown trigger="click"
            @command="(c) => { if(c==='rename') renameSession(s.id); if(c==='delete') removeSession(s.id) }"
            @click.stop>
            <span class="session-more"><el-icon :size="14"><MoreFilled /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename"><el-icon><Edit /></el-icon>重命名</el-dropdown-item>
                <el-dropdown-item command="delete" divided><el-icon><Delete /></el-icon>删除会话</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-scrollbar>
      <div class="aside-foot">
        <el-button text class="foot-btn" @click="$router.push('/login')">退出登录</el-button>
      </div>
    </aside>

    <main class="chat-main">
      <header class="chat-head">
        <div class="head-info">
          <h2>{{ activeSession?.title || '新对话' }}</h2>
          <span class="head-tip">{{ sending ? '正在回复…' : '数据来自服务端接口' }}</span>
        </div>
        <el-tooltip content="清空会话">
          <el-button circle text :icon="Delete" @click="activeSession && (messageMap[activeSession.id] = [])" />
        </el-tooltip>
      </header>

      <section class="chat-body">
        <el-scrollbar ref="scrollbarRef" height="100%" always @scroll="onScroll">
          <div class="msg-list">
            <div v-if="!activeMessages.length" class="empty-tip">开始你的第一次提问吧</div>
            <div v-for="m in activeMessages" :key="m.id" class="msg-row"
              :class="[m.role, { error: m.status === 'error' }]">
              <div class="avatar" :class="m.role">
                <el-icon :size="16"><User v-if="m.role === 'user'" /><Cpu v-else /></el-icon>
              </div>
              <div class="bubble-wrap">
                <div class="bubble" :class="m.role">
                <div v-if="m.role === 'assistant'" class="content markdown-body" v-html="renderMarkdown(m.content)"></div>
                <div v-else class="content">{{ m.content }}</div>
              </div>
                <div v-if="m.role === 'assistant' && m.status !== 'error'" class="msg-actions">
                  <button class="act-btn" title="复制" @click="copyText(m.content)"><el-icon><DocumentCopy /></el-icon></button>
                  <button class="act-btn" title="朗读" @click="speakText(m.content)"><el-icon><Headset /></el-icon></button>
                  <button class="act-btn" :class="{ active: m.liked }" title="赞" @click="likeMsg(m)"><el-icon><Pointer /></el-icon></button>
                  <button class="act-btn" :class="{ active: m.disliked }" title="踩" @click="dislikeMsg(m)"><el-icon><CircleClose /></el-icon></button>
                  <button class="act-btn" title="分享" @click="shareMsg(m.content)"><el-icon><Share /></el-icon></button>
                  <span class="act-time">{{ formatTime(m.createdAt) }}</span>
                </div>
              </div>
            </div>
            <div v-if="sending" class="msg-row assistant">
              <div class="avatar assistant"><el-icon :size="16"><Cpu /></el-icon></div>
              <div class="bubble-wrap">
                <div class="bubble assistant">
                  <div class="thinking-dots"><span></span><span></span><span></span></div>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>
        <transition name="fade">
          <button v-if="showJumpBtn" class="jump-btn" @click="scrollToBottom(true)">
            <el-icon :size="14"><ArrowDown /></el-icon>回到底部
          </button>
        </transition>
      </section>

      <footer class="chat-foot">
        <div class="composer">
          <textarea ref="textareaRef" v-model="input" class="editor" rows="1"
            placeholder="给 Echo AI 发送消息，Enter 发送 / Shift+Enter 换行"
            @input="autoResize" @keydown.enter.exact.prevent="sendMessage" />
          <button class="send-btn" :class="{ disabled: !canSend }" :disabled="!canSend" @click="sendMessage">
            <el-icon :size="16"><Promotion /></el-icon>
          </button>
        </div>
        <p class="foot-tip">内容由 AI 生成，仅供参考</p>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.ai-chat { height: 100vh; display: flex; position: relative; overflow: hidden; background: linear-gradient(135deg, #a8c8e8 0%, #b8c8e8 40%, #8ec8e0 100%); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color: #1e293b; }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(255,255,255,.04) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.04) 1px, transparent 1px); background-size: 48px 48px; mask-image: radial-gradient(ellipse 80% 60% at 50% 50%, #000 40%, transparent 100%); pointer-events: none; }
.glow { position: absolute; border-radius: 50%; filter: blur(100px); pointer-events: none; }
.glow-a { width: 560px; height: 560px; background: #6366f1; top: -200px; right: -100px; opacity: .35; }
.glow-b { width: 460px; height: 460px; background: #06b6d4; bottom: -160px; left: 25%; opacity: .25; }
.glow-c { width: 420px; height: 420px; background: #8b5cf6; top: 30%; left: -120px; opacity: .3; }

.chat-aside { width: 268px; flex-shrink: 0; display: flex; flex-direction: column; padding: 18px 14px; background: rgba(255,255,255,.4); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border-right: 1px solid rgba(255,255,255,.5); position: relative; z-index: 1; }
.aside-head { display: flex; align-items: center; gap: 10px; padding: 4px 6px 18px; }
.logo-mark { width: 34px; height: 34px; border-radius: 10px; background: linear-gradient(135deg, #6366f1, #06b6d4); display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 16px rgba(99,102,241,.3); }
.logo-name { font-size: 17px; font-weight: 700; color: #1e293b; }
.new-btn { width: 100%; height: 42px; display: flex; align-items: center; justify-content: center; gap: 8px; background: linear-gradient(135deg, #6366f1, #06b6d4); border: none; border-radius: 11px; color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; margin-bottom: 14px; box-shadow: 0 4px 16px rgba(99,102,241,.4); }
.new-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(99,102,241,.5); }
.session-list { flex: 1; }
.session-item { display: flex; align-items: center; gap: 10px; padding: 11px 10px; margin-bottom: 3px; border-radius: 10px; cursor: pointer; position: relative; transition: background .2s; }
.session-item:hover { background: rgba(255,255,255,.5); }
.session-item.active { background: rgba(99,102,241,.15); }
.session-item.active::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 3px; height: 22px; background: #6366f1; border-radius: 0 3px 3px 0; }
.session-icon { width: 30px; height: 30px; flex-shrink: 0; border-radius: 8px; background: rgba(255,255,255,.6); display: flex; align-items: center; justify-content: center; color: #6366f1; }
.session-main { flex: 1; min-width: 0; }
.session-title { font-size: 13.5px; font-weight: 500; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-preview { font-size: 12px; color: #94a3b8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
.session-more { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 6px; color: #94a3b8; opacity: 0; }
.session-item:hover .session-more { opacity: 1; }
.aside-foot { display: flex; gap: 4px; padding-top: 10px; border-top: 1px solid rgba(148,163,184,.2); }
.foot-btn { color: #64748b; font-size: 13px; }

.chat-main { flex: 1; min-width: 0; display: flex; flex-direction: column; position: relative; z-index: 1; }
.chat-head { height: 60px; flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; padding: 0 28px; background: rgba(255,255,255,.35); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border-bottom: 1px solid rgba(255,255,255,.4); }
.head-info h2 { margin: 0; font-size: 16px; color: #1e293b; }
.head-tip { font-size: 12px; color: #64748b; }
.chat-body { flex: 1; min-height: 0; position: relative; }
.msg-list { max-width: 820px; margin: 0 auto; padding: 32px 28px 40px; display: flex; flex-direction: column; gap: 26px; }
.msg-row { display: flex; gap: 14px; align-items: flex-start; }
.msg-row.user { flex-direction: row-reverse; }
.avatar { width: 34px; height: 34px; flex-shrink: 0; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.avatar.user { background: linear-gradient(135deg, #6366f1, #06b6d4); color: #fff; box-shadow: 0 2px 8px rgba(99,102,241,.3); }
.avatar.assistant { background: rgba(255,255,255,.7); color: #6366f1; border: 1px solid rgba(255,255,255,.8); box-shadow: 0 2px 8px rgba(0,0,0,.06); }
.bubble-wrap { max-width: calc(100% - 48px); }
.bubble { padding: 16px 28px; border-radius: 14px; font-size: 14.5px; line-height: 1.75; word-break: break-word; backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); }
.bubble.user { background: rgba(99,102,241,.2); border: 1px solid rgba(99,102,241,.25); color: #312e81; border-bottom-right-radius: 4px; }
.bubble.assistant { background: rgba(255,255,255,.6); border: 1px solid rgba(255,255,255,.7); color: #334155; border-bottom-left-radius: 4px; box-shadow: 0 4px 20px rgba(0,0,0,.08); }
.content { white-space: pre-wrap; }
.markdown-body { white-space: normal; line-height: 1.9; font-size: 14.5px; }
.markdown-body > *:first-child { margin-top: 0; }
.markdown-body > *:last-child { margin-bottom: 0; }
.markdown-body h2, .markdown-body h3 { font-size: 15px; font-weight: 700; color: #1e293b; margin: 18px 0 8px; padding-left: 10px; border-left: 3px solid #6366f1; line-height: 1.4; }
.markdown-body h2:first-child, .markdown-body h3:first-child { margin-top: 0; }
.markdown-body p { margin: 8px 0; color: #334155; }
.markdown-body ul { padding-left: 22px; margin: 8px 0; }
.markdown-body li { margin: 6px 0; color: #334155; line-height: 1.8; }
.markdown-body li::marker { color: #6366f1; }
.markdown-body strong { color: #4f46e5; font-weight: 600; }
.markdown-body code { background: rgba(99,102,241,.12); padding: 2px 6px; border-radius: 4px; font-size: 13px; color: #4f46e5; }
.markdown-body hr { border: none; border-top: 1px solid rgba(148,163,184,.25); margin: 16px 0; }
.markdown-body table { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 13.5px; border-radius: 8px; overflow: hidden; }
.markdown-body th { background: rgba(99,102,241,.12); color: #312e81; font-weight: 600; text-align: left; padding: 10px 14px; border: 1px solid rgba(99,102,241,.15); }
.markdown-body td { padding: 9px 14px; border: 1px solid rgba(148,163,184,.2); color: #334155; }
.markdown-body tr:nth-child(even) td { background: rgba(99,102,241,.04); }
.empty-tip { text-align: center; color: #64748b; font-size: 13.5px; padding: 60px 0; }
.msg-actions { display: flex; align-items: center; gap: 4px; margin-top: 6px; padding-left: 2px; }
.act-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border: none; background: transparent; color: #94a3b8; border-radius: 6px; cursor: pointer; transition: all .15s; }
.act-btn:hover { background: rgba(99,102,241,.12); color: #6366f1; }
.act-btn.active { color: #6366f1; background: rgba(99,102,241,.15); }
.act-time { margin-left: 8px; font-size: 12px; color: #94a3b8; }
.thinking-dots { display: flex; gap: 6px; padding: 4px 0; }
.thinking-dots span { width: 8px; height: 8px; border-radius: 50%; background: #94a3b8; animation: dotBlink 1.4s infinite ease-in-out; }
.thinking-dots span:nth-child(2) { animation-delay: .2s; }
.thinking-dots span:nth-child(3) { animation-delay: .4s; }
@keyframes dotBlink { 0%, 80%, 100% { opacity: .3; transform: scale(.8); } 40% { opacity: 1; transform: scale(1); } }
.msg-row.error .bubble { border-color: rgba(248,113,113,.4); background: rgba(248,113,113,.15); color: #fca5a5; }
.jump-btn { position: absolute; bottom: 24px; left: 50%; transform: translateX(-50%); padding: 8px 16px; border-radius: 20px; background: rgba(99,102,241,.9); color: #fff; border: none; cursor: pointer; font-size: 13px; box-shadow: 0 4px 20px rgba(99,102,241,.4); }
.chat-foot { flex-shrink: 0; padding: 0 28px 22px; }
.composer { max-width: 820px; margin: 0 auto; display: flex; align-items: flex-end; gap: 8px; background: rgba(255,255,255,.5); backdrop-filter: blur(30px) saturate(180%); -webkit-backdrop-filter: blur(30px) saturate(180%); border: 1px solid rgba(255,255,255,.6); border-radius: 16px; padding: 8px 8px 8px 14px; transition: all .2s; box-shadow: 0 8px 32px rgba(0,0,0,.08); }
.composer:focus-within { border-color: rgba(99,102,241,.4); box-shadow: 0 0 0 3px rgba(99,102,241,.15), 0 8px 32px rgba(0,0,0,.08); background: rgba(255,255,255,.65); }
.editor { flex: 1; min-height: 26px; max-height: 180px; padding: 8px 0; font-size: 14.5px; line-height: 1.6; font-family: inherit; color: #1e293b; background: transparent; border: none; outline: none; resize: none; overflow-y: hidden; }
.editor::placeholder { color: #94a3b8; }
.send-btn { width: 40px; height: 40px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border: none; border-radius: 12px; cursor: pointer; background: linear-gradient(135deg, #6366f1, #06b6d4); color: #fff; box-shadow: 0 2px 8px rgba(99,102,241,.3); }
.send-btn:hover { transform: translateY(-1px); }
.send-btn.disabled { opacity: .35; cursor: not-allowed; }
.foot-tip { max-width: 820px; margin: 10px auto 0; font-size: 11.5px; color: #64748b; text-align: center; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s, transform .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translate(-50%, 8px); }
:deep(.el-scrollbar__thumb) { background: rgba(100,116,139,.25); }
</style>
