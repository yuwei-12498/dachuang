<template>
  <el-header class="app-navbar">
    <div class="navbar-container">
      <div class="navbar-left" @click="router.push('/')">
        <el-icon class="logo-icon" :size="24"><Location /></el-icon>
        <span class="logo-text">行“城”有数</span>
      </div>

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
          class="ghost-btn"
          @click="router.push('/')">
          返回首页
        </el-button>

        <el-dropdown
          v-if="authState.user"
          trigger="click"
          placement="bottom-end"
          class="user-dropdown"
          @command="handleUserCommand">
          <div class="user-entry">
            <span class="user-avatar">{{ userInitial }}</span>
            <span class="user-name">{{ displayName }}</span>
            <el-icon class="user-arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                当前账号：{{ authState.user.username }}
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button
          v-else
          round
          class="auth-btn"
          @click="goAuth">
          登录 / 注册
        </el-button>
      </div>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Location } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuthUser, useAuthState } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authState = useAuthState()

const isHome = computed(() => route.path === '/')
const displayName = computed(() => authState.user?.nickname || authState.user?.username || '')
const userInitial = computed(() => {
  const value = displayName.value
  return value ? value.slice(0, 1).toUpperCase() : 'U'
})

const scrollTo = (selector) => {
  const el = document.querySelector(selector)
  if (el) {
    const y = el.getBoundingClientRect().top + window.scrollY - 80
    window.scrollTo({ top: y, behavior: 'smooth' })
  }
}

const goAuth = () => {
  const redirect = route.fullPath === '/auth' ? '/' : route.fullPath
  router.push({
    path: '/auth',
    query: {
      redirect
    }
  })
}

const handleUserCommand = async (command) => {
  if (command !== 'logout') {
    return
  }

  await clearAuthUser()
  ElMessage.success('已退出登录')
  if (route.path === '/auth') {
    router.replace('/')
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
  gap: 20px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-icon {
  color: #409eff;
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
  color: #409eff;
}

.navbar-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-shrink: 0;
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

.ghost-btn,
.auth-btn {
  border-radius: 999px;
  border-color: #d9e6f7;
  color: #315170;
  background: rgba(255, 255, 255, 0.92);
}

.auth-btn:hover,
.ghost-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.user-dropdown {
  display: flex;
  align-items: center;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 0 14px 0 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.12), rgba(102, 177, 255, 0.2));
  border: 1px solid rgba(64, 158, 255, 0.18);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.user-entry:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.22);
}

.user-name {
  color: #1f2d3d;
  font-size: 14px;
  font-weight: 600;
  max-width: 88px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-arrow {
  color: #6f87a0;
  font-size: 12px;
}

@media (max-width: 767px) {
  .navbar-container {
    padding: 0 16px;
  }

  .logo-text {
    font-size: 18px;
  }

  .navbar-right {
    gap: 8px;
  }

  .cta-btn,
  .ghost-btn,
  .auth-btn {
    padding: 0 16px;
  }

  .user-name {
    display: none;
  }
}
</style>
