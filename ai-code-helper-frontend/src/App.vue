<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import MarkdownRenderer from './components/MarkdownRenderer.vue'
import { buildChatStreamUrl } from './lib/api'

const STREAM_FLUSH_INTERVAL = 48
const AUTO_SCROLL_THRESHOLD = 88
const DEFAULT_RENDER_WINDOW = 40

const messages = ref([
  createMessage(
    'assistant',
    '你好，我是 AI 编程小助手。你可以继续问我编程学习路线、项目实战、技术答疑和面试辅导相关的问题。'
  )
])
const inputValue = ref('')
const errorText = ref('')
const currentStream = ref(null)
const messageListRef = ref(null)
const memoryId = ref(createMemoryId())
const connectionState = ref('idle')
const activeAssistantMessageId = ref(null)
const renderWindowSize = ref(DEFAULT_RENDER_WINDOW)
const shouldAutoScroll = ref(true)

const rendererRegistry = new Map()
const streamChunkRegistry = new Map()
const pendingMessageIds = new Set()

let flushTimerId = null
let scrollFrameId = null

const canSend = computed(
  () => Boolean(inputValue.value.trim()) && !isStreamBusy.value
)
const isStreamBusy = computed(() =>
  ['connecting', 'streaming'].includes(connectionState.value)
)
const visibleMessages = computed(() => {
  const start = Math.max(0, messages.value.length - renderWindowSize.value)
  return messages.value.slice(start)
})
const hiddenMessageCount = computed(() =>
  Math.max(0, messages.value.length - visibleMessages.value.length)
)
const connectionStatus = computed(() => {
  const mapping = {
    idle: { label: '已就绪', tone: 'idle' },
    connecting: { label: '连接中', tone: 'connecting' },
    streaming: { label: '流式输出中', tone: 'streaming' },
    error: { label: '连接异常', tone: 'error' }
  }

  return mapping[connectionState.value] || mapping.idle
})

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

function bindRendererRef(messageId, instance) {
  if (instance) {
    rendererRegistry.set(messageId, instance)
    return
  }

  rendererRegistry.delete(messageId)
}

function isAtBottom(container) {
  return (
    container.scrollHeight - container.scrollTop - container.clientHeight <
    AUTO_SCROLL_THRESHOLD
  )
}

function scheduleScroll(force = false) {
  if (!force && !shouldAutoScroll.value) {
    return
  }

  if (scrollFrameId) {
    cancelAnimationFrame(scrollFrameId)
  }

  scrollFrameId = requestAnimationFrame(() => {
    scrollFrameId = null
    const container = messageListRef.value

    if (container) {
      container.scrollTo({
        top: container.scrollHeight,
        behavior: force ? 'auto' : 'smooth'
      })
    }
  })
}

function clearStreamTimers() {
  if (flushTimerId) {
    window.clearTimeout(flushTimerId)
    flushTimerId = null
  }

  if (scrollFrameId) {
    cancelAnimationFrame(scrollFrameId)
    scrollFrameId = null
  }
}

function closeStream() {
  if (currentStream.value) {
    currentStream.value.close()
    currentStream.value = null
  }
}

function resetStreamState() {
  activeAssistantMessageId.value = null
  connectionState.value = 'idle'
}

function queueMessageChunk(messageId, chunk) {
  if (!chunk) {
    return
  }

  const queue = streamChunkRegistry.get(messageId) || []
  queue.push(chunk)
  streamChunkRegistry.set(messageId, queue)
  pendingMessageIds.add(messageId)

  if (!flushTimerId) {
    flushTimerId = window.setTimeout(() => flushPendingChunks(), STREAM_FLUSH_INTERVAL)
  }
}

function flushPendingChunks(forceScroll = false) {
  if (flushTimerId) {
    window.clearTimeout(flushTimerId)
    flushTimerId = null
  }

  for (const messageId of pendingMessageIds) {
    const chunks = streamChunkRegistry.get(messageId)
    const message = messages.value.find((item) => item.id === messageId)
    const renderer = rendererRegistry.get(messageId)

    if (!chunks?.length || !message) {
      continue
    }

    const mergedChunk = chunks.join('')
    message.content += mergedChunk
    renderer?.appendChunk(mergedChunk)
    chunks.length = 0
  }

  pendingMessageIds.clear()
  scheduleScroll(forceScroll)
}

function finishStream(assistantMessage, errored = false) {
  flushPendingChunks(true)
  closeStream()
  activeAssistantMessageId.value = null

  if (errored && !assistantMessage.content.trim()) {
    assistantMessage.content =
      '本次流式响应已中断，请检查后端服务、CORS 配置以及 SSE 编码。'
    const renderer = rendererRegistry.get(assistantMessage.id)
    renderer?.replaceAll(assistantMessage.content)
    errorText.value = '连接已关闭，未收到有效内容。'
    connectionState.value = 'error'
    return
  }

  errorText.value = ''
  connectionState.value = 'idle'
}

function resetChat() {
  closeStream()
  clearStreamTimers()
  streamChunkRegistry.clear()
  pendingMessageIds.clear()
  memoryId.value = createMemoryId()
  messages.value = [
    createMessage(
      'assistant',
      '新的会话已经创建。继续提问，我会基于当前会话上下文实时回答。'
    )
  ]
  inputValue.value = ''
  errorText.value = ''
  renderWindowSize.value = DEFAULT_RENDER_WINDOW
  shouldAutoScroll.value = true
  resetStreamState()
  nextTick(() => scheduleScroll(true))
}

function sendMessage() {
  const text = inputValue.value.trim()
  if (!text || isStreamBusy.value) {
    return
  }

  errorText.value = ''
  messages.value.push(createMessage('user', text))

  const assistantMessage = createMessage('assistant')
  messages.value.push(assistantMessage)
  activeAssistantMessageId.value = assistantMessage.id

  inputValue.value = ''
  connectionState.value = 'connecting'
  shouldAutoScroll.value = true

  closeStream()
  scheduleScroll(true)

  const stream = new EventSource(buildChatStreamUrl(memoryId.value, text))
  currentStream.value = stream

  stream.onopen = () => {
    connectionState.value = 'streaming'
    scheduleScroll(true)
  }

  stream.onmessage = (event) => {
    if (event.data === '[DONE]') {
      finishStream(assistantMessage)
      return
    }

    queueMessageChunk(assistantMessage.id, event.data)
  }

  stream.onerror = () => {
    finishStream(assistantMessage, true)
  }
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

function handleMessageListScroll(event) {
  shouldAutoScroll.value = isAtBottom(event.target)
}

function handleRendererRender() {
  scheduleScroll(false)
}

function expandHistory() {
  renderWindowSize.value += DEFAULT_RENDER_WINDOW
  nextTick(() => scheduleScroll(false))
}

function isTypingMessage(message) {
  return (
    message.role === 'assistant' &&
    message.id === activeAssistantMessageId.value &&
    isStreamBusy.value &&
    !message.content
  )
}

onMounted(() => {
  nextTick(() => scheduleScroll(true))
})

onBeforeUnmount(() => {
  closeStream()
  clearStreamTimers()
  rendererRegistry.clear()
  streamChunkRegistry.clear()
  pendingMessageIds.clear()
})
</script>

<template>
  <div class="page-shell">
    <main class="chat-card">
      <header class="hero">
        <div class="hero-copy">
          <p class="eyebrow">Interview Coach Mode</p>
          <h1>AI 编程小助手</h1>
          <p class="subtitle">
            使用 SSE 流式输出，针对技术答疑和面试辅导场景优化 Markdown、代码块和阅读体验。
          </p>
        </div>

        <div class="session-panel">
          <span class="session-label">当前会话 ID</span>
          <strong class="session-id">{{ memoryId }}</strong>
          <span class="status-pill" :class="connectionStatus.tone">
            {{ connectionStatus.label }}
          </span>
          <button class="ghost-button" type="button" @click="resetChat">
            新建会话
          </button>
        </div>
      </header>

      <section
        ref="messageListRef"
        class="message-list"
        @scroll="handleMessageListScroll"
      >
        <button
          v-if="hiddenMessageCount"
          class="history-chip"
          type="button"
          @click="expandHistory"
        >
          查看更早的 {{ hiddenMessageCount }} 条消息
        </button>

        <article
          v-for="message in visibleMessages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="avatar">
            {{ message.role === 'assistant' ? 'AI' : '你' }}
          </div>

          <div class="bubble">
            <span class="role-tag">
              {{ message.role === 'assistant' ? 'AI 回答' : '我的提问' }}
            </span>

            <MarkdownRenderer
              v-if="message.role === 'assistant'"
              :ref="(instance) => bindRendererRef(message.id, instance)"
              class="message-markdown"
              :content="message.content"
              :streaming="message.id === activeAssistantMessageId && isStreamBusy"
              @render="handleRendererRender"
            />

            <p v-else class="plain-text">
              {{ message.content }}
            </p>

            <div
              v-if="isTypingMessage(message)"
              class="typing-indicator"
              aria-label="AI 正在回复"
            >
              <span />
              <span />
              <span />
            </div>
          </div>
        </article>
      </section>

      <p v-if="errorText" class="error-text">{{ errorText }}</p>

      <footer class="composer">
        <textarea
          v-model="inputValue"
          class="composer-input"
          rows="3"
          placeholder="输入编程学习、项目实战或面试辅导问题"
          @keydown="handleKeydown"
        />

        <div class="composer-actions">
          <span class="hint">
            {{
              isStreamBusy
                ? '消息以增量方式写入渲染器，避免 v-html 全量重建。'
                : 'Enter 发送，Shift + Enter 换行。'
            }}
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
