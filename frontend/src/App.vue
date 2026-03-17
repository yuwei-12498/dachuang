<template>
  <el-config-provider :locale="locale">
    <el-container class="app-container" direction="vertical">
      <AppNavbar />
      
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    
    <!-- 全局悬浮 AI 旅行助手，供除了首页右侧面板外的页面独立互动 -->
    <ChatWidget />
  </el-config-provider>
</template>

<script setup>
import { ref } from 'vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import ChatWidget from '@/components/ChatWidget.vue'
import AppNavbar from '@/components/layout/AppNavbar.vue'

const locale = ref(zhCn)
</script>

<style>
/* 恢复默认鼠标和极简舒适的背景 */
body {
  margin: 0;
  background-color: #f7f8fa; /* 极浅的灰白底色，强调主卡片的纯白感 */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
  color: #303133;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  /* Landing Page 模式下不限制最大宽度，由各个 Section 内部控制内容宽度 */
  padding: 0;
  width: 100%;
  flex: 1;
}

/* 优雅的页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
