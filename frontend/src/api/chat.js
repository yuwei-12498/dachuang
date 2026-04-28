import request from './request'

export function reqAskChat(data) {
  return request({
    url: '/api/chat/messages',
    method: 'post',
    data
  })
}

export function reqGetChatStatus() {
  return request({
    url: '/api/chat/messages/status',
    method: 'get',
    skipErrorMessage: true
  }).then(payload => ({
    ...payload,
    toolReady: Boolean(payload?.toolReady),
    geoReady: Boolean(payload?.geoReady),
    embeddingReady: Boolean(payload?.embeddingReady),
    rerankReady: Boolean(payload?.rerankReady),
    warnings: Array.isArray(payload?.warnings) ? payload.warnings : []
  }))
}

export async function reqStreamChat(data, handlers = {}) {
  const token = localStorage.getItem('jwt_token')
  const response = await fetch('/api/chat/messages/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    throw await buildResponseError(response)
  }

  if (!response.body) {
    const error = new Error('当前环境不支持流式响应')
    error.code = 'STREAM_UNSUPPORTED'
    throw error
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let answer = ''
  let relatedTips = []
  let evidence = []

  const handlePayload = (payload) => {
    if (!payload || typeof payload !== 'object') {
      return
    }

    if (payload.type === 'token') {
      const content = typeof payload.content === 'string' ? payload.content : ''
      if (!content) {
        return
      }
      answer += content
      if (typeof handlers.onToken === 'function') {
        handlers.onToken(content, answer)
      }
      return
    }

    if (payload.type === 'meta') {
      relatedTips = Array.isArray(payload.relatedTips) ? payload.relatedTips : []
      evidence = Array.isArray(payload.evidence) ? payload.evidence : []
      if (typeof handlers.onMeta === 'function') {
        handlers.onMeta({ relatedTips, evidence })
      }
      return
    }

    if (payload.type === 'done') {
      if (typeof handlers.onDone === 'function') {
        handlers.onDone({ answer, relatedTips, evidence })
      }
      return
    }

    if (payload.type === 'error') {
      const error = new Error(payload.message || '流式请求失败')
      error.code = payload.code || 'STREAM_ERROR'
      throw error
    }
  }

  const flushBuffer = () => {
    const chunks = buffer.split(/\r?\n\r?\n/)
    buffer = chunks.pop() ?? ''
    for (const chunk of chunks) {
      const payload = parseSsePayload(chunk)
      if (payload) {
        handlePayload(payload)
      }
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    flushBuffer()
    if (done) {
      break
    }
  }

  if (buffer.trim()) {
    const payload = parseSsePayload(buffer)
    if (payload) {
      handlePayload(payload)
    }
  }

  return {
    answer,
    relatedTips,
    evidence
  }
}

function parseSsePayload(chunk) {
  if (!chunk || !chunk.trim()) {
    return null
  }

  const dataLines = chunk
    .split(/\r?\n/)
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice(5).trim())

  if (dataLines.length === 0) {
    return null
  }

  const raw = dataLines.join('\n')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (err) {
    return null
  }
}

async function buildResponseError(response) {
  let message = `请求失败(${response.status})`
  try {
    const payload = await response.json()
    if (payload && typeof payload.message === 'string' && payload.message.trim()) {
      message = payload.message.trim()
    }
  } catch (err) {
    try {
      const text = await response.text()
      if (text && text.trim()) {
        message = text.trim()
      }
    } catch (ignore) {
    }
  }

  const error = new Error(message)
  error.code = response.status
  return error
}
