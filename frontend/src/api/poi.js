import request from './request'

// 1. 分页或查询所有点位 (预留)
export function reqGetPoiList() {
  return request({
    url: '/api/poi/list',
    method: 'get'
  })
}

// 2. 查询单一 POI 详情
export function reqGetPoiDetail(id) {
  return request({
    url: `/api/poi/${id}`,
    method: 'get'
  })
}
