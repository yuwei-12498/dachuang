<template>
  <div class="result-page">
    <div v-if="itinerary" class="result-shell">
      <section class="hero-card">
        <div>
          <p class="eyebrow">已保存的路线快照</p>
          <h1>{{ itinerary.customTitle || '你的专属路线' }}</h1>
          <p class="hero-copy">
            {{ originalReq?.tripDate || '--' }} / {{ originalReq?.startTime || '09:00' }} - {{ originalReq?.endTime || '18:00' }}
          </p>
        </div>
        <div class="hero-actions">
          <el-button round @click="goBack">返回首页</el-button>
          <el-button round @click="goCommunity">社区大厅</el-button>
          <el-button v-if="isLoggedIn" round @click="goHistory">历史行程</el-button>
          <el-button v-if="isLoggedIn && itinerary.isPublic" round @click="openCommunityPost">查看社区帖子</el-button>
          <el-button
            v-if="isLoggedIn"
            round
            :type="itinerary.favorited ? 'warning' : 'default'"
            :loading="favoriteLoading"
            @click="handleFavorite"
          >
            {{ itinerary.favorited ? '取消收藏' : '收藏行程' }}
          </el-button>
          <el-button
            v-if="isLoggedIn"
            round
            :loading="publicLoading"
            @click="handleTogglePublic"
          >
            {{ itinerary.isPublic ? '撤回社区展示' : '发布路线帖' }}
          </el-button>
          <el-button round :loading="posterLoading" @click="handleGeneratePoster">生成分享海报</el-button>
          <el-button type="primary" round :loading="replanning" @click="handleReplan">换一版路线</el-button>
          <el-button v-if="!isLoggedIn" round class="login-action-btn" @click="goLoginForSavedActions">登录后保存与分享</el-button>
        </div>
      </section>

      <section v-if="!isLoggedIn" class="guest-tip-card">
        <div>
          <p class="eyebrow">游客模式</p>
          <h2>先看路线，喜欢再登录</h2>
          <p class="guest-tip-copy">
            你现在可以直接查看地图、对比方案、换一版路线和导出海报。登录后再解锁收藏、历史记录和社区发布。
          </p>
        </div>
        <div class="guest-tip-actions">
          <el-button type="primary" round @click="goLoginForSavedActions">登录后继续</el-button>
        </div>
      </section>

      <section v-if="displayOptions.length" class="option-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">方案对比</p>
            <h2>先比较，再决定用哪条路线出发</h2>
            <p class="panel-copy">
              发布路线帖时会默认采用当前选中的方案，海报与社区展示也会同步使用这条路线版本。
            </p>
          </div>
          <div class="option-counter">候选方案 {{ displayOptions.length }}</div>
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
                {{ option.optionKey === activeOptionKey ? '当前展示' : '点击切换' }}
              </span>
            </div>

            <p class="option-summary">{{ option.summary }}</p>

            <div class="option-metrics">
              <span>总时长 {{ formatDuration(option.totalDuration) }}</span>
              <span>总费用 ¥{{ option.totalCost }}</span>
              <span>点位 {{ option.stopCount }}</span>
            </div>

            <div v-if="option.highlights?.length" class="option-tags">
              <el-tag
                v-for="item in option.highlights"
                :key="`${option.optionKey}-${item}`"
                size="small"
                effect="plain"
              >
                {{ item }}
              </el-tag>
            </div>

            <p class="option-copy"><strong>推荐：</strong>{{ option.recommendReason }}</p>
            <p class="option-copy option-copy-muted"><strong>取舍：</strong>{{ option.notRecommendReason }}</p>
          </button>
        </div>
      </section>

      <section class="stats-grid">
        <el-card shadow="never" class="stat-card">
          <span>总时长</span>
          <strong>{{ formatDuration(activeOption?.totalDuration) }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>总费用</span>
          <strong>¥{{ activeOption?.totalCost ?? itinerary.totalCost }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>路线点位</span>
          <strong>{{ activeOption?.stopCount ?? activeNodes.length }}</strong>
        </el-card>
      </section>

      <section class="copy-grid">
        <el-card shadow="never" class="copy-card">
          <p class="eyebrow">推荐理由</p>
          <p>{{ activeOption?.recommendReason || itinerary.recommendReason }}</p>
        </el-card>
        <el-card shadow="never" class="copy-card">
          <p class="eyebrow">执行提醒</p>
          <p>{{ itinerary.tips }}</p>
        </el-card>
        <el-card shadow="never" class="copy-card">
          <p class="eyebrow">社区状态</p>
          <p>{{ shareStatusText }}</p>
        </el-card>
      </section>

      <section v-if="activeAlerts.length" class="alert-strip">
        <span v-for="item in activeAlerts" :key="item" class="alert-chip">{{ item }}</span>
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
                <p class="stop-index">第 {{ node.stepOrder }} 站</p>
                <h3>{{ node.poiName }}</h3>
              </div>
              <div class="stop-tags">
                <el-tag size="small" effect="plain">{{ node.category }}</el-tag>
                <el-tag size="small" type="info" effect="plain">{{ node.district || '城区待定' }}</el-tag>
              </div>
            </div>

            <p class="stop-copy">{{ node.sysReason }}</p>
            <p v-if="node.statusNote" class="stop-note">{{ node.statusNote }}</p>

            <div class="meta-row">
              <span>路上 {{ node.travelTime || 0 }} 分钟</span>
              <span>停留 {{ node.stayDuration || 0 }} 分钟</span>
              <span>花费 ¥{{ node.cost }}</span>
            </div>

            <div class="actions-row">
              <el-button round @click="goToDetail(node)">{{ detailActionText }}</el-button>
            </div>
          </el-card>
        </div>
      </section>

      <div ref="posterRef" class="poster-stage">
        <div class="poster-card">
          <div class="poster-header">
            <p class="poster-brand">行城有数 | 分享海报</p>
            <h2>{{ posterTitle }}</h2>
            <p class="poster-subtitle">
              {{ originalReq?.tripDate || '--' }} · {{ formatDuration(activeOption?.totalDuration || itinerary.totalDuration) }}
            </p>
          </div>

          <div class="poster-summary">
            <div class="poster-metric">
              <span>总费用</span>
              <strong>¥{{ activeOption?.totalCost ?? itinerary.totalCost }}</strong>
            </div>
            <div class="poster-metric">
              <span>点位数</span>
              <strong>{{ activeNodes.length }}</strong>
            </div>
            <div class="poster-metric">
              <span>亮点数</span>
              <strong>{{ posterHighlights.length }}</strong>
            </div>
          </div>

          <div class="poster-section">
            <p class="poster-label">路线亮点</p>
            <div class="poster-tags">
              <span v-for="highlight in posterHighlights" :key="highlight" class="poster-tag">{{ highlight }}</span>
            </div>
          </div>

          <div class="poster-section">
            <p class="poster-label">结构化时间线</p>
            <div class="poster-timeline">
              <div v-for="node in activeNodes" :key="`poster-${node.poiId}`" class="poster-node">
                <div class="poster-node-time">{{ node.startTime }} - {{ node.endTime }}</div>
                <div class="poster-node-content">
                  <strong>{{ node.poiName }}</strong>
                  <span>{{ node.category }} / {{ node.district || '城区待定' }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="poster-grid">
            <div class="poster-section">
              <p class="poster-label">推荐理由</p>
              <p class="poster-copy">{{ activeOption?.recommendReason || itinerary.recommendReason }}</p>
            </div>
            <div class="poster-section map-preview">
              <p class="poster-label">地图区域预估</p>
              <div class="map-estimate">
                <span v-for="district in posterDistricts" :key="district" class="map-pill">{{ district }}</span>
                <p>{{ posterRoutePreview }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="result-shell">
      <el-empty description="暂时没有找到行程，请先回首页生成。" />
    </div>

    <PublishRouteDialog
      v-model="publishDialogVisible"
      :itinerary="itinerary"
      @published="handlePublished"
    />
  </div>
</template>

<script setup>
import html2canvas from 'html2canvas'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PublishRouteDialog from '@/components/community/PublishRouteDialog.vue'
import ItineraryMapCard from '@/components/itinerary/ItineraryMapCard.vue'
import {
  reqFavoriteItinerary,
  reqGenerateItinerary,
  reqGetItinerary,
  reqGetLatestItinerary,
  reqReplanItinerary,
  reqToggleItineraryPublic,
  reqUnfavoriteItinerary
} from '@/api/itinerary'
import { useAuthState } from '@/store/auth'
import {
  loadItinerarySnapshot,
  localizeItineraryText,
  normalizeItinerarySnapshot,
  saveItinerarySnapshot
} from '@/store/itinerary'

const router = useRouter()
const route = useRoute()
const authState = useAuthState()
const itinerary = ref(null)
const replanning = ref(false)
const favoriteLoading = ref(false)
const publicLoading = ref(false)
const posterLoading = ref(false)
const posterRef = ref(null)
const publishDialogVisible = ref(false)
const publishAutoOpened = ref(false)

const originalReq = computed(() => itinerary.value?.originalReq || null)
const isLoggedIn = computed(() => Boolean(authState.user))
const routeItineraryId = computed(() => {
  const raw = Number(route.query.id)
  return Number.isFinite(raw) && raw > 0 ? raw : null
})

const buildRouteSignature = nodes => {
  return (nodes || [])
    .map(node => node?.poiId)
    .filter(Boolean)
    .join('-')
}

const buildFallbackOption = snapshot => {
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

const resolveOptions = snapshot => {
  if (Array.isArray(snapshot?.options) && snapshot.options.length) {
    return snapshot.options
  }
  return snapshot ? [buildFallbackOption(snapshot)] : []
}

const resolveActiveOption = snapshot => {
  const options = resolveOptions(snapshot)
  if (!options.length) {
    return null
  }
  return options.find(option => option.optionKey === snapshot?.selectedOptionKey) || options[0]
}

const resolveActiveNodes = snapshot => {
  const option = resolveActiveOption(snapshot)
  return Array.isArray(option?.nodes) ? option.nodes : (snapshot?.nodes || [])
}

const ensureSeenRouteSignatures = snapshot => {
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

const persistItinerary = snapshot => {
  if (!snapshot) {
    itinerary.value = null
    return null
  }

  const nextSnapshot = ensureSeenRouteSignatures(normalizeItinerarySnapshot(snapshot))
  itinerary.value = nextSnapshot
  saveItinerarySnapshot(nextSnapshot)
  return nextSnapshot
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
      persistItinerary(data)
    } catch (error) {
      itinerary.value = null
    }
    return
  }

  if (snapshot) {
    itinerary.value = ensureSeenRouteSignatures(snapshot)
    return
  }

  if (isLoggedIn.value) {
    try {
      const latest = await reqGetLatestItinerary()
      if (latest) {
        persistItinerary(latest)
      }
    } catch (error) {
      itinerary.value = null
    }
  } else {
    itinerary.value = null
  }
}

onMounted(() => {
  loadCurrentItinerary()
})

watch(() => route.query.id, () => {
  loadCurrentItinerary()
})

watch(isLoggedIn, loggedIn => {
  if (loggedIn && !itinerary.value) {
    loadCurrentItinerary()
  }
})

watch(
  () => `${route.query.id || ''}:${route.query.publish || ''}`,
  () => {
    publishAutoOpened.value = false
  }
)

watch(
  [() => route.query.publish, isLoggedIn, () => itinerary.value?.id, () => itinerary.value?.isPublic],
  ([publish, loggedIn, id, isPublic]) => {
    if (publish === '1' && loggedIn && id && !isPublic && !publishAutoOpened.value) {
      publishAutoOpened.value = true
      publishDialogVisible.value = true
    }
  },
  { immediate: true }
)

const displayOptions = computed(() => resolveOptions(itinerary.value))
const activeOption = computed(() => resolveActiveOption(itinerary.value))
const activeOptionKey = computed(() => activeOption.value?.optionKey || null)
const activeNodes = computed(() => resolveActiveNodes(itinerary.value))
const detailActionText = computed(() => (isLoggedIn.value ? '查看详情并替换' : '登录后替换站点'))
const activeAlerts = computed(() => {
  if (Array.isArray(activeOption.value?.alerts) && activeOption.value.alerts.length) {
    return activeOption.value.alerts
  }
  return Array.isArray(itinerary.value?.alerts) ? itinerary.value.alerts : []
})
const shareStatusText = computed(() => {
  if (!isLoggedIn.value) {
    return '当前是游客查看模式，登录后才能收藏路线、保存到历史并发布到社区。'
  }
  return itinerary.value?.isPublic
    ? '这条路线已发布到社区，社区大厅和帖子详情页都能看到它。'
    : '这条路线目前仅自己可见，点击“发布路线帖”后即可进入社区展示。'
})

const posterTitle = computed(() => itinerary.value?.customTitle || activeOption.value?.title || '城市轻游路线')
const posterHighlights = computed(() => {
  if (Array.isArray(activeOption.value?.highlights) && activeOption.value.highlights.length) {
    return activeOption.value.highlights.slice(0, 4)
  }
  return activeNodes.value.map(node => node.poiName).filter(Boolean).slice(0, 4)
})
const posterDistricts = computed(() => {
  const districts = [...new Set(activeNodes.value.map(node => node.district).filter(Boolean))]
  return districts.length ? districts.slice(0, 4) : ['城市核心区']
})
const posterRoutePreview = computed(() => {
  const names = activeNodes.value.map(node => node.poiName).filter(Boolean)
  if (!names.length) {
    return '当前路线会根据点位坐标和城区分布自动预估海报中的地图区域。'
  }
  return `覆盖区域：${posterDistricts.value.join(' / ')}；路线顺序：${names.join(' → ')}`
})

const formatDuration = minutes => {
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

const goCommunity = () => {
  router.push('/community')
}

const goHistory = () => {
  if (!ensureLogin('查看历史行程')) return
  router.push('/history')
}

const openCommunityPost = () => {
  if (!itinerary.value?.id) return
  router.push(`/community/${itinerary.value.id}`)
}

const goLoginForSavedActions = () => {
  router.push({
    path: '/auth',
    query: {
      redirect: route.fullPath
    }
  })
}

const goToDetail = node => {
  if (!ensureLogin('查看详情并替换站点')) return
  router.push(`/detail/${node.poiId}`)
}

const ensureLogin = (actionText = '继续操作') => {
  if (isLoggedIn.value) {
    return true
  }
  ElMessage.warning(`${actionText}需要先登录`)
  router.push({
    path: '/auth',
    query: {
      redirect: route.fullPath
    }
  })
  return false
}

const handleSelectOption = optionKey => {
  if (!itinerary.value) return
  itinerary.value = ensureSeenRouteSignatures({
    ...itinerary.value,
    selectedOptionKey: optionKey
  })
  saveItinerarySnapshot(itinerary.value)
}

const handleFavorite = async () => {
  if (!ensureLogin('收藏这条路线')) return
  if (!itinerary.value?.id) return

  favoriteLoading.value = true
  try {
    if (itinerary.value.favorited) {
      await reqUnfavoriteItinerary(itinerary.value.id)
      itinerary.value = {
        ...itinerary.value,
        favorited: false,
        favoriteTime: null
      }
      saveItinerarySnapshot(itinerary.value)
      ElMessage.success('已取消收藏')
      return
    }

    const suggestedTitle = itinerary.value.customTitle || activeOption.value?.title || `${originalReq.value?.tripDate || '本次'}路线`
    const promptResult = await ElMessageBox.prompt(
      '给这条路线起个名字，后面回顾会更方便。',
      '收藏当前路线',
      {
        confirmButtonText: '收藏',
        cancelButtonText: '取消',
        inputValue: suggestedTitle,
        inputPlaceholder: '例如：周末春熙路轻松逛',
        inputValidator: value => {
          if (!value || !value.trim()) return '请输入路线名称'
          if (value.trim().length > 60) return '路线名称不能超过 60 个字'
          return true
        }
      }
    )

    const nextItinerary = await reqFavoriteItinerary(itinerary.value.id, {
      selectedOptionKey: activeOptionKey.value,
      title: promptResult.value.trim()
    })
    persistItinerary(nextItinerary)
    ElMessage.success('已加入收藏')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || '收藏失败，请稍后重试')
    }
  } finally {
    favoriteLoading.value = false
  }
}

const clearPublishQuery = () => {
  if (route.query.publish !== '1') return
  const nextQuery = { ...route.query }
  delete nextQuery.publish
  router.replace({ path: route.path, query: nextQuery })
}

const handlePublished = async payload => {
  const nextSnapshot = persistItinerary(payload)
  publishDialogVisible.value = false
  clearPublishQuery()
  ElMessage.success('路线帖已发布到社区')

  if (!nextSnapshot?.id) {
    router.push('/community')
    return
  }

  try {
    await ElMessageBox.confirm(
      '路线帖已经发布完成，现在去查看帖子详情吗？',
      '发布成功',
      {
        confirmButtonText: '查看帖子',
        cancelButtonText: '回社区大厅',
        type: 'success',
        distinguishCancelAndClose: true
      }
    )
    openCommunityPost()
  } catch (action) {
    if (action === 'cancel' || action === 'close') {
      router.push('/community')
    }
  }
}

const handleTogglePublic = async () => {
  if (!ensureLogin('发布这条路线')) return
  if (!itinerary.value?.id) return

  if (!itinerary.value.isPublic) {
    publishDialogVisible.value = true
    return
  }

  publicLoading.value = true
  try {
    await ElMessageBox.confirm(
      '撤回后，这条路线会从社区中隐藏，但你的历史行程和收藏不会丢失。',
      '撤回社区展示',
      {
        confirmButtonText: '确认撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const nextItinerary = await reqToggleItineraryPublic(itinerary.value.id, { isPublic: false })
    persistItinerary(nextItinerary)
    clearPublishQuery()
    ElMessage.success('已撤回社区展示')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      const message = error?.response?.data?.message || '社区展示状态更新失败，请稍后重试'
      ElMessage.error(message)
    }
  } finally {
    publicLoading.value = false
  }
}

const handleGeneratePoster = async () => {
  if (!posterRef.value || !activeNodes.value.length) {
    ElMessage.warning('当前行程暂无可导出的内容')
    return
  }

  posterLoading.value = true
  try {
    await nextTick()
    const canvas = await html2canvas(posterRef.value, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#f4f8ff'
    })
    const dataUrl = canvas.toDataURL('image/png', 1)
    const link = document.createElement('a')
    link.href = dataUrl
    link.download = `${posterTitle.value || 'itinerary'}-poster.png`
    link.click()
    ElMessage.success('分享海报已生成，并开始下载')
  } catch (error) {
    ElMessage.error('海报生成失败，请稍后重试')
  } finally {
    posterLoading.value = false
  }
}

const handleReplan = async () => {
  if (!itinerary.value || !activeNodes.value.length) return

  replanning.value = true
  try {
    if (!itinerary.value.id) {
      if (!originalReq.value) {
        ElMessage.warning('当前没有可用的行程参数')
        return
      }
      const nextItinerary = await reqGenerateItinerary(originalReq.value)
      persistItinerary(nextItinerary)
      ElMessage.success('已重新生成一版新路线')
      return
    }

    const currentSignature = buildRouteSignature(activeNodes.value)
    const excludedSignatures = Array.isArray(itinerary.value.seenRouteSignatures)
      ? [...new Set([...itinerary.value.seenRouteSignatures, currentSignature].filter(Boolean))]
      : currentSignature ? [currentSignature] : []

    const res = await reqReplanItinerary({
      itineraryId: itinerary.value.id,
      currentNodes: activeNodes.value,
      originalReq: originalReq.value,
      excludedSignatures
    })

    if (res.success && res.changed) {
      const nextItinerary = ensureSeenRouteSignatures(normalizeItinerarySnapshot(res.itinerary))
      const nextSignature = buildRouteSignature(resolveActiveNodes(nextItinerary))
      nextItinerary.seenRouteSignatures = [...new Set([...excludedSignatures, nextSignature].filter(Boolean))]
      itinerary.value = nextItinerary
      saveItinerarySnapshot(itinerary.value)
      ElMessage.success(localizeItineraryText(res.message) || '已为你换出一组新的路线方案。')
      return
    }

    if (res.success) {
      await ElMessageBox.alert(
        localizeItineraryText(res.reason) || '当前条件下没有更优路线了，建议先保留这条路线。',
        '保留当前路线',
        { confirmButtonText: '确定', type: 'info' }
      )
      return
    }

    ElMessage.warning(localizeItineraryText(res.message) || '换线失败，请稍后重试。')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || '换线失败，请稍后重试。')
    }
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
.guest-tip-card,
.option-panel,
.stat-card,
.copy-card,
.stop-card {
  border-radius: 24px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.06);
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

.login-action-btn {
  border-color: #cfe0f5;
}

.guest-tip-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 22px 24px;
  margin-bottom: 20px;
}

.guest-tip-card h2 {
  margin: 0 0 8px;
  color: #1f2d3d;
  font-size: 26px;
}

.guest-tip-copy {
  margin: 0;
  color: #627386;
  line-height: 1.8;
}

.guest-tip-actions {
  flex-shrink: 0;
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

.option-grid,
.stats-grid,
.copy-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
}

.option-card.active {
  border-color: rgba(64, 158, 255, 0.72);
  box-shadow: 0 22px 42px rgba(64, 158, 255, 0.14);
}

.option-head,
.stop-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.option-label,
.stop-index {
  margin: 0 0 6px;
  color: #2d79c7;
  font-size: 13px;
  font-weight: 700;
}

.option-head h3,
.stop-head h3 {
  margin: 0;
  color: #1f2d3d;
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
.option-copy,
.stop-copy,
.stop-note {
  line-height: 1.8;
  color: #55687d;
}

.option-metrics,
.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin: 16px 0 14px;
  color: #66788c;
  font-size: 13px;
}

.option-tags,
.stop-tags,
.alert-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-copy-muted,
.stop-note {
  color: #738498;
}

.stat-card,
.copy-card,
.stop-card {
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
  margin: 18px 0;
}

.alert-chip {
  padding: 10px 14px;
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

.timeline-time {
  color: #409eff;
  font-weight: 700;
}

.actions-row {
  display: flex;
  justify-content: flex-end;
}

.poster-stage {
  position: fixed;
  left: -99999px;
  top: 0;
  width: 960px;
  padding: 24px;
  background: #f4f8ff;
}

.poster-card {
  padding: 28px;
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(64, 158, 255, 0.18), transparent 26%),
    linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  box-shadow: 0 20px 50px rgba(31, 45, 61, 0.12);
}

.poster-brand,
.poster-label {
  margin: 0 0 10px;
  color: #2d79c7;
  font-size: 13px;
  font-weight: 700;
}

.poster-header h2 {
  margin: 0;
  font-size: 34px;
  color: #1f2d3d;
}

.poster-subtitle,
.poster-copy {
  color: #607185;
  line-height: 1.8;
}

.poster-summary,
.poster-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.poster-metric,
.poster-section {
  padding: 18px;
  border-radius: 20px;
  background: rgba(246, 250, 255, 0.96);
  border: 1px solid rgba(220, 230, 242, 0.9);
}

.poster-metric span {
  display: block;
  color: #7a8da3;
  margin-bottom: 8px;
}

.poster-metric strong {
  color: #1f2d3d;
  font-size: 24px;
}

.poster-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.poster-tag,
.map-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #2d79c7;
  font-size: 13px;
}

.poster-timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.poster-node {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 14px;
  align-items: center;
}

.poster-node-time {
  color: #409eff;
  font-weight: 700;
}

.poster-node-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #506377;
}

.map-preview {
  grid-column: span 2;
}

.map-estimate {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.map-estimate p {
  width: 100%;
  margin: 8px 0 0;
  color: #607185;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .option-grid,
  .stats-grid,
  .copy-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hero-card,
  .guest-tip-card,
  .panel-header,
  .stop-head {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
