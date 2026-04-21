<template>
  <article class="feed-card" @click="$emit('open', item.id)">
    <div class="cover-wrap">
      <img :src="item.coverImageUrl" :alt="item.title" class="cover-image">
      <span v-if="item.globalPinned" class="cover-pin">置顶</span>
    </div>

    <div class="card-body">
      <p class="card-date">{{ item.tripDate || '随时出发' }}</p>
      <h3>{{ item.title || '未命名路线帖' }}</h3>
      <p class="card-note">{{ item.shareNote || item.routeSummary || '留一点空白，让路线自己说话。' }}</p>

      <div class="theme-row">
        <span v-for="theme in (item.themes || []).slice(0, 3)" :key="theme" class="theme-chip">{{ theme }}</span>
      </div>

      <div class="metric-row">
        <span>{{ formatDuration(item.totalDuration) }}</span>
        <span>{{ formatCurrency(item.totalCost) }}</span>
        <span>{{ item.nodeCount || 0 }} 站</span>
      </div>
    </div>

    <footer class="card-footer">
      <div>
        <strong>{{ item.authorLabel || '匿名旅人' }}</strong>
        <small>{{ item.routeSummary || '城市慢游路线' }}</small>
      </div>
      <div class="interaction">
        <span>♡ {{ item.likeCount || 0 }}</span>
        <span>💬 {{ item.commentCount || 0 }}</span>
      </div>
    </footer>
  </article>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true
  }
})

defineEmits(['open'])

const formatDuration = minutes => {
  if (!minutes && minutes !== 0) return '--'
  const hour = Math.floor(minutes / 60)
  const minute = minutes % 60
  if (!hour) return `${minute} 分钟`
  if (!minute) return `${hour} 小时`
  return `${hour} 小时 ${minute} 分钟`
}

const formatCurrency = value => {
  if (value === null || value === undefined || value === '') return '--'
  return `¥${value}`
}
</script>

<style scoped>
.feed-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(255, 252, 247, 0.96);
  border: 1px solid rgba(221, 211, 194, 0.82);
  box-shadow: 0 20px 52px rgba(17, 32, 49, 0.08);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.feed-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 28px 64px rgba(17, 32, 49, 0.12);
  border-color: rgba(142, 164, 148, 0.58);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 1.2;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-pin {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(18, 33, 51, 0.82);
  color: #f5ead8;
  font-size: 12px;
}

.card-body {
  padding: 20px 20px 0;
}

.card-date {
  margin: 0;
  color: #8f7751;
  font-size: 12px;
  letter-spacing: 0.14em;
}

.card-body h3 {
  margin: 10px 0 0;
  color: #182435;
  font-size: 24px;
  line-height: 1.3;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.card-note {
  margin: 12px 0 0;
  min-height: 54px;
  color: #5e6b79;
  line-height: 1.75;
}

.theme-row,
.metric-row,
.interaction {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.theme-row {
  margin-top: 16px;
}

.theme-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(211, 226, 215, 0.46);
  color: #405a4a;
  font-size: 12px;
}

.metric-row {
  margin-top: 16px;
  color: #6c7783;
  font-size: 13px;
}

.card-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: end;
  padding: 20px;
}

.card-footer strong {
  display: block;
  color: #1f2f45;
}

.card-footer small {
  display: block;
  margin-top: 6px;
  color: #7b8795;
}

.interaction {
  justify-content: flex-end;
  color: #66707a;
  font-size: 13px;
}
</style>