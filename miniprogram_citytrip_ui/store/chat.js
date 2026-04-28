const { reqAskChat } = require("../api/chat")
const { CHAT_PREFIX } = require("../utils/storage")

const defaultAssistantMessage = "Hi! I am your trip assistant. Ask me about route planning, transport, or stop suggestions."
const defaultTips = [
  "Which stop is best for photos?",
  "What should I visit near Chunxi Road?",
  "Where should I go on a rainy day?",
  "Any kid-friendly attractions?"
]
const fallbackTips = ["What are must-eat foods in Chengdu?", "Can you suggest a one-day route?"]

const chatState = {
  messages: [
    {
      role: "assistant",
      content: defaultAssistantMessage
    }
  ],
  currentTips: defaultTips.slice(),
  currentEvidence: [],
  loading: false
}

let activeStorageKey = ""

function cloneDefaultState() {
  return {
    messages: [
      {
        role: "assistant",
        content: defaultAssistantMessage
      }
    ],
    currentTips: defaultTips.slice(),
    currentEvidence: [],
    loading: false
  }
}

function getChatState() {
  return chatState
}

function buildStorageKey(user) {
  if (!user) {
    return ""
  }
  const identity = user.id || user.userId || user.username
  return identity ? `${CHAT_PREFIX}${identity}` : ""
}

function applyChatState(payload) {
  const next = cloneDefaultState()
  if (payload && Array.isArray(payload.messages) && payload.messages.length) {
    next.messages = payload.messages.slice()
  }
  if (payload && Array.isArray(payload.currentTips) && payload.currentTips.length) {
    next.currentTips = payload.currentTips.slice()
  }
  if (payload && Array.isArray(payload.currentEvidence) && payload.currentEvidence.length) {
    next.currentEvidence = payload.currentEvidence.slice()
  }

  chatState.messages = next.messages
  chatState.currentTips = next.currentTips
  chatState.currentEvidence = next.currentEvidence
  chatState.loading = false
}

function persistChatState() {
  if (!activeStorageKey) {
    return
  }

  wx.setStorageSync(activeStorageKey, {
    messages: chatState.messages,
    currentTips: chatState.currentTips,
    currentEvidence: chatState.currentEvidence
  })
}

function restoreChatState(user) {
  activeStorageKey = buildStorageKey(user)
  if (!activeStorageKey) {
    applyChatState()
    return
  }
  const saved = wx.getStorageSync(activeStorageKey)
  if (saved) {
    applyChatState(saved)
    return
  }
  applyChatState()
}

function clearActiveChatState() {
  activeStorageKey = ""
  applyChatState()
}

function resetChatState() {
  applyChatState()
  persistChatState()
}

async function askChatQuestion(question, context) {
  const value = typeof question === "string" ? question.trim() : ""
  if (!value || chatState.loading) {
    return false
  }

  chatState.messages.push({ role: "user", content: value })
  chatState.currentTips = []
  chatState.currentEvidence = []
  chatState.loading = true
  persistChatState()

  try {
    const res = await reqAskChat({
      question: value,
      context
    })

    chatState.messages.push({
      role: "assistant",
      content: res.answer
    })
    chatState.currentTips = Array.isArray(res.relatedTips) && res.relatedTips.length
      ? res.relatedTips.slice()
      : fallbackTips.slice()
    chatState.currentEvidence = Array.isArray(res.evidence)
      ? res.evidence.slice()
      : []
    persistChatState()
    return true
  } catch (err) {
    if (!err || err.code !== 401) {
      chatState.messages.push({
        role: "assistant",
        content: "I cannot connect right now. Please try again shortly."
      })
      chatState.currentTips = ["Retry"]
      chatState.currentEvidence = []
      persistChatState()
    }
    throw err
  } finally {
    chatState.loading = false
    persistChatState()
  }
}

module.exports = {
  getChatState,
  restoreChatState,
  clearActiveChatState,
  resetChatState,
  askChatQuestion
}