import request from './request'

export function reqAskChat(data) {
  return request({
    url: '/api/chat/messages',
    method: 'post',
    data
  })
}

export function reqGetChatStatus() {
  return request({
    url: '/api/chat/messages/status',
    method: 'get',
    skipErrorMessage: true
  })
}
