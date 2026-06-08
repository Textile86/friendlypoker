import axios, { InternalAxiosRequestConfig } from 'axios'

const api = axios.create({ baseURL: '/api' })

// Attach JWT
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  // stamp start time for duration calc
  ;(config as any).__startTime = Date.now()
  return config
})

// Log + error handling
api.interceptors.response.use(
  (res) => {
    logEntry(res.config, res.status, JSON.stringify(res.data), false)
    return res
  },
  (err) => {
    const status = err.response?.status
    const body = err.response?.data ? JSON.stringify(err.response.data) : err.message
    logEntry(err.config, status, body, true)

    if (status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

function logEntry(
  config: InternalAxiosRequestConfig | undefined,
  status: number | undefined,
  responseBody: string | undefined,
  isError: boolean
) {
  if (!config) return
  const duration = Date.now() - ((config as any).__startTime ?? Date.now())
  const now = new Date()
  const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`

  const url = (config.baseURL ?? '') + (config.url ?? '')
  const method = (config.method ?? 'GET').toUpperCase()

  let requestBody: string | undefined
  if (config.data) {
    requestBody = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
    // mask passwords
    try {
      const parsed = JSON.parse(requestBody)
      if (parsed.password) parsed.password = '***'
      requestBody = JSON.stringify(parsed)
    } catch {}
  }

  // Push to global log queue (DevPanel reads this via CustomEvent)
  window.dispatchEvent(
    new CustomEvent('fp:log', {
      detail: { time, method, url, requestBody, status, responseBody, isError, duration },
    })
  )
}

export default api
