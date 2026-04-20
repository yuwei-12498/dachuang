const request = require("../utils/request")

function reqAdminUsers(params) {
  return request({
    url: "/api/admin/users",
    method: "GET",
    params
  })
}

function reqToggleUserStatus(userId, status) {
  return request({
    url: `/api/admin/users/${userId}/status`,
    method: "PATCH",
    params: { status }
  })
}

function reqAdminPois(params) {
  return request({
    url: "/api/admin/pois",
    method: "GET",
    params
  })
}

function reqUpdatePoi(data) {
  return request({
    url: "/api/admin/pois",
    method: "PUT",
    data
  })
}

module.exports = {
  reqAdminUsers,
  reqToggleUserStatus,
  reqAdminPois,
  reqUpdatePoi
}
