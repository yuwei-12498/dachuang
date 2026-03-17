<template>
  <div class="home-ai-panel">
    <div class="panel-header">
      <div class="ai-avatar">AI</div>
      <div class="ai-title-wrap">
        <h3 class="panel-title">行“城”向导</h3>
        <span class="panel-subtitle">你可以随时向我提问</span>
      </div>
    </div>

    <div class="panel-body" ref="chatBodyRef">
      <div class="msg-list">
        <template v-for="(msg, index) in messages" :key="index">
          <div class="msg-item" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
            <div class="bubble">{{ msg.content }}</div>
          </div>
        </template>
        <div v-if="loading" class="msg-item msg-ai">
          <div class="bubble loading-dots">思考中<span>.</span><span>.</span><span>.</span></div>
        </div>
      </div>
    </div>

    <div class="panel-quick-tips" v-if="quickTips.length > 0">
      <p class="tips-title">大家都在问：</p>
      <div class="tips-container">
        <el-tag 
          v-for="(tip, idx) in quickTips" 
          :key="idx" 
          class="tip-tag" 
          size="small" 
          effect="light"
          @click="sendQuestion(tip)">
          {{ tip }}
        </el-tag>
      </div>
    </div>

    <div class="panel-footer">
      <el-input 
        v-model="inputVal" 
        placeholder="例如：宽窄巷子最佳拍照点在哪？" 
        @keyup.enter="handleSend"
        class="chat-input"
        clearable
      >
        <template #append>
          <el-button @click="handleSend" type="primary" :disabled="!inputVal.trim() || loading">发送</el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { reqAskChat } from '@/api/chat'

// 接收外部传进来的当前表单实时数据，用于组合 Context
const props = defineProps({
  currentForm: {
    type: Object,
    default: () => ({})
  }
})

const chatBodyRef = ref(null)
const inputVal = ref('')
const loading = ref(false)

const messages = ref([
  {
    role: 'assistant',
    content: '你好！我是你的专属旅行顾问。在定制行程前，你有什么想了解的成都景点知识、游玩避坑指南都可以问我哦！'
  }
])

const quickTips = ref([
  '宽窄巷子最佳拍照点在哪？',
  '春熙路附近有什么值得逛的？',
  '雨天适合去哪里？',
  '带小朋友去哪里玩比较好？'
])

const sendQuestion = (q) => {
  inputVal.value = q
  handleSend()
}

const handleSend = async () => {
  if (!inputVal.value.trim() || loading.value) return
  
  const question = inputVal.value.trim()
  inputVal.value = ''
  
  messages.value.push({ role: 'user', content: question })
  quickTips.value = [] // 提问后清空快捷提示，等待新提示返回
  scrollToBottom()
  
  loading.value = true
  
  try {
    // 组装结构化上下文
    const ctx = {
      pageType: 'home',
      preferences: props.currentForm.themes || [],
      rainy: props.currentForm.isRainy || false,
      nightMode: props.currentForm.isNight || false,
      companionType: props.currentForm.companionType || ''
    }
    
    const res = await reqAskChat({
      question: question,
      context: ctx // 传入对象给后端进行针对性回答
    })
    
    messages.value.push({
      role: 'assistant',
      content: res.answer
    })
    
    if (res.relatedTips && res.relatedTips.length > 0) {
      quickTips.value = res.relatedTips
    } else {
      quickTips.value = ['成都有哪些必吃美食？', '有什么适合一日游的路线？']
    }
    
  } catch (err) {
    messages.value.push({
      role: 'assistant',
      content: '暂时无法连接到知识库，请稍后再试。'
    })
    quickTips.value = ['重新连接']
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
.home-ai-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 540px;
  background: linear-gradient(to bottom, #f8faff, #ffffff);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  margin-right: 12px;
  box-shadow: 0 4px 10px rgba(64,158,255,0.3);
}

.ai-title-wrap {
  display: flex;
  flex-direction: column;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.panel-subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.panel-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.msg-user .bubble {
  background-color: #409EFF;
  color: white;
  border-bottom-right-radius: 4px;
}

.msg-ai .bubble {
  background-color: #ffffff;
  color: #303133;
  border: 1px solid #ebeef5;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.panel-quick-tips {
  padding: 12px 20px;
  background: #fafbfc;
  border-top: 1px solid #ebeef5;
}

.tips-title {
  margin: 0 0 8px 0;
  font-size: 12px;
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
  color: #409EFF;
  transition: all 0.2s;
}

.tip-tag:hover {
  background-color: #409EFF;
  color: #ffffff;
}

.panel-footer {
  padding: 16px 20px;
  background: #ffffff;
  border-top: 1px solid #ebeef5;
}

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
