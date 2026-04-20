import request from './request'

export function reqRegister(data) {
  return request({
    url: '/api/users',
    method: 'post',
    data
  })
}

export function reqLogin(data) {
  return request({
    url: '/api/sessions',
    method: 'post',
    data
  })
}

export function reqLogout() {
  return request({
    url: '/api/sessions/current',
    method: 'delete'
  })
}

export function reqCurrentUser() {
  return request({
    url: '/api/users/me',
    method: 'get',
    skipErrorMessage: true,
    skipAuthRedirect: true
  })
}
