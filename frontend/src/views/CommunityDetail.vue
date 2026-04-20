<template>
  <div class="community-detail-page">
    <div class="community-detail-shell" v-if="detail">
      <section class="hero-card">
        <div>
          <p class="eyebrow">{{ text.eyebrow }}</p>
          <h1>{{ detail.title || text.untitledRoute }}</h1>
          <p class="hero-meta">
            {{ detail.authorLabel || text.anonymousAuthor }} / {{ formatDateTime(detail.updatedAt) }}
          </p>
          <p v-if="detail.shareNote" class="hero-note">{{ detail.shareNote }}</p>
        </div>
        <div class="hero-actions">
          <el-button
            round
            :type="detail.liked ? 'danger' : 'default'"
            :loading="likeSubmitting"
            @click="toggleLike"
          >
            {{ detail.liked ? text.unlike : text.like }} {{ detail.likeCount || 0 }}
          </el-button>
          <el-button round @click="goBack">{{ text.back }}</el-button>
          <el-button round @click="goHome">{{ text.home }}</el-button>
        </div>
      </section>

      <section class="stats-row">
        <el-card shadow="never" class="stat-card">
          <span>{{ text.duration }}</span>
          <strong>{{ formatDuration(detail.totalDuration) }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>{{ text.cost }}</span>
          <strong>{{ formatCurrency(detail.totalCost) }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>{{ text.likes }}</span>
          <strong>{{ detail.likeCount || 0 }}</strong>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <span>{{ text.comments }}</span>
          <strong>{{ detail.commentCount || comments.length }}</strong>
        </el-card>
      </section>

      <section class="summary-grid">
        <el-card shadow="never" class="summary-card">
          <p class="eyebrow">{{ text.routeSummary }}</p>
          <p>{{ detail.routeSummary || text.emptyRouteSummary }}</p>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <p class="eyebrow">{{ text.tripWindow }}</p>
          <p>{{ detail.tripDate || '--' }} / {{ detail.startTime || '--' }} - {{ detail.endTime || '--' }}</p>
        </el-card>
      </section>

      <section v-if="detail.highlights?.length" class="tags-section">
        <el-tag
          v-for="highlight in detail.highlights"
          :key="highlight"
          size="small"
          effect="plain"
        >
          {{ highlight }}
        </el-tag>
      </section>

      <section class="timeline-wrap">
        <div
          v-for="node in detail.nodes || []"
          :key="node.poiId"
          class="timeline-item"
        >
          <div class="timeline-time">{{ node.startTime }} - {{ node.endTime }}</div>
          <el-card shadow="never" class="stop-card">
            <div class="stop-head">
              <div>
                <p class="stop-index">{{ text.stopPrefix }}{{ node.stepOrder }}</p>
                <h3>{{ node.poiName }}</h3>
              </div>
              <div class="stop-tags">
                <el-tag size="small" effect="plain">{{ node.category }}</el-tag>
                <el-tag size="small" type="info" effect="plain">{{ node.district || text.unknownDistrict }}</el-tag>
              </div>
            </div>

            <p class="stop-copy">{{ node.sysReason }}</p>

            <div class="meta-row">
              <span>{{ text.travelTime }}{{ node.travelTime || 0 }} {{ text.minute }}</span>
              <span>{{ text.stayTime }}{{ node.stayDuration || 0 }} {{ text.minute }}</span>
              <span>{{ text.stopCost }}{{ formatCurrency(node.cost) }}</span>
            </div>
          </el-card>
        </div>
      </section>

      <section class="comment-panel">
        <div class="comment-header">
          <div>
            <p class="eyebrow">{{ text.comments }}</p>
            <h2>{{ text.commentTitle }}</h2>
          </div>
          <el-button v-if="!authState.user" round @click="goLogin">{{ text.loginToComment }}</el-button>
        </div>

        <div v-if="authState.user" class="composer">
          <div v-if="replyingTo" class="reply-tip">
            <span>{{ text.replyingTo }} {{ replyingTo.authorLabel }}</span>
            <el-button link type="primary" @click="setReplyTarget(null)">{{ text.cancelReply }}</el-button>
          </div>
          <el-input
            v-model="commentDraft"
            type="textarea"
            :rows="3"
            :maxlength="300"
            show-word-limit
            :placeholder="replyingTo ? text.replyPlaceholder : text.commentPlaceholder"
          />
          <div class="composer-actions">
            <el-button v-if="replyingTo" round @click="setReplyTarget(null)">{{ text.cancelReply }}</el-button>
            <el-button type="primary" round :loading="commentSubmitting" @click="submitComment">
              {{ replyingTo ? text.publishReply : text.publishComment }}
            </el-button>
          </div>
        </div>

        <div v-if="comments.length" class="comment-list">
          <el-card v-for="comment in comments" :key="comment.id" shadow="never" class="comment-card">
            <div class="comment-top">
              <strong>{{ comment.authorLabel }}</strong>
              <span>{{ formatDateTime(comment.createTime) }}</span>
            </div>
            <p>{{ comment.content }}</p>
            <div class="comment-actions">
              <el-button link type="primary" @click="setReplyTarget(comment)">{{ text.reply }}</el-button>
            </div>

            <div v-if="comment.replies?.length" class="reply-list">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="comment-top">
                  <strong>{{ reply.authorLabel }}</strong>
                  <span>{{ formatDateTime(reply.createTime) }}</span>
                </div>
                <p>{{ reply.content }}</p>
              </div>
            </div>
          </el-card>
        </div>

        <el-empty v-else :description="text.emptyComments" />
      </section>
    </div>

    <div class="community-detail-shell" v-else-if="loadError">
      <el-result icon="error" :title="text.loadFailed" :sub-title="text.loadFailedTip">
        <template #extra>
          <el-button type="primary" round @click="loadPage">{{ text.retry }}</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  reqCreateCommunityComment,
  reqGetCommunityItinerary,
  reqLikeCommunityItinerary,
  reqListCommunityComments,
  reqUnlikeCommunityItinerary
} from '@/api/itinerary'
import { initAuthState, useAuthState } from '@/store/auth'

const text = {
  eyebrow: '\u793E\u533A\u5206\u4EAB',
  untitledRoute: '\u672A\u547D\u540D\u8DEF\u7EBF',
  anonymousAuthor: '\u533F\u540D\u65C5\u4EBA',
  back: '\u8FD4\u56DE\u793E\u533A',
  home: '\u8FD4\u56DE\u9996\u9875',
  duration: '\u603B\u65F6\u957F',
  cost: '\u603B\u8D39\u7528',
  likes: '\u70B9\u8D5E',
  comments: '\u8BC4\u8BBA',
  routeSummary: '\u8DEF\u7EBF\u6458\u8981',
  tripWindow: '\u51FA\u884C\u65F6\u95F4',
  emptyRouteSummary: '\u6682\u65E0\u8DEF\u7EBF\u6458\u8981',
  stopPrefix: '\u7B2C ',
  unknownDistrict: '\u57CE\u533A\u5F85\u5B9A',
  travelTime: '\u8DEF\u4E0A ',
  stayTime: '\u505C\u7559 ',
  stopCost: '\u82B1\u8D39 ',
  minute: '\u5206\u949F',
  commentTitle: '\u8BF4\u8BF4\u4F60\u7684\u770B\u6CD5',
  loginToComment: '\u767B\u5F55\u540E\u8BC4\u8BBA',
  commentPlaceholder: '\u5199\u4E0B\u4F60\u5BF9\u8FD9\u6761\u8DEF\u7EBF\u7684\u611F\u53D7\u3001\u8865\u5145\u5EFA\u8BAE\u6216\u907F\u5751\u63D0\u9192\u3002',
  replyPlaceholder: '\u5199\u4E0B\u4F60\u7684\u8865\u5145\u60F3\u6CD5\u6216\u56DE\u590D\u5185\u5BB9\u3002',
  publishComment: '\u53D1\u8868\u8BC4\u8BBA',
  publishReply: '\u53D1\u8868\u56DE\u590D',
  like: '\u70B9\u8D5E',
  unlike: '\u5DF2\u8D5E',
  reply: '\u56DE\u590D',
  cancelReply: '\u53D6\u6D88\u56DE\u590D',
  replyingTo: '\u6B63\u5728\u56DE\u590D',
  emptyComments: '\u8FD8\u6CA1\u6709\u4EBA\u7559\u8A00\uFF0C\u6765\u505A\u7B2C\u4E00\u4E2A\u5206\u4EAB\u8005\u5427\u3002',
  loadFailed: '\u793E\u533A\u52A8\u6001\u52A0\u8F7D\u5931\u8D25',
  loadFailedTip: '\u53EF\u80FD\u662F\u8DEF\u7EBF\u5DF2\u53D6\u6D88\u516C\u5F00\u6216\u6682\u65F6\u65E0\u6CD5\u8BBF\u95EE\u3002',
  retry: '\u91CD\u8BD5',
  commentSuccess: '\u8BC4\u8BBA\u5DF2\u53D1\u8868',
  replySuccess: '\u56DE\u590D\u5DF2\u53D1\u8868',
  commentEmpty: '\u8BF7\u5148\u8F93\u5165\u4E00\u70B9\u60F3\u6CD5\u518D\u53D1\u5E03',
  commentLogin: '\u8BF7\u5148\u767B\u5F55\u540E\u518D\u8BC4\u8BBA',
  likeLogin: '\u8BF7\u5148\u767B\u5F55\u540E\u518D\u70B9\u8D5E'
}

const route = useRoute()
const router = useRouter()
const authState = useAuthState()
const detail = ref(null)
const comments = ref([])
const loadError = ref(false)
const commentDraft = ref('')
const commentSubmitting = ref(false)
const likeSubmitting = ref(false)
const replyingTo = ref(null)

const itineraryId = Number(route.params.id)

const loadPage = async () => {
  loadError.value = false
  try {
    const [detailRes, commentRes] = await Promise.all([
      reqGetCommunityItinerary(itineraryId),
      reqListCommunityComments(itineraryId)
    ])
    detail.value = detailRes
    comments.value = Array.isArray(commentRes) ? commentRes : []
  } catch (error) {
    detail.value = null
    comments.value = []
    loadError.value = true
  }
}

const setReplyTarget = comment => {
  if (comment && !authState.user) {
    ElMessage.warning(text.commentLogin)
    goLogin()
    return
  }
  replyingTo.value = comment
}

const submitComment = async () => {
  if (!authState.user) {
    ElMessage.warning(text.commentLogin)
    goLogin()
    return
  }
  if (!commentDraft.value.trim()) {
    ElMessage.warning(text.commentEmpty)
    return
  }

  commentSubmitting.value = true
  try {
    const comment = await reqCreateCommunityComment(itineraryId, {
      content: commentDraft.value.trim(),
      parentId: replyingTo.value?.id || null
    })

    if (comment.parentId) {
      comments.value = comments.value.map(item => (
        item.id === comment.parentId
          ? { ...item, replies: [...(item.replies || []), comment] }
          : item
      ))
    } else {
      comments.value = [...comments.value, { ...comment, replies: comment.replies || [] }]
    }

    detail.value = {
      ...detail.value,
      commentCount: Number(detail.value?.commentCount || 0) + 1
    }
    commentDraft.value = ''
    replyingTo.value = null
    ElMessage.success(comment.parentId ? text.replySuccess : text.commentSuccess)
  } finally {
    commentSubmitting.value = false
  }
}

const toggleLike = async () => {
  if (!authState.user) {
    ElMessage.warning(text.likeLogin)
    goLogin()
    return
  }

  likeSubmitting.value = true
  try {
    detail.value = detail.value?.liked
      ? await reqUnlikeCommunityItinerary(itineraryId)
      : await reqLikeCommunityItinerary(itineraryId)
  } finally {
    likeSubmitting.value = false
  }
}

const goBack = () => {
  router.push('/community')
}

const goHome = () => {
  router.push('/')
}

const goLogin = () => {
  router.push({
    path: '/auth',
    query: {
      redirect: route.fullPath
    }
  })
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

const formatDateTime = value => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

onMounted(async () => {
  await initAuthState()
  await loadPage()
})
</script>

<style scoped>
.community-detail-page {
  min-height: calc(100vh - 64px);
  padding: 32px 20px 48px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #f6f9fe 0%, #f7f8fa 100%);
}

.community-detail-shell {
  max-width: 1100px;
  margin: 0 auto;
}

.hero-card,
.stat-card,
.summary-card,
.stop-card,
.comment-panel,
.comment-card {
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

.hero-meta,
.hero-note {
  color: #607185;
  line-height: 1.8;
}

.hero-note {
  margin: 14px 0 0;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(246, 250, 255, 0.96);
  border: 1px solid rgba(220, 230, 242, 0.9);
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.stats-row,
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stat-card,
.summary-card,
.stop-card,
.comment-panel {
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

.summary-card p:last-child {
  margin: 0;
  color: #4f6278;
  line-height: 1.8;
}

.tags-section {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.timeline-wrap {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 24px;
}

.timeline-time {
  color: #409eff;
  font-weight: 700;
}

.stop-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.stop-index {
  margin: 0 0 6px;
  color: #2d79c7;
  font-size: 13px;
  font-weight: 700;
}

.stop-head h3 {
  margin: 0;
  color: #1f2d3d;
}

.stop-tags,
.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.stop-copy {
  color: #55687d;
  line-height: 1.8;
}

.meta-row {
  margin-top: 14px;
  color: #66788c;
  font-size: 13px;
}

.comment-header,
.composer-actions,
.comment-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.comment-header {
  margin-bottom: 16px;
}

.composer {
  margin-bottom: 18px;
}

.reply-tip {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(246, 250, 255, 0.96);
  color: #5c6f84;
}

.composer-actions {
  margin-top: 12px;
  justify-content: flex-end;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-card p {
  margin: 12px 0 0;
  color: #516478;
  line-height: 1.8;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.reply-list {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(223, 232, 244, 0.9);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 251, 255, 0.95);
}

.comment-top span {
  color: #7a8da3;
  font-size: 13px;
}

@media (max-width: 900px) {
  .hero-card,
  .summary-grid,
  .stop-head,
  .comment-header,
  .reply-tip {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
