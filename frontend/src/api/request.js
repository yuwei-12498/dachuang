import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '',
  timeout: 60000,
  withCredentials: true
})

service.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error: ', error)
    const skipErrorMessage = Boolean(error.config && error.config.skipErrorMessage)
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '服务异常'

    error.code = status || error.code

    if (!skipErrorMessage) {
      if (status) {
        ElMessage.error(message)
      } else {
        ElMessage.error('网络连接异常或后端服务未启动')
      }
    }

    return Promise.reject(error)
  }
)

export default service
