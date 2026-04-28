const {
  reqFavoriteItinerary,
  reqGetItinerary,
  reqGetLatestItinerary,
  reqReplanItinerary,
  reqUnfavoriteItinerary
} = require("../../api/itinerary")
const { getAuthState, initAuthState } = require("../../store/auth")
const {
  loadItinerarySnapshot,
  localizeItineraryText,
  normalizeItinerarySnapshot,
  saveItinerarySnapshot
} = require("../../store/itinerary")
const { formatDuration } = require("../../utils/format")

function buildRouteSignature(nodes) {
  return (nodes || [])
    .map((node) => node && node.poiId)
    .filter((id) => id !== null && id !== undefined && id !== "")
    .join("-")
}

function buildFallbackOption(snapshot) {
  const nodes = Array.isArray(snapshot && snapshot.nodes) ? snapshot.nodes : []
  return {
    optionKey: "default",
    title: "褰撳墠榛樿鏂规",
    subtitle: "褰撳墠淇濆瓨鐨勮矾绾跨増鏈?,
    signature: buildRouteSignature(nodes),
    totalDuration: snapshot && snapshot.totalDuration ? snapshot.totalDuration : 0,
    totalCost: snapshot && snapshot.totalCost ? snapshot.totalCost : 0,
    stopCount: nodes.length,
    summary: snapshot && snapshot.recommendReason ? snapshot.recommendReason : "",
    recommendReason: snapshot && snapshot.recommendReason ? snapshot.recommendReason : "",
    notRecommendReason: snapshot && snapshot.tips ? snapshot.tips : "",
    highlights: [],
    alerts: Array.isArray(snapshot && snapshot.alerts) ? snapshot.alerts : [],
    nodes
  }
}

function resolveOptions(snapshot) {
  if (Array.isArray(snapshot && snapshot.options) && snapshot.options.length) {
    return snapshot.options
  }
  return snapshot ? [buildFallbackOption(snapshot)] : []
}

function resolveActiveOption(snapshot) {
  const options = resolveOptions(snapshot)
  if (!options.length) {
    return null
  }
  const selected = options.find((option) => option.optionKey === (snapshot && snapshot.selectedOptionKey))
  return selected || options[0]
}

function resolveActiveNodes(snapshot) {
  const option = resolveActiveOption(snapshot)
  return Array.isArray(option && option.nodes) ? option.nodes : []
}

function ensureSeenRouteSignatures(snapshot) {
  if (!snapshot) {
    return snapshot
  }
  const currentSignature = buildRouteSignature(resolveActiveNodes(snapshot))
  const seenRouteSignatures = Array.isArray(snapshot.seenRouteSignatures)
    ? snapshot.seenRouteSignatures.filter((item) => typeof item === "string" && item.trim())
    : []

  if (currentSignature && seenRouteSignatures.indexOf(currentSignature) < 0) {
    seenRouteSignatures.push(currentSignature)
  }

  return Object.assign({}, snapshot, {
    seenRouteSignatures
  })
}

function decorateOption(option, activeKey) {
  return Object.assign({}, option, {
    stateText: option.optionKey === activeKey ? "褰撳墠灞曠ず" : "鐐瑰嚮鏌ョ湅",
    durationText: formatDuration(option.totalDuration),
    stopText: `${option.stopCount || 0}`,
    costText: `${option.totalCost || 0}`
  })
}

function decorateNode(node) {
  const stepOrder = Number(node.stepOrder)
  const isFirstLeg = stepOrder === 1
  const modeText = isFirstLeg
    ? (node.departureTransportMode || node.travelTransportMode || "")
    : (node.travelTransportMode || "")
  const rawDistance = isFirstLeg
    ? (node.departureDistanceKm || node.travelDistanceKm)
    : node.travelDistanceKm
  const distanceNumber = Number(rawDistance)
  const departureMinutes = Number(node.departureTravelTime)
  const travelMinutes = Number(node.travelTime)
  const hasDepartureMinutes = Number.isFinite(departureMinutes) && departureMinutes > 0
  const effectiveMinutes = isFirstLeg && hasDepartureMinutes
    ? departureMinutes
    : (Number.isFinite(travelMinutes) && travelMinutes >= 0 ? travelMinutes : 0)
  const distanceText = Number.isFinite(distanceNumber) && distanceNumber > 0
    ? `${distanceNumber.toFixed(1)} 鍏噷`
    : ""

  return Object.assign({}, node, {
    travelText: `${Math.round(effectiveMinutes)} 鍒嗛挓`,
    modeText,
    distanceText,
    stayText: `${node.stayDuration || 0} 鍒嗛挓`,
    costText: `楼${node.cost || 0}`
  })
}

function buildMapData(nodes) {
  const validNodes = (nodes || []).filter(
    (node) => node && node.latitude !== null && node.latitude !== undefined && node.longitude !== null && node.longitude !== undefined
  )

  if (validNodes.length < 2) {
    return {
      showMap: false,
      latitude: 30.67,
      longitude: 104.06,
      markers: [],
      polyline: []
    }
  }

  const points = validNodes.map((node) => ({
    latitude: Number(node.latitude),
    longitude: Number(node.longitude)
  }))

  const markers = validNodes.map((node, index) => ({
    id: index + 1,
    latitude: Number(node.latitude),
    longitude: Number(node.longitude),
    width: 34,
    height: 34,
    callout: {
      content: `${index + 1}. ${node.poiName || ""}`,
      color: "#ffffff",
      fontSize: 12,
      bgColor: "#409EFF",
      borderRadius: 12,
      padding: 4,
      display: "BYCLICK"
    }
  }))

  return {
    showMap: true,
    latitude: points[0].latitude,
    longitude: points[0].longitude,
    markers,
    polyline: [
      {
        points,
        color: "#409EFF",
        width: 4
      }
    ]
  }
}

function buildDepartureText(originalReq, firstNode) {
  const lat = Number(originalReq && originalReq.departureLatitude)
  const lon = Number(originalReq && originalReq.departureLongitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
    return ""
  }

  const mode = firstNode
    ? (firstNode.departureTransportMode || firstNode.travelTransportMode || "")
    : ""
  const rawDistance = firstNode
    ? (firstNode.departureDistanceKm || firstNode.travelDistanceKm)
    : null
  const distance = Number(rawDistance)
  const rawMinutes = firstNode
    ? (firstNode.departureTravelTime || firstNode.travelTime)
    : null
  const minutes = Number(rawMinutes)
  const firstStopName = (firstNode && firstNode.poiName) || "绗竴涓櫙鐐?

  const estimateParts = []
  if (mode) {
    estimateParts.push(mode)
  }
  if (Number.isFinite(distance) && distance > 0) {
    estimateParts.push(`绾?${distance.toFixed(1)} 鍏噷`)
  }
  if (Number.isFinite(minutes) && minutes > 0) {
    estimateParts.push(`绾?${Math.round(minutes)} 鍒嗛挓`)
  }

  const estimateText = estimateParts.length
    ? `锛涢娈靛埌銆?{firstStopName}銆嶅缓璁?${estimateParts.join("锛?)}`
    : ""

  return `鏈璺嚎宸叉寜浣犲綋鍓嶄綅缃綔涓哄嚭鍙戠偣锛?{lat.toFixed(5)}, ${lon.toFixed(5)}锛?{estimateText}銆俙
}

Page({
  data: {
    routeId: null,
    itinerary: null,
    originalReq: null,
    displayOptions: [],
    activeOption: null,
    activeOptionKey: "",
    activeNodes: [],
    activeAlerts: [],
    activeDurationText: "--",
    activeCostText: "--",
    activeStopCount: 0,
    heroTitleText: "",
    heroDateText: "",
    departureText: "",
    reasonText: "",
    compareText: "",
    replanning: false,
    favoriteLoading: false,
    mapData: {
      showMap: false,
      latitude: 30.67,
      longitude: 104.06,
      markers: [],
      polyline: []
    }
  },

  async onLoad(options) {
    const routeId = Number(options && options.id)
    this.setData({
      routeId: Number.isFinite(routeId) && routeId > 0 ? routeId : null
    })

    await initAuthState()
    const authState = getAuthState()
    if (!authState.user) {
      const redirect = this.data.routeId
        ? `/pages/result/index?id=${this.data.routeId}`
        : "/pages/result/index"
      wx.navigateTo({
        url: `/pages/auth/index?redirect=${encodeURIComponent(redirect)}`
      })
      return
    }

    await this.loadCurrentItinerary()
  },

  onShow() {
    const chatWidget = this.selectComponent("#chatWidget")
    if (chatWidget && chatWidget.refresh) {
      chatWidget.refresh()
    }
  },

  applySnapshot(snapshot) {
    if (!snapshot) {
      this.setData({
        itinerary: null,
        displayOptions: [],
        activeOption: null,
        activeOptionKey: "",
        activeNodes: [],
        activeAlerts: [],
        activeDurationText: "--",
        activeCostText: "--",
        activeStopCount: 0,
        heroTitleText: "",
        heroDateText: "",
        departureText: "",
        reasonText: "",
        compareText: "",
        mapData: buildMapData([])
      })
      return
    }

    const options = resolveOptions(snapshot)
    const activeOption = resolveActiveOption(snapshot)
    const activeNodes = resolveActiveNodes(snapshot).map(decorateNode)
    const activeAlerts = Array.isArray(activeOption && activeOption.alerts) && activeOption.alerts.length
      ? activeOption.alerts
      : Array.isArray(snapshot.alerts) ? snapshot.alerts : []

    this.setData({
      itinerary: snapshot,
      originalReq: snapshot.originalReq || null,
      displayOptions: options.map((option) => decorateOption(option, activeOption && activeOption.optionKey)),
      activeOption,
      activeOptionKey: activeOption ? activeOption.optionKey : "",
      activeNodes,
      activeAlerts,
      activeDurationText: formatDuration(activeOption && activeOption.totalDuration),
      activeCostText: `楼${activeOption && activeOption.totalCost !== undefined ? activeOption.totalCost : snapshot.totalCost || 0}`,
      activeStopCount: activeOption && activeOption.stopCount !== undefined ? activeOption.stopCount : activeNodes.length,
      heroTitleText: snapshot.customTitle || "杩欐涓嶅彧缁欎綘涓€鏉¤矾绾匡紝鑰屾槸缁欎綘涓€缁勫彲姣旇緝鐨勬柟妗?,
      heroDateText: `${(snapshot.originalReq && snapshot.originalReq.tripDate) || "--"} / ${(snapshot.originalReq && snapshot.originalReq.startTime) || "09:00"} - ${(snapshot.originalReq && snapshot.originalReq.endTime) || "18:00"}`,
      departureText: buildDepartureText(snapshot.originalReq, activeNodes[0]),
      reasonText: (activeOption && activeOption.recommendReason) || snapshot.recommendReason || "",
      compareText: (activeOption && activeOption.notRecommendReason) || snapshot.tips || "",
      mapData: buildMapData(activeNodes)
    })
  },

  async loadCurrentItinerary() {
    const routeId = this.data.routeId
    const snapshot = loadItinerarySnapshot()

    if (routeId) {
      if (snapshot && Number(snapshot.id) === routeId) {
        this.applySnapshot(ensureSeenRouteSignatures(snapshot))
        return
      }
      try {
        const data = await reqGetItinerary(routeId)
        const normalized = ensureSeenRouteSignatures(normalizeItinerarySnapshot(data))
        saveItinerarySnapshot(normalized)
        this.applySnapshot(normalized)
      } catch (err) {
        this.applySnapshot(null)
      }
      return
    }

    if (snapshot) {
      this.applySnapshot(ensureSeenRouteSignatures(snapshot))
      return
    }

    try {
      const latest = await reqGetLatestItinerary()
      if (latest) {
        const normalized = ensureSeenRouteSignatures(normalizeItinerarySnapshot(latest))
        saveItinerarySnapshot(normalized)
        this.applySnapshot(normalized)
      } else {
        this.applySnapshot(null)
      }
    } catch (err) {
      this.applySnapshot(null)
    }
  },

  formatDurationText(e) {
    return formatDuration(e)
  },

  goBack() {
    wx.reLaunch({
      url: "/pages/home/index"
    })
  },

  goHistory() {
    wx.redirectTo({
      url: "/pages/history/index"
    })
  },

  goToDetail(e) {
    const poiId = e.currentTarget.dataset.poiid
    if (!poiId) {
      return
    }
    wx.navigateTo({
      url: `/pages/detail/index?id=${poiId}`
    })
  },

  handleSelectOption(e) {
    const optionKey = e.currentTarget.dataset.key
    const itinerary = this.data.itinerary
    if (!itinerary || !optionKey) {
      return
    }

    const selected = (this.data.displayOptions || []).find((item) => item.optionKey === optionKey)
    if (!selected) {
      return
    }

    const next = ensureSeenRouteSignatures(
      Object.assign({}, itinerary, {
        selectedOptionKey: optionKey
      })
    )
    saveItinerarySnapshot(next)
    this.applySnapshot(next)
  },

  async handleFavorite() {
    const itinerary = this.data.itinerary
    if (!itinerary || !itinerary.id || this.data.favoriteLoading) {
      return
    }
    this.setData({ favoriteLoading: true })
    try {
      if (itinerary.favorited) {
        await reqUnfavoriteItinerary(itinerary.id)
        const next = Object.assign({}, itinerary, {
          favorited: false,
          favoriteTime: null
        })
        saveItinerarySnapshot(next)
        this.applySnapshot(next)
        wx.showToast({
          title: "宸插彇娑堟敹钘?,
          icon: "success"
        })
      } else {
        const suggestedTitle = itinerary.customTitle
          || (this.data.activeOption && this.data.activeOption.title)
          || `${(this.data.originalReq && this.data.originalReq.tripDate) || "鏈"}璺嚎`
        const nextItinerary = await reqFavoriteItinerary(itinerary.id, {
          selectedOptionKey: this.data.activeOptionKey,
          title: suggestedTitle
        })
        const next = ensureSeenRouteSignatures(normalizeItinerarySnapshot(nextItinerary))
        saveItinerarySnapshot(next)
        this.applySnapshot(next)
        wx.showToast({
          title: "宸插姞鍏ユ敹钘?,
          icon: "success"
        })
      }
    } catch (err) {
      // request 灞傚凡澶勭悊
    } finally {
      this.setData({ favoriteLoading: false })
    }
  },

  async handleReplan() {
    const itinerary = this.data.itinerary
    const activeNodes = this.data.activeNodes
    if (!itinerary || !activeNodes || !activeNodes.length || this.data.replanning) {
      return
    }

    this.setData({ replanning: true })
    try {
      const currentSignature = buildRouteSignature(activeNodes)
      const excludedSignatures = Array.isArray(itinerary.seenRouteSignatures)
        ? Array.from(new Set([].concat(itinerary.seenRouteSignatures, currentSignature).filter(Boolean)))
        : currentSignature ? [currentSignature] : []

      const req = {
        itineraryId: itinerary.id,
        currentNodes: activeNodes,
        originalReq: itinerary.originalReq,
        excludedSignatures
      }
      const res = await reqReplanItinerary(req)
      if (res && res.success) {
        if (res.changed) {
          const nextItinerary = ensureSeenRouteSignatures(normalizeItinerarySnapshot(res.itinerary))
          const nextSignature = buildRouteSignature(resolveActiveNodes(nextItinerary))
          nextItinerary.seenRouteSignatures = Array.from(
            new Set([].concat(excludedSignatures, nextSignature).filter(Boolean))
          )
          saveItinerarySnapshot(nextItinerary)
          this.applySnapshot(nextItinerary)
          wx.showToast({
            title: localizeItineraryText(res.message) || "宸蹭负浣犲垏鎹㈡柊璺嚎",
            icon: "none"
          })
        } else {
          wx.showModal({
            title: "淇濈暀褰撳墠璺嚎",
            content: localizeItineraryText(res.reason) || "褰撳墠鏉′欢涓嬫病鏈夋洿浼樿矾绾夸簡锛屽缓璁厛淇濈暀褰撳墠璺嚎銆?,
            showCancel: false
          })
        }
      } else {
        wx.showToast({
          title: localizeItineraryText((res && res.message) || "閲嶆帓澶辫触锛岃绋嶅悗閲嶈瘯銆?),
          icon: "none"
        })
      }
    } catch (err) {
      // request 灞傚凡澶勭悊
    } finally {
      this.setData({ replanning: false })
    }
  }
})
