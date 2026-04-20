const request = require("../utils/request")

function reqGenerateItinerary(data) {
  return request({
    url: "/api/itineraries",
    method: "POST",
    data
  })
}

function reqListItineraries(params) {
  return request({
    url: "/api/itineraries",
    method: "GET",
    params: params || {},
    skipErrorMessage: true
  })
}

function reqGetItinerary(id) {
  return request({
    url: `/api/itineraries/${id}`,
    method: "GET",
    skipErrorMessage: true
  })
}

async function reqGetLatestItinerary() {
  const list = await reqListItineraries({ limit: 1 })
  if (!Array.isArray(list) || list.length === 0) {
    return null
  }
  return reqGetItinerary(list[0].id)
}

function reqReplacePoi(payload) {
  const { itineraryId, targetPoiId } = payload
  const data = Object.assign({}, payload)
  delete data.itineraryId
  delete data.targetPoiId

  return request({
    url: `/api/itineraries/${itineraryId}/nodes/${targetPoiId}/replacement`,
    method: "PATCH",
    data
  })
}

function reqReplanItinerary(payload) {
  const { itineraryId } = payload
  const data = Object.assign({}, payload)
  delete data.itineraryId

  return request({
    url: `/api/itineraries/${itineraryId}/replan`,
    method: "PATCH",
    data
  })
}

function reqFavoriteItinerary(id, data) {
  return request({
    url: `/api/itineraries/${id}/favorite`,
    method: "PUT",
    data: data || {}
  })
}

function reqUnfavoriteItinerary(id) {
  return request({
    url: `/api/itineraries/${id}/favorite`,
    method: "DELETE"
  })
}

module.exports = {
  reqGenerateItinerary,
  reqListItineraries,
  reqGetItinerary,
  reqGetLatestItinerary,
  reqReplacePoi,
  reqReplanItinerary,
  reqFavoriteItinerary,
  reqUnfavoriteItinerary
}
