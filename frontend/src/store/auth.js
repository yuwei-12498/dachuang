import { reactive } from 'vue'
import { reqCurrentUser, reqLogout } from '@/api/auth'
import { clearActiveChatState, restoreChatState } from '@/store/chat'

const authState = reactive({
  user: null,
  initialized: false,
  loading: false
})

export function useAuthState() {
  return authState
}

export async function initAuthState() {
  if (authState.initialized || authState.loading) {
    return authState.user
  }

  authState.loading = true
  try {
    const token = localStorage.getItem('jwt_token')
    if (!token) {
      authState.user = null
      clearActiveChatState()
      return null
    }

    const user = await reqCurrentUser()
    authState.user = user
    if (user?.token) {
      localStorage.setItem('jwt_token', user.token)
    }
    restoreChatState(user)
    return user
  } catch (err) {
    authState.user = null
    localStorage.removeItem('jwt_token')
    clearActiveChatState()
    return null
  } finally {
    authState.loading = false
    authState.initialized = true
  }
}

export function setAuthUser(user) {
  authState.user = user
  if (user && user.token) {
    localStorage.setItem('jwt_token', user.token)
  }
  authState.initialized = true
  restoreChatState(user)
}

export async function clearAuthUser() {
  let success = true
  try {
    await reqLogout()
  } catch (err) {
    success = false
  } finally {
    authState.user = null
    localStorage.removeItem('jwt_token')
    authState.initialized = true
    clearActiveChatState()
  }
  return success
}
