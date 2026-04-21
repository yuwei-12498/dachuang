<template>
  <section class="community-hero">
    <div class="hero-copy">
      <p class="hero-kicker">COMMUNITY EDITION</p>
      <h1>把路线变成值得被收藏的旅行分享帖</h1>
      <p>
        这里不只是公开列表，而是一条条能被翻阅、借鉴、二次启程的城市路线灵感。
      </p>
      <div class="hero-actions">
        <el-button type="primary" round size="large" @click="$emit('publish')">发布路线帖</el-button>
        <el-button round size="large" @click="$emit('refresh')">刷新社区</el-button>
      </div>
    </div>

    <div class="hero-board">
      <div class="board-card featured">
        <span>精品社区流</span>
        <strong>{{ total }}</strong>
        <small>当前可浏览路线帖</small>
      </div>
      <div class="board-grid">
        <div class="board-card">
          <span>全站置顶</span>
          <strong>{{ pinnedCount }}</strong>
        </div>
        <div class="board-card">
          <span>主题标签</span>
          <strong>{{ themeCount }}</strong>
        </div>
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
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr);
  gap: 22px;
  padding: 30px;
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(131, 174, 138, 0.22), transparent 32%),
    linear-gradient(135deg, rgba(13, 27, 42, 0.96), rgba(35, 52, 71, 0.95));
  color: #f9f3e7;
  box-shadow: 0 28px 80px rgba(11, 24, 39, 0.28);
}

.hero-kicker {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: rgba(244, 223, 184, 0.9);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.15;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.hero-copy p:last-of-type {
  margin: 18px 0 0;
  max-width: 620px;
  color: rgba(241, 233, 220, 0.82);
  line-height: 1.85;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.hero-board {
  display: grid;
  gap: 14px;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.board-card {
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 248, 239, 0.08);
  border: 1px solid rgba(255, 240, 219, 0.14);
  backdrop-filter: blur(14px);
}

.board-card span {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(244, 223, 184, 0.82);
}

.board-card strong {
  display: block;
  margin: 12px 0 6px;
  font-size: 34px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.board-card small {
  color: rgba(241, 233, 220, 0.72);
}

.featured {
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(180deg, rgba(255, 244, 226, 0.16), rgba(255, 244, 226, 0.07));
}

@media (max-width: 900px) {
  .community-hero,
  .board-grid {
    grid-template-columns: 1fr;
  }
}
</style>