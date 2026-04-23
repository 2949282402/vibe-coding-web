import http from './http';

export const fetchSiteHomeApi = () => http.get('/public/site');
export const fetchPostListApi = (params) => http.get('/public/posts', { params });
export const fetchPostDetailApi = (slug, trackView = true) =>
  http.get(`/public/posts/${slug}`, { params: { trackView } });
export const submitCommentApi = (payload) => http.post('/public/comments', payload);
export const searchKnowledgePublicApi = (question) =>
  http.get('/public/rag/search', { params: { question } });
export const fetchKnowledgeHistoryApi = (sessionId) =>
  http.get('/public/rag/history', { params: { sessionId } });
export const fetchKnowledgeSessionsApi = (includeDeleted = true) =>
  http.get('/public/rag/sessions', { params: { includeDeleted } });
export const renameKnowledgeSessionApi = (sessionId, payload) =>
  http.patch(`/public/rag/sessions/${sessionId}`, payload);
export const deleteKnowledgeSessionApi = (sessionId) =>
  http.delete(`/public/rag/sessions/${sessionId}`);
export const purgeKnowledgeSessionApi = (sessionId) =>
  http.delete(`/public/rag/sessions/${sessionId}/purge`);
export const restoreKnowledgeSessionApi = (sessionId) =>
  http.post(`/public/rag/sessions/${sessionId}/restore`);
export const submitKnowledgeFeedbackApi = (payload) => http.post('/public/rag/feedback', payload);
export const createAgentTaskApi = (payload) => http.post('/agent/tasks', payload, { timeout: 120000 });
export const fetchAgentTaskDetailApi = (taskId) => http.get(`/agent/tasks/${taskId}`);
export const askKnowledgeApi = (payload) => http.post('/public/rag/ask', payload, { timeout: 300000 });
export const replayKnowledgeApi = (payload) => http.post('/public/rag/replay', payload, { timeout: 300000 });

const readSseStream = async (response, handlers = {}) => {
  if (!response.ok || !response.body) {
    const message = await response.text();
    throw new Error(message || 'Stream request failed');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  const emitEvent = async (rawEvent) => {
    const lines = rawEvent.split('\n');
    let eventName = 'message';
    const dataLines = [];

    lines.forEach((line) => {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      }
      if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim());
      }
    });

    if (!dataLines.length) {
      return;
    }

    const rawData = dataLines.join('\n');
    let payloadData = rawData;
    try {
      payloadData = JSON.parse(rawData);
    } catch {
      payloadData = rawData;
    }
    await handlers.onEvent?.(eventName, payloadData);
  };

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });

    let separatorIndex = buffer.indexOf('\n\n');
    while (separatorIndex >= 0) {
      const rawEvent = buffer.slice(0, separatorIndex).trim();
      buffer = buffer.slice(separatorIndex + 2);
      if (rawEvent) {
        await emitEvent(rawEvent);
      }
      separatorIndex = buffer.indexOf('\n\n');
    }

    if (done) {
      const finalEvent = buffer.trim();
      if (finalEvent) {
        await emitEvent(finalEvent);
      }
      break;
    }
  }
};

export const streamAgentTaskApi = async (taskId, handlers = {}) => {
  const token = localStorage.getItem('blog-auth-token') || localStorage.getItem('blog-admin-token');
  const response = await fetch(`/api/agent/tasks/${taskId}/stream`, {
    method: 'GET',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    signal: handlers.signal
  });
  await readSseStream(response, handlers);
};

export const askKnowledgeStreamApi = async (payload, handlers = {}) => {
  const token = localStorage.getItem('blog-auth-token') || localStorage.getItem('blog-admin-token');
  const response = await fetch('/api/public/rag/ask/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal: handlers.signal
  });

  await readSseStream(response, handlers);
};
