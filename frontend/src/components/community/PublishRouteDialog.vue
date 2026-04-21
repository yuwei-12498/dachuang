<template>
  <el-dialog
    :model-value="modelValue"
    width="920px"
    destroy-on-close
    class="publish-dialog"
    @close="handleClose"
  >
    <template #header>
      <div class="dialog-header">
        <p class="dialog-kicker">PUBLISH ROUTE POST</p>
        <h2>发布路线帖</h2>
        <span>从“公开”升级为真正可阅读、可浏览、可互动的社区分享帖。</span>
      </div>
    </template>

    <el-steps :active="activeStep" finish-status="success" class="step-bar">
      <el-step title="确认路线版本" />
      <el-step title="补充分享内容" />
      <el-step title="发布预览" />
    </el-steps>

    <section v-if="activeStep === 0" class="step-panel version-panel">
      <div
        v-for="option in displayOptions"
        :key="option.optionKey"
        class="version-card"
        :class="{ active: form.selectedOptionKey === option.optionKey }"
        @click="form.selectedOptionKey = option.optionKey"
      >
        <div>
          <p class="version-label">{{ option.title }}</p>
          <h3>{{ option.subtitle }}</h3>
        </div>
        <div class="version-meta">
          <span>{{ formatDuration(option.totalDuration) }}</span>
          <span>¥{{ option.totalCost ?? 0 }}</span>
          <span>{{ option.stopCount }} 站</span>
        </div>
        <p class="version-copy">{{ option.summary || option.recommendReason || '选一条最适合被分享出去的路线版本。' }}</p>
      </div>
    </section>

    <section v-else-if="activeStep === 1" class="step-panel form-panel">
      <el-form label-position="top">
        <el-form-item label="帖子标题">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="例如：周末武汉江滩慢游路线" />
        </el-form-item>
        <el-form-item label="分享语">
          <el-input
            v-model="form.shareNote"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="这条路线适合谁、什么时候去、最推荐的亮点是什么？"
          />
        </el-form-item>
        <el-form-item label="主题标签（最多 3 个）">
          <el-select
            v-model="form.themes"
            multiple
            allow-create
            filterable
            default-first-option
            collapse-tags
            collapse-tags-tooltip
            placeholder="例如：Citywalk、拍照、夜游"
            @change="limitThemes"
          >
            <el-option v-for="theme in themeSuggestions" :key="theme" :label="theme" :value="theme" />
          </el-select>
        </el-form-item>
      </el-form>
    </section>

    <section v-else class="step-panel preview-panel">
      <div class="preview-grid">
        <article class="preview-card hero-preview">
          <img :src="coverImage" :alt="form.title || previewOption.title" class="preview-image">
          <div class="preview-overlay">
            <span class="preview-badge">路线分享帖</span>
            <h3>{{ form.title || previewOption.title }}</h3>
            <p>{{ form.shareNote || previewOption.summary || '把路线写成别人愿意点开的故事。' }}</p>
          </div>
        </article>

        <article class="preview-card info-preview">
          <p class="mini-kicker">首页卡片预览</p>
          <h4>{{ form.title || previewOption.title }}</h4>
          <p class="preview-copy">{{ form.shareNote || previewOption.summary || '把路线写成别人愿意点开的故事。' }}</p>
          <div class="chip-row">
            <span v-for="theme in form.themes" :key="theme" class="preview-chip">{{ theme }}</span>
          </div>
          <div class="preview-metrics">
            <span>{{ formatDuration(previewOption.totalDuration) }}</span>
            <span>¥{{ previewOption.totalCost ?? 0 }}</span>
            <span>{{ previewOption.stopCount }} 站</span>
          </div>
        </article>
      </div>
    </section>

    <template #footer>
      <div class="dialog-actions">
        <el-button round @click="handleClose">取消</el-button>
        <el-button v-if="activeStep > 0" round @click="activeStep -= 1">上一步</el-button>
        <el-button v-if="activeStep < 2" type="primary" round @click="goNext">下一步</el-button>
        <el-button v-else type="primary" round :loading="submitting" @click="submitPublish">确认发布</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { reqToggleItineraryPublic } from '@/api/itinerary'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  itinerary: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'published'])

const activeStep = ref(0)
const submitting = ref(false)
const form = reactive({
  selectedOptionKey: '',
  title: '',
  shareNote: '',
  themes: []
})

const buildRouteSignature = nodes => (nodes || []).map(node => node?.poiId).filter(Boolean).join('-')

const buildFallbackOption = snapshot => {
  const nodes = Array.isArray(snapshot?.nodes) ? snapshot.nodes : []
  return {
    optionKey: snapshot?.selectedOptionKey || 'default',
    title: '当前默认方案',
    subtitle: '从当前保存版本直接发布',
    signature: buildRouteSignature(nodes),
    totalDuration: snapshot?.totalDuration || 0,
    totalCost: snapshot?.totalCost || 0,
    stopCount: nodes.length,
    summary: snapshot?.recommendReason || '',
    recommendReason: snapshot?.recommendReason || '',
    nodes
  }
}

const displayOptions = computed(() => {
  if (Array.isArray(props.itinerary?.options) && props.itinerary.options.length) {
    return props.itinerary.options.map(option => ({
      ...option,
      title: option.title || '备选路线',
      subtitle: option.subtitle || '适合当前分享的路线版本',
      stopCount: Array.isArray(option.nodes) ? option.nodes.length : Number(option.stopCount || 0),
      summary: option.summary || option.recommendReason || '',
      signature: option.signature || buildRouteSignature(option.nodes)
    }))
  }
  return props.itinerary ? [buildFallbackOption(props.itinerary)] : []
})

const previewOption = computed(() => {
  return displayOptions.value.find(item => item.optionKey === form.selectedOptionKey) || displayOptions.value[0] || buildFallbackOption(props.itinerary)
})

const themeSuggestions = computed(() => {
  const originalThemes = Array.isArray(props.itinerary?.originalReq?.themes) ? props.itinerary.originalReq.themes : []
  const optionHighlights = Array.isArray(previewOption.value?.highlights) ? previewOption.value.highlights : []
  return [...new Set([...originalThemes, ...optionHighlights].filter(Boolean))]
})

const coverImage = computed(() => props.itinerary?.coverImageUrl || props.itinerary?.coverImage || previewOption.value?.coverImageUrl || 'https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=1400&q=80')

const initForm = () => {
  activeStep.value = 0
  const options = displayOptions.value
  const selectedOptionKey = props.itinerary?.selectedOptionKey || options[0]?.optionKey || 'default'
  form.selectedOptionKey = selectedOptionKey
  form.title = props.itinerary?.customTitle || options[0]?.title || ''
  form.shareNote = props.itinerary?.shareNote || ''
  form.themes = Array.isArray(props.itinerary?.originalReq?.themes) ? [...props.itinerary.originalReq.themes].slice(0, 3) : []
}

watch(() => props.modelValue, value => {
  if (value) {
    initForm()
  }
}, { immediate: true })

const limitThemes = values => {
  form.themes = (values || []).map(item => `${item}`.trim()).filter(Boolean).slice(0, 3)
}

const goNext = () => {
  if (activeStep.value === 0 && !form.selectedOptionKey) {
    ElMessage.warning('请先选择要发布的路线版本')
    return
  }
  if (activeStep.value === 1 && !form.title.trim()) {
    ElMessage.warning('请先填写帖子标题')
    return
  }
  activeStep.value += 1
}

const submitPublish = async () => {
  if (!props.itinerary?.id) return
  submitting.value = true
  try {
    const result = await reqToggleItineraryPublic(props.itinerary.id, {
      isPublic: true,
      title: form.title.trim(),
      shareNote: form.shareNote.trim(),
      selectedOptionKey: form.selectedOptionKey,
      themes: form.themes
    })
    emit('published', {
      ...result,
      originalReq: {
        ...(props.itinerary?.originalReq || {}),
        themes: form.themes
      }
    })
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
}

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
.dialog-header h2 {
  margin: 8px 0 0;
  color: #142033;
  font-size: 30px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.dialog-header span,
.dialog-kicker {
  color: #7a6a55;
}

.dialog-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.18em;
}

.step-bar {
  margin-bottom: 24px;
}

.step-panel {
  min-height: 360px;
}

.version-panel {
  display: grid;
  gap: 14px;
}

.version-card {
  padding: 20px;
  border-radius: 24px;
  border: 1px solid rgba(220, 210, 194, 0.82);
  background: rgba(255, 251, 244, 0.92);
  cursor: pointer;
  transition: all 0.22s ease;
}

.version-card.active {
  border-color: rgba(23, 41, 63, 0.28);
  background: rgba(238, 245, 239, 0.9);
  box-shadow: 0 18px 36px rgba(17, 32, 49, 0.08);
}

.version-label,
.mini-kicker {
  margin: 0;
  color: #8f7751;
  font-size: 12px;
  letter-spacing: 0.14em;
}

.version-card h3,
.info-preview h4 {
  margin: 10px 0 0;
  color: #182435;
  font-size: 22px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.version-meta,
.preview-metrics,
.chip-row {
  display: flex;
  gap: 10px 12px;
  flex-wrap: wrap;
}

.version-meta,
.preview-metrics {
  margin-top: 12px;
  color: #61707f;
  font-size: 13px;
}

.version-copy,
.preview-copy {
  margin: 12px 0 0;
  color: #5e6b79;
  line-height: 1.75;
}

.preview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.85fr);
  gap: 18px;
}

.preview-card {
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid rgba(220, 210, 194, 0.82);
  background: rgba(255, 251, 244, 0.96);
}

.hero-preview {
  position: relative;
  min-height: 360px;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 24px;
  color: #f9f6f0;
  background: linear-gradient(180deg, rgba(16, 26, 37, 0.08), rgba(12, 18, 27, 0.84));
}

.preview-badge,
.preview-chip {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
}

.preview-badge {
  margin-bottom: 12px;
  background: rgba(252, 236, 210, 0.16);
  border: 1px solid rgba(252, 236, 210, 0.22);
  color: #f7dfb8;
}

.preview-overlay h3 {
  margin: 0;
  font-size: 28px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.preview-overlay p {
  margin: 12px 0 0;
  line-height: 1.8;
  color: rgba(245, 240, 232, 0.86);
}

.info-preview {
  padding: 22px;
}

.chip-row {
  margin-top: 14px;
}

.preview-chip {
  background: rgba(211, 226, 215, 0.46);
  color: #405a4a;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 900px) {
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>