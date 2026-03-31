import request from './request'

export function reqGenerateItinerary(data) {
  return request({
    url: '/api/itineraries',
    method: 'post',
    data
  })
}

export function reqListItineraries(params = {}) {
  return request({
    url: '/api/itineraries',
    method: 'get',
    params,
    skipErrorMessage: true
  })
}

export function reqGetItinerary(id) {
  return request({
    url: `/api/itineraries/${id}`,
    method: 'get',
    skipErrorMessage: true
  })
}

export async function reqGetLatestItinerary() {
  const list = await reqListItineraries({ limit: 1 })
  if (!Array.isArray(list) || list.length === 0) {
    return null
  }
  return reqGetItinerary(list[0].id)
}

export function reqReplacePoi({ itineraryId, targetPoiId, ...data }) {
  return request({
    url: `/api/itineraries/${itineraryId}/nodes/${targetPoiId}/replacement`,
    method: 'patch',
    data
  })
}

export function reqReplanItinerary({ itineraryId, ...data }) {
  return request({
    url: `/api/itineraries/${itineraryId}/replan`,
    method: 'patch',
    data
  })
}

export function reqFavoriteItinerary(id, data = {}) {
  return request({
    url: `/api/itineraries/${id}/favorite`,
    method: 'put',
    data
  })
}

export function reqUnfavoriteItinerary(id) {
  return request({
    url: `/api/itineraries/${id}/favorite`,
    method: 'delete'
  })
}
