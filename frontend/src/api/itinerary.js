import request from './request'

// 1. 生成初始行程
export function reqGenerateItinerary(data) {
  return request({
    url: '/api/itinerary/generate',
    method: 'post',
    data
  })
}

// 2. 替换某个行程点并重新生成时间轴
export function reqReplacePoi(data) {
  return request({
    url: '/api/itinerary/replace',
    method: 'post',
    data
  })
}

// 3.// 智能顺滑重排（多次尝试）
export function reqReplanItinerary(data) {
  return request({
    url: '/api/itinerary/replan',
    method: 'post',
    data
  })
}
