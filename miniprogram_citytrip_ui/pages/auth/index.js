const { reqLogin, reqRegister } = require("../../api/auth")
const { getAuthState, initAuthState, setAuthUser } = require("../../store/auth")

function safeRedirect(raw) {
  if (!raw || typeof raw !== "string") {
    return "/pages/home/index"
  }
  return raw.startsWith("/") ? raw : `/${raw}`
}

Page({
  data: {
    activeTab: "login",
    redirectPath: "/pages/home/index",
    loginLoading: false,
    registerLoading: false,
    loginForm: {
      username: "",
      password: ""
    },
    registerForm: {
      username: "",
      nickname: "",
      password: "",
      confirmPassword: ""
    }
  },

  async onLoad(options) {
    const redirectPath = safeRedirect(options && options.redirect ? decodeURIComponent(options.redirect) : "")
    this.setData({
      activeTab: options && options.mode === "register" ? "register" : "login",
      redirectPath
    })
  },

  async onShow() {
    await initAuthState()
    const authState = getAuthState()
    if (authState.user) {
      wx.redirectTo({
        url: this.data.redirectPath
      })
    }
  },

  switchTab(e) {
    this.setData({
      activeTab: e.currentTarget.dataset.tab
    })
  },

  onInput(e) {
    const formKey = e.currentTarget.dataset.form
    const field = e.currentTarget.dataset.field
    const value = e.detail.value || ""
    if (!formKey || !field) {
      return
    }
    const next = Object.assign({}, this.data[formKey], {
      [field]: value
    })
    this.setData({
      [formKey]: next
    })
  },

  async handleLogin() {
    const form = this.data.loginForm
    if (!form.username || !form.password) {
      wx.showToast({
        title: "请输入用户名和密码",
        icon: "none"
      })
      return
    }

    this.setData({ loginLoading: true })
    try {
      const user = await reqLogin(form)
      setAuthUser(user)
      wx.showToast({
        title: "登录成功",
        icon: "success"
      })
      wx.redirectTo({
        url: this.data.redirectPath
      })
    } catch (err) {
      // request 封装已处理提示
    } finally {
      this.setData({ loginLoading: false })
    }
  },

  async handleRegister() {
    const form = this.data.registerForm
    if (!form.username || !form.nickname || !form.password || !form.confirmPassword) {
      wx.showToast({
        title: "请完整填写注册信息",
        icon: "none"
      })
      return
    }
    if (form.username.length < 4 || form.username.length > 20) {
      wx.showToast({
        title: "用户名长度需 4~20 位",
        icon: "none"
      })
      return
    }
    if (form.nickname.length < 2 || form.nickname.length > 20) {
      wx.showToast({
        title: "昵称长度需 2~20 位",
        icon: "none"
      })
      return
    }
    if (form.password.length < 6 || form.password.length > 20) {
      wx.showToast({
        title: "密码长度需 6~20 位",
        icon: "none"
      })
      return
    }
    if (form.password !== form.confirmPassword) {
      wx.showToast({
        title: "两次输入的密码不一致",
        icon: "none"
      })
      return
    }

    this.setData({ registerLoading: true })
    try {
      const user = await reqRegister({
        username: form.username,
        nickname: form.nickname,
        password: form.password
      })
      setAuthUser(user)
      wx.showToast({
        title: "注册成功，已自动登录",
        icon: "success"
      })
      wx.redirectTo({
        url: this.data.redirectPath
      })
    } catch (err) {
      // request 封装已处理提示
    } finally {
      this.setData({ registerLoading: false })
    }
  }
})
