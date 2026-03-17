<template>
  <div class="detail-wrapper">
    <div class="mb-24">
      <el-button @click="goBack" icon="ArrowLeft">返回路线总览</el-button>
    </div>

    <template v-if="poiDetail">
      <el-card class="poi-info-card" shadow="sm">
        <div class="card-title-row">
          <h2 class="poi-title">{{ poiDetail.name }}</h2>
          <el-tag type="info" size="large">{{ poiDetail.district }}</el-tag>
        </div>
        
        <div class="tags-container mb-24">
          <el-tag v-for="tag in (poiDetail.tags || '').split(',')" :key="tag" effect="plain" class="mr-8">{{ tag }}</el-tag>
          <el-tag type="warning" v-if="poiDetail.indoor === 1" effect="light" class="mr-8">室内场馆</el-tag>
        </div>
        
        <el-descriptions border :column="2" direction="vertical" class="custom-desc">
          <el-descriptions-item label="建议停留">{{ poiDetail.stayDuration }} 分钟</el-descriptions-item>
          <el-descriptions-item label="预估人均消费">¥{{ poiDetail.avgCost }}</el-descriptions-item>
          <el-descriptions-item label="开放时间">{{ poiDetail.openTime || '全天' }} - {{ poiDetail.closeTime || '全天' }}</el-descriptions-item>
          <el-descriptions-item label="步行强度期望">{{ poiDetail.walkingLevel }}</el-descriptions-item>
          <el-descriptions-item label="适合人群" :span="2">{{ poiDetail.suitableFor }}</el-descriptions-item>
          <el-descriptions-item label="景点介绍" :span="2">
            <div class="desc-text">{{ poiDetail.description }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 替换面板组件 -->
      <el-card class="replace-card" shadow="sm">
        <template #header>
          <div class="replace-header">
            <el-icon class="replace-icon"><Warning /></el-icon>
            <span class="replace-title">需要调整行程吗？</span>
          </div>
        </template>
        <div class="replace-content">
          <p class="replace-hint">如果不满意当前点位，系统可自动在“同区域”或“同分类”中为您挑选其他合适的地点并重新安排您的时间线。</p>
          <el-button type="default" size="large" :loading="replacing" @click="handleReplace" icon="Switch">
            替换为相似点位
          </el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { reqGetPoiDetail } from '@/api/poi'
import { reqReplacePoi } from '@/api/itinerary'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const poiDetail = ref(null)
const replacing = ref(false)

const targetPoiId = Number(route.params.id)

onMounted(async () => {
  try {
    const res = await reqGetPoiDetail(targetPoiId)
    poiDetail.value = res
  } catch (err) {
    ElMessage.error('无法获取点位详情')
  }
})

const goBack = () => {
  router.back()
}

const handleReplace = async () => {
  const currentItineraryStr = sessionStorage.getItem('current_itinerary')
  const originalReqStr = sessionStorage.getItem('original_req_form')
  
  if (!currentItineraryStr) {
    ElMessage.warning('未能找到当前行程上下文，请重新生成')
    router.push('/')
    return
  }

  const currentNodes = JSON.parse(currentItineraryStr).nodes
  const originalReq = originalReqStr ? JSON.parse(originalReqStr) : {}

  replacing.value = true
  try {
    const req = {
      targetPoiId: targetPoiId,
      currentNodes: currentNodes,
      originalReq: originalReq
    }
    
    const res = await reqReplacePoi(req)
    sessionStorage.setItem('current_itinerary', JSON.stringify(res))
    ElMessage.success('成功替换为新地点！时间轴已更新。')
    
    setTimeout(() => {
      router.push('/result')
    }, 500)
    
  } catch (err) {
  } finally {
    replacing.value = false
  }
}
</script>

<style scoped>
.detail-wrapper {
  max-width: 760px;
  margin: 0 auto;
  padding-bottom: 40px;
}
.mb-24 {
  margin-bottom: 24px;
}
.mr-8 {
  margin-right: 8px;
}

.poi-info-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  padding: 10px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.poi-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2d3d;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.custom-desc :deep(.el-descriptions__label) {
  font-weight: 600;
  color: #475669;
  background-color: #fafafa;
}
.desc-text {
  line-height: 1.6;
  color: #5e6d82;
}

.replace-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.03);
}
.replace-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.replace-icon {
  color: #E6A23C;
  font-size: 18px;
}
.replace-title {
  font-weight: 600;
  color: #1f2d3d;
  font-size: 16px;
}
.replace-content {
  padding: 10px 0;
}
.replace-hint {
  color: #5e6d82;
  margin: 0 0 20px 0;
  font-size: 14px;
  line-height: 1.5;
}
</style>
