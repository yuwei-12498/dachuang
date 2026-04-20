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
    title: "当前默认方案",
    subtitle: "当前保存的路线版本",
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
    stateText: option.optionKey === activeKey ? "当前展示" : "点击查看",
    durationText: formatDuration(option.totalDuration),
    stopText: `${option.stopCount || 0}`,
    costText: `${option.totalCost || 0}`
  })
}

function decorateNode(node) {
  return Object.assign({}, node, {
    travelText: `${node.travelTime || 0} 分钟`,
    stayText: `${node.stayDuration || 0} 分钟`,
    costText: `¥${node.cost || 0}`
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
      activeCostText: `¥${activeOption && activeOption.totalCost !== undefined ? activeOption.totalCost : snapshot.totalCost || 0}`,
      activeStopCount: activeOption && activeOption.stopCount !== undefined ? activeOption.stopCount : activeNodes.length,
      heroTitleText: snapshot.customTitle || "这次不只给你一条路线，而是给你一组可比较的方案",
      heroDateText: `${(snapshot.originalReq && snapshot.originalReq.tripDate) || "--"} / ${(snapshot.originalReq && snapshot.originalReq.startTime) || "09:00"} - ${(snapshot.originalReq && snapshot.originalReq.endTime) || "18:00"}`,
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
          title: "已取消收藏",
          icon: "success"
        })
      } else {
        const suggestedTitle = itinerary.customTitle
          || (this.data.activeOption && this.data.activeOption.title)
          || `${(this.data.originalReq && this.data.originalReq.tripDate) || "本次"}路线`
        const nextItinerary = await reqFavoriteItinerary(itinerary.id, {
          selectedOptionKey: this.data.activeOptionKey,
          title: suggestedTitle
        })
        const next = ensureSeenRouteSignatures(normalizeItinerarySnapshot(nextItinerary))
        saveItinerarySnapshot(next)
        this.applySnapshot(next)
        wx.showToast({
          title: "已加入收藏",
          icon: "success"
        })
      }
    } catch (err) {
      // request 层已处理
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
            title: localizeItineraryText(res.message) || "已为你切换新路线",
            icon: "none"
          })
        } else {
          wx.showModal({
            title: "保留当前路线",
            content: localizeItineraryText(res.reason) || "当前条件下没有更优路线了，建议先保留当前路线。",
            showCancel: false
          })
        }
      } else {
        wx.showToast({
          title: localizeItineraryText((res && res.message) || "重排失败，请稍后重试。"),
          icon: "none"
        })
      }
    } catch (err) {
      // request 层已处理
    } finally {
      this.setData({ replanning: false })
    }
  }
})
