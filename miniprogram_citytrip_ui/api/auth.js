const request = require("../utils/request")

function reqRegister(data) {
  return request({
    url: "/api/users",
    method: "POST",
    data
  })
}

function reqLogin(data) {
  return request({
    url: "/api/sessions",
    method: "POST",
    data
  })
}

function reqLogout() {
  return request({
    url: "/api/sessions/current",
    method: "DELETE"
  })
}

function reqCurrentUser() {
  return request({
    url: "/api/users/me",
    method: "GET",
    skipErrorMessage: true
  })
}

module.exports = {
  reqRegister,
  reqLogin,
  reqLogout,
  reqCurrentUser
}
