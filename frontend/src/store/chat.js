import { reactive } from 'vue'
import { reqStreamChat } from '@/api/chat'

const defaultAssistantMessage = '你好！我是你的旅行小助手。你可以问我附近有什么、怎么去、适合什么主题路线等问题。'
const defaultTips = [
  '宽窄巷子最佳拍照点在哪？',
  '春熙路附近有什么值得逛的？',
  '雨天适合去哪儿？',
  '带小朋友去哪里比较好？'
]
const fallbackTips = ['成都有哪些必吃美食？', '有什么适合一日游的路线？']
const reconnectTips = ['重新连接']
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
  currentEvidence: [],
  loading: false,
  streamTick: 0
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

  if (Array.isArray(payload.currentEvidence) && payload.currentEvidence.length > 0) {
    nextState.currentEvidence = [...payload.currentEvidence]
  }

  chatState.messages = nextState.messages
  chatState.currentTips = nextState.currentTips
  chatState.currentEvidence = nextState.currentEvidence
  chatState.loading = false
  chatState.streamTick = 0
}

function persistChatState() {
  if (!activeStorageKey || !canUseSessionStorage()) {
    return
  }

  try {
    window.sessionStorage.setItem(activeStorageKey, JSON.stringify({
      messages: chatState.messages,
      currentTips: chatState.currentTips,
      currentEvidence: chatState.currentEvidence
    }))
  } catch (err) {
  }
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

function touchStream() {
  chatState.streamTick += 1
}

function buildChatErrorMessage(err) {
  return err?.response?.data?.message || err?.message || '暂时无法连接到聊天服务，请稍后再试。'
}

function resolveTips(candidate) {
  return Array.isArray(candidate) && candidate.length > 0 ? [...candidate] : [...fallbackTips]
}

function resolveEvidence(candidate) {
  return Array.isArray(candidate) ? candidate.filter(Boolean).slice(0, 8) : []
}

export async function askChatQuestion(question, context) {
  const value = typeof question === 'string' ? question.trim() : ''
  if (!value || chatState.loading) {
    return false
  }

  chatState.messages.push({ role: 'user', content: value })
  chatState.currentTips = []
  chatState.currentEvidence = []
  chatState.loading = true
  touchStream()
  persistChatState()

  try {
    let assistantMessage = null
    const result = await reqStreamChat(
      {
        question: value,
        context
      },
      {
        onToken: (token) => {
          if (!token) {
            return
          }

          if (!assistantMessage) {
            assistantMessage = {
              role: 'assistant',
              content: ''
            }
            chatState.messages.push(assistantMessage)
          }

          assistantMessage.content += token
          touchStream()
        },
        onMeta: ({ relatedTips, evidence }) => {
          chatState.currentTips = resolveTips(relatedTips)
          chatState.currentEvidence = resolveEvidence(evidence)
          touchStream()
        }
      }
    )

    if (!assistantMessage) {
      assistantMessage = {
        role: 'assistant',
        content: result.answer || ''
      }
      chatState.messages.push(assistantMessage)
    } else if (!assistantMessage.content.trim() && result.answer) {
      assistantMessage.content = result.answer
    }

    if (!assistantMessage.content.trim()) {
      assistantMessage.content = '暂时没有生成有效回复，请稍后再试。'
    }

    if (!chatState.currentTips.length) {
      chatState.currentTips = resolveTips(result.relatedTips)
    }
    if (!chatState.currentEvidence.length) {
      chatState.currentEvidence = resolveEvidence(result.evidence)
    }

    touchStream()
    persistChatState()
    return true
  } catch (err) {
    if (!err || err.code !== 401) {
      chatState.messages.push({
        role: 'assistant',
        content: buildChatErrorMessage(err)
      })
      chatState.currentTips = [...reconnectTips]
      chatState.currentEvidence = []
      touchStream()
      persistChatState()
    }
    throw err
  } finally {
    chatState.loading = false
    touchStream()
    persistChatState()
  }
}
