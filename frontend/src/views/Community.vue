<template>
  <div class="community-page">
    <div class="community-shell">
      <section class="hero-card">
        <div>
          <p class="eyebrow">Community MVP</p>
          <h1>{{ text.title }}</h1>
          <p class="hero-copy">{{ text.heroCopy }}</p>
        </div>
        <div class="hero-actions">
          <el-button round @click="router.push('/')">{{ text.backHome }}</el-button>
          <el-button type="primary" round @click="loadCommunityList">{{ text.refresh }}</el-button>
        </div>
      </section>

      <section class="stats-row">
        <el-card shadow="never" class="stat-card">
          <span>{{ text.currentPage }}</span>
          <strong>{{ displayPage }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>{{ text.publicRoutes }}</span>
          <strong>{{ displayTotal }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>{{ text.pageSize }}</span>
          <strong>{{ displaySize }}</strong>
        </el-card>
      </section>

      <section v-loading="loading">
        <el-result
          v-if="loadError"
          icon="error"
          :title="text.loadErrorTitle"
          :sub-title="text.loadErrorSubtitle"
          class="error-panel"
        >
          <template #extra>
            <el-button type="primary" round @click="loadCommunityList">{{ text.retry }}</el-button>
          </template>
        </el-result>

        <div v-else-if="records.length" class="card-grid">
          <el-card
            v-for="item in records"
            :key="item.id"
            shadow="never"
            class="community-card"
            @click="openCommunityDetail(item.id)"
          >
            <div class="card-top">
              <div>
                <p class="card-kicker">{{ item.authorLabel || text.anonymousAuthor }}</p>
                <h2>{{ item.title || text.untitledRoute }}</h2>
              </div>
              <span class="card-badge">{{ text.publicShared }}</span>
            </div>

            <div class="meta-strip">
              <span>{{ text.durationLabel }}{{ formatDuration(item.totalDuration) }}</span>
              <span>{{ text.costLabel }}{{ formatCurrency(item.totalCost) }}</span>
              <span>{{ text.nodeCountLabel }}{{ item.nodeCount || 0 }}</span>
            </div>

            <p class="route-summary">{{ item.routeSummary || text.emptyRouteSummary }}</p>
            <p v-if="item.shareNote" class="share-note">{{ item.shareNote }}</p>

            <div class="highlight-list">
              <el-tag
                v-for="highlight in item.highlights || []"
                :key="`${item.id}-${highlight}`"
                size="small"
                effect="plain"
              >
                {{ highlight }}
              </el-tag>
            </div>

            <div class="card-footer">
              <p class="footer-copy">{{ text.authorLabel }}{{ item.authorLabel || text.anonymousAuthor }}</p>
              <span class="comment-count">{{ text.commentCount }}{{ item.commentCount || 0 }}</span>
            </div>
          </el-card>
        </div>

        <el-empty
          v-else
          :description="text.emptyDescription"
        />
      </section>

      <div v-if="!loadError && total > size" class="pager-wrap">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { reqListCommunityItineraries } from '@/api/itinerary'

const text = {
  title: '\u793E\u533A\u5927\u5385',
  heroCopy: '\u6D4F\u89C8\u5176\u4ED6\u7528\u6237\u516C\u5F00\u5206\u4EAB\u7684\u77ED\u9014\u8DEF\u7EBF\uFF0C\u5FEB\u901F\u611F\u53D7\u4E0D\u540C\u4E3B\u9898\u3001\u65F6\u957F\u548C\u9884\u7B97\u4E0B\u7684\u57CE\u5E02\u73A9\u6CD5\u3002',
  backHome: '\u8FD4\u56DE\u9996\u9875',
  refresh: '\u5237\u65B0\u5217\u8868',
  currentPage: '\u5F53\u524D\u9875\u7801',
  publicRoutes: '\u516C\u5F00\u8DEF\u7EBF',
  pageSize: '\u6BCF\u9875\u5C55\u793A',
  anonymousAuthor: '\u533F\u540D\u65C5\u4EBA',
  untitledRoute: '\u672A\u547D\u540D\u8DEF\u7EBF',
  publicShared: '\u516C\u5F00\u5206\u4EAB',
  durationLabel: '\u603B\u65F6\u957F\uFF1A',
  costLabel: '\u603B\u8D39\u7528\uFF1A',
  nodeCountLabel: '\u8DEF\u7EBF\u70B9\u4F4D\uFF1A',
  emptyRouteSummary: '\u6682\u65E0\u8DEF\u7EBF\u6458\u8981',
  authorLabel: '\u4F5C\u8005\u6807\u8BC6\uFF1A',
  commentCount: '\u8BC4\u8BBA ',
  emptyDescription: '\u793E\u533A\u5927\u5385\u8FD8\u6CA1\u6709\u516C\u5F00\u8DEF\u7EBF\uFF0C\u5148\u53BB\u751F\u6210\u5E76\u516C\u5F00\u4E00\u6761\u5427\u3002',
  loadErrorTitle: '\u793E\u533A\u5217\u8868\u52A0\u8F7D\u5931\u8D25',
  loadErrorSubtitle: '\u8FD9\u6B21\u6CA1\u80FD\u53D6\u5230\u6700\u65B0\u7684\u516C\u5F00\u8DEF\u7EBF\uFF0C\u53EF\u4EE5\u7A0D\u540E\u91CD\u8BD5\u3002',
  retry: '\u91CD\u8BD5'
}

const router = useRouter()
const loading = ref(false)
const loadError = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const records = ref([])
const displayPage = computed(() => (loadError.value ? '--' : page.value))
const displayTotal = computed(() => (loadError.value ? '--' : total.value))
const displaySize = computed(() => (loadError.value ? '--' : size.value))

const loadCommunityList = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await reqListCommunityItineraries({
      page: page.value,
      size: size.value
    })
    records.value = Array.isArray(res?.records) ? res.records : []
    total.value = Number(res?.total || 0)
    page.value = Number(res?.page || page.value)
    size.value = Number(res?.size || size.value)
  } catch (error) {
    records.value = []
    total.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const handlePageChange = nextPage => {
  page.value = nextPage
  loadCommunityList()
}

const openCommunityDetail = id => {
  router.push(`/community/${id}`)
}

const formatDuration = minutes => {
  if (!minutes && minutes !== 0) return '--'
  const hour = Math.floor(minutes / 60)
  const minute = minutes % 60
  if (hour === 0) return `${minute} \u5206\u949F`
  if (minute === 0) return `${hour} \u5C0F\u65F6`
  return `${hour} \u5C0F\u65F6 ${minute} \u5206\u949F`
}

const formatCurrency = value => {
  if (value === null || value === undefined || value === '') return '--'
  return `\u00A5${value}`
}

onMounted(() => {
  loadCommunityList()
})
</script>

<style scoped>
.community-page {
  min-height: calc(100vh - 64px);
  padding: 32px 20px 48px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.14), transparent 30%),
    linear-gradient(180deg, #f4f9ff 0%, #f7f8fa 100%);
}

.community-shell {
  max-width: 1160px;
  margin: 0 auto;
}

.hero-card,
.stat-card,
.community-card,
.error-panel {
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

.hero-card h1 {
  margin: 0 0 10px;
  color: #1f2d3d;
  font-size: 34px;
}

.hero-copy {
  margin: 0;
  max-width: 640px;
  color: #607185;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.stat-card {
  padding: 20px 22px;
}

.stat-card span {
  display: block;
  color: #7a8da3;
  margin-bottom: 10px;
}

.stat-card strong {
  color: #1f2d3d;
  font-size: 28px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.community-card {
  padding: 22px;
  min-height: 260px;
  cursor: pointer;
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.card-kicker {
  margin: 0 0 8px;
  color: #7b8ea4;
  font-size: 13px;
}

.community-card h2 {
  margin: 0;
  color: #1f2d3d;
  font-size: 22px;
  line-height: 1.5;
}

.card-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.12);
  color: #2d79c7;
  font-size: 12px;
  font-weight: 700;
}

.meta-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin: 18px 0 14px;
  color: #61748a;
  font-size: 13px;
}

.route-summary {
  margin: 0 0 14px;
  color: #41556b;
  font-size: 15px;
  line-height: 1.8;
}

.share-note {
  margin: 0 0 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(246, 250, 255, 0.96);
  border: 1px solid rgba(220, 230, 242, 0.9);
  color: #54677b;
  line-height: 1.8;
}

.highlight-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 42px;
}

.card-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.footer-copy {
  margin: 0;
  color: #7a8da3;
  font-size: 13px;
}

.comment-count {
  color: #5f7287;
  font-size: 13px;
  font-weight: 600;
}

.pager-wrap {
  margin-top: 26px;
  display: flex;
  justify-content: center;
}

.error-panel {
  padding: 12px 8px;
}

@media (max-width: 900px) {
  .hero-card,
  .stats-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
