const { reqCurrentUser, reqLogout } = require("../api/auth")
const { TOKEN_KEY, USER_KEY } = require("../utils/storage")

const authState = {
  user: null,
  initialized: false,
  loading: false
}

function getAuthState() {
  return authState
}

async function initAuthState() {
  if (authState.initialized || authState.loading) {
    return authState.user
  }

  authState.loading = true
  try {
    const token = wx.getStorageSync(TOKEN_KEY)
    if (!token) {
      throw new Error("No token")
    }

    const user = await reqCurrentUser()
    authState.user = user
    wx.setStorageSync(USER_KEY, user)

    const { restoreChatState } = require("./chat")
    restoreChatState(user)
    return user
  } catch (err) {
    authState.user = null
    wx.removeStorageSync(USER_KEY)
    const { clearActiveChatState } = require("./chat")
    clearActiveChatState()
    return null
  } finally {
    authState.loading = false
    authState.initialized = true
  }
}

function setAuthUser(user) {
  authState.user = user
  authState.initialized = true
  if (user && user.token) {
    wx.setStorageSync(TOKEN_KEY, user.token)
  }
  if (user) {
    wx.setStorageSync(USER_KEY, user)
  }

  const { restoreChatState } = require("./chat")
  restoreChatState(user)
}

async function clearAuthUser() {
  let success = true
  try {
    await reqLogout()
  } catch (err) {
    success = false
  } finally {
    authState.user = null
    authState.initialized = true
    wx.removeStorageSync(TOKEN_KEY)
    wx.removeStorageSync(USER_KEY)
    const { clearActiveChatState } = require("./chat")
    clearActiveChatState()
  }
  return success
}

function requireAuth(redirectPath) {
  if (authState.user) {
    return true
  }

  const nextPath = redirectPath || "/pages/home/index"
  wx.navigateTo({
    url: `/pages/auth/index?redirect=${encodeURIComponent(nextPath)}`
  })
  return false
}

module.exports = {
  getAuthState,
  initAuthState,
  setAuthUser,
  clearAuthUser,
  requireAuth
}
