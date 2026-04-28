<template>
  <section class="map-card">
    <div class="card-header">
      <div>
        <p class="eyebrow">路线地图</p>
        <h2>把这条路线放到地图上看</h2>
        <p class="header-copy">
          先从空间分布判断顺路程度，再对照右侧时间线确认每一站的节奏；地图区现在更偏专业预览面板，而不是普通地图截图。
        </p>
      </div>
      <div class="header-badge">{{ headerBadge }}</div>
    </div>

    <div class="map-stage" :class="[`map-stage--${motionStage}`, { 'is-empty': !hasRenderableMap }]">
      <div v-if="hasRenderableMap" ref="mapRef" class="map-canvas"></div>
      <div v-else class="map-empty">
        <el-empty description="当前路线缺少足够的坐标信息，暂时无法绘制地图。" />
      </div>
    </div>

    <div v-if="mapInsightItems.length" class="insight-grid">
      <article v-for="item in mapInsightItems" :key="item.label" class="insight-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.copy }}</p>
      </article>
    </div>

    <div v-if="segmentMeta.length" class="segment-strip">
      <article
        v-for="segment in segmentMeta"
        :key="segment.id"
        class="segment-pill interactive"
        :class="[segment.segmentClass, { focused: isFocusedSegment(segment.index), locked: isPinnedSegment(segment.index) }]"
        role="button"
        tabindex="0"
        @mouseenter="handleSegmentHover(segment.index)"
        @mouseleave="handleSegmentLeave(segment.index)"
        @click="handleSegmentPin(segment.index)"
        @keydown.enter.prevent="handleSegmentPin(segment.index)"
        @keydown.space.prevent="handleSegmentPin(segment.index)"
      >
        <span class="segment-pill-line"></span>
        <div class="segment-copy">
          <small>{{ segment.label }}</small>
          <strong>{{ segment.title }}</strong>
          <p>{{ segment.copy }}</p>
        </div>
      </article>
    </div>

    <div v-if="validNodes.length > 0" class="stop-strip">
      <div
        v-for="(node, index) in validNodes"
        :key="`${node.poiId}-${index}`"
        class="stop-pill"
        :class="[getStopPillClass(node, index), { external: node.sourceType === 'external' }]"
      >
        <span class="stop-index">{{ index + 1 }}</span>
        <div class="stop-copy">
          <span class="stop-name">{{ node.poiName }}</span>
          <div class="stop-meta-row">
            <small>{{ node.district || node.category || "待补充信息" }}</small>
            <div class="stop-tag-row">
              <span class="stop-mini-tag role">{{ getNodeRoleLabel(index) }}</span>
              <span class="stop-mini-tag" :class="node.sourceType === 'external' ? 'external' : 'local'">
                {{ node.sourceType === "external" ? "外部 POI" : "本地 POI" }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <p v-if="communityStatusText" class="community-status-copy">{{ communityStatusText }}</p>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { buildLeafletFitTarget } from './mapFitLayers'

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  departurePoint: {
    type: Object,
    default: null
  },
  communityStatusText: {
    type: String,
    default: ''
  },
  dayLabel: {
    type: String,
    default: ''
  },
  motionStage: {
    type: String,
    default: 'steady'
  },
  motionToken: {
    type: Number,
    default: 0
  },
  activeSegmentIndex: {
    type: Number,
    default: null
  },
  pinnedSegmentIndex: {
    type: Number,
    default: null
  }
})
const emit = defineEmits([
  'segment-hover',
  'segment-leave',
  'segment-pin'
])

const mapRef = ref(null)
const communityStatusText = computed(() => props.communityStatusText)
const motionStage = computed(() => props.motionStage || 'steady')
const currentLocationLabel = computed(() => '当前位置')
const firstStopLabel = computed(() => '首站')

const toFiniteNumber = value => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const isValidCoordinate = (latitude, longitude) => {
  return Number.isFinite(latitude)
    && Number.isFinite(longitude)
    && Math.abs(latitude) <= 90
    && Math.abs(longitude) <= 180
}

const toLatLng = point => {
  if (!point) return null
  const latitude = toFiniteNumber(point.latitude ?? point.lat)
  const longitude = toFiniteNumber(point.longitude ?? point.lng)
  if (!isValidCoordinate(latitude, longitude)) {
    return null
  }
  return [latitude, longitude]
}

const normalizedDeparturePoint = computed(() => {
  if (!props.departurePoint) return null
  const position = toLatLng(props.departurePoint)
  if (!position) {
    return null
  }
  return {
    latitude: position[0],
    longitude: position[1],
    label: [props.departurePoint.label, props.departurePoint.name].find(item => item && item !== 'CURRENT_LOCATION') || '????'
  }
})

const validNodes = computed(() => {
  return (props.nodes || []).filter(node => toLatLng(node))
})
const hasRenderableMap = computed(() => validNodes.value.length > 0)
const uniqueDistricts = computed(() => [...new Set(validNodes.value.map(node => node?.district).filter(Boolean))])
const uniqueCategories = computed(() => [...new Set(validNodes.value.map(node => node?.category).filter(Boolean))])
const startNode = computed(() => validNodes.value[0] || null)
const endNode = computed(() => validNodes.value[validNodes.value.length - 1] || null)
const externalNodeCount = computed(() => validNodes.value.filter(node => node?.sourceType === 'external').length)

const normalizeRoutePathPoints = points => {
  if (!Array.isArray(points)) {
    return []
  }
  return points
    .map(point => toLatLng(point))
    .filter(Boolean)
    .filter((point, index, list) => {
      if (index === 0) return true
      const previous = list[index - 1]
      return previous[0] !== point[0] || previous[1] !== point[1]
    })
}

const measurePointDistanceKm = (fromPoint, toPoint) => {
  if (!fromPoint || !toPoint) {
    return Number.POSITIVE_INFINITY
  }
  const [lat1, lng1] = fromPoint
  const [lat2, lng2] = toPoint
  const toRadians = value => value * Math.PI / 180
  const deltaLat = toRadians(lat2 - lat1)
  const deltaLng = toRadians(lng2 - lng1)
  const a = Math.sin(deltaLat / 2) ** 2
    + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(deltaLng / 2) ** 2
  return 6371 * 2 * Math.asin(Math.sqrt(a))
}

const buildCurvedConnectionPath = (fromPoint, toPoint, index = 0) => {
  if (!fromPoint || !toPoint) {
    return []
  }
  const [fromLat, fromLng] = fromPoint
  const [toLat, toLng] = toPoint
  const deltaLat = toLat - fromLat
  const deltaLng = toLng - fromLng
  const vectorLength = Math.sqrt(deltaLat ** 2 + deltaLng ** 2)
  if (!vectorLength) {
    return [fromPoint]
  }

  const perpendicularLat = -deltaLng / vectorLength
  const perpendicularLng = deltaLat / vectorLength
  const curveBase = Math.max(0.0012, Math.min(0.014, measurePointDistanceKm(fromPoint, toPoint) * 0.0032))
  const signedCurve = index % 2 === 0 ? curveBase : -curveBase
  const buildIntermediatePoint = (ratio, curveOffset) => ([
    Number((fromLat + deltaLat * ratio + perpendicularLat * curveOffset).toFixed(6)),
    Number((fromLng + deltaLng * ratio + perpendicularLng * curveOffset).toFixed(6))
  ])

  return [
    fromPoint,
    buildIntermediatePoint(0.32, signedCurve),
    buildIntermediatePoint(0.72, -signedCurve * 0.28),
    toPoint
  ]
}

const buildFallbackSegmentPath = (index, node) => {
  const fromPoint = index === 0 ? toLatLng(normalizedDeparturePoint.value) : toLatLng(validNodes.value[index - 1])
  const toPoint = toLatLng(node)
  if (!fromPoint || !toPoint) {
    return []
  }
  return buildCurvedConnectionPath(fromPoint, toPoint, index)
}

const pathAnchorsMatchSegment = (pathPoints, fromPoint, toPoint) => {
  if (!Array.isArray(pathPoints) || pathPoints.length < 2 || !fromPoint || !toPoint) {
    return false
  }
  const startDistance = measurePointDistanceKm(pathPoints[0], fromPoint)
  const endDistance = measurePointDistanceKm(pathPoints[pathPoints.length - 1], toPoint)
  return startDistance <= 1.2 && endDistance <= 1.2
}

const resolveSegmentPathPoints = (index, node, routePathPoints) => {
  const fallbackPath = buildFallbackSegmentPath(index, node)
  if (routePathPoints.length < 2) {
    return {
      pathPoints: fallbackPath,
      isFallbackGeometry: true
    }
  }
  if (!fallbackPath.length) {
    return {
      pathPoints: routePathPoints,
      isFallbackGeometry: false
    }
  }
  if (!pathAnchorsMatchSegment(routePathPoints, fallbackPath[0], fallbackPath[fallbackPath.length - 1])) {
    return {
      pathPoints: fallbackPath,
      isFallbackGeometry: true
    }
  }
  return {
    pathPoints: routePathPoints,
    isFallbackGeometry: false
  }
}

const buildSegmentCopy = (index, node, previousNode, isExternalLink, isFallbackGeometry) => {
  if (node?.travelNarrative) {
    return node.travelNarrative
  }
  if (isExternalLink) {
    return '该段串联了外部 POI，已强化接驳视觉提示。'
  }
  if (isFallbackGeometry) {
    return '本段暂时缺少完整路线几何，已用起终点先行占位展示。'
  }
  if (index === 0) {
    return `从${normalizedDeparturePoint.value?.label || '当前位置'}出发后先抵达${node?.poiName || '首站'}。`
  }
  return `${previousNode?.poiName || '上一站'} 到 ${node?.poiName || '下一站'} 这段已按顺序串联展开。`
}

const segmentMeta = computed(() => {
  return validNodes.value.map((node, index) => {
    const previousNode = index > 0 ? validNodes.value[index - 1] : null
    const routePathPoints = normalizeRoutePathPoints(node?.routePathPoints)
    const { pathPoints, isFallbackGeometry } = resolveSegmentPathPoints(index, node, routePathPoints)
    if (pathPoints.length < 2) {
      return null
    }
    const isDeparture = index === 0
    const isArrival = index === validNodes.value.length - 1
    const isExternalLink = node?.sourceType === 'external' || previousNode?.sourceType === 'external'
    const isActiveSegment = index === resolvedSegmentIndex.value
    const isMutedSegment = !isActiveSegment && !isExternalLink
    const label = isDeparture
      ? '出发段'
      : isArrival
        ? '收束段'
        : isExternalLink
          ? '外部串联段'
          : `第 ${index + 1} 段`
    const fromName = isDeparture
      ? normalizedDeparturePoint.value?.label || '当前位置'
      : previousNode?.poiName || '上一站'
    const title = `${fromName} → ${node?.poiName || '下一站'}`
    return {
      id: `segment-${index}`,
      index,
      from: pathPoints[0],
      to: pathPoints[pathPoints.length - 1],
      pathPoints,
      title,
      shortTitle: isDeparture ? '当前位置出发' : title,
      label,
      copy: buildSegmentCopy(index, node, previousNode, isExternalLink, isFallbackGeometry),
      isExternalLink,
      isActiveSegment,
      isMutedSegment,
      isDeparture,
      isArrival,
      isFallbackGeometry,
      segmentClass: [
        isActiveSegment ? 'active-segment' : 'muted-segment',
        isExternalLink ? 'external-segment' : '',
        isDeparture ? 'departure-segment' : '',
        isArrival ? 'arrival-segment' : '',
        isFallbackGeometry ? 'fallback-segment' : ''
      ].filter(Boolean)
    }
  }).filter(Boolean)
})

const defaultSegmentIndex = computed(() => {
  if (!segmentMeta.value.length) return 0
  const externalIndex = segmentMeta.value.findIndex(segment => segment.isExternalLink)
  if (externalIndex >= 0) {
    return externalIndex
  }
  return Math.floor((segmentMeta.value.length - 1) / 2)
})

const resolvedSegmentIndex = computed(() => {
  return Number.isInteger(props.activeSegmentIndex) ? props.activeSegmentIndex : defaultSegmentIndex.value
})

const routeTimeRange = computed(() => {
  const startTime = startNode.value?.startTime
  const endTime = endNode.value?.endTime
  if (startTime && endTime) {
    return `${startTime} - ${endTime}`
  }
  if (startTime || endTime) {
    return `${startTime || '--'} - ${endTime || '--'}`
  }
  return '时间待确认'
})

const districtLabel = computed(() => {
  if (!uniqueDistricts.value.length) {
    return '城区待补'
  }
  if (uniqueDistricts.value.length <= 2) {
    return uniqueDistricts.value.join(' / ')
  }
  return `${uniqueDistricts.value.slice(0, 2).join(' / ')} +${uniqueDistricts.value.length - 2}`
})

const headerBadge = computed(() => {
  if (!validNodes.value.length) {
    return props.dayLabel ? `${props.dayLabel} · 等待路线坐标` : '等待路线坐标'
  }
  const summary = `${validNodes.value.length} 个停靠点 · ${Math.max(uniqueDistricts.value.length, 1)} 个片区`
  return props.dayLabel ? `${props.dayLabel} · ${summary}` : summary
})

const mapHeadline = computed(() => {
  if (!validNodes.value.length) {
    return '等待路线数据'
  }
  if (normalizedDeparturePoint.value && endNode.value) {
    return `${normalizedDeparturePoint.value.label} → ${endNode.value?.poiName || '终点'}`
  }
  if (validNodes.value.length === 1) {
    return startNode.value?.poiName || '当前路线站点'
  }
  return `${startNode.value?.poiName || '起点'} → ${endNode.value?.poiName || '终点'}`
})

const mapNarrative = computed(() => {
  if (!validNodes.value.length) {
    return '地图会根据路线点位自动生成更直观的空间预览。'
  }
  if (normalizedDeparturePoint.value && segmentMeta.value.length) {
    return `已经从${normalizedDeparturePoint.value.label}出发，按每一段真实路径串联 ${segmentMeta.value.length} 段路线。`
  }
  if (externalNodeCount.value > 0) {
    return `当前路线中有 ${externalNodeCount.value} 个外部 POI，已用高亮分段强化接驳可读性。`
  }
  return '可以先看空间走向，再对照右侧时间线判断节奏。'
})

const routeModeLabel = computed(() => {
  if (!validNodes.value.length) return '待生成'
  if (normalizedDeparturePoint.value) return '带出发点串联'
  if (externalNodeCount.value > 0) return '混合接驳线'
  return validNodes.value.length <= 3 ? '轻量短线' : '半日主线'
})

const categoryLabel = computed(() => {
  if (!uniqueCategories.value.length) {
    return '体验待补充'
  }
  return uniqueCategories.value.slice(0, 3).join(' / ')
})

const decisionLabel = computed(() => {
  if (!validNodes.value.length) return '等待路线生成'
  if (normalizedDeparturePoint.value) return '先看出发段行程'
  if (externalNodeCount.value > 0) return '先看外部接驳段'
  return '地图与时间线交叉确认'
})

const decisionCopy = computed(() => {
  if (!validNodes.value.length) {
    return '地图生成后会给你更直接的空间判断。'
  }
  if (normalizedDeparturePoint.value) {
    return '首站会优先对齐出发位置与真实路径。'
  }
  return '可以先看整体路线形态，再细读每一段的接驳提示。'
})

const mapInsightItems = computed(() => {
  if (!validNodes.value.length) return []
  return [
    {
      label: '路线类型',
      value: routeModeLabel.value,
      copy: validNodes.value.length > 1
        ? `从 ${normalizedDeparturePoint.value?.label || startNode.value?.poiName || '--'} 走到 ${endNode.value?.poiName || '--'}`
        : '当前仅有一个有效点位'
    },
    {
      label: '主题体验',
      value: categoryLabel.value,
      copy: uniqueCategories.value.length > 1
        ? `共 ${uniqueCategories.value.length} 类体验`
        : '当前体验风格相对集中'
    },
    {
      label: '阅读建议',
      value: decisionLabel.value,
      copy: decisionCopy.value
    }
  ]
})

let map = null
let routeHaloLayer = null
let polylineLayer = null
let markersLayer = null
let routeSegmentLayers = []
let routeFlowLayer = null
let pendingAnimatedRender = false
let mapResizeObserver = null

const invokeNextFrame = callback => {
  if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
    window.requestAnimationFrame(callback)
    return
  }
  setTimeout(callback, 16)
}

const resolveBoundsPadding = () => {
  const stageWidth = mapRef.value?.clientWidth || 0
  if (stageWidth && stageWidth < 960) {
    return {
      paddingTopLeft: [56, 56],
      paddingBottomRight: [56, 72],
      maxZoom: 15
    }
  }
  return {
    paddingTopLeft: [336, 72],
    paddingBottomRight: [304, 96],
    maxZoom: 15
  }
}

const fitMapToBounds = (targetBounds, useFlyTo = false) => {
  if (!map || !targetBounds?.isValid?.()) {
    return
  }
  const options = resolveBoundsPadding()
  if (useFlyTo) {
    map.flyToBounds(targetBounds, {
      ...options,
      duration: 0.95,
      easeLinearity: 0.18
    })
    return
  }
  map.fitBounds(targetBounds, options)
}

const refreshMapViewport = () => {
  if (!map) {
    return
  }
  invokeNextFrame(() => {
    if (!map) {
      return
    }
    map.invalidateSize(false)
    const fitTarget = buildLeafletFitTarget(L, [routeHaloLayer, polylineLayer, routeFlowLayer, ...routeSegmentLayers, markersLayer])
    if (fitTarget) {
      fitMapToBounds(fitTarget.getBounds(), false)
    }
  })
}

const ensureMapResizeObserver = () => {
  if (mapResizeObserver || typeof ResizeObserver === 'undefined' || !mapRef.value) {
    return
  }
  mapResizeObserver = new ResizeObserver(() => {
    refreshMapViewport()
  })
  mapResizeObserver.observe(mapRef.value)
}

const disconnectMapResizeObserver = () => {
  if (!mapResizeObserver) {
    return
  }
  mapResizeObserver.disconnect()
  mapResizeObserver = null
}

const getNodeRoleLabel = index => {
  if (index === 0) return '首站'
  if (index === validNodes.value.length - 1) return '末站'
  return '沿途站'
}

const getNodeVisualRole = (node, index) => {
  if (index === 0) return 'start'
  if (index === validNodes.value.length - 1) return 'end'
  if (node?.sourceType === 'external') return 'external'
  return 'waypoint'
}

const getNodeSourceLabel = node => {
  return node?.sourceType === 'external' ? '外部 POI' : '本地 POI'
}

const isFocusedSegment = segmentIndex => segmentIndex === resolvedSegmentIndex.value

const isPinnedSegment = segmentIndex => {
  return Number.isInteger(props.pinnedSegmentIndex) && segmentIndex === props.pinnedSegmentIndex
}

const handleSegmentHover = segmentIndex => {
  emit('segment-hover', segmentIndex)
}

const handleSegmentLeave = segmentIndex => {
  emit('segment-leave', segmentIndex)
}

const handleSegmentPin = segmentIndex => {
  emit('segment-pin', segmentIndex)
}

const getStopPillClass = (node, index) => {
  if (index === 0) return 'start'
  if (index === validNodes.value.length - 1) return 'end'
  if (node?.sourceType === 'external') return 'external'
  return 'waypoint'
}

const buildSegmentStyle = segment => {
  if (segment.isExternalLink) {
    return {
      color: '#f2a93b',
      weight: segment.isActiveSegment ? 8 : 6.5,
      opacity: segment.isActiveSegment ? 0.96 : 0.86,
      lineCap: 'round',
      lineJoin: 'round',
      dashArray: segment.isFallbackGeometry ? '10 8' : null
    }
  }
  if (segment.isArrival) {
    return {
      color: '#8a6bff',
      weight: segment.isActiveSegment ? 8 : 6,
      opacity: segment.isActiveSegment ? 0.94 : 0.82,
      lineCap: 'round',
      lineJoin: 'round',
      dashArray: segment.isFallbackGeometry ? '10 8' : null
    }
  }
  if (segment.isDeparture) {
    return {
      color: '#24c0a6',
      weight: segment.isActiveSegment ? 8 : 6,
      opacity: segment.isActiveSegment ? 0.96 : 0.84,
      lineCap: 'round',
      lineJoin: 'round',
      dashArray: segment.isFallbackGeometry ? '10 8' : null
    }
  }
  return {
    color: '#56b6ff',
    weight: segment.isActiveSegment ? 7.5 : 5.2,
    opacity: segment.isMutedSegment ? 0.42 : 0.78,
    lineCap: 'round',
    lineJoin: 'round',
    dashArray: segment.isFallbackGeometry || segment.isMutedSegment ? '12 10' : '3 0'
  }
}

const createMarkerIcon = (label, node, index) => {
  // html: `<span><i>${label}</i></span>`
  const visualRole = getNodeVisualRole(node, index)
  const markerClasses = ['marker-face', `${visualRole}-marker`, node?.sourceType === 'external' ? 'external-marker' : 'local-marker']
  const badge = index === 0 ? 'START' : index === validNodes.value.length - 1 ? 'END' : node?.sourceType === 'external' ? 'EXT' : 'POI'
  return L.divIcon({
    className: 'itinerary-map-marker',
    html: `<span class="${markerClasses.join(' ')}" data-visual-role="${visualRole}"><i>${label}</i><em>${badge}</em></span>`,
    iconSize: [52, 52],
    iconAnchor: [26, 26]
  })
}

const createDepartureMarkerIcon = () => {
  return L.divIcon({
    className: 'itinerary-map-marker',
    html: '<span class="marker-face current-location-marker departure-marker local-marker" data-visual-role="current-location"><i>我</i><em>YOU</em></span>',
    iconSize: [56, 56],
    iconAnchor: [28, 28]
  })
}

const attachSegmentLayerEvents = (layer, segment) => {
  layer.on('mouseover', () => {
    emit('segment-hover', segment.index)
  })
  layer.on('mouseout', () => {
    emit('segment-leave', segment.index)
  })
  layer.on('click', () => {
    emit('segment-pin', segment.index)
  })
}

const clearRouteLayers = () => {
  if (routeHaloLayer) {
    routeHaloLayer.remove()
    routeHaloLayer = null
  }
  if (polylineLayer) {
    polylineLayer.remove()
    polylineLayer = null
  }
  if (markersLayer) {
    markersLayer.remove()
    markersLayer = null
  }
  if (routeFlowLayer) {
    routeFlowLayer.remove()
    routeFlowLayer = null
  }
  if (routeSegmentLayers.length) {
    routeSegmentLayers.forEach(layer => layer.remove())
    routeSegmentLayers = []
  }
}

const collectRouteTracePoints = () => {
  const merged = []
  segmentMeta.value.forEach(segment => {
    segment.pathPoints.forEach(point => {
      const previous = merged[merged.length - 1]
      if (!previous || previous[0] !== point[0] || previous[1] !== point[1]) {
        merged.push(point)
      }
    })
  })
  return merged
}

const buildNodePopupHtml = (node, index) => {
  const roleLabel = getNodeRoleLabel(index)
  const sourceLabel = getNodeSourceLabel(node)
  const analysis = node?.travelNarrative ? `<div style="margin-top: 8px; color: #53708f; line-height: 1.6;">${node.travelNarrative}</div>` : ''
  return `
    <div style="min-width: 240px;">
      <strong style="font-size: 15px; color: #163152;">${node?.poiName || '未命名点位'}</strong>
      <div style="margin-top: 8px; color: #5f6f82;">${node?.startTime || '--'} - ${node?.endTime || '--'}</div>
      <div style="margin-top: 6px; color: #7a8da3;">${node?.category || '体验待补'}${node?.district ? ` ? ${node.district}` : ''}</div>
      <div style="margin-top: 8px; color: ${node?.sourceType === 'external' ? '#a66b08' : '#53708f'}; font-weight: 600;">${roleLabel} ? ${sourceLabel}</div>
      ${analysis}
    </div>
  `
}

const renderMap = async ({ useFlyTo = false, animateFlow = false } = {}) => {
  await nextTick()

  if (!mapRef.value || !hasRenderableMap.value) {
    return
  }

  if (!map) {
    map = L.map(mapRef.value, {
      zoomControl: false,
      scrollWheelZoom: false
    })

    L.control.zoom({
      position: 'bottomright'
    }).addTo(map)

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map)
  }

  ensureMapResizeObserver()

  clearRouteLayers()

  const tracePoints = collectRouteTracePoints()
  if (tracePoints.length >= 2) {
    routeHaloLayer = L.polyline(tracePoints, {
      color: 'rgba(95, 158, 255, 0.18)',
      weight: 14,
      opacity: 1,
      lineCap: 'round',
      lineJoin: 'round'
    }).addTo(map)

    polylineLayer = L.polyline(tracePoints, {
      color: 'rgba(73, 109, 170, 0.24)',
      weight: 4,
      opacity: 0.88,
      lineCap: 'round',
      lineJoin: 'round',
      dashArray: '12 12'
    }).addTo(map)

    routeFlowLayer = L.polyline(tracePoints, {
      color: animateFlow ? 'rgba(154, 214, 255, 0.95)' : 'rgba(154, 214, 255, 0.68)',
      weight: animateFlow ? 6 : 5,
      opacity: animateFlow ? 0.94 : 0.72,
      lineCap: 'round',
      lineJoin: 'round',
      dashArray: '18 14',
      className: 'route-flow-line'
    }).addTo(map)

    const flowElement = routeFlowLayer.getElement?.()
    if (flowElement) {
      flowElement.classList.toggle('is-animated', animateFlow)
    }
  }

  routeSegmentLayers = segmentMeta.value.map(segment => {
    const layer = L.polyline(segment.pathPoints, buildSegmentStyle(segment)).addTo(map)
    attachSegmentLayerEvents(layer, segment)
    return layer
  })

  const markerLayers = []
  if (normalizedDeparturePoint.value) {
    markerLayers.push(
      L.marker([normalizedDeparturePoint.value.latitude, normalizedDeparturePoint.value.longitude], {
        icon: createDepartureMarkerIcon()
      }).bindPopup(`
        <div style="min-width: 220px;">
          <strong style="font-size: 15px; color: #163152;">${normalizedDeparturePoint.value.label}</strong>
          <div style="margin-top: 8px; color: #5f6f82;">当前位置</div>
          <div style="margin-top: 6px; color: #24c0a6; font-weight: 600;">路线将从这里开始</div>
        </div>
      `)
    )
  }

  validNodes.value.forEach((node, index) => {
    markerLayers.push(
      L.marker(toLatLng(node), {
        icon: createMarkerIcon(index + 1, node, index)
      }).bindPopup(buildNodePopupHtml(node, index))
    )
  })

  markersLayer = L.layerGroup(markerLayers).addTo(map)

  const fitTarget = buildLeafletFitTarget(L, [routeHaloLayer, polylineLayer, routeFlowLayer, ...routeSegmentLayers, markersLayer])
  const targetBounds = fitTarget?.getBounds?.()

  if (targetBounds?.isValid?.()) {
    fitMapToBounds(targetBounds, useFlyTo)
  } else if (markerLayers.length) {
    const center = normalizedDeparturePoint.value
      ? [normalizedDeparturePoint.value.latitude, normalizedDeparturePoint.value.longitude]
      : toLatLng(validNodes.value[0])
    if (center) {
      map.setView(center, 13)
    }
  }

  map.invalidateSize(false)
  invokeNextFrame(() => {
    if (map) {
      map.invalidateSize(false)
    }
  })
}

const destroyMap = () => {
  disconnectMapResizeObserver()
  clearRouteLayers()
  if (map) {
    map.remove()
    map = null
  }
}

onMounted(() => {
  renderMap()
})

onBeforeUnmount(() => {
  destroyMap()
})

watch([validNodes, normalizedDeparturePoint, segmentMeta], () => {
  if (!hasRenderableMap.value) {
    destroyMap()
    pendingAnimatedRender = false
    return
  }
  if (props.motionStage === 'switching-out') {
    pendingAnimatedRender = true
    return
  }
  renderMap({
    useFlyTo: props.motionStage === 'switching-in',
    animateFlow: props.motionStage === 'switching-in' || pendingAnimatedRender
  })
  pendingAnimatedRender = false
}, { deep: true })

watch(() => props.motionStage, stage => {
  if (stage === 'switching-out') {
    pendingAnimatedRender = true
  }
})

watch(() => props.motionToken, (token, previousToken) => {
  if (token === previousToken || !hasRenderableMap.value) {
    return
  }
  renderMap({
    useFlyTo: true,
    animateFlow: true
  })
  pendingAnimatedRender = false
})

watch(
  () => [props.activeSegmentIndex, props.pinnedSegmentIndex],
  () => {
    if (!map || !hasRenderableMap.value) {
      return
    }
    renderMap({
      useFlyTo: false,
      animateFlow: false
    })
  }
)
</script>

<style scoped>
.map-card {
  border-radius: var(--radius-panel);
  border: 1px solid rgba(188, 214, 255, 0.84);
  background:
    radial-gradient(circle at top right, rgba(167, 209, 255, 0.18), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 249, 255, 0.93));
  box-shadow: var(--shadow-soft);
  padding: 28px;
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.map-card:hover {
  transform: translateY(-4px);
  border-color: rgba(95, 158, 255, 0.52);
  box-shadow: var(--shadow-strong);
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
  margin: 0 0 10px;
  color: var(--text-strong);
  font-size: 32px;
  line-height: 1.08;
  font-family: var(--font-display);
}

.header-copy {
  margin: 0;
  max-width: 720px;
  color: var(--text-body);
  line-height: 1.8;
}

.header-badge {
  padding: 10px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(95, 158, 255, 0.12), rgba(126, 183, 255, 0.24));
  color: var(--brand-600);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.map-stage {
  position: relative;
  min-height: 560px;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid rgba(210, 224, 245, 0.92);
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(237, 245, 255, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.74),
    0 16px 36px rgba(81, 120, 177, 0.1);
  transition:
    transform 0.34s ease,
    filter 0.34s ease,
    box-shadow 0.34s ease;
}

.map-stage::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.56), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0));
  z-index: 0;
}

.map-stage::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(120deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0));
  opacity: 0;
  transform: translateX(-20%);
  transition: opacity 0.28s ease;
  z-index: 0;
}

.map-stage.is-empty {
  min-height: 320px;
}

.map-stage--switching-out {
  transform: translateY(8px) scale(0.988);
  filter: saturate(0.92) brightness(0.98);
}

.map-stage--switching-in {
  animation: mapStageReveal 420ms cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 20px 44px rgba(81, 120, 177, 0.16);
}

.map-stage--switching-in::after {
  opacity: 1;
}

.map-canvas {
  position: relative;
  z-index: 0;
  width: 100%;
  height: 560px;
  background: linear-gradient(180deg, #f8fbff 0%, #edf5ff 100%);
}

.map-empty {
  position: relative;
  z-index: 0;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.floating-panel {
  position: absolute;
  z-index: 2;
  border: 1px solid rgba(214, 228, 248, 0.92);
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px);
  box-shadow: 0 12px 26px rgba(81, 120, 177, 0.1);
}

.overview-panel {
  top: 18px;
  left: 18px;
  width: min(360px, calc(100% - 36px));
  padding: 18px 18px 16px;
  border-radius: 22px;
}

.overview-panel--compact {
  top: 16px;
  left: 16px;
  width: min(296px, calc(100% - 132px));
  padding: 14px 14px 12px;
}

.panel-kicker {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  color: var(--brand-600);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.overview-panel strong,
.insight-card strong,
.legend-item strong,
.panel-metric strong,
.segment-pill strong,
.segment-chip strong {
  color: var(--text-strong);
}

.overview-panel strong {
  display: block;
  font-size: 20px;
}

.overview-headline {
  line-height: 1.24;
}

.overview-copy {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.overview-panel p,
.insight-card p,
.community-status-copy,
.segment-pill p {
  margin: 10px 0 0;
  color: var(--text-body);
  line-height: 1.8;
}

.panel-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.panel-metric {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(248, 252, 255, 0.9);
  border: 1px solid rgba(214, 228, 248, 0.84);
}

.panel-metric span,
.insight-card span,
.legend-item small,
.segment-pill small,
.segment-chip small,
.stop-copy small {
  display: block;
  color: var(--text-soft);
}

.panel-metric strong {
  display: block;
  margin-top: 6px;
}

.legend-panel {
  right: 18px;
  bottom: 18px;
  max-width: min(540px, calc(100% - 36px));
  padding: 16px;
  border-radius: 20px;
}

.legend-panel--compact {
  right: 16px;
  bottom: 16px;
  width: min(264px, calc(100% - 132px));
  max-width: none;
  padding: 12px;
  border-radius: 18px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-panel--compact .legend-row {
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.legend-panel--compact .legend-item {
  min-width: 0;
  padding: 8px 10px;
  border-radius: 14px;
  background: rgba(248, 252, 255, 0.82);
  border: 1px solid rgba(214, 228, 248, 0.84);
}

.legend-item.poi-source {
  padding: 8px 12px;
  border-radius: 16px;
  background: rgba(255, 245, 226, 0.88);
  border: 1px solid rgba(242, 169, 59, 0.28);
}

.legend-panel--compact .legend-item.poi-source {
  padding: 8px 10px;
  border-radius: 14px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(95, 158, 255, 0.12);
}

.legend-dot.current {
  background: linear-gradient(135deg, #24c0a6, #67e0c8);
  box-shadow: 0 0 0 5px rgba(36, 192, 166, 0.18);
}

.legend-dot.start {
  background: linear-gradient(135deg, #5f9eff, #7cb8ff);
}

.legend-dot.end {
  background: linear-gradient(135deg, #7c5cff, #a994ff);
}

.legend-dot.external {
  background: linear-gradient(135deg, #f2a93b, #ffd17a);
  box-shadow: 0 0 0 5px rgba(242, 169, 59, 0.18);
}

.legend-divider {
  width: 28px;
  height: 2px;
  background: linear-gradient(90deg, rgba(95, 158, 255, 0.45), rgba(124, 92, 255, 0.45));
}

.legend-panel--compact .legend-divider {
  display: none;
}

.legend-segment-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.segment-chip,
.segment-pill {
  border: 1px solid rgba(201, 220, 245, 0.92);
  background: rgba(248, 252, 255, 0.88);
}

.segment-chip {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 18px;
}

.segment-chip.interactive,
.segment-pill.interactive {
  cursor: pointer;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease,
    opacity 0.24s ease;
}

.segment-chip.interactive:hover,
.segment-pill.interactive:hover {
  transform: translateY(-2px);
}

.segment-chip.interactive:focus-visible,
.segment-pill.interactive:focus-visible {
  outline: 2px solid rgba(95, 158, 255, 0.52);
  outline-offset: 2px;
}

.segment-chip-line,
.segment-pill-line {
  position: relative;
  flex-shrink: 0;
  width: 12px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(95, 158, 255, 0.94), rgba(124, 92, 255, 0.84));
}

.segment-chip-line {
  height: 44px;
}

.segment-pill-line {
  height: 100%;
  min-height: 66px;
}

.segment-chip.active-segment,
.segment-pill.active-segment {
  border-color: rgba(95, 158, 255, 0.52);
  background: linear-gradient(180deg, rgba(240, 247, 255, 0.96), rgba(232, 244, 255, 0.88));
  box-shadow: 0 16px 28px rgba(95, 158, 255, 0.12);
}

.segment-chip.focused,
.segment-pill.focused {
  border-color: rgba(95, 158, 255, 0.56);
}

.segment-chip.locked,
.segment-pill.locked {
  box-shadow:
    0 0 0 1px rgba(124, 92, 255, 0.18),
    0 18px 30px rgba(95, 158, 255, 0.14);
}

.segment-chip.muted-segment,
.segment-pill.muted-segment {
  opacity: 0.72;
}

.segment-chip.external-segment,
.segment-pill.external-segment {
  border-color: rgba(242, 169, 59, 0.36);
  background: linear-gradient(180deg, rgba(255, 249, 238, 0.96), rgba(255, 244, 224, 0.9));
}

.segment-chip.external-segment .segment-chip-line,
.segment-pill.external-segment .segment-pill-line {
  background: linear-gradient(180deg, rgba(242, 169, 59, 0.94), rgba(255, 209, 122, 0.84));
}

.segment-chip.arrival-segment .segment-chip-line,
.segment-pill.arrival-segment .segment-pill-line {
  background: linear-gradient(180deg, rgba(124, 92, 255, 0.92), rgba(169, 148, 255, 0.82));
}

.segment-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.segment-pill {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 14px;
  padding: 14px 16px;
  border-radius: 22px;
  box-shadow: 0 12px 26px rgba(81, 120, 177, 0.08);
}

.segment-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.insight-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(188, 214, 255, 0.84);
  background: rgba(248, 252, 255, 0.82);
  box-shadow: 0 14px 28px rgba(81, 120, 177, 0.08);
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
  gap: 12px;
  min-height: 54px;
  padding: 10px 14px 10px 10px;
  border-radius: 999px;
  background: rgba(247, 251, 255, 0.92);
  border: 1px solid rgba(214, 226, 242, 0.95);
  transition:
    transform 0.24s ease,
    border-color 0.24s ease,
    box-shadow 0.24s ease;
}

.stop-pill.start {
  border-color: rgba(95, 158, 255, 0.36);
  background: linear-gradient(135deg, rgba(242, 249, 255, 0.96), rgba(233, 244, 255, 0.92));
}

.stop-pill.end {
  border-color: rgba(124, 92, 255, 0.3);
  background: linear-gradient(135deg, rgba(247, 243, 255, 0.96), rgba(239, 233, 255, 0.92));
}

.stop-pill.external {
  border-color: rgba(242, 169, 59, 0.34);
  background: linear-gradient(135deg, rgba(255, 250, 240, 0.98), rgba(255, 245, 226, 0.92));
}

.stop-pill:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(81, 120, 177, 0.12);
}

.stop-index {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--brand-500), var(--brand-600));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.stop-pill.end .stop-index {
  background: linear-gradient(135deg, #7c5cff, #a994ff);
}

.stop-pill.external .stop-index {
  background: linear-gradient(135deg, #f2a93b, #ffcf74);
}

.stop-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stop-name {
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 700;
}

.stop-meta-row {
  display: flex;
  align-items: center;
  gap: 8px 10px;
  flex-wrap: wrap;
}

.stop-tag-row {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
}

.stop-mini-tag {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.stop-mini-tag.role,
.stop-mini-tag.local {
  background: rgba(95, 158, 255, 0.12);
  color: #2d79c7;
}

.stop-mini-tag.external {
  background: rgba(242, 169, 59, 0.16);
  color: #a66b08;
}

.community-status-copy {
  margin-top: 18px;
}

:deep(.leaflet-control-zoom) {
  border: none;
  box-shadow: 0 14px 24px rgba(81, 120, 177, 0.16);
}

:deep(.leaflet-control-zoom a) {
  width: 34px;
  height: 34px;
  line-height: 34px;
  color: var(--text-strong);
}

:deep(.route-flow-line) {
  stroke-dashoffset: 0;
}

:deep(.route-flow-line.is-animated) {
  animation: routeFlowDash 1.25s linear infinite;
}

:deep(.itinerary-map-marker) {
  background: transparent;
  border: none;
}

:deep(.itinerary-map-marker .marker-face) {
  position: relative;
  width: 52px;
  height: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  border: 2px solid rgba(255, 255, 255, 0.94);
  background: linear-gradient(135deg, #5f9eff, #7cb8ff);
  box-shadow: 0 14px 28px rgba(95, 158, 255, 0.28);
}

:deep(.itinerary-map-marker .marker-face.start-marker) {
  background: linear-gradient(135deg, #4b94ff, #7cc8ff);
}

:deep(.itinerary-map-marker .marker-face.current-location-marker),
:deep(.itinerary-map-marker .marker-face.departure-marker) {
  width: 56px;
  height: 56px;
  border-radius: 20px;
  background: linear-gradient(135deg, #24c0a6, #67e0c8);
  box-shadow:
    0 0 0 6px rgba(36, 192, 166, 0.18),
    0 16px 30px rgba(36, 192, 166, 0.24);
}

:deep(.itinerary-map-marker .marker-face.end-marker) {
  background: linear-gradient(135deg, #7c5cff, #b59bff);
  box-shadow: 0 14px 28px rgba(124, 92, 255, 0.26);
}

:deep(.itinerary-map-marker .marker-face.external-marker) {
  box-shadow:
    0 0 0 5px rgba(242, 169, 59, 0.18),
    0 14px 28px rgba(242, 169, 59, 0.22);
}

:deep(.itinerary-map-marker .marker-face i) {
  color: #fff;
  font-style: normal;
  font-weight: 800;
  font-size: 15px;
}

:deep(.itinerary-map-marker .marker-face.current-location-marker i),
:deep(.itinerary-map-marker .marker-face.departure-marker i) {
  font-size: 18px;
}

:deep(.itinerary-map-marker .marker-face em) {
  position: absolute;
  right: -4px;
  bottom: -4px;
  min-width: 26px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 28, 46, 0.88);
  color: #fff;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

:deep(.itinerary-map-marker .marker-face.external-marker em) {
  background: linear-gradient(135deg, #f2a93b, #ffcf74);
  color: #5f3c00;
}

:deep(.itinerary-map-marker .marker-face.current-location-marker em),
:deep(.itinerary-map-marker .marker-face.departure-marker em) {
  background: rgba(10, 53, 46, 0.88);
  color: #dffff8;
}

@keyframes mapStageReveal {
  0% {
    opacity: 0.72;
    transform: translateY(10px) scale(0.985);
    filter: saturate(0.92);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: saturate(1);
  }
}

@keyframes routeFlowDash {
  0% {
    stroke-dashoffset: 0;
  }
  100% {
    stroke-dashoffset: -64;
  }
}

@media (max-width: 1100px) {
  .legend-segment-row,
  .segment-strip,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .card-header,
  .legend-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .map-card {
    padding: 22px;
  }

  .card-header h2 {
    font-size: 28px;
  }

  .overview-panel,
  .legend-panel {
    position: static;
    width: auto;
    max-width: none;
    margin: 12px 12px 0;
  }

  .map-stage {
    min-height: 400px;
  }

  .map-canvas {
    height: 400px;
  }
}
</style>
