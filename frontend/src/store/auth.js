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
    authState.user = await reqCurrentUser()
    restoreChatState(authState.user)
    return authState.user
  } catch (err) {
    authState.user = null
    clearActiveChatState()
    return null
  } finally {
    authState.loading = false
    authState.initialized = true
  }
}

export function setAuthUser(user) {
  authState.user = user
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
    authState.initialized = true
    clearActiveChatState()
  }
  return success
}
