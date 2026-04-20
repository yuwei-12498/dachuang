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

    <ChatWidget />
  </el-config-provider>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import ChatWidget from '@/components/ChatWidget.vue'
import AppNavbar from '@/components/layout/AppNavbar.vue'
import { initAuthState } from '@/store/auth'

const locale = ref(zhCn)

onMounted(() => {
  initAuthState()
})
</script>

<style>
body {
  margin: 0;
  background-color: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
  color: #303133;
}

#app {
  min-height: 100vh;
}

html.lenis,
html.lenis body {
  height: auto;
}

.lenis.lenis-smooth {
  scroll-behavior: auto !important;
}

.lenis.lenis-stopped {
  overflow: hidden;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  padding: 0;
  width: 100%;
  flex: 1;
  overflow: visible !important;
}

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
