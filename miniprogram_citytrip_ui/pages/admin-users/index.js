const { reqAdminUsers, reqToggleUserStatus } = require("../../api/admin")
const { getAuthState, initAuthState } = require("../../store/auth")

Page({
  data: {
    loading: false,
    tableData: [],
    currentPage: 1,
    pageSize: 10,
    total: 0,
    maxPage: 1,
    searchQuery: ""
  },

  async onLoad() {
    await initAuthState()
    const authState = getAuthState()
    if (!authState.user || authState.user.role !== 1) {
      wx.showToast({
        title: "未授权访问后台",
        icon: "none"
      })
      wx.navigateTo({
        url: "/pages/home/index"
      })
      return
    }
    await this.fetchData()
  },

  async fetchData() {
    this.setData({ loading: true })
    try {
      const res = await reqAdminUsers({
        page: this.data.currentPage,
        size: this.data.pageSize,
        username: this.data.searchQuery || undefined
      })
      this.setData({
        tableData: Array.isArray(res && res.records) ? res.records : [],
        total: Number(res && res.total) || 0,
        maxPage: Math.max(1, Math.ceil((Number(res && res.total) || 0) / this.data.pageSize))
      })
    } catch (err) {
      // request 层已处理
    } finally {
      this.setData({ loading: false })
    }
  },

  onSearchInput(e) {
    this.setData({
      searchQuery: e.detail.value || ""
    })
  },

  async onSearch() {
    this.setData({ currentPage: 1 })
    await this.fetchData()
  },

  async toggleStatus(e) {
    const id = e.currentTarget.dataset.id
    const username = e.currentTarget.dataset.username
    const targetStatus = Number(e.currentTarget.dataset.status)
    const actionName = targetStatus === 1 ? "解封" : "冻结"

    wx.showModal({
      title: "高危操作",
      content: `确定要${actionName}用户 "${username}" 吗？`,
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await reqToggleUserStatus(id, targetStatus)
          wx.showToast({
            title: `${actionName}成功`,
            icon: "success"
          })
          this.fetchData()
        } catch (err) {
          // request 层已处理
        }
      }
    })
  },

  async prevPage() {
    if (this.data.currentPage <= 1) {
      return
    }
    this.setData({
      currentPage: this.data.currentPage - 1
    })
    await this.fetchData()
  },

  async nextPage() {
    const maxPage = this.data.maxPage
    if (this.data.currentPage >= maxPage) {
      return
    }
    this.setData({
      currentPage: this.data.currentPage + 1
    })
    await this.fetchData()
  }
})
