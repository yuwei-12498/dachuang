import request from './request'

export function reqRegister(data) {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}

export function reqLogin(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function reqLogout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}

export function reqCurrentUser() {
  return request({
    url: '/api/auth/me',
    method: 'get',
    skipErrorMessage: true
  })
}
