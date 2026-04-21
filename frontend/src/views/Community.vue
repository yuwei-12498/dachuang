<template>
  <div class="community-page">
    <div class="community-shell">
      <CommunityHero
        :total="pinnedRecords.length + total"
        :pinned-count="pinnedRecords.length"
        :theme-count="availableThemes.length"
        @publish="handlePublish"
        @refresh="loadCommunityList"
      />

      <CommunityFilterBar
        :model-sort="sort"
        :model-keyword="keyword"
        :model-theme="theme"
        :themes="availableThemes"
        @update:sort="sort = $event"
        @update:keyword="keyword = $event"
        @update:theme="theme = $event"
        @search="applySearch"
      />

      <section v-if="loadError" class="state-card">
        <el-result icon="error" title="社区加载失败" sub-title="这次没能拿到最新路线帖，可以稍后再试。">
          <template #extra>
            <el-button type="primary" round @click="loadCommunityList">重新加载</el-button>
          </template>
        </el-result>
      </section>

      <template v-else>
        <CommunityPinnedSection :items="pinnedRecords" @open="openCommunityDetail" />

        <section v-loading="loading" class="feed-section">
          <div class="section-head">
            <div>
              <p class="section-kicker">DISCOVER</p>
              <h2>路线动态流</h2>
            </div>
            <span>双列阅读流 · {{ total }} 条结果</span>
          </div>

          <div v-if="records.length" class="feed-grid">
            <CommunityFeedCard
              v-for="item in records"
              :key="item.id"
              :item="item"
              @open="openCommunityDetail"
            />
          </div>

          <el-empty
            v-else
            description="暂时没有符合当前筛选条件的路线帖，换个标签或关键词看看。"
            class="state-card"
          />
        </section>
      </template>

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
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CommunityFeedCard from '@/components/community/CommunityFeedCard.vue'
import CommunityFilterBar from '@/components/community/CommunityFilterBar.vue'
import CommunityHero from '@/components/community/CommunityHero.vue'
import CommunityPinnedSection from '@/components/community/CommunityPinnedSection.vue'
import { reqListCommunityItineraries } from '@/api/itinerary'
import { initAuthState, useAuthState } from '@/store/auth'

const router = useRouter()
const authState = useAuthState()
const loading = ref(false)
const loadError = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const sort = ref('latest')
const keyword = ref('')
const theme = ref('')
const records = ref([])
const pinnedRecords = ref([])
const availableThemes = ref([])

const loadCommunityList = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await reqListCommunityItineraries({
      page: page.value,
      size: size.value,
      sort: sort.value,
      keyword: keyword.value || undefined,
      theme: theme.value || undefined
    })
    records.value = Array.isArray(res?.records) ? res.records : []
    pinnedRecords.value = Array.isArray(res?.pinnedRecords) ? res.pinnedRecords : []
    availableThemes.value = Array.isArray(res?.availableThemes) ? res.availableThemes : []
    total.value = Number(res?.total || 0)
    page.value = Number(res?.page || page.value)
    size.value = Number(res?.size || size.value)
  } catch (error) {
    records.value = []
    pinnedRecords.value = []
    availableThemes.value = []
    total.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const applySearch = () => {
  page.value = 1
  loadCommunityList()
}

const handlePageChange = nextPage => {
  page.value = nextPage
  loadCommunityList()
}

const openCommunityDetail = id => {
  router.push(`/community/${id}`)
}

const handlePublish = async () => {
  await initAuthState()
  if (!authState.user) {
    router.push({
      path: '/auth',
      query: {
        redirect: '/history'
      }
    })
    return
  }
  router.push('/history')
}

onMounted(() => {
  loadCommunityList()
})
</script>

<style scoped>
.community-page {
  min-height: calc(100vh - 64px);
  padding: 32px 20px 56px;
  background:
    radial-gradient(circle at top left, rgba(200, 216, 202, 0.28), transparent 28%),
    radial-gradient(circle at top right, rgba(246, 224, 190, 0.22), transparent 22%),
    linear-gradient(180deg, #f6f1e8 0%, #f9f7f2 38%, #f3f7fa 100%);
}

.community-shell {
  max-width: 1240px;
  margin: 0 auto;
}

.feed-section {
  margin-top: 30px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
  margin-bottom: 18px;
}

.section-kicker {
  margin: 0 0 8px;
  color: #8f7550;
  letter-spacing: 0.16em;
  font-size: 12px;
}

.section-head h2 {
  margin: 0;
  color: #142033;
  font-size: 28px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.section-head span {
  color: #6e7c8e;
  font-size: 13px;
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.state-card {
  margin-top: 24px;
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.96);
  border: 1px solid rgba(221, 211, 194, 0.82);
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 900px) {
  .feed-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>