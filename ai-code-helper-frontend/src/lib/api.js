import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000
})

export function buildChatStreamUrl(memoryId, message) {
  return apiClient.getUri({
    url: '/ai/chat',
    params: {
      memoryId,
      message
    }
  })
}
