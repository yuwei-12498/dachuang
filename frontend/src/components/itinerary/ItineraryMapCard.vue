<template>
  <section class="map-card">
    <div class="card-header">
      <div>
        <p class="eyebrow">路线地图</p>
        <h2>把这条路线放到地图上看</h2>
        <p class="header-copy">按当前推荐顺序展示站点位置与连线，方便你快速判断顺路程度。</p>
      </div>
      <div class="header-badge">{{ validNodes.length }} 个停靠点</div>
    </div>

    <div v-if="validNodes.length > 1" ref="mapRef" class="map-canvas"></div>
    <div v-else class="map-empty">
      <el-empty description="当前路线缺少足够的坐标信息，暂时无法绘制地图。" />
    </div>

    <div v-if="validNodes.length > 0" class="stop-strip">
      <div
        v-for="(node, index) in validNodes"
        :key="`${node.poiId}-${index}`"
        class="stop-pill"
      >
        <span class="stop-index">{{ index + 1 }}</span>
        <span class="stop-name">{{ node.poiName }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  }
})

const mapRef = ref(null)
const validNodes = computed(() => {
  return (props.nodes || []).filter(node => node && node.latitude != null && node.longitude != null)
})

let map = null
let polylineLayer = null
let markersLayer = null

const createMarkerIcon = (label) => {
  return L.divIcon({
    className: 'itinerary-map-marker',
    html: `<span>${label}</span>`,
    iconSize: [34, 34],
    iconAnchor: [17, 17]
  })
}

const renderMap = async () => {
  await nextTick()

  if (!mapRef.value || validNodes.value.length < 2) {
    return
  }

  const points = validNodes.value.map(node => [Number(node.latitude), Number(node.longitude)])

  if (!map) {
    map = L.map(mapRef.value, {
      zoomControl: true,
      scrollWheelZoom: false
    })

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map)
  }

  if (polylineLayer) {
    polylineLayer.remove()
  }
  if (markersLayer) {
    markersLayer.remove()
  }

  polylineLayer = L.polyline(points, {
    color: '#409eff',
    weight: 4,
    opacity: 0.85,
    lineJoin: 'round'
  }).addTo(map)

  markersLayer = L.layerGroup(
    validNodes.value.map((node, index) => {
      return L.marker([Number(node.latitude), Number(node.longitude)], {
        icon: createMarkerIcon(index + 1)
      }).bindPopup(`
        <div style="min-width: 180px;">
          <strong>${node.poiName || '未命名点位'}</strong>
          <div style="margin-top: 6px; color: #5f6f82;">${node.startTime || '--'} - ${node.endTime || '--'}</div>
          <div style="margin-top: 4px; color: #7a8da3;">${node.category || ''} ${node.district ? `· ${node.district}` : ''}</div>
        </div>
      `)
    })
  ).addTo(map)

  map.fitBounds(polylineLayer.getBounds(), {
    padding: [28, 28]
  })
  map.invalidateSize()
}

const destroyMap = () => {
  if (map) {
    map.remove()
    map = null
    polylineLayer = null
    markersLayer = null
  }
}

onMounted(() => {
  renderMap()
})

onBeforeUnmount(() => {
  destroyMap()
})

watch(validNodes, () => {
  if (validNodes.value.length < 2) {
    destroyMap()
    return
  }
  renderMap()
}, { deep: true })
</script>

<style scoped>
.map-card {
  border-radius: 24px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.06);
  padding: 24px;
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.map-card:hover {
  transform: translateY(-6px) scale(1.01);
  border-color: rgba(108, 176, 255, 0.58);
  box-shadow: 0 24px 48px rgba(31, 45, 61, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2d79c7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.card-header h2 {
  margin: 0 0 8px;
  color: #1f2d3d;
  font-size: 28px;
}

.header-copy {
  margin: 0;
  color: #67788c;
  line-height: 1.7;
}

.header-badge {
  padding: 10px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.12), rgba(102, 177, 255, 0.22));
  color: #2d79c7;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.map-canvas {
  width: 100%;
  height: 380px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(220, 230, 242, 0.9);
  background: linear-gradient(180deg, #f8fbff 0%, #edf5ff 100%);
}

.map-empty {
  border-radius: 20px;
  border: 1px dashed rgba(195, 210, 228, 0.9);
  background: linear-gradient(180deg, #fbfdff 0%, #f4f8ff 100%);
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stop-strip {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stop-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 14px 0 10px;
  border-radius: 999px;
  background: #f7fbff;
  border: 1px solid rgba(214, 226, 242, 0.95);
}

.stop-index {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.stop-name {
  color: #425468;
  font-size: 13px;
  font-weight: 600;
}

:deep(.itinerary-map-marker) {
  background: transparent;
  border: none;
}

:deep(.itinerary-map-marker span) {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(64, 158, 255, 0.28);
  border: 2px solid rgba(255, 255, 255, 0.95);
}

@media (max-width: 900px) {
  .card-header {
    flex-direction: column;
  }

  .map-canvas {
    height: 320px;
  }
}
</style>
