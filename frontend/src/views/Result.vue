<template>
  <div class="result-page">
    <div class="result-shell">
      <template v-if="itinerary">
        <section class="hero-card">
          <div class="hero-copy">
            <div class="hero-badge">你的成都推荐路线</div>
            <h1 class="hero-title">这条路线，已经按你的节奏安排好了</h1>
            <p class="hero-desc">
              从 {{ originalReq?.startTime || '09:00' }} 出发，到 {{ originalReq?.endTime || '18:00' }} 前收尾，
              系统已经帮你把停留时间、路上耗时和游玩顺序一起理顺。
            </p>
          </div>

          <div class="hero-actions">
            <el-button round class="ghost-btn" @click="goBack" icon="ArrowLeft">
              返回调整偏好
            </el-button>
            <el-button type="primary" round class="primary-btn" @click="handleReplan" :loading="replanning" icon="Sort">
              换一版路线试试
            </el-button>
          </div>
        </section>

        <section class="overview-grid">
          <el-card class="overview-card" shadow="never">
            <div class="section-head">
              <div>
                <p class="section-kicker">路线概览</p>
                <h3>先看整体节奏</h3>
              </div>
            </div>

            <div class="stat-grid">
              <div class="stat-block">
                <span class="stat-label">总时长</span>
                <strong>{{ formatDuration(itinerary.totalDuration) }}</strong>
                <small>更方便你判断当天会不会太赶</small>
              </div>
              <div class="stat-block">
                <span class="stat-label">预计花费</span>
                <strong>¥{{ itinerary.totalCost }}</strong>
                <small>按整条路线的大致消费来估算</small>
              </div>
              <div class="stat-block">
                <span class="stat-label">推荐点位</span>
                <strong>{{ itinerary.nodes.length }} 站</strong>
                <small>停留节奏已经顺着当天时间排好</small>
              </div>
            </div>

            <div class="message-stack">
              <div class="message-card success-card">
                <p class="message-label">推荐理由</p>
                <p>{{ itinerary.recommendReason }}</p>
              </div>
              <div class="message-card warning-card">
                <p class="message-label">出行提醒</p>
                <p>{{ itinerary.tips }}</p>
              </div>
            </div>
          </el-card>

          <el-card class="note-card" shadow="never">
            <p class="section-kicker">行程提示</p>
            <h3>按这个时间范围来玩，会更舒服</h3>
            <p class="note-line">建议出发：{{ originalReq?.startTime || '09:00' }}</p>
            <p class="note-line">最晚结束：{{ originalReq?.endTime || '18:00' }}</p>
            <div class="note-divider"></div>
            <p class="note-text">如果你觉得某一站不够合心意，可以点进详情页换一个相似地点；如果只是想让路线更顺，可以直接试试“换一版路线”。</p>
          </el-card>
        </section>

        <section class="timeline-wrap">
          <div class="section-head timeline-head">
            <div>
              <p class="section-kicker">定制时间轴</p>
              <h3>今天就照着这条路线慢慢玩</h3>
            </div>
            <p class="timeline-subtitle">每一站都已经把抵达时间、停留时长和路上耗时考虑进去了。</p>
          </div>

          <el-timeline class="custom-timeline">
            <el-timeline-item
              v-for="node in itinerary.nodes"
              :key="node.poiId"
              :timestamp="`${node.startTime} - ${node.endTime}`"
              placement="top"
              type="primary"
              size="large">
              <div v-if="node.travelTime > 0" class="travel-pill">
                <el-icon><Van /></el-icon>
                路上大约 {{ node.travelTime }} 分钟
              </div>

              <el-card class="stop-card" shadow="never">
                <div class="stop-top">
                  <div>
                    <p class="stop-index">第 {{ node.stepOrder }} 站</p>
                    <h4>{{ node.poiName }}</h4>
                  </div>
                  <div class="tag-group">
                    <el-tag size="small" effect="plain">{{ node.category }}</el-tag>
                    <el-tag size="small" type="info" effect="plain">{{ node.district }}</el-tag>
                  </div>
                </div>

                <p class="stop-reason">{{ node.sysReason }}</p>

                <div class="stop-meta">
                  <span><el-icon><Timer /></el-icon> 建议停留 {{ node.stayDuration }} 分钟</span>
                  <span><el-icon><Wallet /></el-icon> 预估消费 ¥{{ node.cost }}</span>
                </div>

                <div class="stop-actions">
                  <el-button round @click="goToDetail(node)">查看详情并换个类似地点</el-button>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </section>
      </template>

      <el-empty v-else description="还没有生成路线，先回首页挑选你的偏好吧。" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reqReplanItinerary } from '@/api/itinerary'

const router = useRouter()
const itinerary = ref(null)
const replanning = ref(false)
const originalReq = ref(null)

onMounted(() => {
  const data = sessionStorage.getItem('current_itinerary')
  if (data) {
    itinerary.value = JSON.parse(data)
  }

  const originalReqStr = sessionStorage.getItem('original_req_form')
  if (originalReqStr) {
    originalReq.value = JSON.parse(originalReqStr)
  }
})

const formatDuration = (minutes) => {
  if (!minutes && minutes !== 0) return '--'
  const hour = Math.floor(minutes / 60)
  const minute = minutes % 60
  if (hour === 0) return `${minute} 分钟`
  if (minute === 0) return `${hour} 小时`
  return `${hour} 小时 ${minute} 分钟`
}

const goBack = () => {
  router.push('/')
}

const goToDetail = (node) => {
  router.push(`/detail/${node.poiId}`)
}

const handleReplan = async () => {
  if (!itinerary.value || !itinerary.value.nodes) return

  replanning.value = true
  try {
    const req = {
      currentNodes: itinerary.value.nodes,
      originalReq: originalReq.value
    }
    const res = await reqReplanItinerary(req)

    if (res.success) {
      if (res.changed) {
        itinerary.value = res.itinerary
        sessionStorage.setItem('current_itinerary', JSON.stringify(res.itinerary))
        ElMessage.success({
          message: res.message || '已经为你换了一版更顺路的玩法。',
          duration: 3000
        })
      } else {
        ElMessageBox.alert(
          res.reason || '当前这条路线已经比较顺了，暂时没有更合适的调整方案。',
          '这条路线已经很合适',
          {
            confirmButtonText: '去看看某一站详情',
            type: 'info'
          }
        )
      }
    } else {
      ElMessage.warning(res.message || '路线调整失败，请稍后再试')
    }
  } catch (err) {
  } finally {
    replanning.value = false
  }
}
</script>

<style scoped>
.result-page {
  min-height: calc(100vh - 64px);
  padding: 40px 24px 64px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #f6f9fe 0%, #f7f8fa 100%);
}

.result-shell {
  max-width: 1120px;
  margin: 0 auto;
}

.hero-card,
.overview-card,
.note-card,
.timeline-wrap {
  border-radius: 28px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 28px;
  padding: 34px 36px;
  margin-bottom: 24px;
}

.hero-copy {
  max-width: 640px;
}

.hero-badge,
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

.hero-title {
  margin: 18px 0 14px;
  font-size: 38px;
  line-height: 1.2;
  color: #1f2d3d;
}

.hero-desc {
  margin: 0;
  color: #627386;
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  min-width: 240px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
}

.primary-btn,
.ghost-btn {
  min-height: 44px;
  font-weight: 600;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
  gap: 24px;
  margin-bottom: 24px;
}

.overview-card,
.note-card {
  padding: 28px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 22px;
}

.section-head h3 {
  margin: 14px 0 0;
  font-size: 28px;
  color: #1f2d3d;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-block {
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f9fbff 0%, #f4f8ff 100%);
  border: 1px solid rgba(220, 230, 242, 0.9);
}

.stat-label {
  display: block;
  color: #7a8da3;
  font-size: 13px;
  margin-bottom: 10px;
}

.stat-block strong {
  display: block;
  color: #1f2d3d;
  font-size: 28px;
  line-height: 1.1;
}

.stat-block small {
  display: block;
  margin-top: 8px;
  color: #8c9caf;
  line-height: 1.6;
}

.message-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message-card {
  border-radius: 20px;
  padding: 18px 20px;
}

.message-card p {
  margin: 0;
  line-height: 1.8;
  color: #4f6278;
}

.message-label {
  margin-bottom: 8px !important;
  font-size: 13px;
  font-weight: 700;
}

.success-card {
  background: linear-gradient(180deg, #f4fbf7 0%, #eefaf2 100%);
  border: 1px solid rgba(196, 231, 210, 0.9);
}

.success-card .message-label {
  color: #3b8f63;
}

.warning-card {
  background: linear-gradient(180deg, #fffaf1 0%, #fff5e7 100%);
  border: 1px solid rgba(246, 220, 177, 0.9);
}

.warning-card .message-label {
  color: #c78821;
}

.note-card h3 {
  margin: 14px 0 20px;
  font-size: 26px;
  line-height: 1.35;
  color: #1f2d3d;
}

.note-line {
  margin: 0 0 12px;
  color: #4f6278;
  font-size: 15px;
}

.note-divider {
  height: 1px;
  margin: 18px 0;
  background: linear-gradient(90deg, rgba(217, 230, 247, 0), rgba(217, 230, 247, 1), rgba(217, 230, 247, 0));
}

.note-text {
  margin: 0;
  color: #6c7d90;
  line-height: 1.8;
}

.timeline-wrap {
  padding: 28px 30px 18px;
}

.timeline-head {
  margin-bottom: 10px;
}

.timeline-subtitle {
  max-width: 360px;
  margin: 0;
  color: #6f8196;
  line-height: 1.8;
}

.travel-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px -14px;
  padding: 6px 14px;
  border-radius: 999px;
  background: #fdf6ec;
  color: #c78821;
  font-size: 13px;
  font-weight: 700;
}

.stop-card {
  border-radius: 24px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.05);
}

.stop-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.stop-index {
  margin: 0 0 8px;
  color: #7c8da1;
  font-size: 13px;
  font-weight: 700;
}

.stop-top h4 {
  margin: 0;
  font-size: 22px;
  color: #1f2d3d;
}

.tag-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stop-reason {
  margin: 18px 0;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f7f9fc;
  color: #56697f;
  line-height: 1.8;
}

.stop-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #738397;
  font-size: 14px;
}

.stop-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.stop-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.custom-timeline :deep(.el-timeline-item__timestamp) {
  color: #409eff;
  font-weight: 700;
  margin-bottom: 10px;
}

@media (max-width: 1024px) {
  .hero-card,
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
  }

  .hero-actions {
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .result-page {
    padding: 24px 16px 40px;
  }

  .hero-card,
  .overview-card,
  .note-card,
  .timeline-wrap {
    border-radius: 22px;
  }

  .hero-card,
  .overview-card,
  .note-card,
  .timeline-wrap {
    padding: 22px 20px;
  }

  .hero-title {
    font-size: 30px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .stop-top,
  .section-head {
    flex-direction: column;
  }

  .stop-actions {
    justify-content: stretch;
  }

  .stop-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
