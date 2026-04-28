<template>
  <section class="community-hero">
    <div class="hero-copy">
      <p class="hero-kicker">COMMUNITY EDITION</p>
      <h1>把路线变成值得收藏与复用的城市灵感帖</h1>
      <p>
        社区不是简单的公开列表，而是把你满意的路线整理成可浏览、可借鉴、可继续启程的蓝白路线广场。
      </p>
      <div class="hero-pills">
        <span class="hero-pill">蓝白路线广场</span>
        <span class="hero-pill">精选优先浏览</span>
        <span class="hero-pill">看到即可复用</span>
      </div>
      <div class="hero-actions">
        <el-button type="primary" round size="large" class="hero-primary-btn" @click="$emit('publish')">发布路线帖</el-button>
        <el-button round size="large" class="hero-secondary-btn" @click="$emit('refresh')">刷新社区</el-button>
      </div>
    </div>

    <div class="hero-board">
      <article class="board-card featured">
        <span>社区路线帖</span>
        <strong>{{ total }}</strong>
        <small>当前可浏览的公开路线内容</small>
      </article>
      <div class="board-grid">
        <article class="board-card compact">
          <span>全站置顶</span>
          <strong>{{ pinnedCount }}</strong>
        </article>
        <article class="board-card compact">
          <span>主题标签</span>
          <strong>{{ themeCount }}</strong>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  total: {
    type: Number,
    default: 0
  },
  pinnedCount: {
    type: Number,
    default: 0
  },
  themeCount: {
    type: Number,
    default: 0
  }
})

defineEmits(['publish', 'refresh'])
</script>

<style scoped>
.community-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.9fr);
  gap: 28px;
  padding: 34px;
  border-radius: var(--radius-panel);
  border: 1px solid rgba(188, 214, 255, 0.84);
  background:
    radial-gradient(circle at top right, rgba(136, 192, 255, 0.24), transparent 26%),
    radial-gradient(circle at bottom left, rgba(214, 233, 255, 0.52), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(242, 248, 255, 0.96));
  box-shadow: var(--shadow-soft);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-kicker {
  margin: 0 0 12px;
  color: var(--brand-600);
  font-size: 12px;
  letter-spacing: 0.18em;
}

.hero-copy h1 {
  margin: 0;
  max-width: 680px;
  color: var(--text-strong);
  font-size: clamp(32px, 4vw, 50px);
  line-height: 1.08;
  font-family: var(--font-display);
}

.hero-copy p:last-of-type {
  margin: 18px 0 0;
  max-width: 640px;
  color: var(--text-body);
  line-height: 1.9;
}

.hero-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-pill {
  padding: 9px 14px;
  border-radius: 999px;
  border: 1px solid rgba(188, 214, 255, 0.84);
  background: rgba(246, 250, 255, 0.92);
  color: var(--text-body);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.hero-primary-btn {
  box-shadow: 0 14px 30px rgba(95, 158, 255, 0.24);
}

.hero-secondary-btn {
  border-color: rgba(188, 214, 255, 0.84);
  background: rgba(255, 255, 255, 0.84);
  color: var(--text-strong);
}

.hero-board,
.board-grid {
  display: grid;
  gap: 14px;
}

.board-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.board-card {
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid rgba(188, 214, 255, 0.84);
  background: rgba(255, 255, 255, 0.78);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 14px 30px rgba(81, 120, 177, 0.08);
}

.board-card span {
  display: block;
  color: var(--text-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.board-card strong {
  display: block;
  margin: 10px 0 6px;
  color: var(--text-strong);
  font-size: 34px;
  font-family: var(--font-display);
}

.board-card small {
  color: var(--text-body);
}

.featured {
  min-height: 186px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.94), rgba(236, 245, 255, 0.94));
}

.compact strong {
  font-size: 28px;
}

@media (max-width: 900px) {
  .community-hero,
  .board-grid {
    grid-template-columns: 1fr;
  }
}
</style>
