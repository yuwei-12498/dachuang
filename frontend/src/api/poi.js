import request from './request'

export function reqGetPoiList() {
  return request({
    url: '/api/pois',
    method: 'get'
  })
}

export function reqGetPoiDetail(id, tripDate) {
  return request({
    url: `/api/pois/${id}`,
    method: 'get',
    params: tripDate ? { tripDate } : {}
  })
}

export function reqSearchPoi(keyword, city, limit = 8) {
  return request({
    url: '/api/pois/search',
    method: 'get',
    params: { keyword, city, limit }
  })
}

export function reqListCustomPois() {
  return request({
    url: '/api/custom-pois',
    method: 'get',
    skipErrorMessage: true
  })
}
