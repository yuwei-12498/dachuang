<template>
  <el-header class="app-navbar">
    <div class="navbar-container">
      <div class="navbar-left">
        <el-icon class="logo-icon" :size="24"><Location /></el-icon>
        <span class="logo-text">行“城”有数</span>
      </div>
      
      <!-- 只在首页显示锚点导航 -->
      <div class="navbar-center" v-if="isHome">
        <a href="#hero" class="nav-link" @click.prevent="scrollTo('#hero')">首页</a>
        <a href="#core" class="nav-link" @click.prevent="scrollTo('#core')">开始规划</a>
        <a href="#scenarios" class="nav-link" @click.prevent="scrollTo('#scenarios')">热门场景</a>
        <a href="#features" class="nav-link" @click.prevent="scrollTo('#features')">系统能力</a>
        <a href="#examples" class="nav-link" @click.prevent="scrollTo('#examples')">示例路线</a>
      </div>

      <div class="navbar-right">
        <el-button 
          v-if="isHome" 
          type="primary" 
          round 
          class="cta-btn"
          @click="scrollTo('#core')">
          立即体验
        </el-button>
        <el-button 
          v-else 
          type="default" 
          round 
          @click="router.push('/')">
          返回首页
        </el-button>
      </div>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Location } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const isHome = computed(() => route.path === '/')

const scrollTo = (selector) => {
  const el = document.querySelector(selector)
  if (el) {
    // 减去 navbar 的高度 64px 加上一点 padding
    const y = el.getBoundingClientRect().top + window.scrollY - 80
    window.scrollTo({ top: y, behavior: 'smooth' })
  }
}
</script>

<style scoped>
.app-navbar {
  position: sticky;
  top: 0;
  width: 100%;
  height: 64px !important;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(235, 238, 245, 0.8);
  z-index: 1000;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);
}

.navbar-container {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.logo-icon {
  color: #409EFF;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #1f2d3d;
}

.navbar-center {
  display: none;
}

@media (min-width: 768px) {
  .navbar-center {
    display: flex;
    align-items: center;
    gap: 32px;
  }
}

.nav-link {
  text-decoration: none;
  color: #606266;
  font-size: 15px;
  font-weight: 500;
  transition: color 0.3s;
}

.nav-link:hover {
  color: #409EFF;
}

.cta-btn {
  font-weight: 600;
  padding: 0 24px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}
</style>
