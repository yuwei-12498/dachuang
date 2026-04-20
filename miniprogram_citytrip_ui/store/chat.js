const { reqAskChat } = require("../api/chat")
const { CHAT_PREFIX } = require("../utils/storage")

const defaultAssistantMessage = "你好，我是你的旅行小助手。登录后，我可以结合你的时间和偏好，帮你规划更顺路、更省心的成都行程。"
const defaultTips = [
  "宽窄巷子最佳拍照点在哪？",
  "春熙路附近有什么值得逛的？",
  "雨天适合去哪里？",
  "带小朋友去哪里玩比较好？"
]
const fallbackTips = ["成都有哪些必吃美食？", "有什么适合一日游的路线？"]

const chatState = {
  messages: [
    {
      role: "assistant",
      content: defaultAssistantMessage
    }
  ],
  currentTips: defaultTips.slice(),
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

  chatState.messages = next.messages
  chatState.currentTips = next.currentTips
  chatState.loading = false
}

function persistChatState() {
  if (!activeStorageKey) {
    return
  }

  wx.setStorageSync(activeStorageKey, {
    messages: chatState.messages,
    currentTips: chatState.currentTips
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
    persistChatState()
    return true
  } catch (err) {
    if (!err || err.code !== 401) {
      chatState.messages.push({
        role: "assistant",
        content: "暂时无法连接到知识库，请稍后再试。"
      })
      chatState.currentTips = ["重新连接"]
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
