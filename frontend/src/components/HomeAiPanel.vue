<template>
  <div class="home-ai-panel" @wheel.stop.prevent="handleWheelScroll">
    <div class="panel-header">
      <div class="ai-avatar">AI</div>
      <div class="ai-title-wrap">
        <h3 class="panel-title">行“城”向导</h3>
        <span class="panel-subtitle">{{ authState.user ? '说说你的偏好，我来帮你理顺游玩思路' : '登录后即可获得更贴合你的路线建议' }}</span>
      </div>
    </div>

    <div class="panel-body" ref="chatBodyRef">
      <div class="msg-list">
        <template v-for="(msg, index) in chatState.messages" :key="index">
          <div class="msg-item" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
            <div class="bubble">{{ msg.content }}</div>
          </div>
        </template>
        <div v-if="chatState.loading" class="msg-item msg-ai">
          <div class="bubble loading-dots">思考中<span>.</span><span>.</span><span>.</span></div>
        </div>
      </div>
    </div>

    <div class="panel-quick-tips" v-if="chatState.currentTips.length > 0">
      <p class="tips-title">不妨先从这些问题开始：</p>
      <div class="tips-container">
        <el-tag
          v-for="(tip, idx) in chatState.currentTips"
          :key="idx"
          class="tip-tag"
          size="small"
          effect="light"
          @click="sendQuestion(tip)">
          {{ tip }}
        </el-tag>
      </div>
    </div>

    <div class="panel-footer" v-if="authState.user">
      <el-input
        v-model="inputVal"
        placeholder="例如：宽窄巷子最佳拍照点在哪？"
        @keyup.enter="handleSend"
        class="chat-input"
        clearable>
        <template #append>
          <el-button @click="handleSend" type="primary" :disabled="!inputVal.trim() || chatState.loading">发送</el-button>
        </template>
      </el-input>
    </div>

    <div v-else class="panel-footer login-footer">
      <p class="login-copy">登录后就能向 AI 询问景点灵感、路线建议和出行提醒，帮你把一天安排得更顺路。</p>
      <el-button type="primary" round class="login-btn" @click="goLogin">登录后再问</el-button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthState } from '@/store/auth'
import { askChatQuestion, useChatState } from '@/store/chat'

const props = defineProps({
  currentForm: {
    type: Object,
    default: () => ({})
  }
})

const route = useRoute()
const router = useRouter()
const authState = useAuthState()
const chatState = useChatState()
const chatBodyRef = ref(null)
const inputVal = ref('')

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
  ElMessage.warning('登录后即可向 AI 咨询路线灵感、景点建议和避坑提醒。')
  goLogin()
  return false
}

const buildContext = () => ({
  pageType: 'home',
  preferences: props.currentForm.themes || [],
  rainy: props.currentForm.isRainy || false,
  nightMode: props.currentForm.isNight || false,
  companionType: props.currentForm.companionType || ''
})

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
.home-ai-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: linear-gradient(to bottom, #f8faff, #ffffff);
  border-radius: 18px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  box-shadow: 0 16px 32px rgba(31, 45, 61, 0.06);
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  margin-right: 12px;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
}

.ai-title-wrap {
  display: flex;
  flex-direction: column;
}

.panel-title {
  margin: 0;
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.panel-subtitle {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.panel-body {
  flex: 1;
  min-height: 0;
  padding: 12px 14px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-item {
  display: flex;
  max-width: 85%;
}

.msg-user {
  align-self: flex-end;
  justify-content: flex-end;
}

.msg-ai {
  align-self: flex-start;
  justify-content: flex-start;
}

.bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-all;
}

.msg-user .bubble {
  background-color: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.msg-ai .bubble {
  background-color: #ffffff;
  color: #303133;
  border: 1px solid #ebeef5;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.panel-quick-tips {
  padding: 8px 14px;
  background: #fafbfc;
  border-top: 1px solid #ebeef5;
}

.tips-title {
  margin: 0 0 6px 0;
  font-size: 11px;
  color: #909399;
}

.tips-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tip-tag {
  cursor: pointer;
  border-radius: 14px;
  padding: 0 12px;
  border-color: #d9ecff;
  color: #409eff;
  transition: all 0.2s;
}

.tip-tag:hover {
  background-color: #409eff;
  color: #ffffff;
}

.panel-footer {
  padding: 10px 14px;
  background: #ffffff;
  border-top: 1px solid #ebeef5;
}

.login-footer {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}

.login-copy {
  margin: 0;
  color: #6a7a8d;
  line-height: 1.6;
  font-size: 13px;
}

.login-btn {
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.16);
}

@media (max-width: 991px) {
  .home-ai-panel {
    min-height: 420px;
  }
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
