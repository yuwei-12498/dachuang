<template>
  <div class="result-page">
    <div class="result-shell" v-if="itinerary">
      <section class="hero-card">
        <div>
          <p class="eyebrow">{{ text.heroBadge }}</p>
          <h1>{{ itinerary.customTitle || text.heroTitle }}</h1>
          <p class="hero-copy">
            {{ originalReq?.tripDate || '--' }} / {{ originalReq?.startTime || '09:00' }} - {{ originalReq?.endTime || '18:00' }}
          </p>
        </div>
        <div class="hero-actions">
          <el-button round @click="goBack">{{ text.back }}</el-button>
          <el-button round @click="goHistory">{{ text.history }}</el-button>
          <el-button
            round
            :type="itinerary.favorited ? 'warning' : 'default'"
            :loading="favoriteLoading"
            @click="handleFavorite"
          >
            {{ itinerary.favorited ? text.unfavorite : text.favorite }}
          </el-button>
          <el-button type="primary" round :loading="replanning" @click="handleReplan">{{ text.replan }}</el-button>
        </div>
      </section>

      <section class="option-panel" v-if="displayOptions.length">
        <div class="panel-header">
          <div>
            <p class="eyebrow">{{ text.optionBadge }}</p>
            <h2>{{ text.optionTitle }}</h2>
            <p class="panel-copy">{{ text.optionCopy }}</p>
          </div>
          <div class="option-counter">{{ text.optionCountPrefix }} {{ displayOptions.length }}</div>
        </div>

        <div class="option-grid">
          <button
            v-for="option in displayOptions"
            :key="option.optionKey"
            type="button"
            class="option-card"
            :class="{ active: option.optionKey === activeOptionKey }"
            @click="handleSelectOption(option.optionKey)"
          >
            <div class="option-head">
              <div>
                <p class="option-label">{{ option.title }}</p>
                <h3>{{ option.subtitle }}</h3>
              </div>
              <span class="option-state">
                {{ option.optionKey === activeOptionKey ? text.currentOption : text.switchOption }}
              </span>
            </div>

            <p class="option-summary">{{ option.summary }}</p>

            <div class="option-metrics">
              <span>{{ text.totalTime }} {{ formatDuration(option.totalDuration) }}</span>
              <span>{{ text.totalCost }} {{ text.currency }}{{ option.totalCost }}</span>
              <span>{{ text.stopCount }} {{ option.stopCount }}</span>
            </div>

            <div class="option-tags" v-if="option.highlights?.length">
              <el-tag
                v-for="item in option.highlights"
                :key="`${option.optionKey}-${item}`"
                size="small"
                effect="plain"
              >
                {{ item }}
              </el-tag>
            </div>

            <p class="option-copy">
              <strong>{{ text.whyRecommend }}</strong>{{ option.recommendReason }}
            </p>
            <p class="option-copy option-copy-muted">
              <strong>{{ text.whyNot }}</strong>{{ option.notRecommendReason }}
            </p>
          </button>
        </div>
      </section>

      <section class="stats-grid">
        <el-card shadow="never" class="stat-card">
          <span>{{ text.totalTime }}</span>
          <strong>{{ formatDuration(activeOption?.totalDuration) }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>{{ text.totalCost }}</span>
          <strong>{{ text.currency }}{{ activeOption?.totalCost ?? itinerary.totalCost }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>{{ text.stopCount }}</span>
          <strong>{{ activeOption?.stopCount ?? activeNodes.length }}</strong>
        </el-card>
      </section>

      <section class="copy-grid">
        <el-card shadow="never" class="copy-card">
          <p class="eyebrow">{{ text.reasonTitle }}</p>
          <p>{{ activeOption?.recommendReason || itinerary.recommendReason }}</p>
        </el-card>
        <el-card shadow="never" class="copy-card">
          <p class="eyebrow">{{ text.compareTitle }}</p>
          <p>{{ activeOption?.notRecommendReason || itinerary.tips }}</p>
        </el-card>
        <el-card shadow="never" class="copy-card">
          <p class="eyebrow">{{ text.tipTitle }}</p>
          <p>{{ itinerary.tips }}</p>
        </el-card>
      </section>

      <section class="alert-strip" v-if="activeAlerts.length">
        <span class="alert-chip" v-for="item in activeAlerts" :key="item">{{ item }}</span>
      </section>

      <ItineraryMapCard :nodes="activeNodes" class="map-section" />

      <section class="timeline-wrap">
        <div
          v-for="node in activeNodes"
          :key="node.poiId"
          class="timeline-item"
        >
          <div class="timeline-time">{{ node.startTime }} - {{ node.endTime }}</div>
          <el-card shadow="never" class="stop-card">
            <div class="stop-head">
              <div>
                <p class="stop-index">{{ text.stopPrefix }} {{ node.stepOrder }}</p>
                <h3>{{ node.poiName }}</h3>
              </div>
              <div class="stop-tags">
                <el-tag size="small" effect="plain">{{ node.category }}</el-tag>
                <el-tag size="small" type="info" effect="plain">{{ node.district }}</el-tag>
              </div>
            </div>

            <p class="stop-copy">{{ node.sysReason }}</p>
            <p v-if="node.statusNote" class="stop-note">{{ node.statusNote }}</p>

            <div class="meta-row">
              <span>{{ text.travel }}{{ node.travelTime || 0 }} {{ text.minute }}</span>
              <span>{{ text.stay }}{{ node.stayDuration || 0 }} {{ text.minute }}</span>
              <span>{{ text.cost }}{{ text.currency }}{{ node.cost }}</span>
            </div>

            <div class="actions-row">
              <el-button round @click="goToDetail(node)">{{ text.detailAction }}</el-button>
            </div>
          </el-card>
        </div>
      </section>
    </div>

    <div class="result-shell" v-else>
      <el-empty :description="text.empty" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ItineraryMapCard from '@/components/itinerary/ItineraryMapCard.vue'
import {
  reqFavoriteItinerary,
  reqGetItinerary,
  reqGetLatestItinerary,
  reqReplanItinerary,
  reqUnfavoriteItinerary
} from '@/api/itinerary'
import {
  loadItinerarySnapshot,
  localizeItineraryText,
  normalizeItinerarySnapshot,
  saveItinerarySnapshot
} from '@/store/itinerary'

const text = {
  heroBadge: '你的已保存行程',
  heroTitle: '这次不只给你一条路线，而是给你一组可比较的方案',
  back: '返回首页',
  history: '历史行程',
  favorite: '收藏行程',
  unfavorite: '取消收藏',
  replan: '换一版路线',
  optionBadge: '方案对比',
  optionTitle: '先比较，再决定用哪条路线出发',
  optionCopy: '每个方案都给出推荐理由和取舍说明，方便你明确“为什么选它、为什么不选别的”。',
  optionCountPrefix: '候选方案',
  currentOption: '当前展示',
  switchOption: '点击查看',
  totalTime: '总时长',
  totalCost: '预计花费',
  stopCount: '推荐点位',
  reasonTitle: '为什么推荐这个',
  compareTitle: '为什么不优先选别的',
  tipTitle: '执行提醒',
  whyRecommend: '推荐：',
  whyNot: '不推荐：',
  stopPrefix: '第',
  travel: '路上：',
  stay: '停留：',
  cost: '花费：',
  minute: '分钟',
  detailAction: '查看详情并替换',
  empty: '暂时没有找到行程，请先回首页生成。',
  currency: '¥',
  replanUpdated: '已为你换了一组新的路线方案。',
  routeKept: '保留当前路线',
  replanFallback: '在当前时间、偏好和营业状态下，新的可行路线已经尝试完了，建议保留当前路线',
  replanFailed: '换线失败，请稍后重试。',
  favoriteAdded: '已加入收藏。',
  favoriteRemoved: '已取消收藏。',
  favoritePromptTitle: '收藏当前路线',
  favoritePromptMessage: '给这条路线起个名字，后面回顾会更方便。',
  favoritePromptPlaceholder: '例如：周末春熙路轻松逛',
  favoritePromptRequired: '请输入路线名称',
  favoritePromptInvalid: '路线名称不能超过 60 个字'
}

const router = useRouter()
const route = useRoute()
const itinerary = ref(null)
const replanning = ref(false)
const favoriteLoading = ref(false)
const originalReq = computed(() => itinerary.value?.originalReq || null)
const routeItineraryId = computed(() => {
  const raw = route.query.id
  const value = Number(raw)
  return Number.isFinite(value) && value > 0 ? value : null
})

const buildRouteSignature = (nodes) => {
  return (nodes || [])
    .map(node => node?.poiId)
    .filter(id => id !== null && id !== undefined && id !== '')
    .join('-')
}

const buildFallbackOption = (snapshot) => {
  const nodes = Array.isArray(snapshot?.nodes) ? snapshot.nodes : []
  return {
    optionKey: 'default',
    title: '当前默认方案',
    subtitle: '当前保存的路线版本',
    signature: buildRouteSignature(nodes),
    totalDuration: snapshot?.totalDuration || 0,
    totalCost: snapshot?.totalCost || 0,
    stopCount: nodes.length,
    totalTravelTime: nodes.reduce((total, node) => total + Number(node?.travelTime || 0), 0),
    summary: snapshot?.recommendReason || '',
    recommendReason: snapshot?.recommendReason || '',
    notRecommendReason: snapshot?.tips || '',
    highlights: [],
    tradeoffs: [],
    alerts: Array.isArray(snapshot?.alerts) ? snapshot.alerts : [],
    nodes
  }
}

const resolveOptions = (snapshot) => {
  if (Array.isArray(snapshot?.options) && snapshot.options.length) {
    return snapshot.options
  }
  return snapshot ? [buildFallbackOption(snapshot)] : []
}

const resolveActiveOption = (snapshot) => {
  const options = resolveOptions(snapshot)
  if (!options.length) {
    return null
  }
  const selected = options.find(option => option.optionKey === snapshot?.selectedOptionKey)
  return selected || options[0]
}

const resolveActiveNodes = (snapshot) => {
  const option = resolveActiveOption(snapshot)
  return Array.isArray(option?.nodes) ? option.nodes : (snapshot?.nodes || [])
}

const ensureSeenRouteSignatures = (snapshot) => {
  if (!snapshot) {
    return snapshot
  }

  const currentSignature = buildRouteSignature(resolveActiveNodes(snapshot))
  const seenRouteSignatures = Array.isArray(snapshot.seenRouteSignatures)
    ? snapshot.seenRouteSignatures.filter(item => typeof item === 'string' && item.trim())
    : []

  if (currentSignature && !seenRouteSignatures.includes(currentSignature)) {
    seenRouteSignatures.push(currentSignature)
  }

  return {
    ...snapshot,
    seenRouteSignatures
  }
}

const loadCurrentItinerary = async () => {
  const snapshot = loadItinerarySnapshot()

  if (routeItineraryId.value) {
    if (snapshot && Number(snapshot.id) === routeItineraryId.value) {
      itinerary.value = ensureSeenRouteSignatures(snapshot)
      return
    }
    try {
      const data = await reqGetItinerary(routeItineraryId.value)
      itinerary.value = ensureSeenRouteSignatures(normalizeItinerarySnapshot(data))
      saveItinerarySnapshot(itinerary.value)
    } catch (err) {
      itinerary.value = null
    }
    return
  }

  if (snapshot) {
    itinerary.value = ensureSeenRouteSignatures(snapshot)
    return
  }

  try {
    const latest = await reqGetLatestItinerary()
    if (latest) {
      itinerary.value = ensureSeenRouteSignatures(normalizeItinerarySnapshot(latest))
      saveItinerarySnapshot(itinerary.value)
    }
  } catch (err) {
  }
}

onMounted(() => {
  loadCurrentItinerary()
})

watch(() => route.query.id, () => {
  loadCurrentItinerary()
})

const displayOptions = computed(() => resolveOptions(itinerary.value))
const activeOption = computed(() => resolveActiveOption(itinerary.value))
const activeOptionKey = computed(() => activeOption.value?.optionKey || null)
const activeNodes = computed(() => resolveActiveNodes(itinerary.value))
const activeAlerts = computed(() => {
  if (Array.isArray(activeOption.value?.alerts) && activeOption.value.alerts.length) {
    return activeOption.value.alerts
  }
  return Array.isArray(itinerary.value?.alerts) ? itinerary.value.alerts : []
})

const formatDuration = (minutes) => {
  if (!minutes && minutes !== 0) return '--'
  const hour = Math.floor(minutes / 60)
  const minute = minutes % 60
  if (hour === 0) return `${minute}${text.minute}`
  if (minute === 0) return `${hour}小时`
  return `${hour}小时 ${minute}${text.minute}`
}

const goBack = () => {
  router.push('/')
}

const goHistory = () => {
  router.push('/history')
}

const goToDetail = (node) => {
  router.push(`/detail/${node.poiId}`)
}

const handleSelectOption = (optionKey) => {
  if (!itinerary.value) {
    return
  }
  const selected = displayOptions.value.find(option => option.optionKey === optionKey)
  if (!selected) {
    return
  }
  itinerary.value = ensureSeenRouteSignatures({
    ...itinerary.value,
    selectedOptionKey: optionKey
  })
  saveItinerarySnapshot(itinerary.value)
}

const handleFavorite = async () => {
  if (!itinerary.value?.id) {
    return
  }

  favoriteLoading.value = true
  try {
    if (itinerary.value.favorited) {
      await reqUnfavoriteItinerary(itinerary.value.id)
      itinerary.value.favorited = false
      itinerary.value.favoriteTime = null
      ElMessage.success(text.favoriteRemoved)
    } else {
      const suggestedTitle = itinerary.value.customTitle
        || activeOption.value?.title
        || `${originalReq.value?.tripDate || '这次'}路线`
      const promptResult = await ElMessageBox.prompt(
        text.favoritePromptMessage,
        text.favoritePromptTitle,
        {
          confirmButtonText: '收藏',
          cancelButtonText: '取消',
          inputValue: suggestedTitle,
          inputPlaceholder: text.favoritePromptPlaceholder,
          inputValidator: (value) => {
            if (!value || !value.trim()) {
              return text.favoritePromptRequired
            }
            if (value.trim().length > 60) {
              return text.favoritePromptInvalid
            }
            return true
          }
        }
      )

      const nextItinerary = await reqFavoriteItinerary(itinerary.value.id, {
        selectedOptionKey: activeOptionKey.value,
        title: promptResult.value.trim()
      })
      itinerary.value = ensureSeenRouteSignatures(normalizeItinerarySnapshot(nextItinerary))
      ElMessage.success(text.favoriteAdded)
    }
    saveItinerarySnapshot(itinerary.value)
  } catch (err) {
  } finally {
    favoriteLoading.value = false
  }
}

const handleReplan = async () => {
  if (!itinerary.value || !activeNodes.value.length) return

  replanning.value = true
  try {
    const currentSignature = buildRouteSignature(activeNodes.value)
    const excludedSignatures = Array.isArray(itinerary.value.seenRouteSignatures)
      ? [...new Set([
          ...itinerary.value.seenRouteSignatures,
          currentSignature
        ].filter(Boolean))]
      : currentSignature ? [currentSignature] : []

    const req = {
      itineraryId: itinerary.value.id,
      currentNodes: activeNodes.value,
      originalReq: originalReq.value,
      excludedSignatures
    }
    const res = await reqReplanItinerary(req)

    if (res.success) {
      if (res.changed) {
        const nextItinerary = ensureSeenRouteSignatures(normalizeItinerarySnapshot(res.itinerary))
        const nextSignature = buildRouteSignature(resolveActiveNodes(nextItinerary))
        nextItinerary.seenRouteSignatures = [...new Set([
          ...excludedSignatures,
          nextSignature
        ].filter(Boolean))]
        itinerary.value = nextItinerary
        saveItinerarySnapshot(itinerary.value)
        ElMessage.success(localizeItineraryText(res.message) || text.replanUpdated)
      } else {
        ElMessageBox.alert(
          localizeItineraryText(res.reason) || text.replanFallback,
          text.routeKept,
          { confirmButtonText: '确定', type: 'info' }
        )
      }
    } else {
      ElMessage.warning(localizeItineraryText(res.message) || text.replanFailed)
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
  padding: 32px 20px 48px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #f6f9fe 0%, #f7f8fa 100%);
}

.result-shell {
  max-width: 1120px;
  margin: 0 auto;
}

.hero-card,
.option-panel,
.stat-card,
.copy-card,
.stop-card {
  border-radius: 24px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.06);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
  transform-origin: center;
}

.hero-card:hover,
.option-panel:hover,
.stat-card:hover,
.copy-card:hover,
.stop-card:hover,
.option-card:hover {
  transform: translateY(-6px) scale(1.01);
  border-color: rgba(108, 176, 255, 0.58);
  box-shadow: 0 24px 48px rgba(31, 45, 61, 0.1);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 30px;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2d79c7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-card h1,
.panel-header h2 {
  margin: 0 0 8px;
  font-size: 34px;
  color: #1f2d3d;
}

.hero-copy,
.panel-copy {
  margin: 0;
  color: #627386;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.option-panel {
  padding: 24px;
  margin-bottom: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.option-counter {
  padding: 10px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.12), rgba(102, 177, 255, 0.22));
  color: #2d79c7;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.option-grid,
.stats-grid,
.copy-grid {
  display: grid;
  gap: 18px;
}

.option-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 20px;
}

.copy-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 18px;
}

.option-card {
  width: 100%;
  padding: 22px;
  border-radius: 22px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.96));
  box-shadow: 0 18px 32px rgba(31, 45, 61, 0.05);
  cursor: pointer;
  text-align: left;
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease,
    background 0.22s ease;
}

.option-card.active {
  border-color: rgba(64, 158, 255, 0.72);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(240, 247, 255, 0.98)),
    linear-gradient(135deg, rgba(64, 158, 255, 0.08), transparent);
  box-shadow: 0 22px 42px rgba(64, 158, 255, 0.14);
}

.option-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.option-label {
  margin: 0 0 6px;
  color: #2d79c7;
  font-size: 13px;
  font-weight: 700;
}

.option-head h3 {
  margin: 0;
  color: #1f2d3d;
  font-size: 20px;
  line-height: 1.4;
}

.option-state {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #2d79c7;
  font-size: 12px;
  font-weight: 700;
}

.option-summary,
.option-copy {
  margin: 0;
  color: #55687d;
  line-height: 1.8;
}

.option-summary {
  min-height: 72px;
}

.option-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0 14px;
  color: #66788c;
  font-size: 13px;
}

.option-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.option-copy strong {
  margin-right: 4px;
  color: #2f4b68;
}

.option-copy-muted {
  margin-top: 10px;
  color: #738498;
}

.stat-card,
.copy-card {
  padding: 22px;
}

.stat-card span {
  display: block;
  color: #7a8da3;
  margin-bottom: 10px;
}

.stat-card strong {
  font-size: 28px;
  color: #1f2d3d;
}

.copy-card p:last-child {
  margin: 0;
  color: #4f6278;
  line-height: 1.8;
}

.alert-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.alert-chip {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 247, 230, 0.92);
  border: 1px solid rgba(245, 182, 93, 0.45);
  color: #9a6a1f;
  font-size: 13px;
}

.map-section {
  margin-bottom: 20px;
}

.timeline-wrap {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-time {
  color: #409eff;
  font-weight: 700;
}

.stop-card {
  padding: 22px;
}

.stop-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.stop-index {
  margin: 0 0 6px;
  color: #7c8da1;
  font-size: 12px;
}

.stop-head h3 {
  margin: 0;
  color: #1f2d3d;
  font-size: 22px;
}

.stop-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stop-copy,
.stop-note {
  margin: 16px 0 0;
  color: #56697f;
  line-height: 1.8;
}

.stop-note {
  color: #8c6b29;
}

.meta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: #738397;
  margin: 16px 0;
}

.actions-row {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .option-grid,
  .copy-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .stop-head,
  .panel-header {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
