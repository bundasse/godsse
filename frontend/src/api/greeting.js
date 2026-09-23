import { apiClient } from '@/api/client'

/** GET /api/hello */
export const fetchHello = () => apiClient.get('/api/hello')

/** GET /api/hello/{name} */
export const fetchHelloTo = (name) => apiClient.get(`/api/hello/${encodeURIComponent(name)}`)

/** POST /api/hello/echo */
export const sendEcho = ({ name, message }) => apiClient.post('/api/hello/echo', { name, message })
