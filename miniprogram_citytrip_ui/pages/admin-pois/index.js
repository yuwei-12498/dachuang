const { reqAdminPois, reqUpdatePoi } = require("../../api/admin")
const { getAuthState, initAuthState } = require("../../store/auth")

Page({
  data: {
    loading: false,
    tableData: [],
    currentPage: 1,
    pageSize: 10,
    total: 0,
    maxPage: 1,
    searchQuery: "",
    showEditor: false,
    editingRow: null
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
      const res = await reqAdminPois({
        page: this.data.currentPage,
        size: this.data.pageSize,
        name: this.data.searchQuery || undefined
      })
      const total = Number(res && res.total) || 0
      this.setData({
        tableData: Array.isArray(res && res.records) ? res.records : [],
        total,
        maxPage: Math.max(1, Math.ceil(total / this.data.pageSize))
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

  openEdit(e) {
    const row = {
      id: Number(e.currentTarget.dataset.id),
      name: e.currentTarget.dataset.name || "",
      category: e.currentTarget.dataset.category || "",
      district: e.currentTarget.dataset.district || "",
      indoor: Number(e.currentTarget.dataset.indoor),
      nightAvailable: Number(e.currentTarget.dataset.night),
      priorityScore: Number(e.currentTarget.dataset.priority)
    }
    this.setData({
      showEditor: true,
      editingRow: row
    })
  },

  closeEditor() {
    this.setData({
      showEditor: false,
      editingRow: null
    })
  },

  onPriorityInput(e) {
    const editingRow = this.data.editingRow || {}
    editingRow.priorityScore = Number(e.detail.value)
    this.setData({
      editingRow
    })
  },

  async submitEdit() {
    const row = this.data.editingRow
    if (!row) {
      return
    }
    if (!row.priorityScore || row.priorityScore < 1 || row.priorityScore > 10) {
      wx.showToast({
        title: "权重需在 1~10 之间",
        icon: "none"
      })
      return
    }

    try {
      await reqUpdatePoi(row)
      wx.showToast({
        title: "景点参数更新成功",
        icon: "success"
      })
      this.closeEditor()
      this.fetchData()
    } catch (err) {
      // request 层已处理
    }
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
    if (this.data.currentPage >= this.data.maxPage) {
      return
    }
    this.setData({
      currentPage: this.data.currentPage + 1
    })
    await this.fetchData()
  }
})
