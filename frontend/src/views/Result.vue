<template>
  <div class="result-wrapper">
    <div class="header-actions">
      <el-button @click="goBack" icon="ArrowLeft">修改偏好</el-button>
      <el-button type="primary" plain @click="handleReplan" :loading="replanning" icon="Sort">
        智能顺滑重排
      </el-button>
    </div>

    <!-- 增加一个明确的时间范围约束提示栏 -->
    <el-alert 
      v-if="originalReq"
      :title="`你的时间约束范围：${originalReq.startTime} 出发 ～ 最晚 ${originalReq.endTime} 结束`" 
      type="info" 
      :closable="false" 
      class="mb-24 time-constraint-alert" 
      show-icon
    />

    <template v-if="itinerary">
      <el-card class="summary-card" shadow="sm">
        <h3 class="card-title">路线概览</h3>
        <div class="stats">
          <span class="stat-item">
            <el-icon><Timer /></el-icon> 
            预计总跨度: <strong>{{ Math.floor(itinerary.totalDuration / 60) }}小时{{ itinerary.totalDuration % 60 }}分钟</strong>
          </span>
          <span class="stat-item">
            <el-icon><Wallet /></el-icon> 
            预计总开销: <strong>¥{{ itinerary.totalCost }}</strong>
          </span>
        </div>
        <el-alert :title="itinerary.recommendReason" type="success" :closable="false" class="mt-12" />
        <el-alert :title="itinerary.tips" type="warning" :closable="false" class="mt-12" />
      </el-card>

      <el-card class="timeline-card" shadow="sm">
        <div class="timeline-header">
          <h3 class="card-title">定制时间轴</h3>
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="(node, index) in itinerary.nodes"
            :key="node.poiId"
            :timestamp="node.startTime + ' - ' + node.endTime"
            placement="top"
            type="primary"
            size="large"
          >
            <!-- 若存在通行时间（即非第一个点），绘制一段通行路程标识 -->
            <div v-if="node.travelTime > 0" class="travel-time-indicator">
              <el-icon><Van /></el-icon> 预计通行约 {{ node.travelTime }} 分钟
            </div>

            <el-card shadow="hover" class="node-card">
              <div class="node-header">
                <h4 class="poi-name">{{ node.stepOrder }}. {{ node.poiName }}</h4>
                <div class="tags">
                  <el-tag size="small">{{ node.category }}</el-tag>
                  <el-tag size="small" type="info">{{ node.district }}</el-tag>
                </div>
              </div>
              <p class="node-reason">{{ node.sysReason }}</p>
              <div class="node-footer">
                <span>⏱️ 游玩停留: {{ node.stayDuration }} 分钟</span>
                <span>💰 预估花销: ¥{{ node.cost }}</span>
              </div>
              <div class="node-actions">
                <el-button size="small" @click="goToDetail(node)">查看详情与找平替</el-button>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
    
    <el-empty v-else description="暂无行程数据，请先生成" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { reqReplanItinerary } from '@/api/itinerary'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const itinerary = ref(null)
const replanning = ref(false)
const originalReq = ref(null)

onMounted(() => {
  const data = sessionStorage.getItem('current_itinerary')
  if (data) {
    itinerary.value = JSON.parse(data)
  }
  
  const originalReqStr = sessionStorage.getItem('original_req_form')  
  if (originalReqStr) {
    originalReq.value = JSON.parse(originalReqStr)
  }
})

const goBack = () => {
  router.push('/')
}

const goToDetail = (node) => {
  router.push(`/detail/${node.poiId}`)
}

const handleReplan = async () => {
  if (!itinerary.value || !itinerary.value.nodes) return
  
  replanning.value = true
  try {
    const req = { 
      currentNodes: itinerary.value.nodes,
      originalReq: originalReq.value
    }
    const res = await reqReplanItinerary(req)
    
    // res 应该是包含 success, changed, reason, itinerary 的新结构
    if (res.success) {
      if (res.changed) {
        itinerary.value = res.itinerary
        sessionStorage.setItem('current_itinerary', JSON.stringify(res.itinerary))
        ElMessage.success({
          message: res.message || '重排成功！为您尝试了新形态顺流组合',
          duration: 3000
        })
      } else {
        // 没有发生更好变化的时候明确提醒
        ElMessageBox.alert(
          res.reason || '当前路线已经较优，没有更好的重排选项了。',
          '重排提示',
          {
            confirmButtonText: '尝试去替换某个点位',
            type: 'info'
          }
        )
      }
    } else {
      ElMessage.warning(res.message || '重排异常')
    }
  } catch (err) {
  } finally {
    replanning.value = false
  }
}
</script>

<style scoped>
.result-wrapper {
  max-width: 760px;
  margin: 0 auto;
  padding-bottom: 40px;
}
.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

.mb-24 {
  margin-bottom: 24px;
}
.time-constraint-alert {
  border-radius: 8px;
  font-weight: 600;
}

.summary-card, .timeline-card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.card-title {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
}

.stats {
  display: flex;
  gap: 24px;
  color: #475669;
  font-size: 15px;
  background: #f9fafc;
  padding: 16px;
  border-radius: 8px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mt-12 {
  margin-top: 12px;
}

.travel-time-indicator {
  margin-bottom: 12px;
  margin-left: -24px;
  font-size: 13px;
  color: #E6A23C;
  font-weight: bold;
  background: #fdf6ec;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 0 12px 12px 0;
}

.node-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: none;
  transition: box-shadow 0.2s;
}
.node-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.node-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.poi-name {
  margin: 0;
  font-size: 16px;
  color: #1f2d3d;
  font-weight: 600;
}
.tags {
  display: flex;
  gap: 6px;
}
.node-reason {
  color: #5e6d82;
  font-size: 14px;
  background: #f4f4f5;
  padding: 10px 14px;
  border-radius: 6px;
  margin: 12px 0;
  border-left: 3px solid #dcdfe6;
}
.node-footer {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #8492a6;
  margin-bottom: 16px;
}
.node-actions {
  text-align: right;
  border-top: 1px dashed #ebeef5;
  padding-top: 12px;
}

::v-deep(.el-timeline-item__timestamp) {
  font-size: 15px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 12px;
}
</style>
