const request = require("../utils/request")

function reqGetPoiList() {
  return request({
    url: "/api/pois",
    method: "GET"
  })
}

function reqGetPoiDetail(id, tripDate) {
  return request({
    url: `/api/pois/${id}`,
    method: "GET",
    params: tripDate ? { tripDate } : {}
  })
}

module.exports = {
  reqGetPoiList,
  reqGetPoiDetail
}
