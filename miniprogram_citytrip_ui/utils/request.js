const { apiBaseUrl } = require("../config/env")
const { buildQuery } = require("./query")
const { TOKEN_KEY, USER_KEY } = require("./storage")

function resolveUrl(url) {
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  return `${apiBaseUrl}${url}`
}

function redirectToAuthPage() {
  const pages = getCurrentPages()
  if (!pages || !pages.length) {
    return
  }
  const current = pages[pages.length - 1]
  if (current && current.route === "pages/auth/index") {
    return
  }

  const redirect = current ? `/${current.route}` : "/pages/home/index"
  wx.navigateTo({
    url: `/pages/auth/index?redirect=${encodeURIComponent(redirect)}`
  })
}

function request(options) {
  const method = (options.method || "GET").toUpperCase()
  const query = buildQuery(options.params)
  const url = `${resolveUrl(options.url)}${query}`

  const token = wx.getStorageSync(TOKEN_KEY)
  const header = Object.assign(
    {
      "Content-Type": "application/json"
    },
    options.header || {}
  )

  if (token) {
    header.Authorization = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method,
      data: options.data,
      header,
      timeout: options.timeout || 60000,
      success(res) {
        const { statusCode, data } = res
        if (statusCode >= 200 && statusCode < 300) {
          resolve(data)
          return
        }

        const message = (data && data.message) || "服务异常"
        const err = {
          code: statusCode,
          message,
          response: res
        }

        if (statusCode === 401) {
          wx.removeStorageSync(TOKEN_KEY)
          wx.removeStorageSync(USER_KEY)
          redirectToAuthPage()
        } else if (!options.skipErrorMessage) {
          wx.showToast({
            title: message,
            icon: "none",
            duration: 2200
          })
        }

        reject(err)
      },
      fail(err) {
        if (!options.skipErrorMessage) {
          wx.showToast({
            title: "网络连接异常或后端服务未启动",
            icon: "none",
            duration: 2200
          })
        }
        reject({
          code: err && err.errMsg ? err.errMsg : "NETWORK_ERROR",
          message: (err && err.errMsg) || "网络异常",
          error: err
        })
      }
    })
  })
}

module.exports = request
