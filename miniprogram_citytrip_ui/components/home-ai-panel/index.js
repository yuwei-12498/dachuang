const { getAuthState } = require("../../store/auth")
const { askChatQuestion, getChatState } = require("../../store/chat")

Component({
  properties: {
    currentForm: {
      type: Object,
      value: {}
    }
  },
  data: {
    user: null,
    inputVal: "",
    messages: [],
    currentTips: [],
    loading: false
  },
  lifetimes: {
    attached() {
      this.refresh()
    }
  },
  methods: {
    refresh() {
      const authState = getAuthState()
      const chatState = getChatState()
      this.setData({
        user: authState.user,
        messages: chatState.messages || [],
        currentTips: chatState.currentTips || [],
        loading: !!chatState.loading
      })
    },
    ensureLogin() {
      const authState = getAuthState()
      if (authState.user) {
        return true
      }
      wx.navigateTo({
        url: "/pages/auth/index?redirect=%2Fpages%2Fhome%2Findex"
      })
      wx.showToast({
        title: "登录后才可以继续问 AI",
        icon: "none"
      })
      return false
    },
    onInput(e) {
      this.setData({
        inputVal: e.detail.value || ""
      })
    },
    sendByTip(e) {
      const tip = e.currentTarget.dataset.tip
      if (!tip) {
        return
      }
      this.setData({ inputVal: tip }, () => {
        this.handleSend()
      })
    },
    buildContext() {
      const form = this.properties.currentForm || {}
      return {
        pageType: "home",
        preferences: form.themes || [],
        rainy: !!form.isRainy,
        nightMode: !!form.isNight,
        companionType: form.companionType || ""
      }
    },
    async handleSend() {
      if (!this.ensureLogin()) {
        return
      }

      const question = (this.data.inputVal || "").trim()
      if (!question || this.data.loading) {
        return
      }

      this.setData({
        inputVal: ""
      })

      try {
        await askChatQuestion(question, this.buildContext())
      } catch (err) {
        if (err && err.code === 401) {
          wx.showToast({
            title: "登录已失效，请重新登录",
            icon: "none"
          })
        }
      }

      this.refresh()
    },
    goLogin() {
      wx.navigateTo({
        url: "/pages/auth/index?redirect=%2Fpages%2Fhome%2Findex"
      })
    }
  }
})
