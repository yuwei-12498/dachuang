const { getAuthState } = require("../../store/auth")
const { askChatQuestion, getChatState } = require("../../store/chat")
const { buildSharedChatContext } = require("../../utils/chatContext")

Component({
  properties: {
    showWidget: {
      type: Boolean,
      value: true
    }
  },
  data: {
    isOpen: false,
    inputVal: "",
    messages: [],
    currentTips: [],
    currentEvidence: [],
    loading: false
  },
  lifetimes: {
    attached() {
      this.refresh()
    }
  },
  methods: {
    refresh() {
      const chatState = getChatState()
      this.setData({
        messages: chatState.messages || [],
        currentTips: chatState.currentTips || [],
        currentEvidence: chatState.currentEvidence || [],
        loading: !!chatState.loading
      })
    },
    ensureLogin() {
      const authState = getAuthState()
      if (authState.user) {
        return true
      }

      const pages = getCurrentPages()
      const current = pages[pages.length - 1]
      const redirect = current ? `/${current.route}` : "/pages/home/index"
      wx.navigateTo({
        url: `/pages/auth/index?redirect=${encodeURIComponent(redirect)}`
      })
      wx.showToast({
        title: "登录后即可继续咨询 AI",
        icon: "none"
      })
      return false
    },
    toggleChat() {
      if (!this.ensureLogin()) {
        return
      }
      this.setData({
        isOpen: !this.data.isOpen
      })
      this.refresh()
    },
    onInput(e) {
      this.setData({
        inputVal: e.detail.value || ""
      })
    },
    sendByTip(e) {
      const question = e.currentTarget.dataset.tip
      if (!question) {
        return
      }
      this.setData({ inputVal: question }, () => {
        this.handleSend()
      })
    },
    buildContext() {
      const pages = getCurrentPages()
      const current = pages[pages.length - 1]
      const route = current ? current.route : ""
      return buildSharedChatContext({
        pageType: route || "page"
      })
    },
    async handleSend() {
      if (!this.ensureLogin()) {
        return
      }
      const question = (this.data.inputVal || "").trim()
      if (!question || this.data.loading) {
        return
      }

      this.setData({ inputVal: "" })
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
    }
  }
})
