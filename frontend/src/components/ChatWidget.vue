<template>
  <div class="chat-widget" v-if="showWidget">
    <div class="chat-fab" @click="toggleChat" v-if="!isOpen">
      <el-icon :size="24" color="#fff"><Service /></el-icon>
      <span class="fab-text">问问行程助手</span>
    </div>

    <transition name="chat-slide">
      <div class="chat-panel" v-if="isOpen" @wheel.stop.prevent="handleWheelScroll">
        <div class="chat-header">
          <div class="header-info">
            <el-icon :size="20"><LocationInformation /></el-icon>
            <span class="chat-title">路线灵感助手</span>
          </div>
          <el-icon class="close-icon" @click="toggleChat"><Close /></el-icon>
        </div>

        <div class="chat-body" ref="chatBodyRef">
          <div class="msg-container">
            <template v-for="(msg, index) in chatState.messages" :key="index">
              <div v-if="msg.role === 'assistant'" class="msg-row msg-left">
                <div class="avatar bg-blue">AI</div>
                <div class="msg-bubble assistant-bubble">{{ msg.content }}</div>
              </div>

              <div v-if="msg.role === 'user'" class="msg-row msg-right">
                <div class="msg-bubble user-bubble">{{ msg.content }}</div>
                <div class="avatar bg-gray">我</div>
              </div>
            </template>
            <div v-if="chatState.loading" class="msg-row msg-left">
              <div class="avatar bg-blue">AI</div>
              <div class="msg-bubble assistant-bubble loading-dots">正在帮你梳理路线<span>.</span><span>.</span><span>.</span></div>
            </div>
          </div>
        </div>

        <div class="quick-tips" v-if="chatState.currentTips.length > 0">
          <el-tag
            v-for="(tip, idx) in chatState.currentTips"
            :key="idx"
            size="small"
            class="tip-tag"
            effect="plain"
            @click="sendQuestion(tip)"
          >
            {{ tip }}
          </el-tag>
        </div>

        <div class="quick-tips" v-if="chatState.currentEvidence.length > 0">
          <el-tag
            v-for="(item, idx) in chatState.currentEvidence"
            :key="`evidence-${idx}`"
            size="small"
            class="tip-tag evidence-tag"
            effect="plain"
          >
            {{ item }}
          </el-tag>
        </div>

        <div class="chat-footer">
          <el-input
            v-model="inputVal"
            placeholder="想问哪里更适合你？我来帮你想路线..."
            @keyup.enter="handleSend"
            class="chat-input"
          >
            <template #append>
              <el-button @click="handleSend" icon="Position" type="primary" :disabled="!inputVal.trim() || chatState.loading" />
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthState } from '@/store/auth'
import { askChatQuestion, useChatState } from '@/store/chat'
import { buildSharedChatContext } from '@/utils/chatContext'

const route = useRoute()
const router = useRouter()
const authState = useAuthState()
const chatState = useChatState()
const showWidget = computed(() => route.path !== '/' && !route.meta.hideGlobalChat)
const isOpen = ref(false)
const inputVal = ref('')
const chatBodyRef = ref(null)

const goLogin = () => {
  router.push({
    path: '/auth',
    query: {
      redirect: route.fullPath
    }
  })
}

const ensureLogin = () => {
  if (authState.user) {
    return true
  }
  ElMessage.warning('登录后即可继续向 AI 咨询路线和景点建议。')
  goLogin()
  return false
}

const buildContext = () => buildSharedChatContext({
  pageType: route.name ? String(route.name).toLowerCase() : 'page'
})

const toggleChat = () => {
  if (!authState.user) {
    ensureLogin()
    return
  }
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollToBottom()
  }
}

const sendQuestion = (q) => {
  if (!ensureLogin()) return
  inputVal.value = q
  handleSend()
}

const handleSend = async () => {
  if (!ensureLogin()) return

  const question = inputVal.value.trim()
  if (!question || chatState.loading) return

  inputVal.value = ''

  try {
    await askChatQuestion(question, buildContext())
  } catch (err) {
    if (err && err.code === 401) {
      ElMessage.warning('登录状态已失效，请重新登录后继续提问。')
      goLogin()
    }
  } finally {
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

const handleWheelScroll = (event) => {
  const container = chatBodyRef.value
  if (!container) return

  const maxScrollTop = container.scrollHeight - container.clientHeight
  if (maxScrollTop <= 0) return

  const nextScrollTop = Math.max(0, Math.min(maxScrollTop, container.scrollTop + event.deltaY))
  container.scrollTop = nextScrollTop
}

watch(() => chatState.messages.length, () => {
  scrollToBottom()
})

watch(() => chatState.streamTick, () => {
  scrollToBottom()
})

watch(() => chatState.loading, () => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-widget {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.chat-fab {
  background: linear-gradient(135deg, #409eff, #3a8ee6);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  color: white;
  height: 54px;
  border-radius: 27px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.chat-fab:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.6);
}

.fab-text {
  margin-left: 8px;
  font-weight: 600;
  font-size: 15px;
}

.chat-panel {
  width: 380px;
  height: 550px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.chat-header {
  height: 56px;
  background: #f7f8fa;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2d3d;
}

.chat-title {
  font-weight: 600;
  font-size: 15px;
}

.close-icon {
  cursor: pointer;
  font-size: 20px;
  color: #909399;
  transition: color 0.2s;
}

.close-icon:hover {
  color: #f56c6c;
}

.chat-body {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow-y: auto;
  background: #fdfdfd;
  overscroll-behavior: contain;
}

.msg-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.msg-left {
  justify-content: flex-start;
}

.msg-right {
  justify-content: flex-end;
}

.avatar {
  min-width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
  color: white;
}

.bg-blue {
  background: #409eff;
}

.bg-gray {
  background: #e6a23c;
}

.msg-bubble {
  max-width: 240px;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
}

.assistant-bubble {
  background: #f4f4f5;
  color: #303133;
  border-top-left-radius: 2px;
}

.user-bubble {
  background: #409eff;
  color: #ffffff;
  border-top-right-radius: 2px;
}

.quick-tips {
  padding: 10px 16px;
  background: #fdfdfd;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-top: 1px solid #f0f2f5;
}

.tip-tag {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s;
}

.tip-tag:hover {
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.evidence-tag {
  cursor: default;
}

.evidence-tag:hover {
  background: inherit;
  border-color: inherit;
}

.chat-footer {
  padding: 12px 16px;
  background: #ffffff;
  border-top: 1px solid #ebeef5;
}

.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.3s;
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  transform: translateY(20px) scale(0.95);
  opacity: 0;
}

.loading-dots span {
  animation: typing 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0;
  }

  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
