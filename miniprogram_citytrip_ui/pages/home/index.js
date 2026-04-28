const { reqGenerateItinerary } = require("../../api/itinerary")
const { getAuthState, initAuthState } = require("../../store/auth")
const { getDefaultTripDate, saveItinerarySnapshot } = require("../../store/itinerary")
const { ORIGINAL_FORM_KEY } = require("../../utils/storage")

const tripDayOptions = [
  { label: "半天闲逛", value: 0.5 },
  { label: "全天游玩", value: 1.0 },
  { label: "两日深度", value: 2.0 }
]

const budgetOptions = [
  { label: "低预算（0~100元/人）", value: "低" },
  { label: "中预算（100~300元/人）", value: "中" },
  { label: "高预算（300元以上/人）", value: "高" }
]

const themeOptions = [
  { label: "历史文化", value: "文化" },
  { label: "特色美食", value: "美食" },
  { label: "自然风光", value: "自然" },
  { label: "商业购物", value: "购物" },
  { label: "网红打卡", value: "网红" },
  { label: "休闲放松", value: "休闲" }
]

const companionOptions = [
  { label: "独自漫游", value: "独自" },
  { label: "三五好友", value: "朋友" },
  { label: "情侣约会", value: "情侣" },
  { label: "家庭亲子", value: "亲子" }
]

const walkingOptions = [
  { label: "低（少走路）", value: "低" },
  { label: "中（正常散步）", value: "中" },
  { label: "高（能走能爬）", value: "高" }
]

Page({
  data: {
    user: null,
    loading: false,
    tripDayOptions,
    budgetOptions,
    themeOptions,
    companionOptions,
    walkingOptions,
    tripModeHint: "",
    form: {
      tripDays: 1.0,
      tripDate: getDefaultTripDate(),
      startTime: "09:00",
      endTime: "18:00",
      budgetLevel: "中",
      themes: [],
      isRainy: false,
      isNight: true,
      walkingLevel: "中",
      companionType: "朋友"
    }
  },

  async onLoad() {
    await initAuthState()
    this.syncAuthState()
  },

  onShow() {
    this.syncAuthState()
    const aiPanel = this.selectComponent("#homeAiPanel")
    if (aiPanel && aiPanel.refresh) {
      aiPanel.refresh()
    }
  },

  syncAuthState() {
    const authState = getAuthState()
    this.setData({
      user: authState.user || null
    })
    this.syncTripModeHint(this.data.form)
  },

  updateForm(nextPatch) {
    const nextForm = Object.assign({}, this.data.form, nextPatch)
    this.setData({
      form: nextForm
    })
    this.syncTripModeHint(nextForm)
  },

  syncTripModeHint(form) {
    this.setData({
      tripModeHint: form && form.tripDays > 1
        ? "两日模式会提高候选点位和打卡密度，当前结果仍以首日出发时间窗为主。"
        : ""
    })
  },

  selectTripDay(e) {
    const value = Number(e.currentTarget.dataset.value)
    if (!value) {
      return
    }

    if (value === 0.5) {
      this.updateForm({
        tripDays: value,
        startTime: "09:00",
        endTime: "13:00",
        isNight: false
      })
      return
    }

    this.updateForm({
      tripDays: value,
      startTime: "09:00",
      endTime: "18:00",
      isNight: false
    })
  },

  onTripDateChange(e) {
    this.updateForm({
      tripDate: e.detail.value
    })
  },

  onStartTimeChange(e) {
    this.updateForm({
      startTime: e.detail.value
    })
  },

  onEndTimeChange(e) {
    const endTime = e.detail.value
    this.updateForm({
      endTime
    })
  },

  selectBudget(e) {
    const value = e.currentTarget.dataset.value
    this.updateForm({
      budgetLevel: value
    })
  },

  toggleTheme(e) {
    const value = e.currentTarget.dataset.value
    const themes = (this.data.form.themes || []).slice()
    const index = themes.indexOf(value)
    if (index >= 0) {
      themes.splice(index, 1)
    } else {
      themes.push(value)
    }
    this.updateForm({ themes })
  },

  selectCompanion(e) {
    this.updateForm({
      companionType: e.currentTarget.dataset.value
    })
  },

  selectWalking(e) {
    this.updateForm({
      walkingLevel: e.currentTarget.dataset.value
    })
  },

  switchRainy(e) {
    this.updateForm({
      isRainy: !!e.detail.value
    })
  },

  switchNight(e) {
    this.updateForm({
      isNight: !!e.detail.value
    })
  },

  scrollToCore() {
    wx.pageScrollTo({
      selector: "#core",
      duration: 300
    })
  },

  validateForm() {
    const form = this.data.form
    if (!form.tripDate) {
      return "请选择出行日期"
    }
    if (!form.startTime) {
      return "请选择出发时间"
    }
    if (!form.endTime) {
      return "请选择结束时间"
    }
    if (!Array.isArray(form.themes) || form.themes.length === 0) {
      return "请至少选择一个偏好主题"
    }
    return ""
  },

  async onSubmit() {
    const authState = getAuthState()
    if (!authState.user) {
      wx.showToast({
        title: "登录后才能生成专属行程",
        icon: "none"
      })
      wx.navigateTo({
        url: "/pages/auth/index?redirect=%2Fpages%2Fhome%2Findex"
      })
      return
    }

    const errorMessage = this.validateForm()
    if (errorMessage) {
      wx.showToast({
        title: errorMessage,
        icon: "none"
      })
      return
    }

    this.setData({ loading: true })
    const payload = Object.assign({}, this.data.form)
    const currentLocation = await this.resolveCurrentLocationWithPrompt()
    if (!currentLocation) {
      this.setData({ loading: false })
      return
    }
    payload.departureLatitude = currentLocation.latitude
    payload.departureLongitude = currentLocation.longitude
    payload.departurePlaceName = "CURRENT_LOCATION"
    wx.setStorageSync(ORIGINAL_FORM_KEY, payload)

    try {
      const itinerary = await reqGenerateItinerary(payload)
      saveItinerarySnapshot(itinerary)
      wx.navigateTo({
        url: "/pages/result/index"
      })
    } catch (err) {
      if (err && String(err.code) === "401") {
        wx.showToast({
          title: "登录状态已失效，请重新登录",
          icon: "none"
        })
        wx.navigateTo({
          url: "/pages/auth/index?redirect=%2Fpages%2Fhome%2Findex"
        })
      } else {
        wx.showToast({
          title: "生成失败，请检查网络后重试",
          icon: "none"
        })
      }
    } finally {
      this.setData({ loading: false })
    }
  },

  resolveCurrentLocation() {
    return new Promise((resolve) => {
      wx.getLocation({
        type: "gcj02",
        isHighAccuracy: true,
        highAccuracyExpireTime: 5000,
        success: (res) => {
          resolve({
            latitude: Number(res.latitude),
            longitude: Number(res.longitude)
          })
        },
        fail: () => resolve(null)
      })
    })
  },

  resolveCurrentLocationWithPrompt() {
    return this.resolveCurrentLocation().then((location) => {
      if (location) {
        return location
      }
      return this.handleLocationDenied().then((canRetry) => {
        if (!canRetry) {
          return null
        }
        return this.resolveCurrentLocation()
      })
    })
  },

  handleLocationDenied() {
    return new Promise((resolve) => {
      wx.showModal({
        title: "需要定位权限",
        content: "生成路线前需要先获取你当前定位，才能精确计算“当前位置 → 第一个景点”的通行时长。",
        confirmText: "去开启",
        cancelText: "暂不",
        success: (result) => {
          if (!result.confirm) {
            resolve(false)
            return
          }
          wx.openSetting({
            success: (settingRes) => {
              const enabled = !!(settingRes && settingRes.authSetting && settingRes.authSetting["scope.userLocation"])
              if (!enabled) {
                wx.showToast({
                  title: "请先开启定位权限后再生成路线",
                  icon: "none"
                })
              }
              resolve(enabled)
            },
            fail: () => resolve(false)
          })
        },
        fail: () => resolve(false)
      })
    })
  }
})
