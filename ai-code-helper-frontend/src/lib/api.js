import axios from 'axios'

export const apiClient = axios.create({
  baseURL: 'http://localhost:8081/api',
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
