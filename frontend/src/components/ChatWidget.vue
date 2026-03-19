<template>
  <div class="chat-widget" v-if="route.path !== '/'">
    <!-- 悬浮聊天入口按钮 -->
    <div class="chat-fab" @click="toggleChat" v-if="!isOpen">
      <el-icon :size="24" color="#fff"><Service /></el-icon>
      <span class="fab-text">智能问答</span>
    </div>

    <!-- 聊天主面板 -->
    <transition name="chat-slide">
      <div class="chat-panel" v-if="isOpen">
        <div class="chat-header">
          <div class="header-info">
            <el-icon :size="20"><LocationInformation /></el-icon>
            <span class="chat-title">旅游百事通助手</span>
          </div>
          <el-icon class="close-icon" @click="toggleChat"><Close /></el-icon>
        </div>

        <div class="chat-body" ref="chatBodyRef">
          <div class="msg-container">
            <template v-for="(msg, index) in messageList" :key="index">
              <!-- 左侧助手消息 -->
              <div v-if="msg.role === 'assistant'" class="msg-row msg-left">
                <div class="avatar bg-blue">AI</div>
                <div class="msg-bubble assistant-bubble">{{ msg.content }}</div>
              </div>
              
              <!-- 右侧用户消息 -->
              <div v-if="msg.role === 'user'" class="msg-row msg-right">
                <div class="msg-bubble user-bubble">{{ msg.content }}</div>
                <div class="avatar bg-gray">我</div>
              </div>
            </template>
            <div v-if="loading" class="msg-row msg-left">
              <div class="avatar bg-blue">AI</div>
              <div class="msg-bubble assistant-bubble loading-dots">正在思考中<span>.</span><span>.</span><span>.</span></div>
            </div>
          </div>
        </div>

        <!-- 快捷问答区 -->
        <div class="quick-tips" v-if="currentTips.length > 0">
          <el-tag 
            v-for="(tip, idx) in currentTips" 
            :key="idx" 
            size="small" 
            class="tip-tag" 
            effect="plain" 
            @click="sendQuestion(tip)">
            {{ tip }}
          </el-tag>
        </div>

        <div class="chat-footer">
          <el-input 
            v-model="inputVal" 
            placeholder="问问我成都哪好玩..." 
            @keyup.enter="handleSend"
            class="chat-input"
          >
            <template #append>
              <el-button @click="handleSend" icon="Position" type="primary" :disabled="!inputVal.trim() || loading"/>
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { reqAskChat } from '@/api/chat'
import { ElMessage } from 'element-plus'

const route = useRoute()
const isOpen = ref(false)
const inputVal = ref('')
const loading = ref(false)
const chatBodyRef = ref(null)

const messageList = ref([
  {
    role: 'assistant',
    content: '你好！我是你的专属成都旅游小助手。你可以问我关于历史文化、美食打卡、行程安排的任何问题哦！'
  }
])

const currentTips = ref([
  '成都哪些地方出片？',
  '武侯祠有什么历史？',
  '雨天适合去哪里？',
  '推荐地道火锅！'
])

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollToBottom()
  }
}

const sendQuestion = (q) => {
  inputVal.value = q
  handleSend()
}

const handleSend = async () => {
  if (!inputVal.value.trim() || loading.value) return
  
  const question = inputVal.value.trim()
  inputVal.value = ''
  
  // 压入用户消息
  messageList.value.push({ role: 'user', content: question })
  currentTips.value = [] // 发送中先清空提示
  scrollToBottom()
  
  loading.value = true
  try {
    const contextStr = sessionStorage.getItem('original_req_form')
    let contextObj = null
    if (contextStr) {
      try {
        contextObj = JSON.parse(contextStr)
      } catch (e) {
        console.error('Failed to parse context', e)
      }
    }
    const reqPayload = {
      question: question,
      context: contextObj
    }
    console.log('【前端拦截】1. 准备调用 api/chat/qa，参数:', reqPayload)
    
    const res = await reqAskChat(reqPayload)
    console.log('【前端拦截】2. 后端成功返回结果:', res)
    
    // 压入回复
    messageList.value.push({
      role: 'assistant',
      content: res.answer
    })
    
    if (res.relatedTips && res.relatedTips.length > 0) {
      currentTips.value = res.relatedTips
    } else {
      currentTips.value = ['还有哪些好玩的地方？', '带父母来怎么安排？']
    }
    
  } catch (err) {
    console.error('【前端拦截】3. 调用异常:', err)
    messageList.value.push({
      role: 'assistant',
      content: '抱歉，本地网络暂时断开，我没能听清你的问题。'
    })
    currentTips.value = ['重新连接']
  } finally {
    loading.value = false
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
  background: linear-gradient(135deg, #409EFF, #3a8ee6);
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
  color: #F56C6C;
}

.chat-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #fdfdfd;
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
.bg-blue { background: #409EFF; }
.bg-gray { background: #E6A23C; }

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
  background: #409EFF;
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

.chat-footer {
  padding: 12px 16px;
  background: #ffffff;
  border-top: 1px solid #ebeef5;
}

/* 简单的过渡动画 */
.chat-slide-enter-active, .chat-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.3s;
}
.chat-slide-enter-from, .chat-slide-leave-to {
  transform: translateY(20px) scale(0.95);
  opacity: 0;
}

/* loading 点阵动画 */
.loading-dots span {
  animation: typing 1.4s infinite ease-in-out both;
}
.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); opacity: 0; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
