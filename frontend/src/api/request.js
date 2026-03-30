import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '',
  timeout: 60000,
  withCredentials: true
})

service.interceptors.response.use(
  response => {
    const res = response.data
    const skipErrorMessage = Boolean(response.config && response.config.skipErrorMessage)

    if (res.code !== 200) {
      if (!skipErrorMessage) {
        ElMessage.error(res.msg || '服务端错误')
      }
      const error = new Error(res.msg || 'Error')
      error.code = res.code
      error.data = res.data
      return Promise.reject(error)
    }

    return res.data
  },
  error => {
    console.error('API Error: ', error)
    const skipErrorMessage = Boolean(error.config && error.config.skipErrorMessage)
    if (!skipErrorMessage) {
      ElMessage.error('网络连接异常或后端服务未启动')
    }
    return Promise.reject(error)
  }
)

export default service
