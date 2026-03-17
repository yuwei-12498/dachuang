import request from './request'

export function reqAskChat(data) {
  return request({
    url: '/api/chat/qa',
    method: 'post',
    data
  })
}
