const { reqFavoriteItinerary, reqListItineraries, reqUnfavoriteItinerary } = require("../../api/itinerary")
const { formatDateTime, formatDuration } = require("../../utils/format")
const { getAuthState, initAuthState } = require("../../store/auth")

function decorateItem(item) {
  return Object.assign({}, item, {
    durationText: formatDuration(item.totalDuration),
    updatedText: formatDateTime(item.updatedAt)
  })
}

Page({
  data: {
    loading: false,
    activeFilter: "all",
    allList: [],
    favoriteList: [],
    displayList: [],
    allCount: 0,
    favoriteCount: 0,
    latestLabel: "--",
    emptyText: "还没有历史行程，先去生成一条吧。",
    favoritePendingIds: []
  },

  async onLoad() {
    await initAuthState()
    const authState = getAuthState()
    if (!authState.user) {
      wx.navigateTo({
        url: "/pages/auth/index?redirect=%2Fpages%2Fhistory%2Findex"
      })
      return
    }
    await this.loadLists()
  },

  onShow() {
    const chatWidget = this.selectComponent("#chatWidget")
    if (chatWidget && chatWidget.refresh) {
      chatWidget.refresh()
    }
  },

  applyDerived() {
    const allCount = this.data.allList.length
    const favoriteCount = this.data.favoriteList.length
    const displayList = this.data.activeFilter === "favorite" ? this.data.favoriteList : this.data.allList
    const latestLabel = this.data.allList[0] ? this.data.allList[0].updatedText : "--"
    const emptyText = this.data.activeFilter === "favorite" ? "你还没有收藏路线。" : "还没有历史行程，先去生成一条吧。"

    this.setData({
      allCount,
      favoriteCount,
      displayList,
      latestLabel,
      emptyText
    })
  },

  async loadLists() {
    this.setData({ loading: true })
    try {
      const data = await Promise.all([
        reqListItineraries(),
        reqListItineraries({ favorite: true })
      ])
      const all = Array.isArray(data[0]) ? data[0].map(decorateItem) : []
      const favorites = Array.isArray(data[1]) ? data[1].map(decorateItem) : []
      this.setData({
        allList: all,
        favoriteList: favorites
      })
      this.applyDerived()
    } catch (err) {
      // request 层已处理
    } finally {
      this.setData({ loading: false })
    }
  },

  switchFilter(e) {
    this.setData({
      activeFilter: e.currentTarget.dataset.filter
    })
    this.applyDerived()
  },

  async toggleFavorite(e) {
    const id = e.currentTarget.dataset.id
    if (!id) {
      return
    }
    if ((this.data.favoritePendingIds || []).includes(id)) {
      return
    }
    const favoritedRaw = e.currentTarget.dataset.favorited
    const favorited = favoritedRaw === true || favoritedRaw === "true" || Number(favoritedRaw) === 1
    const title = e.currentTarget.dataset.title || "我的收藏路线"
    this.setData({
      favoritePendingIds: (this.data.favoritePendingIds || []).concat(id)
    })

    try {
      if (favorited) {
        await reqUnfavoriteItinerary(id)
        wx.showToast({
          title: "已取消收藏",
          icon: "success"
        })
      } else {
        await reqFavoriteItinerary(id, { title })
        wx.showToast({
          title: "已加入收藏",
          icon: "success"
        })
      }
      await this.loadLists()
    } catch (err) {
      // request 层已处理
    } finally {
      this.setData({
        favoritePendingIds: (this.data.favoritePendingIds || []).filter((itemId) => itemId !== id)
      })
    }
  },

  goHome() {
    wx.reLaunch({
      url: "/pages/home/index"
    })
  },

  openItinerary(e) {
    const id = e.currentTarget.dataset.id
    if (!id) {
      return
    }
    wx.redirectTo({
      url: `/pages/result/index?id=${id}`
    })
  }
})
