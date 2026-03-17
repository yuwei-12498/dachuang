<template>
  <section class="core-section" id="core">
    <div class="core-container">
      <div class="section-header text-center">
        <h2 class="section-title">定制专属行程</h2>
        <p class="section-subtitle">只需三个步骤，为你的成都之行画下完美的闭环</p>
      </div>

      <div class="main-card">
        <el-row :gutter="40">
          <!-- 左侧：表单配置区 -->
          <el-col :md="14" :lg="15">
            <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" class="custom-form">
              
              <!-- ================= 第1组：基础信息 ================= -->
              <div class="form-section">
                <div class="sub-section-title">基础时间分配</div>
                <el-row :gutter="40">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="出行模板推荐" prop="tripDays">
                      <el-radio-group v-model="form.tripDays" class="full-width-radio">
                        <el-radio-button :label="0.5">半天闲逛</el-radio-button>
                        <el-radio-button :label="1.0">全天游玩</el-radio-button>
                        <el-radio-button :label="2.0">两日深度</el-radio-button>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-row :gutter="10">
                      <el-col :span="12">
                        <el-form-item label="每日出发时间" prop="startTime">
                          <el-time-select
                            v-model="form.startTime"
                            start="06:00"
                            step="00:30"
                            end="14:00"
                            placeholder="出发时间"
                            style="width: 100%"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="每日结束回家" prop="endTime">
                          <el-time-select
                            v-model="form.endTime"
                            :min-time="form.startTime"
                            start="11:00"
                            step="00:30"
                            end="23:30"
                            placeholder="结束时间"
                            style="width: 100%"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-col>
                </el-row>

                <el-row :gutter="40">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="预算等级" prop="budgetLevel">
                      <el-select v-model="form.budgetLevel" placeholder="选择预算等级" style="width: 100%">
                        <el-option label="低 (0~100元/人)" value="低" />
                        <el-option label="中 (100~300元/人)" value="中" />
                        <el-option label="高 (300元以上/人)" value="高" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>

              <el-divider border-style="dashed" />

              <!-- ================= 第2组：偏好设置 ================= -->
              <div class="form-section">
                <div class="sub-section-title">偏好设置</div>
                
                <el-form-item label="主题偏好 (可多选)" prop="themes">
                  <el-checkbox-group v-model="form.themes" class="theme-checkbox-group">
                    <el-checkbox-button label="文化">历史文化</el-checkbox-button>
                    <el-checkbox-button label="美食">特色美食</el-checkbox-button>
                    <el-checkbox-button label="自然">自然风光</el-checkbox-button>
                    <el-checkbox-button label="购物">商业购物</el-checkbox-button>
                    <el-checkbox-button label="网红">网红打卡</el-checkbox-button>
                    <el-checkbox-button label="休闲">休闲放松</el-checkbox-button>
                  </el-checkbox-group>
                </el-form-item>

                <el-row :gutter="40" style="margin-top: 24px;">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="同行类型" prop="companionType">
                      <el-radio-group v-model="form.companionType">
                        <el-radio label="独自" border>独自漫游</el-radio>
                        <el-radio label="朋友" border>三五好友</el-radio>
                        <el-radio label="情侣" border>情侣约会</el-radio>
                        <el-radio label="亲子" border>家庭亲子</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="步行强度期望" prop="walkingLevel">
                      <el-radio-group v-model="form.walkingLevel">
                        <el-radio label="低">低 (少走路、多休息)</el-radio>
                        <el-radio label="中">中 (正常散步游览)</el-radio>
                        <el-radio label="高">高 (能走、接受爬山)</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>

              <el-divider border-style="dashed" />

              <!-- ================= 第3组：场景设置 ================= -->
              <div class="form-section">
                <div class="sub-section-title">场景探索</div>
                <el-row :gutter="40">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="是否遭遇雨天?" class="switch-item">
                      <el-switch v-model="form.isRainy" active-text="优先安排室内" inactive-text="晴天出行" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="是否体验夜游?" class="switch-item">
                      <el-switch v-model="form.isNight" active-text="需要夜跑/夜市" inactive-text="傍晚结束" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>

              <!-- ================= 底部操作 ================= -->
              <div class="form-actions">
                <el-button 
                  type="primary" 
                  size="large" 
                  class="submit-btn" 
                  @click="onSubmit" 
                  :loading="loading">
                  开始生成行程
                </el-button>
              </div>

            </el-form>
          </el-col>
          
          <!-- 右侧：独立的 AI旅行助手 模块 -->
          <el-col :md="10" :lg="9" class="ai-panel-col">
            <HomeAiPanel :currentForm="form" />
          </el-col>

        </el-row>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { reqGenerateItinerary } from '@/api/itinerary'
import HomeAiPanel from '@/components/HomeAiPanel.vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  tripDays: 1.0,
  startTime: '09:00',
  endTime: '18:00',
  budgetLevel: '中',
  themes: [],
  isRainy: false,
  isNight: true,
  walkingLevel: '中',
  companionType: '朋友'
})

const rules = {
  themes: [
    { type: 'array', required: true, message: '请至少选择一个偏好主题', trigger: 'change' }
  ],
  startTime: [
    { required: true, message: '请选择出发时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' }
  ]
}

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
  if (newVal) {
    const hour = parseInt(newVal.split(':')[0])
    if (hour >= 19) {
      form.isNight = true
    } else {
      form.isNight = false
    }
  }
})

const onSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const responseData = await reqGenerateItinerary(form)
        sessionStorage.setItem('current_itinerary', JSON.stringify(responseData))
        sessionStorage.setItem('original_req_form', JSON.stringify(form))
        router.push('/result')
      } catch (err) {
        // 请求拦截器处理
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.core-section {
  padding: 80px 24px;
  background-color: #ffffff;
}

.core-container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  margin-bottom: 50px;
}

.text-center {
  text-align: center;
}

.section-title {
  font-size: 32px;
  font-weight: 800;
  color: #1f2d3d;
  margin: 0 0 16px 0;
}

.section-subtitle {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

.main-card {
  width: 100%;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(228, 231, 237, 0.6);
  padding: 40px 50px;
  box-sizing: border-box;
}

.ai-panel-col {
  margin-top: 24px;
}

@media (min-width: 992px) {
  .ai-panel-col {
    margin-top: 0;
  }
}

.form-section {
  margin-bottom: 24px;
}

.sub-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #475669;
  margin-bottom: 24px;
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
  padding-bottom: 8px;
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
  margin-right: 12px;
  margin-bottom: 12px;
  box-shadow: none !important;
}

.theme-checkbox-group :deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  border-color: #409EFF;
}

.custom-form :deep(.el-radio.is-bordered) {
  margin-right: 12px;
  margin-bottom: 12px;
  margin-left: 0 !important; 
}

.switch-item :deep(.el-form-item__content) {
  height: 40px;
  display: flex;
  align-items: center;
}

.form-actions {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

.submit-btn {
  width: 280px;
  height: 48px;
  font-size: 16px;
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
    padding: 24px 20px;
  }
  .submit-btn {
    width: 100%;
  }
}
</style>
