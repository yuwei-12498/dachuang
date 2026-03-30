<template>
  <div class="detail-page">
    <div class="detail-shell">
      <template v-if="poiDetail">
        <section class="hero-card">
          <div class="hero-top">
            <el-button round class="ghost-btn" @click="goBack" icon="ArrowLeft">返回路线总览</el-button>
            <el-tag size="large" type="info" effect="plain">{{ poiDetail.district }}</el-tag>
          </div>

          <h1 class="hero-title">{{ poiDetail.name }}</h1>
          <p class="hero-desc">{{ poiDetail.description }}</p>

          <div class="hero-tags">
            <el-tag v-for="tag in tagList" :key="tag" effect="plain" class="hero-tag">{{ tag }}</el-tag>
            <el-tag v-if="poiDetail.indoor === 1" type="warning" effect="light" class="hero-tag">室内场馆</el-tag>
          </div>
        </section>

        <section class="detail-grid">
          <div class="main-column">
            <el-card class="info-card" shadow="never">
              <div class="section-head">
                <div>
                  <p class="section-kicker">游玩信息</p>
                  <h3>先看这站值不值得停留</h3>
                </div>
              </div>

              <div class="info-grid">
                <div class="info-item">
                  <span>建议停留</span>
                  <strong>{{ poiDetail.stayDuration }} 分钟</strong>
                  <small>留出轻松拍照和休息的时间</small>
                </div>
                <div class="info-item">
                  <span>人均消费</span>
                  <strong>¥{{ poiDetail.avgCost }}</strong>
                  <small>按景点或周边消费的大致水平估算</small>
                </div>
                <div class="info-item">
                  <span>开放时间</span>
                  <strong>{{ poiDetail.openTime || '全天' }} - {{ poiDetail.closeTime || '全天' }}</strong>
                  <small>出发前再确认一遍会更稳妥</small>
                </div>
                <div class="info-item">
                  <span>步行感受</span>
                  <strong>{{ poiDetail.walkingLevel }}</strong>
                  <small>可以和当天整体体力安排一起判断</small>
                </div>
              </div>
            </el-card>

            <el-card class="story-card" shadow="never">
              <p class="section-kicker">这站适合谁</p>
              <h3>它更适合这样的出行节奏</h3>
              <div class="story-list">
                <div class="story-row">
                  <span>适合人群</span>
                  <strong>{{ poiDetail.suitableFor || '所有游客' }}</strong>
                </div>
                <div class="story-row">
                  <span>推荐理由</span>
                  <p>{{ poiDetail.description }}</p>
                </div>
              </div>
            </el-card>
          </div>

          <el-card class="replace-card" shadow="never">
            <p class="section-kicker">调整建议</p>
            <h3>想换个更合心意的地点？</h3>
            <p class="replace-hint">如果这一站不够对味，系统会优先在相近区域或相近类型里，帮你换一个更合适的地点，并把整条路线重新顺一遍。</p>
            <el-button type="primary" size="large" round class="replace-btn" :loading="replacing" @click="handleReplace" icon="Switch">
              换个类似地点看看
            </el-button>
          </el-card>
        </section>
      </template>

      <el-empty v-else description="这条地点信息暂时还没加载出来，请稍后再试。" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { reqGetPoiDetail } from '@/api/poi'
import { reqReplacePoi } from '@/api/itinerary'

const route = useRoute()
const router = useRouter()
const poiDetail = ref(null)
const replacing = ref(false)

const targetPoiId = Number(route.params.id)

const tagList = computed(() => {
  if (!poiDetail.value || !poiDetail.value.tags) {
    return []
  }
  return poiDetail.value.tags.split(',').map(tag => tag.trim()).filter(Boolean)
})

onMounted(async () => {
  try {
    const res = await reqGetPoiDetail(targetPoiId)
    poiDetail.value = res
  } catch (err) {
    ElMessage.error('无法获取点位详情')
  }
})

const goBack = () => {
  router.back()
}

const handleReplace = async () => {
  const currentItineraryStr = sessionStorage.getItem('current_itinerary')
  const originalReqStr = sessionStorage.getItem('original_req_form')

  if (!currentItineraryStr) {
    ElMessage.warning('没找到当前路线信息，请先回首页重新生成。')
    router.push('/')
    return
  }

  const currentNodes = JSON.parse(currentItineraryStr).nodes
  const originalReq = originalReqStr ? JSON.parse(originalReqStr) : {}

  replacing.value = true
  try {
    const req = {
      targetPoiId,
      currentNodes,
      originalReq
    }

    const res = await reqReplacePoi(req)
    sessionStorage.setItem('current_itinerary', JSON.stringify(res))
    ElMessage.success('已经帮你换了一个相近地点，路线也同步更新了。')

    setTimeout(() => {
      router.push('/result')
    }, 500)
  } catch (err) {
  } finally {
    replacing.value = false
  }
}
</script>

<style scoped>
.detail-page {
  min-height: calc(100vh - 64px);
  padding: 40px 24px 64px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #f6f9fe 0%, #f7f8fa 100%);
}

.detail-shell {
  max-width: 1120px;
  margin: 0 auto;
}

.hero-card,
.info-card,
.story-card,
.replace-card {
  border-radius: 28px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.06);
}

.hero-card {
  padding: 30px 34px;
  margin-bottom: 24px;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.ghost-btn {
  min-height: 42px;
  font-weight: 600;
}

.hero-title {
  margin: 22px 0 14px;
  font-size: 40px;
  line-height: 1.2;
  color: #1f2d3d;
}

.hero-desc {
  margin: 0;
  max-width: 760px;
  color: #607185;
  line-height: 1.85;
  font-size: 16px;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.hero-tag {
  border-radius: 999px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
  gap: 24px;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-card,
.story-card,
.replace-card {
  padding: 28px;
}

.section-kicker {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #2d79c7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.section-head h3,
.story-card h3,
.replace-card h3 {
  margin: 14px 0 0;
  font-size: 28px;
  color: #1f2d3d;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.info-item {
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f9fbff 0%, #f4f8ff 100%);
  border: 1px solid rgba(220, 230, 242, 0.9);
}

.info-item span {
  display: block;
  color: #7a8da3;
  font-size: 13px;
  margin-bottom: 10px;
}

.info-item strong {
  display: block;
  color: #1f2d3d;
  font-size: 24px;
  line-height: 1.35;
}

.info-item small {
  display: block;
  margin-top: 8px;
  color: #8c9caf;
  line-height: 1.7;
}

.story-list {
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.story-row {
  padding: 18px 20px;
  border-radius: 20px;
  background: #f7f9fc;
}

.story-row span {
  display: block;
  color: #7a8da3;
  font-size: 13px;
  margin-bottom: 8px;
}

.story-row strong {
  color: #1f2d3d;
  font-size: 18px;
}

.story-row p {
  margin: 0;
  color: #586a80;
  line-height: 1.85;
}

.replace-card {
  align-self: start;
}

.replace-hint {
  margin: 18px 0 22px;
  color: #647588;
  line-height: 1.85;
}

.replace-btn {
  width: 100%;
  min-height: 46px;
  box-shadow: 0 10px 18px rgba(64, 158, 255, 0.16);
}

@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .detail-page {
    padding: 24px 16px 40px;
  }

  .hero-card,
  .info-card,
  .story-card,
  .replace-card {
    border-radius: 22px;
    padding: 22px 20px;
  }

  .hero-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-title {
    font-size: 30px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
