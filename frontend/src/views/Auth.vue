<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-intro">
        <div class="intro-badge">登录后开启专属推荐</div>
        <h1 class="intro-title">
          让每一次出发
          <span>都更懂你的期待</span>
        </h1>
        <p class="intro-desc">
          登录后，你可以向 AI 询问路线灵感、生成更贴合偏好的推荐行程，也能把喜欢的玩法慢慢沉淀下来。
        </p>

        <div class="intro-points">
          <div class="point-item">
            <div class="point-icon">01</div>
            <div>
              <h3>推荐更贴心</h3>
              <p>告诉我们你想怎么玩，系统会尽量为你安排更顺路、更省心的游玩节奏。</p>
            </div>
          </div>
          <div class="point-item">
            <div class="point-icon">02</div>
            <div>
              <h3>出行更省心</h3>
              <p>登录后就能继续和 AI 对话、生成推荐路线，让你的旅行思路始终连贯。</p>
            </div>
          </div>
          <div class="point-item">
            <div class="point-icon">03</div>
            <div>
              <h3>旅程可慢慢完善</h3>
              <p>后续你还可以保存喜欢的路线、记录评分与分享心得，让每次出发都更从容。</p>
            </div>
          </div>
        </div>
      </section>

      <el-card class="auth-card" shadow="never">
        <div class="card-head">
          <div class="head-kicker">开启专属旅程</div>
          <h2>{{ activeTab === 'login' ? '登录后继续规划' : '创建账号，开始你的路线灵感' }}</h2>
          <p>{{ activeTab === 'login' ? '登录后即可向 AI 询问路线建议，并生成你的专属行程。' : '注册成功后将自动登录，立即解锁 AI 问答与行程推荐。' }}</p>
        </div>

        <el-tabs v-model="activeTab" stretch class="auth-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              size="large"
              class="auth-form">
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  autocomplete="username"
                  @keyup.enter="handleLogin" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                  autocomplete="current-password"
                  @keyup.enter="handleLogin" />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="loginLoading"
                @click="handleLogin">
                登录并开始规划
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              label-position="top"
              size="large"
              class="auth-form">
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="registerForm.username"
                  placeholder="4~20 位字母、数字或下划线"
                  autocomplete="username" />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  placeholder="请输入显示昵称"
                  autocomplete="nickname" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  show-password
                  placeholder="6~20 位密码"
                  autocomplete="new-password" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入密码"
                  autocomplete="new-password"
                  @keyup.enter="handleRegister" />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="registerLoading"
                @click="handleRegister">
                注册并开启推荐
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="switch-tip">
          <span>{{ activeTab === 'login' ? '还没有账号？' : '已经有账号？' }}</span>
          <button class="switch-link" @click="toggleTab">
            {{ activeTab === 'login' ? '去注册' : '去登录' }}
          </button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { reqLogin, reqRegister } from '@/api/auth'
import { initAuthState, setAuthUser, useAuthState } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authState = useAuthState()

const activeTab = ref(route.query.mode === 'register' ? 'register' : 'login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const redirectPath = computed(() => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  return redirect && redirect !== '/auth' ? redirect : '/'
})

watch(
  () => route.query.mode,
  (mode) => {
    activeTab.value = mode === 'register' ? 'register' : 'login'
  }
)

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需为 4 到 20 位', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度需为 2 到 20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为 6 到 20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const navigateAfterAuth = () => {
  router.replace(redirectPath.value)
}

const toggleTab = () => {
  activeTab.value = activeTab.value === 'login' ? 'register' : 'login'
}

const handleLogin = async () => {
  if (!loginFormRef.value) {
    return
  }

  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loginLoading.value = true
  try {
    const user = await reqLogin(loginForm)
    setAuthUser(user)
    ElMessage.success('登录成功')
    navigateAfterAuth()
  } catch (err) {
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) {
    return
  }

  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  registerLoading.value = true
  try {
    const user = await reqRegister({
      username: registerForm.username,
      nickname: registerForm.nickname,
      password: registerForm.password
    })
    setAuthUser(user)
    ElMessage.success('注册成功，已自动登录')
    navigateAfterAuth()
  } catch (err) {
  } finally {
    registerLoading.value = false
  }
}

onMounted(async () => {
  await initAuthState()
  if (authState.user) {
    navigateAfterAuth()
  }
})
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 64px);
  padding: 48px 24px 64px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.12), transparent 34%),
    linear-gradient(180deg, #f6f9fe 0%, #f7f8fa 100%);
}

.auth-shell {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 460px);
  gap: 32px;
  align-items: stretch;
}

.auth-intro {
  position: relative;
  overflow: hidden;
  padding: 44px 44px 40px;
  border-radius: 24px;
  background: linear-gradient(145deg, #ffffff, #f4f9ff);
  border: 1px solid rgba(217, 230, 247, 0.9);
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.06);
}

.auth-intro::after {
  content: '';
  position: absolute;
  right: -120px;
  top: -120px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.16) 0%, rgba(64, 158, 255, 0) 72%);
}

.intro-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #2d79c7;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 24px;
}

.intro-title {
  margin: 0;
  font-size: 46px;
  line-height: 1.18;
  color: #1f2d3d;
  max-width: 560px;
}

.intro-title span {
  display: inline-block;
  color: transparent;
  background: linear-gradient(90deg, #409eff, #66b1ff);
  background-clip: text;
  -webkit-background-clip: text;
}

.intro-desc {
  margin: 22px 0 34px;
  max-width: 560px;
  font-size: 17px;
  line-height: 1.7;
  color: #5f6f82;
}

.intro-points {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.point-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(223, 232, 244, 0.9);
}

.point-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  box-shadow: 0 10px 20px rgba(64, 158, 255, 0.18);
}

.point-item h3 {
  margin: 2px 0 8px;
  font-size: 17px;
  color: #1f2d3d;
}

.point-item p {
  margin: 0;
  line-height: 1.7;
  color: #647588;
  font-size: 14px;
}

.auth-card {
  border: none;
  border-radius: 24px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 48px rgba(31, 45, 61, 0.08);
}

.card-head {
  padding: 16px 18px 0;
}

.head-kicker {
  display: inline-flex;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  align-items: center;
  background: #f1f7ff;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.card-head h2 {
  margin: 18px 0 10px;
  font-size: 30px;
  color: #1f2d3d;
}

.card-head p {
  margin: 0;
  color: #738397;
  line-height: 1.6;
}

.auth-tabs {
  margin-top: 18px;
}

.auth-form {
  padding: 10px 18px 0;
}

.auth-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475669;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px rgba(220, 223, 230, 0.7) inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.submit-btn {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 10px 18px rgba(64, 158, 255, 0.2);
}

.switch-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 18px 20px;
  color: #738397;
  font-size: 14px;
}

.switch-link {
  border: none;
  background: none;
  color: #409eff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}

@media (max-width: 1024px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .intro-title {
    font-size: 38px;
  }
}

@media (max-width: 767px) {
  .auth-page {
    padding: 28px 16px 40px;
  }

  .auth-intro,
  .auth-card {
    border-radius: 20px;
  }

  .auth-intro {
    padding: 28px 24px;
  }

  .intro-title {
    font-size: 30px;
  }

  .intro-desc {
    font-size: 15px;
  }
}
</style>
