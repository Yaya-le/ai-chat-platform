const TOKEN_KEY = 'ADMIN_TOKEN'

// 安全读取 localStorage：规避 SSR 环境（localStorage 不存在）及隐私模式异常
function safeGet(key) {
  try {
    return window.localStorage.getItem(key)
  } catch (e) {
    return null
  }
}

function safeSet(key, value) {
  try {
    window.localStorage.setItem(key, value)
  } catch (e) {
    // 隐私模式 / 配额溢出时静默失败，避免阻塞登录流程
  }
}

function safeRemove(key) {
  try {
    window.localStorage.removeItem(key)
  } catch (e) {
    // ignore
  }
}

export function getToken() {
  return safeGet(TOKEN_KEY)
}

export function setToken(token) {
  if (typeof token !== 'string' || token.length === 0) return
  if (/^https?:\/\//i.test(token) || /microsoft|login\.live/i.test(token)) {
    console.warn('[auth] 拒绝写入非法的 token 值:', token.slice(0, 40))
    return
  }
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  safeRemove(TOKEN_KEY)
}
