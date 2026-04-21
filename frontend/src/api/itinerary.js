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

export function reqListCommunityItineraries(params = {}) {
  return request({
    url: '/api/itineraries/community',
    method: 'get',
    params,
    skipErrorMessage: true
  })
}

export function reqGetCommunityItinerary(id) {
  return request({
    url: `/api/itineraries/community/${id}`,
    method: 'get',
    skipErrorMessage: true
  })
}

export function reqListCommunityComments(id) {
  return request({
    url: `/api/itineraries/community/${id}/comments`,
    method: 'get',
    skipErrorMessage: true
  })
}

export function reqCreateCommunityComment(id, data) {
  return request({
    url: `/api/itineraries/community/${id}/comments`,
    method: 'post',
    data
  })
}

export function reqPinCommunityComment(id, commentId) {
  return request({
    url: `/api/itineraries/community/${id}/comments/${commentId}/pin`,
    method: 'patch'
  })
}

export function reqDeleteCommunityPost(id) {
  return request({
    url: `/api/itineraries/community/${id}`,
    method: 'delete'
  })
}

export function reqLikeCommunityItinerary(id) {
  return request({
    url: `/api/itineraries/community/${id}/like`,
    method: 'post'
  })
}

export function reqUnlikeCommunityItinerary(id) {
  return request({
    url: `/api/itineraries/community/${id}/like`,
    method: 'delete'
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

export function reqToggleItineraryPublic(id, payload) {
  const data = typeof payload === 'object' && payload !== null
    ? payload
    : { isPublic: payload }
  return request({
    url: `/api/itineraries/${id}/public`,
    method: 'patch',
    data
  })
}