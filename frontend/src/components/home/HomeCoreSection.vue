<template>
  <section class="core-section" id="core">
    <div class="core-container">
      <div class="section-header text-center">
        <h2 class="section-title">{{ text.title }}</h2>
        <p class="section-subtitle">{{ text.subtitle }}</p>
      </div>

      <div class="main-card">
        <el-row :gutter="28" class="core-layout">
          <el-col :md="14" :lg="15" class="form-col">
            <div ref="formPaneRef" class="form-pane">
              <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="custom-form">
                <div class="form-section">
                  <div class="sub-section-title">{{ text.sectionBasic }}</div>
                  <el-row :gutter="24">
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.tripMode" prop="tripDays">
                        <el-radio-group v-model="form.tripDays" class="full-width-radio">
                          <el-radio-button
                            v-for="option in tripDayOptions"
                            :key="option.value"
                            :label="option.value"
                          >
                            {{ option.label }}
                          </el-radio-button>
                        </el-radio-group>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.tripDate" prop="tripDate">
                        <el-date-picker
                          v-model="form.tripDate"
                          type="date"
                          value-format="YYYY-MM-DD"
                          :placeholder="text.tripDatePlaceholder"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row :gutter="24">
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.startTime" prop="startTime">
                        <el-time-select
                          v-model="form.startTime"
                          start="06:00"
                          step="00:30"
                          end="14:00"
                          :placeholder="text.startTime"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.endTime" prop="endTime">
                        <el-time-select
                          v-model="form.endTime"
                          :min-time="form.startTime"
                          start="11:00"
                          step="00:30"
                          end="23:30"
                          :placeholder="text.endTime"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row :gutter="24">
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.budget" prop="budgetLevel">
                        <el-select v-model="form.budgetLevel" :placeholder="text.budgetPlaceholder" style="width: 100%">
                          <el-option
                            v-for="option in budgetOptions"
                            :key="option.value"
                            :label="option.label"
                            :value="option.value"
                          />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>

                <el-divider border-style="dashed" />

                <div class="form-section">
                  <div class="sub-section-title">{{ text.sectionPreference }}</div>

                  <el-form-item :label="text.themes" prop="themes">
                    <el-checkbox-group v-model="form.themes" class="theme-checkbox-group">
                      <el-checkbox-button
                        v-for="option in themeOptions"
                        :key="option.value"
                        :label="option.value"
                      >
                        {{ option.label }}
                      </el-checkbox-button>
                    </el-checkbox-group>
                  </el-form-item>

                  <el-row :gutter="24" class="compact-row">
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.companion" prop="companionType">
                        <el-radio-group v-model="form.companionType">
                          <el-radio
                            v-for="option in companionOptions"
                            :key="option.value"
                            :label="option.value"
                            border
                          >
                            {{ option.label }}
                          </el-radio>
                        </el-radio-group>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.walking" prop="walkingLevel">
                        <el-radio-group v-model="form.walkingLevel">
                          <el-radio
                            v-for="option in walkingOptions"
                            :key="option.value"
                            :label="option.value"
                          >
                            {{ option.label }}
                          </el-radio>
                        </el-radio-group>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>

                <el-divider border-style="dashed" />

                <div class="form-section">
                  <div class="sub-section-title">{{ text.sectionScene }}</div>
                  <el-row :gutter="24">
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.rainy" class="switch-item">
                        <el-switch v-model="form.isRainy" :active-text="text.rainyOn" :inactive-text="text.rainyOff" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item :label="text.night" class="switch-item">
                        <el-switch v-model="form.isNight" :active-text="text.nightOn" :inactive-text="text.nightOff" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>

                <div class="form-actions">
                  <div v-if="!authState.user" class="login-reminder">{{ text.loginReminder }}</div>
                  <el-button
                    type="primary"
                    size="large"
                    class="submit-btn"
                    @click="onSubmit"
                    :loading="loading"
                  >
                    {{ authState.user ? text.submit : text.submitNeedLogin }}
                  </el-button>
                </div>
              </el-form>
            </div>
          </el-col>

          <el-col :md="10" :lg="9" class="ai-panel-col">
            <HomeAiPanel :currentForm="form" :style="aiPanelStyle" />
          </el-col>
        </el-row>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { reqGenerateItinerary } from '@/api/itinerary'
import HomeAiPanel from '@/components/HomeAiPanel.vue'
import { useAuthState } from '@/store/auth'
import { getDefaultTripDate, saveItinerarySnapshot } from '@/store/itinerary'

const text = {
  title: '\u5B9A\u5236\u4E13\u5C5E\u884C\u7A0B',
  subtitle: '\u544A\u8BC9\u6211\u4EEC\u4F60\u7684\u65F6\u95F4\u3001\u9884\u7B97\u548C\u504F\u597D\uFF0C\u7CFB\u7EDF\u4F1A\u5E2E\u4F60\u6574\u7406\u4E00\u6761\u66F4\u987A\u8DEF\u3001\u66F4\u7701\u5FC3\u7684\u6210\u90FD\u73A9\u6CD5\u3002',
  sectionBasic: '\u57FA\u7840\u65F6\u95F4\u5B89\u6392',
  sectionPreference: '\u504F\u597D\u8BBE\u7F6E',
  sectionScene: '\u573A\u666F\u63A2\u7D22',
  tripMode: '\u51FA\u884C\u6A21\u5F0F',
  tripDate: '\u51FA\u884C\u65E5\u671F',
  tripDatePlaceholder: '\u9009\u62E9\u51FA\u884C\u65E5\u671F',
  startTime: '\u51FA\u53D1\u65F6\u95F4',
  endTime: '\u7ED3\u675F\u65F6\u95F4',
  budget: '\u9884\u7B97\u7B49\u7EA7',
  budgetPlaceholder: '\u9009\u62E9\u9884\u7B97\u7B49\u7EA7',
  themes: '\u4E3B\u9898\u504F\u597D\uFF08\u53EF\u591A\u9009\uFF09',
  companion: '\u540C\u884C\u7C7B\u578B',
  walking: '\u6B65\u884C\u5F3A\u5EA6',
  rainy: '\u662F\u5426\u9047\u5230\u96E8\u5929',
  rainyOn: '\u4F18\u5148\u5B89\u6392\u5BA4\u5185',
  rainyOff: '\u6674\u5929\u51FA\u884C',
  night: '\u662F\u5426\u4F53\u9A8C\u591C\u6E38',
  nightOn: '\u9700\u8981\u591C\u666F / \u591C\u5E02',
  nightOff: '\u508D\u665A\u7ED3\u675F',
  loginReminder: '\u767B\u5F55\u540E\u5373\u53EF\u751F\u6210\u4E13\u5C5E\u884C\u7A0B\uFF0C\u7CFB\u7EDF\u4F1A\u6839\u636E\u4F60\u7684\u504F\u597D\u7ED9\u51FA\u66F4\u8D34\u5408\u7684\u6E38\u73A9\u5EFA\u8BAE\u3002',
  submit: '\u5F00\u59CB\u751F\u6210\u884C\u7A0B',
  submitNeedLogin: '\u767B\u5F55\u540E\u5F00\u542F\u884C\u7A0B\u63A8\u8350',
  dateRequired: '\u8BF7\u9009\u62E9\u51FA\u884C\u65E5\u671F',
  themeRequired: '\u8BF7\u81F3\u5C11\u9009\u62E9\u4E00\u4E2A\u504F\u597D\u4E3B\u9898',
  startRequired: '\u8BF7\u9009\u62E9\u51FA\u53D1\u65F6\u95F4',
  endRequired: '\u8BF7\u9009\u62E9\u7ED3\u675F\u65F6\u95F4',
  loginWarning: '\u767B\u5F55\u540E\u624D\u80FD\u751F\u6210\u4E13\u5C5E\u884C\u7A0B\u3002',
  timeoutError: '\u884C\u7A0B\u751F\u6210\u8D85\u65F6\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5\u3002',
  authExpired: '\u767B\u5F55\u72B6\u6001\u5DF2\u5931\u6548\uFF0C\u8BF7\u91CD\u65B0\u767B\u5F55\u540E\u518D\u7EE7\u7EED\u89C4\u5212\u3002',
  generateFailed: '\u751F\u6210\u5931\u8D25\uFF0C\u8BF7\u68C0\u67E5\u7F51\u7EDC\u540E\u91CD\u8BD5\u3002'
}

const tripDayOptions = [
  { label: '\u534A\u5929\u95F2\u901B', value: 0.5 },
  { label: '\u5168\u5929\u6E38\u73A9', value: 1.0 },
  { label: '\u4E24\u65E5\u6DF1\u5EA6', value: 2.0 }
]

const budgetOptions = [
  { label: '\u4F4E\u9884\u7B97\uFF080~100\u5143/\u4EBA\uFF09', value: '\u4F4E' },
  { label: '\u4E2D\u9884\u7B97\uFF08100~300\u5143/\u4EBA\uFF09', value: '\u4E2D' },
  { label: '\u9AD8\u9884\u7B97\uFF08300\u5143\u4EE5\u4E0A/\u4EBA\uFF09', value: '\u9AD8' }
]

const themeOptions = [
  { label: '\u5386\u53F2\u6587\u5316', value: '\u6587\u5316' },
  { label: '\u7279\u8272\u7F8E\u98DF', value: '\u7F8E\u98DF' },
  { label: '\u81EA\u7136\u98CE\u5149', value: '\u81EA\u7136' },
  { label: '\u5546\u4E1A\u8D2D\u7269', value: '\u8D2D\u7269' },
  { label: '\u7F51\u7EA2\u6253\u5361', value: '\u7F51\u7EA2' },
  { label: '\u4F11\u95F2\u653E\u677E', value: '\u4F11\u95F2' }
]

const companionOptions = [
  { label: '\u72EC\u81EA\u6F2B\u6E38', value: '\u72EC\u81EA' },
  { label: '\u4E09\u4E94\u597D\u53CB', value: '\u670B\u53CB' },
  { label: '\u60C5\u4FA3\u7EA6\u4F1A', value: '\u60C5\u4FA3' },
  { label: '\u5BB6\u5EAD\u4EB2\u5B50', value: '\u4EB2\u5B50' }
]

const walkingOptions = [
  { label: '\u4F4E\uFF08\u5C11\u8D70\u8DEF\u3001\u591A\u4F11\u606F\uFF09', value: '\u4F4E' },
  { label: '\u4E2D\uFF08\u6B63\u5E38\u6563\u6B65\u6E38\u89C8\uFF09', value: '\u4E2D' },
  { label: '\u9AD8\uFF08\u80FD\u8D70\u3001\u63A5\u53D7\u722C\u5C71\uFF09', value: '\u9AD8' }
]

const route = useRoute()
const router = useRouter()
const authState = useAuthState()
const formRef = ref()
const formPaneRef = ref(null)
const loading = ref(false)
const panelHeight = ref(0)
let formPaneObserver = null

const form = reactive({
  tripDays: 1.0,
  tripDate: getDefaultTripDate(),
  startTime: '09:00',
  endTime: '18:00',
  budgetLevel: '\u4E2D',
  themes: [],
  isRainy: false,
  isNight: true,
  walkingLevel: '\u4E2D',
  companionType: '\u670B\u53CB'
})

const rules = {
  tripDate: [{ required: true, message: text.dateRequired, trigger: 'change' }],
  themes: [{ type: 'array', required: true, message: text.themeRequired, trigger: 'change' }],
  startTime: [{ required: true, message: text.startRequired, trigger: 'change' }],
  endTime: [{ required: true, message: text.endRequired, trigger: 'change' }]
}

const syncPanelHeight = () => {
  if (typeof window === 'undefined') return
  if (window.innerWidth < 992 || !formPaneRef.value) {
    panelHeight.value = 0
    return
  }
  panelHeight.value = Math.ceil(formPaneRef.value.getBoundingClientRect().height)
}

const aiPanelStyle = computed(() => {
  return panelHeight.value ? { height: `${panelHeight.value}px` } : {}
})

watch(() => form.tripDays, (newVal) => {
  if (newVal === 0.5) {
    form.startTime = '09:00'
    form.endTime = '13:00'
  } else {
    form.startTime = '09:00'
    form.endTime = '18:00'
  }
})

watch(() => form.endTime, (newVal) => {
  if (!newVal) return
  const hour = parseInt(newVal.split(':')[0], 10)
  form.isNight = hour >= 19
})

onMounted(() => {
  nextTick(syncPanelHeight)
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', syncPanelHeight)
  }
  if (typeof ResizeObserver !== 'undefined' && formPaneRef.value) {
    formPaneObserver = new ResizeObserver(syncPanelHeight)
    formPaneObserver.observe(formPaneRef.value)
  }
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', syncPanelHeight)
  }
  if (formPaneObserver) {
    formPaneObserver.disconnect()
    formPaneObserver = null
  }
})

const onSubmit = async () => {
  if (!authState.user) {
    ElMessage.warning(text.loginWarning)
    router.push({
      path: '/auth',
      query: { redirect: route.fullPath }
    })
    return
  }

  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const responseData = await reqGenerateItinerary(form)
    saveItinerarySnapshot(responseData)
    router.push('/result')
  } catch (err) {
    if (err && err.code === 'ECONNABORTED') {
      ElMessage.error(text.timeoutError)
    } else if (err && err.code === 401) {
      ElMessage.warning(text.authExpired)
      router.push({
        path: '/auth',
        query: { redirect: route.fullPath }
      })
    } else {
      ElMessage.error(text.generateFailed)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.core-section {
  padding: 28px 20px 36px;
  background-color: #ffffff;
}

.core-container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  margin-bottom: 18px;
}

.text-center {
  text-align: center;
}

.section-title {
  font-size: 28px;
  font-weight: 800;
  color: #1f2d3d;
  margin: 0 0 10px 0;
}

.section-subtitle {
  font-size: 14px;
  color: #606266;
  margin: 0 auto;
  max-width: 680px;
  line-height: 1.6;
}

.main-card {
  width: 100%;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.06);
  border: 1px solid rgba(228, 231, 237, 0.6);
  padding: 22px 24px;
  box-sizing: border-box;
}

.core-layout {
  align-items: stretch;
}

.form-col,
.ai-panel-col {
  display: flex;
  margin-top: 24px;
}

@media (min-width: 992px) {
  .ai-panel-col {
    margin-top: 0;
  }
}

.custom-form,
.form-pane,
.ai-panel-col :deep(.home-ai-panel) {
  width: 100%;
}

.ai-panel-col :deep(.home-ai-panel) {
  min-height: 0;
}

.form-section {
  margin-bottom: 10px;
}

.sub-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #475669;
  margin-bottom: 12px;
  position: relative;
  padding-left: 12px;
}

.sub-section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  background-color: #409EFF;
  border-radius: 2px;
}

.custom-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #475669;
  padding-bottom: 4px;
  line-height: 1.35;
}

.custom-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.custom-form :deep(.el-divider--horizontal) {
  margin: 8px 0 12px;
}

.compact-row {
  margin-top: 10px;
}

.full-width-radio {
  display: flex !important;
  width: 100%;
}

.full-width-radio :deep(.el-radio-button) {
  flex: 1;
}

.full-width-radio :deep(.el-radio-button__inner) {
  width: 100%;
}

.theme-checkbox-group :deep(.el-checkbox-button__inner) {
  border-radius: 4px !important;
  border: 1px solid #dcdfe6;
  margin-right: 8px;
  margin-bottom: 8px;
  padding: 8px 14px;
  box-shadow: none !important;
}

.theme-checkbox-group :deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  border-color: #409EFF;
}

.custom-form :deep(.el-radio.is-bordered) {
  margin-right: 8px;
  margin-bottom: 8px;
  margin-left: 0 !important;
  height: 38px;
  padding: 0 12px;
}

.custom-form :deep(.el-radio__label) {
  font-size: 13px;
}

.switch-item :deep(.el-form-item__content) {
  height: 34px;
  display: flex;
  align-items: center;
}

.form-actions {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.login-reminder {
  max-width: 420px;
  text-align: center;
  font-size: 13px;
  line-height: 1.6;
  color: #6c7d90;
}

.submit-btn {
  width: 260px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 24px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.2);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);
}

@media (max-width: 768px) {
  .main-card {
    padding: 20px 16px;
  }

  .submit-btn {
    width: 100%;
  }
}
</style>
