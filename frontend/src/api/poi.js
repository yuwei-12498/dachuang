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
