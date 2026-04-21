<template>
  <section v-if="items.length" class="pinned-section">
    <div class="section-head">
      <div>
        <p class="section-kicker">PINNED PICKS</p>
        <h2>本周置顶路线</h2>
      </div>
      <span>管理员精选 · 不与下方动态流重复</span>
    </div>

    <div class="pinned-grid">
      <article
        v-for="item in items"
        :key="item.id"
        class="pinned-card"
        @click="$emit('open', item.id)"
      >
        <img :src="item.coverImageUrl" :alt="item.title" class="cover-image">
        <div class="card-overlay">
          <span class="card-badge">置顶推荐</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.shareNote || item.routeSummary || '这条路线适合被慢慢读完。' }}</p>
          <div class="card-meta">
            <span>{{ item.authorLabel }}</span>
            <span>{{ formatDuration(item.totalDuration) }}</span>
            <span>评论 {{ item.commentCount || 0 }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
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
</script>

<style scoped>
.pinned-section {
  margin-top: 28px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
  margin-bottom: 16px;
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

.pinned-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.pinned-card {
  position: relative;
  min-height: 320px;
  border-radius: 28px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 20px 54px rgba(15, 31, 49, 0.14);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 22px;
  background: linear-gradient(180deg, rgba(16, 26, 37, 0.06), rgba(12, 18, 27, 0.84));
  color: #f9f6f0;
}

.card-badge {
  width: fit-content;
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(252, 236, 210, 0.16);
  border: 1px solid rgba(252, 236, 210, 0.22);
  color: #f7dfb8;
  font-size: 12px;
}

.card-overlay h3 {
  margin: 0;
  font-size: 24px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.card-overlay p {
  margin: 12px 0 0;
  line-height: 1.75;
  color: rgba(245, 240, 232, 0.86);
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
  font-size: 12px;
  color: rgba(245, 240, 232, 0.72);
}

@media (max-width: 1100px) {
  .pinned-grid {
    grid-template-columns: 1fr;
  }
}
</style>