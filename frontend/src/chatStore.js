import { reactive } from 'vue'
import { reqAskChat } from '@/api/chat'

const defaultAssistantMessage = '你好，我是你的旅行小助手。登录后，我可以结合你的时间和偏好，帮你规划更顺路、更省心的成都行程。'

const defaultTips = [
  '宽窄巷子最佳拍照点在哪？',
  '春熙路附近有什么值得逛的？',
  '雨天适合去哪里？',
  '带小朋友去哪里玩比较好？'
]

const fallbackTips = ['成都有哪些必吃美食？', '有什么适合一日游的路线？']
const storagePrefix = 'trip_chat_state_'
let activeStorageKey = ''

const createDefaultMessages = () => ([
  {
    role: 'assistant',
    content: defaultAssistantMessage
  }
])

const createDefaultState = () => ({
  messages: createDefaultMessages(),
  currentTips: [...defaultTips],
  loading: false
})

const chatState = reactive(createDefaultState())

function canUseSessionStorage() {
  return typeof window !== 'undefined' && typeof window.sessionStorage !== 'undefined'
}

function buildStorageKey(user) {
  if (!user) {
    return ''
  }

  const identity = user.id ?? user.userId ?? user.username
  if (!identity) {
    return ''
  }

  return `${storagePrefix}${identity}`
}

function applyChatState(payload = {}) {
  const nextState = createDefaultState()

  if (Array.isArray(payload.messages) && payload.messages.length > 0) {
    nextState.messages = [...payload.messages]
  }

  if (Array.isArray(payload.currentTips) && payload.currentTips.length > 0) {
    nextState.currentTips = [...payload.currentTips]
  }

  chatState.messages = nextState.messages
  chatState.currentTips = nextState.currentTips
  chatState.loading = false
}

function persistChatState() {
  if (!activeStorageKey || !canUseSessionStorage()) {
    return
  }

  try {
    window.sessionStorage.setItem(activeStorageKey, JSON.stringify({
      messages: chatState.messages,
      currentTips: chatState.currentTips
    }))
  } catch (err) {
  }
}

function buildChatErrorMessage(err) {
  return err?.response?.data?.message || err?.message || '暂时无法连接到聊天服务，请稍后再试。'
}

export function useChatState() {
  return chatState
}

export function restoreChatState(user) {
  activeStorageKey = buildStorageKey(user)

  if (!activeStorageKey || !canUseSessionStorage()) {
    applyChatState()
    return
  }

  try {
    const raw = window.sessionStorage.getItem(activeStorageKey)
    if (raw) {
      applyChatState(JSON.parse(raw))
      return
    }
  } catch (err) {
  }

  applyChatState()
}

export function clearActiveChatState() {
  activeStorageKey = ''
  applyChatState()
}

export function resetChatState() {
  applyChatState()
  persistChatState()
}

export async function askChatQuestion(question, context) {
  const value = typeof question === 'string' ? question.trim() : ''
  if (!value || chatState.loading) {
    return false
  }

  chatState.messages.push({ role: 'user', content: value })
  chatState.currentTips = []
  chatState.loading = true
  persistChatState()

  try {
    const res = await reqAskChat({
      question: value,
      context
    })

    chatState.messages.push({
      role: 'assistant',
      content: res.answer
    })
    chatState.currentTips = res.relatedTips && res.relatedTips.length > 0
      ? [...res.relatedTips]
      : [...fallbackTips]
    persistChatState()
    return true
  } catch (err) {
    if (!err || err.code !== 401) {
      chatState.messages.push({
        role: 'assistant',
        content: buildChatErrorMessage(err)
      })
      chatState.currentTips = ['重新连接']
      persistChatState()
    }
    throw err
  } finally {
    chatState.loading = false
    persistChatState()
  }
}
