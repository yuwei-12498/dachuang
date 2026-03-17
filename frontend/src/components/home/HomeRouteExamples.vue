<template>
  <section class="examples-section" id="examples">
    <div class="examples-container">
      <div class="section-header">
        <h2 class="section-title">灵感来源于真实的足迹</h2>
        <p class="section-subtitle">看看系统为不同偏好生成的样板路线，也许其中就有你的心仪之选</p>
      </div>

      <el-row :gutter="30" class="examples-row">
        <!-- 样例 1 -->
        <el-col :xs="24" :md="12">
          <div class="example-card">
            <div class="card-cover cover-1">
              <div class="cover-tags">
                <el-tag effect="dark" type="success" size="small">两日深度游</el-tag>
                <el-tag effect="dark" type="warning" size="small">网红打卡</el-tag>
              </div>
            </div>
            <div class="card-content">
              <h3 class="ex-title">寻味锦官城：周末特种兵觅食记</h3>
              <p class="ex-desc">周六直奔建设路扫街，避开下午两点人流高峰夜游九眼桥；周日早起打卡大熊猫基地，紧接春熙路扫货。</p>
              <div class="ex-meta">
                <span>预算：￥400/人</span>
                <span class="dot">·</span>
                <span>包含 9 个点位</span>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 样例 2 -->
        <el-col :xs="24" :md="12">
          <div class="example-card">
            <div class="card-cover cover-2">
              <div class="cover-tags">
                <el-tag effect="dark" color="#a18cd1" size="small" style="border: none">半天闲逛</el-tag>
                <el-tag effect="dark" color="#409EFF" size="small" style="border: none">低步行强度</el-tag>
              </div>
            </div>
            <div class="card-content">
              <h3 class="ex-title">带娃听雨：悠闲的室内早教之旅</h3>
              <p class="ex-desc">因应雨天偏好，系统将原本室外采风自动切为成都自然博物馆半日巡游，搭配就近的亲子无障碍餐厅午饭，轻松省心。</p>
              <div class="ex-meta">
                <span>预算：￥120/人</span>
                <span class="dot">·</span>
                <span>包含 3 个点位</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </section>
</template>

<script setup>
// 无需外部导入
</script>

<style scoped>
.examples-section {
  padding: 100px 24px;
  background-color: #f7f8fa;
}

.examples-container {
  max-width: 1000px; /* 故意收窄以提升质感 */
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-title {
  font-size: 32px;
  font-weight: 800;
  color: #1f2d3d;
  margin: 0 0 16px 0;
}

.section-subtitle {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

.examples-row {
  display: flex;
  flex-wrap: wrap;
}

.example-card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  height: 100%;
}

.example-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

.card-cover {
  height: 200px;
  position: relative;
  background-size: cover;
  background-position: center;
}

/* 采用柔和的高级纯色块替代无图时的空洞感 */
.cover-1 {
  background: linear-gradient(120deg, #fdfbfb 0%, #ebedee 100%);
  border-bottom: 1px solid #f0f2f5;
}

.cover-2 {
  background: linear-gradient(120deg, #e0c3fc 0%, #8ec5fc 100%);
}

.cover-tags {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  gap: 8px;
}

.card-content {
  padding: 24px;
}

.ex-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px 0;
}

.ex-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 20px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ex-meta {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.dot {
  margin: 0 8px;
  font-weight: bold;
}

@media (max-width: 768px) {
  .example-card {
    margin-bottom: 30px;
  }
}
</style>
