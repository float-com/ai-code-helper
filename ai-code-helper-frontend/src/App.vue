<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'
import { buildChatStreamUrl } from './lib/api'

const markdown = new MarkdownIt({
  breaks: true,
  linkify: true
})

const defaultLinkRenderer =
  markdown.renderer.rules.link_open ||
  function renderLink(tokens, idx, options, env, self) {
    return self.renderToken(tokens, idx, options)
  }

markdown.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return defaultLinkRenderer(tokens, idx, options, env, self)
}

const messages = ref([
  {
    id: 1,
    role: 'assistant',
    content:
      '你好，我是 AI 编程小助手。你可以问我编程学习路线、项目实战、简历优化和面试准备相关的问题。'
  }
])
const inputValue = ref('')
const isStreaming = ref(false)
const errorText = ref('')
const messageListRef = ref(null)
const currentStream = ref(null)
const memoryId = ref(createMemoryId())

const canSend = computed(() => inputValue.value.trim() && !isStreaming.value)

function createMemoryId() {
  return Math.floor(100000000 + Math.random() * 900000000)
}

function createMessage(role, content = '') {
  return {
    id: Date.now() + Math.random(),
    role,
    content
  }
}

function renderMessage(content) {
  return DOMPurify.sanitize(markdown.render(content || ''))
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function closeStream() {
  if (currentStream.value) {
    currentStream.value.close()
    currentStream.value = null
  }
}

function resetChat() {
  closeStream()
  memoryId.value = createMemoryId()
  messages.value = [
    {
      id: Date.now(),
      role: 'assistant',
      content: '新的会话已经创建。继续提问，我会基于当前会话上下文实时回答。'
    }
  ]
  inputValue.value = ''
  isStreaming.value = false
  errorText.value = ''
}

function sendMessage() {
  const text = inputValue.value.trim()
  if (!text || isStreaming.value) {
    return
  }

  errorText.value = ''
  messages.value.push(createMessage('user', text))

  const assistantMessage = createMessage('assistant', '')
  messages.value.push(assistantMessage)
  const assistantIndex = messages.value.length - 1

  inputValue.value = ''
  isStreaming.value = true
  scrollToBottom()

  closeStream()
  const stream = new EventSource(buildChatStreamUrl(memoryId.value, text))
  currentStream.value = stream

  stream.onmessage = (event) => {
    messages.value[assistantIndex].content += event.data
    scrollToBottom()
  }

  stream.onerror = () => {
    closeStream()
    isStreaming.value = false

    if (!messages.value[assistantIndex].content.trim()) {
      messages.value[assistantIndex].content =
        '本次流式响应已中断，请检查后端服务、CORS 配置和接口编码设置。'
    }

    scrollToBottom()
  }
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

onMounted(scrollToBottom)
onBeforeUnmount(closeStream)
</script>

<template>
  <div class="page-shell">
    <main class="chat-card">
      <header class="hero">
        <div>
          <p class="eyebrow">AI Programming Copilot</p>
          <h1>AI 编程小助手</h1>
          <p class="subtitle">
            帮你拆解编程学习路径、项目实战方案和求职面试问题。
          </p>
        </div>
        <div class="session-panel">
          <span class="session-label">当前会话 ID</span>
          <strong class="session-id">{{ memoryId }}</strong>
          <button class="ghost-button" type="button" @click="resetChat">
            新建会话
          </button>
        </div>
      </header>

      <section ref="messageListRef" class="message-list">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="avatar">
            {{ message.role === 'assistant' ? 'AI' : '我' }}
          </div>
          <div class="bubble">
            <span class="role-tag">
              {{ message.role === 'assistant' ? 'AI 编程小助手' : '用户' }}
            </span>
            <div
              v-if="message.role === 'assistant'"
              class="message-markdown"
              v-html="renderMessage(message.content || '...')"
            />
            <p v-else>{{ message.content || '...' }}</p>
          </div>
        </article>
      </section>

      <p v-if="errorText" class="error-text">{{ errorText }}</p>

      <footer class="composer">
        <textarea
          v-model="inputValue"
          class="composer-input"
          rows="3"
          placeholder="输入你的编程学习、项目实战或面试相关问题"
          @keydown="handleKeydown"
        />
        <div class="composer-actions">
          <span class="hint">
            {{ isStreaming ? 'AI 正在实时回复...' : 'Enter 发送，Shift + Enter 换行' }}
          </span>
          <button
            class="send-button"
            type="button"
            :disabled="!canSend"
            @click="sendMessage"
          >
            发送
          </button>
        </div>
      </footer>
    </main>
  </div>
</template>
