const { reqGetLatestItinerary, reqReplacePoi } = require("../../api/itinerary")
const { reqGetPoiDetail } = require("../../api/poi")
const { getAuthState, initAuthState } = require("../../store/auth")
const {
  loadItinerarySnapshot,
  localizeItineraryText,
  normalizeItinerarySnapshot,
  normalizePoiDetail,
  saveItinerarySnapshot
} = require("../../store/itinerary")

function resolveActiveNodes(snapshot) {
  if (!snapshot) {
    return []
  }
  if (Array.isArray(snapshot.options) && snapshot.options.length) {
    const selected = snapshot.options.find((option) => option.optionKey === snapshot.selectedOptionKey) || snapshot.options[0]
    if (selected && Array.isArray(selected.nodes)) {
      return selected.nodes
    }
  }
  return Array.isArray(snapshot.nodes) ? snapshot.nodes : []
}

Page({
  data: {
    targetPoiId: null,
    poiDetail: null,
    replacing: false,
    itinerary: null
  },

  async onLoad(options) {
    const targetPoiId = Number(options && options.id)
    this.setData({
      targetPoiId: Number.isFinite(targetPoiId) ? targetPoiId : null
    })

    await initAuthState()
    const authState = getAuthState()
    if (!authState.user) {
      const redirect = `/pages/detail/index?id=${this.data.targetPoiId || ""}`
      wx.navigateTo({
        url: `/pages/auth/index?redirect=${encodeURIComponent(redirect)}`
      })
      return
    }

    await this.loadData()
  },

  onShow() {
    const chatWidget = this.selectComponent("#chatWidget")
    if (chatWidget && chatWidget.refresh) {
      chatWidget.refresh()
    }
  },

  async loadData() {
    let itinerary = loadItinerarySnapshot()
    if (!itinerary) {
      try {
        const latest = await reqGetLatestItinerary()
        if (latest) {
          itinerary = normalizeItinerarySnapshot(latest)
          saveItinerarySnapshot(itinerary)
        }
      } catch (err) {
        itinerary = null
      }
    }

    this.setData({
      itinerary
    })

    try {
      const detail = await reqGetPoiDetail(this.data.targetPoiId, itinerary && itinerary.originalReq ? itinerary.originalReq.tripDate : "")
      this.setData({
        poiDetail: normalizePoiDetail(detail)
      })
    } catch (err) {
      wx.showToast({
        title: "加载点位详情失败",
        icon: "none"
      })
    }
  },

  goBack() {
    wx.navigateBack({
      delta: 1,
      fail: () => {
        wx.navigateTo({
          url: "/pages/result/index"
        })
      }
    })
  },

  async handleReplace() {
    const itinerary = this.data.itinerary
    const currentNodes = resolveActiveNodes(itinerary)
    if (!itinerary || !currentNodes.length) {
      wx.showToast({
        title: "没有可用行程，请先生成或恢复",
        icon: "none"
      })
      wx.redirectTo({
        url: "/pages/result/index"
      })
      return
    }

    this.setData({ replacing: true })
    try {
      const res = await reqReplacePoi({
        itineraryId: itinerary.id,
        targetPoiId: this.data.targetPoiId,
        currentNodes,
        originalReq: itinerary.originalReq
      })
      const next = normalizeItinerarySnapshot(res)
      saveItinerarySnapshot(next)
      wx.showToast({
        title: localizeItineraryText("Stop replaced and route refreshed."),
        icon: "none"
      })
      wx.redirectTo({
        url: "/pages/result/index"
      })
    } catch (err) {
      // request 层已处理
    } finally {
      this.setData({ replacing: false })
    }
  }
})
