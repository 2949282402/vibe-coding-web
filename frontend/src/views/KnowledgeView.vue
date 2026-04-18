<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  askKnowledgeApi,
  askKnowledgeStreamApi,
  deleteKnowledgeSessionApi,
  fetchKnowledgeHistoryApi,
  fetchKnowledgeSessionsApi,
  renameKnowledgeSessionApi,
  restoreKnowledgeSessionApi
} from '../api/blog';
import { renderMarkdown } from '../utils/markdown';
import { usePreferencesStore } from '../stores/preferences';

const STORAGE_SESSION_KEY = 'blog-rag-session-id';
const DEFAULT_TOP_K = 4;
const SCROLL_STICKY_THRESHOLD = 96;

const preferences = usePreferencesStore();
const sessions = ref([]);
const loading = ref(false);
const historyLoading = ref(false);
const sessionsLoading = ref(false);
const streaming = ref(false);
const question = ref('');
const result = ref(null);
const history = ref([]);
const optimisticQuestion = ref('');
const activeSessionId = ref(readSessionId());
const drawerMode = ref(false);
const sidebarCollapsed = ref(typeof window !== 'undefined' ? window.innerWidth < 1480 : false);
const sidebarDrawerOpen = ref(false);
const conversationViewport = ref(null);
const shouldStickToBottom = ref(true);

let activeController = null;

function readSessionId() {
  const existing = localStorage.getItem(STORAGE_SESSION_KEY);
  if (existing) {
    return existing;
  }
  return createSessionId();
}

function createSessionId() {
  const generated =
    globalThis.crypto?.randomUUID?.() ||
    `rag-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
  localStorage.setItem(STORAGE_SESSION_KEY, generated);
  return generated;
}

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      sidebarLabel: 'RAG Workspace',
      title: '博客知识助手',
      description: '基于已发布文章进行检索增强问答，回答保留会话历史，并严格附带引用标记。',
      newChat: '新建会话',
      ask: '发送',
      asking: '生成中',
      stop: '停止',
      placeholder: '输入你的问题，例如：当前博客系统是如何部署的？',
      streaming: '正在生成带引用的回答...',
      sessionsTitle: '会话列表',
      deletedTitle: '最近删除',
      examplesTitle: '推荐提问',
      sourcesTitle: '参考来源',
      followUpsTitle: '延伸问题',
      threadMapTitle: '当前线索',
      capabilityTitle: '能力边界',
      capabilityCitations: '严格引用',
      capabilityHistory: '多轮会话',
      capabilityRag: '发布即切片索引',
      sessionEmpty: '还没有历史会话，发起第一个问题后会出现在这里。',
      deletedEmpty: '暂无已删除会话。',
      historyTitle: '当前对话',
      modeLlm: '模型回答',
      modeRetrieval: '检索回答',
      strictCitation: '严格引用',
      userRole: '用户',
      assistantRole: '助手',
      emptyStateTitle: '开始一次新的知识问答',
      emptyStateBody: '输入问题后，系统会先检索相关文章片段，再生成带引用的回答。',
      composerHint: 'Enter 换行，Ctrl + Enter 发送',
      sourceHint: '回答中的 [1]、[2] 会映射到这里的参考来源。',
      noSources: '本轮还没有来源卡片。',
      noFollowUps: '回答完成后会在这里给出下一步追问建议。',
      noThreadMap: '当前会话还没有用户提问节点。',
      fallbackWarning: '流式通道不可用，已切换为普通请求。',
      historyFailed: '加载会话历史失败',
      sessionsFailed: '加载会话列表失败',
      rename: '重命名',
      remove: '删除',
      restore: '恢复',
      renameTitle: '重命名会话',
      renamePrompt: '输入新的会话标题',
      renamePlaceholder: '请输入会话标题',
      renameSuccess: '会话已重命名',
      deleteConfirmTitle: '删除会话',
      deleteConfirmBody: '该会话会进入最近删除列表，之后仍可恢复。',
      deleteSuccess: '会话已删除',
      restoreSuccess: '会话已恢复',
      activeSession: '当前会话',
      untitledSession: '未命名会话',
      turnLabel: '问题',
      messageCount: '{count} 条消息',
      indexed: '{posts} 篇文章 / {chunks} 个切片',
      sessionLoading: '正在同步会话...',
      workingTitle: '正在整理回答'
    };
  }

  return {
    sidebarLabel: 'RAG Workspace',
    title: 'Blog Knowledge Assistant',
    description: 'Ask over published articles with grounded retrieval, persistent chat sessions, and strict citations.',
    newChat: 'New Chat',
    ask: 'Send',
    asking: 'Thinking',
    stop: 'Stop',
    placeholder: 'Ask the knowledge base something, for example: how is this blog deployed?',
    streaming: 'Generating cited answer...',
    sessionsTitle: 'Sessions',
    deletedTitle: 'Recently Deleted',
    examplesTitle: 'Suggested Prompts',
    sourcesTitle: 'Sources',
    followUpsTitle: 'Follow-ups',
    threadMapTitle: 'Thread Map',
    capabilityTitle: 'Capabilities',
    capabilityCitations: 'Strict citations',
    capabilityHistory: 'Multi-session memory',
    capabilityRag: 'Publish-time chunk indexing',
    sessionEmpty: 'No chat sessions yet. Your first question will create one.',
    deletedEmpty: 'No deleted sessions.',
    historyTitle: 'Conversation',
    modeLlm: 'Model Answer',
    modeRetrieval: 'Retrieval Answer',
    strictCitation: 'Strict Citation',
    userRole: 'You',
    assistantRole: 'Assistant',
    emptyStateTitle: 'Start a grounded knowledge conversation',
    emptyStateBody: 'Ask a question and the system will retrieve relevant post chunks before answering with citations.',
    composerHint: 'Enter for newline, Ctrl + Enter to send',
    sourceHint: 'Citation markers like [1] and [2] map to the sources here.',
    noSources: 'No source panel yet for this turn.',
    noFollowUps: 'Follow-up suggestions appear here after a completed answer.',
    noThreadMap: 'The current session has no user turns yet.',
    fallbackWarning: 'Streaming was unavailable, switched to the standard request mode.',
    historyFailed: 'Failed to load conversation history',
    sessionsFailed: 'Failed to load conversation sessions',
    rename: 'Rename',
    remove: 'Delete',
    restore: 'Restore',
    renameTitle: 'Rename Session',
    renamePrompt: 'Enter a new title for this conversation',
    renamePlaceholder: 'Conversation title',
    renameSuccess: 'Session renamed',
    deleteConfirmTitle: 'Delete Session',
    deleteConfirmBody: 'The conversation will move to Recently Deleted and can be restored later.',
    deleteSuccess: 'Session deleted',
    restoreSuccess: 'Session restored',
    activeSession: 'Active Session',
    untitledSession: 'Untitled Session',
    turnLabel: 'Turn',
    messageCount: '{count} messages',
    indexed: '{posts} posts / {chunks} chunks indexed',
    sessionLoading: 'Refreshing sessions...',
    workingTitle: 'Preparing the answer'
  };
});

const examples = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return [
      '这个博客系统当前使用了哪些核心技术栈？',
      '后台管理端现在覆盖了哪些能力？',
      '下一轮产品迭代最值得优先做什么？',
      '当前项目里的 RAG 架构是如何实现的？'
    ];
  }

  return [
    'What are the core technologies used in this blog system?',
    'What capabilities are covered by the admin console?',
    'What should be the next product iteration for this project?',
    'How is the current RAG architecture implemented?'
  ];
});

const activeSession = computed(() =>
  sessions.value.find((item) => item.sessionId === activeSessionId.value && !item.deleted) || null
);

const visibleSessions = computed(() => sessions.value.filter((item) => !item.deleted));
const deletedSessions = computed(() => sessions.value.filter((item) => item.deleted));
const latestSources = computed(() => result.value?.sources || []);
const latestFollowUps = computed(() => result.value?.followUpQuestions || []);
const hasConversation = computed(
  () => history.value.length > 0 || Boolean(optimisticQuestion.value) || streaming.value || Boolean(result.value?.answer)
);
const sidebarExpanded = computed(() => (drawerMode.value ? sidebarDrawerOpen.value : !sidebarCollapsed.value));
const shellClasses = computed(() => ({
  'sidebar-collapsed': !drawerMode.value && sidebarCollapsed.value,
  'sidebar-open': drawerMode.value && sidebarDrawerOpen.value
}));

const draftAssistantMessage = computed(() => {
  if (!result.value?.answer) {
    return null;
  }

  const lastAssistant = [...history.value].reverse().find((message) => message.role === 'assistant');
  if (!streaming.value && lastAssistant?.content === result.value.answer) {
    return null;
  }

  return {
    id: 'draft-assistant',
    role: 'assistant',
    content: result.value.answer,
    renderedContent: renderMarkdown(result.value.answer),
    citations: [],
    createdAt: new Date().toISOString(),
    pending: streaming.value
  };
});

const pendingAssistantMessage = computed(() => {
  if (!streaming.value || result.value?.answer) {
    return null;
  }

  return {
    id: 'pending-assistant',
    role: 'assistant',
    content: '',
    renderedContent: '',
    citations: [],
    createdAt: new Date().toISOString(),
    pending: true,
    skeleton: true
  };
});

const timelineMessages = computed(() => {
  const messages = history.value.map((message) => ({
    ...message,
    renderedContent:
      message.role === 'assistant' ? renderMarkdown(message.content || '') : message.content || '',
    pending: false,
    skeleton: false
  }));

  const lastUser = [...messages].reverse().find((message) => message.role === 'user');
  if (optimisticQuestion.value && lastUser?.content !== optimisticQuestion.value) {
    messages.push({
      id: 'pending-user',
      role: 'user',
      content: optimisticQuestion.value,
      mode: null,
      citations: [],
      createdAt: new Date().toISOString(),
      renderedContent: optimisticQuestion.value,
      pending: true,
      skeleton: false
    });
  }

  if (draftAssistantMessage.value) {
    messages.push(draftAssistantMessage.value);
  } else if (pendingAssistantMessage.value) {
    messages.push(pendingAssistantMessage.value);
  }

  return messages.map((message, messageIndex) => ({
    ...message,
    messageIndex
  }));
});

const threadMap = computed(() =>
  timelineMessages.value
    .filter((message) => message.role === 'user')
    .map((message, order) => ({
      id: `${message.id || 'turn'}-${order}`,
      order: order + 1,
      messageIndex: message.messageIndex,
      preview:
        (message.content || '')
          .replace(/\s+/g, ' ')
          .trim()
          .slice(0, 72) || copy.value.untitledSession
    }))
);

const composerStatus = computed(() => {
  if (sessionsLoading.value) {
    return copy.value.sessionLoading;
  }

  if (streaming.value) {
    return copy.value.streaming;
  }

  if (result.value) {
    return copy.value
      .indexed.replace('{posts}', result.value.indexedPosts)
      .replace('{chunks}', result.value.indexedChunks);
  }

  return copy.value.composerHint;
});

function formatSessionTime(value) {
  if (!value) {
    return '';
  }
  return preferences.formatDateTime(value);
}

function formatMessageCount(count) {
  return copy.value.messageCount.replace('{count}', count || 0);
}

function persistActiveSession() {
  localStorage.setItem(STORAGE_SESSION_KEY, activeSessionId.value);
}

function stopStreaming() {
  if (activeController) {
    activeController.abort();
    activeController = null;
  }
  loading.value = false;
  streaming.value = false;
}

function handleConversationScroll() {
  if (!conversationViewport.value) {
    return;
  }

  const viewport = conversationViewport.value;
  shouldStickToBottom.value =
    viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight < SCROLL_STICKY_THRESHOLD;
}

async function scrollConversationToBottom(force = false) {
  await nextTick();
  if (!conversationViewport.value) {
    return;
  }
  if (!force && !shouldStickToBottom.value && !streaming.value) {
    return;
  }

  conversationViewport.value.scrollTop = conversationViewport.value.scrollHeight;
}

function syncResponsiveLayout() {
  if (typeof window === 'undefined') {
    return;
  }

  drawerMode.value = window.innerWidth <= 1120;
  if (drawerMode.value) {
    sidebarCollapsed.value = true;
    return;
  }

  sidebarDrawerOpen.value = false;
}

function toggleSidebar() {
  if (drawerMode.value) {
    sidebarDrawerOpen.value = !sidebarDrawerOpen.value;
    return;
  }

  sidebarCollapsed.value = !sidebarCollapsed.value;
}

function closeSidebar() {
  if (drawerMode.value) {
    sidebarDrawerOpen.value = false;
  }
}

async function jumpToMessage(messageIndex) {
  await nextTick();
  const target = conversationViewport.value?.querySelector(`[data-message-index="${messageIndex}"]`);
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  closeSidebar();
}

function applyResponse(response) {
  result.value = response;
  history.value = response.history || [];
  optimisticQuestion.value = '';

  if (response.sessionId && response.sessionId !== activeSessionId.value) {
    activeSessionId.value = response.sessionId;
    persistActiveSession();
  }
}

async function loadSessions() {
  sessionsLoading.value = true;
  try {
    const res = await fetchKnowledgeSessionsApi(true);
    sessions.value = res.data.sessions || [];
  } catch (error) {
    ElMessage.warning(error?.message || copy.value.sessionsFailed);
  } finally {
    sessionsLoading.value = false;
  }
}

async function loadHistory(sessionToLoad = activeSessionId.value) {
  historyLoading.value = true;
  result.value = null;
  optimisticQuestion.value = '';

  try {
    const res = await fetchKnowledgeHistoryApi(sessionToLoad);
    history.value = res.data.messages || [];
    await scrollConversationToBottom(true);
  } catch (error) {
    history.value = [];
    ElMessage.warning(error?.message || copy.value.historyFailed);
  } finally {
    historyLoading.value = false;
  }
}

function createEmptyResult(normalizedQuestion) {
  return {
    sessionId: activeSessionId.value,
    question: normalizedQuestion,
    answer: '',
    mode: 'llm',
    llmEnabled: true,
    indexedPosts: result.value?.indexedPosts || 0,
    indexedChunks: result.value?.indexedChunks || 0,
    followUpQuestions: [],
    sources: [],
    history: history.value,
    strictCitation: true
  };
}

async function runStreamRequest(normalizedQuestion) {
  result.value = createEmptyResult(normalizedQuestion);
  optimisticQuestion.value = normalizedQuestion;
  const controller = new AbortController();
  activeController = controller;
  loading.value = true;
  streaming.value = true;
  shouldStickToBottom.value = true;

  try {
    await askKnowledgeStreamApi(
      {
        sessionId: activeSessionId.value,
        question: normalizedQuestion,
        topK: DEFAULT_TOP_K
      },
      {
        signal: controller.signal,
        onEvent: async (eventName, payload) => {
          const type = payload?.type || eventName;

          if (type === 'meta' && payload.response) {
            applyResponse(payload.response);
            result.value = {
              ...payload.response,
              answer: ''
            };
            await loadSessions();
            await scrollConversationToBottom();
            return;
          }

          if (type === 'delta') {
            const current = result.value || createEmptyResult(normalizedQuestion);
            result.value = {
              ...current,
              answer: `${current.answer || ''}${payload.delta || ''}`
            };
            await scrollConversationToBottom();
            return;
          }

          if (type === 'done' && payload.response) {
            applyResponse(payload.response);
            streaming.value = false;
            await loadSessions();
            await scrollConversationToBottom(true);
            return;
          }

          if (type === 'error') {
            throw new Error(payload.message || 'Streaming request failed');
          }
        }
      }
    );
  } finally {
    if (activeController === controller) {
      activeController = null;
    }
    loading.value = false;
    streaming.value = false;
  }
}

async function submitQuestion(presetQuestion = '') {
  if (presetQuestion) {
    question.value = presetQuestion;
  }

  const normalizedQuestion = question.value.trim();
  if (!normalizedQuestion) {
    return;
  }

  stopStreaming();
  closeSidebar();

  try {
    await runStreamRequest(normalizedQuestion);
    question.value = '';
  } catch (error) {
    if (error?.name === 'AbortError') {
      return;
    }

    try {
      optimisticQuestion.value = normalizedQuestion;
      const res = await askKnowledgeApi({
        sessionId: activeSessionId.value,
        question: normalizedQuestion,
        topK: DEFAULT_TOP_K
      });
      applyResponse(res.data);
      question.value = '';
      await loadSessions();
      ElMessage.warning(error?.message || copy.value.fallbackWarning);
      await scrollConversationToBottom(true);
    } catch (fallbackError) {
      optimisticQuestion.value = '';
      ElMessage.error(fallbackError?.message || error?.message || 'Request failed');
    } finally {
      loading.value = false;
      streaming.value = false;
    }
  }
}

async function switchSession(sessionToLoad) {
  if (!sessionToLoad || sessionToLoad === activeSessionId.value) {
    closeSidebar();
    return;
  }

  stopStreaming();
  activeSessionId.value = sessionToLoad;
  persistActiveSession();
  await loadHistory(sessionToLoad);
  closeSidebar();
}

function startNewChat() {
  stopStreaming();
  question.value = '';
  result.value = null;
  history.value = [];
  optimisticQuestion.value = '';
  activeSessionId.value = createSessionId();
  persistActiveSession();
  closeSidebar();
}

async function renameSession(session) {
  try {
    const { value } = await ElMessageBox.prompt(copy.value.renamePrompt, copy.value.renameTitle, {
      inputValue: session.title,
      inputPlaceholder: copy.value.renamePlaceholder,
      confirmButtonText: copy.value.rename,
      cancelButtonText: 'Cancel'
    });

    if (!value?.trim()) {
      return;
    }

    await renameKnowledgeSessionApi(session.sessionId, { title: value.trim() });
    await loadSessions();
    ElMessage.success(copy.value.renameSuccess);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || 'Request failed');
    }
  }
}

async function deleteSession(session) {
  try {
    await ElMessageBox.confirm(copy.value.deleteConfirmBody, copy.value.deleteConfirmTitle, {
      confirmButtonText: copy.value.remove,
      cancelButtonText: 'Cancel',
      type: 'warning'
    });

    await deleteKnowledgeSessionApi(session.sessionId);
    await loadSessions();
    ElMessage.success(copy.value.deleteSuccess);

    if (session.sessionId === activeSessionId.value) {
      const nextSession = sessions.value.find((item) => !item.deleted);
      if (nextSession) {
        await switchSession(nextSession.sessionId);
      } else {
        startNewChat();
      }
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || 'Request failed');
    }
  }
}

async function restoreSession(session) {
  try {
    await restoreKnowledgeSessionApi(session.sessionId);
    await loadSessions();
    ElMessage.success(copy.value.restoreSuccess);
  } catch (error) {
    ElMessage.error(error?.message || 'Request failed');
  }
}

watch(
  () => timelineMessages.value.length,
  async () => {
    await scrollConversationToBottom();
  }
);

onMounted(async () => {
  syncResponsiveLayout();
  window.addEventListener('resize', syncResponsiveLayout);
  await loadSessions();
  if (!sessions.value.some((item) => item.sessionId === activeSessionId.value && !item.deleted) && visibleSessions.value.length) {
    activeSessionId.value = visibleSessions.value[0].sessionId;
    persistActiveSession();
  }
  await loadHistory(activeSessionId.value);
});

onBeforeUnmount(() => {
  stopStreaming();
  window.removeEventListener('resize', syncResponsiveLayout);
});
</script>

<template>
  <div class="knowledge-shell" :class="shellClasses">
    <button
      v-if="drawerMode && sidebarDrawerOpen"
      type="button"
      class="sidebar-overlay"
      aria-label="Close session sidebar"
      @click="closeSidebar()"
    ></button>

    <aside class="knowledge-sidebar section-card" :class="{ 'is-collapsed': !sidebarExpanded }">
      <div class="sidebar-toolbar">
        <button
          type="button"
          class="sidebar-toggle"
          :aria-label="sidebarExpanded ? 'Collapse sidebar' : 'Expand sidebar'"
          @click="toggleSidebar()"
        >
          <span></span>
          <span></span>
          <span></span>
        </button>
        <el-button v-if="sidebarExpanded" class="new-chat-btn" type="primary" @click="startNewChat()">
          {{ copy.newChat }}
        </el-button>
        <button v-else type="button" class="mini-action" aria-label="New chat" @click="startNewChat()">
          +
        </button>
      </div>

      <div v-if="sidebarExpanded" class="sidebar-scroll">
        <div class="sidebar-head">
          <span class="hero-kicker">{{ copy.sidebarLabel }}</span>
          <h1 class="sidebar-title">{{ copy.title }}</h1>
          <p class="sidebar-copy muted">{{ copy.description }}</p>
        </div>

        <section class="sidebar-section">
          <div class="section-heading compact-heading">
            <h2>{{ copy.sessionsTitle }}</h2>
            <span class="muted">{{ visibleSessions.length }}</span>
          </div>

          <div v-if="visibleSessions.length" class="session-list">
            <article
              v-for="session in visibleSessions"
              :key="session.sessionId"
              class="session-item"
              :class="{ active: session.sessionId === activeSessionId }"
              @click="switchSession(session.sessionId)"
            >
              <div class="session-copy">
                <strong>{{ session.title || copy.untitledSession }}</strong>
                <p class="muted">{{ session.preview || copy.untitledSession }}</p>
              </div>
              <div class="session-meta muted">
                <span>{{ formatMessageCount(session.messageCount) }}</span>
                <span>{{ formatSessionTime(session.updatedAt) }}</span>
              </div>
              <div class="session-actions">
                <button type="button" class="ghost-action" @click.stop="renameSession(session)">
                  {{ copy.rename }}
                </button>
                <button type="button" class="ghost-action danger" @click.stop="deleteSession(session)">
                  {{ copy.remove }}
                </button>
              </div>
            </article>
          </div>
          <p v-else class="muted sidebar-empty">{{ copy.sessionEmpty }}</p>
        </section>

        <section class="sidebar-section">
          <div class="section-heading compact-heading">
            <h2>{{ copy.deletedTitle }}</h2>
            <span class="muted">{{ deletedSessions.length }}</span>
          </div>

          <div v-if="deletedSessions.length" class="session-list deleted-list">
            <article
              v-for="session in deletedSessions"
              :key="`deleted-${session.sessionId}`"
              class="session-item deleted"
            >
              <div class="session-copy">
                <strong>{{ session.title || copy.untitledSession }}</strong>
                <p class="muted">{{ session.preview || copy.untitledSession }}</p>
              </div>
              <div class="session-meta muted">
                <span>{{ formatMessageCount(session.messageCount) }}</span>
                <span>{{ formatSessionTime(session.updatedAt) }}</span>
              </div>
              <div class="session-actions">
                <button type="button" class="ghost-action" @click="restoreSession(session)">
                  {{ copy.restore }}
                </button>
              </div>
            </article>
          </div>
          <p v-else class="muted sidebar-empty">{{ copy.deletedEmpty }}</p>
        </section>

        <section class="sidebar-section">
          <div class="section-heading compact-heading">
            <h2>{{ copy.capabilityTitle }}</h2>
          </div>
          <div class="capability-list">
            <span class="capability-pill">{{ copy.capabilityCitations }}</span>
            <span class="capability-pill">{{ copy.capabilityHistory }}</span>
            <span class="capability-pill">{{ copy.capabilityRag }}</span>
          </div>
        </section>

        <section class="sidebar-section">
          <div class="section-heading compact-heading">
            <h2>{{ copy.examplesTitle }}</h2>
          </div>
          <div class="prompt-list">
            <button
              v-for="item in examples"
              :key="item"
              type="button"
              class="prompt-chip"
              :disabled="loading"
              @click="submitQuestion(item)"
            >
              {{ item }}
            </button>
          </div>
        </section>
      </div>

      <div v-else class="sidebar-collapsed-actions">
        <button
          v-for="session in visibleSessions.slice(0, 6)"
          :key="`collapsed-${session.sessionId}`"
          type="button"
          class="mini-action"
          :class="{ active: session.sessionId === activeSessionId }"
          :aria-label="session.title"
          @click="switchSession(session.sessionId)"
        >
          {{ (session.title || copy.untitledSession).slice(0, 1).toUpperCase() }}
        </button>
      </div>
    </aside>

    <main class="chat-stage section-card">
      <header class="chat-stage-head">
        <div>
          <p class="chat-stage-label muted">{{ copy.activeSession }}</p>
          <h2 class="chat-stage-title">
            {{ activeSession?.title || copy.title }}
          </h2>
          <p class="stage-subtitle muted">
            {{ activeSession?.preview || copy.workingTitle }}
          </p>
        </div>

        <div class="stage-actions">
          <button
            type="button"
            class="rail-toggle"
            :aria-label="sidebarExpanded ? 'Collapse sidebar' : 'Expand sidebar'"
            @click="toggleSidebar()"
          >
            {{ sidebarExpanded ? 'Hide Panel' : 'Open Panel' }}
          </button>
          <div v-if="result" class="stage-badges">
            <span class="stage-badge">{{ result.mode === 'llm' ? copy.modeLlm : copy.modeRetrieval }}</span>
            <span class="stage-badge">{{ copy.strictCitation }}</span>
          </div>
        </div>
      </header>

      <div
        ref="conversationViewport"
        class="conversation-viewport"
        :class="{ 'is-loading': historyLoading }"
        @scroll="handleConversationScroll"
      >
        <div v-if="historyLoading" class="conversation-timeline skeleton-timeline">
          <article v-for="index in 4" :key="`skeleton-${index}`" class="chat-row is-assistant">
            <div class="avatar-ring">
              <span>AI</span>
            </div>
            <div class="bubble-stack">
              <div class="bubble-meta">
                <strong>{{ copy.assistantRole }}</strong>
              </div>
              <div class="chat-bubble pending-bubble">
                <span class="placeholder-line long"></span>
                <span class="placeholder-line medium"></span>
                <span class="placeholder-line short"></span>
              </div>
            </div>
          </article>
        </div>

        <div v-else-if="hasConversation" class="conversation-timeline">
          <article
            v-for="message in timelineMessages"
            :key="`${message.id}-${message.role}-${message.messageIndex}`"
            class="chat-row"
            :class="`is-${message.role}`"
            :data-message-index="message.messageIndex"
          >
            <div class="avatar-ring">
              <span>{{ message.role === 'assistant' ? 'AI' : 'ME' }}</span>
            </div>
            <div class="bubble-stack">
              <div class="bubble-meta">
                <strong>{{ message.role === 'assistant' ? copy.assistantRole : copy.userRole }}</strong>
                <div v-if="message.role === 'assistant' && message.citations?.length" class="bubble-citations">
                  <span
                    v-for="citation in message.citations"
                    :key="`${message.id}-${citation}`"
                    class="citation-pill"
                  >
                    [{{ citation }}]
                  </span>
                </div>
              </div>

              <div v-if="message.skeleton" class="chat-bubble pending-bubble">
                <span class="placeholder-line long"></span>
                <span class="placeholder-line medium"></span>
                <span class="placeholder-line short"></span>
              </div>

              <div
                v-else-if="message.role === 'assistant'"
                class="chat-bubble content-html markdown-body"
                :class="{ pending: message.pending }"
                v-html="message.renderedContent"
              ></div>

              <div v-else class="chat-bubble user-bubble" :class="{ pending: message.pending }">
                {{ message.content }}
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <span class="hero-kicker">{{ copy.strictCitation }}</span>
          <h3>{{ copy.emptyStateTitle }}</h3>
          <p class="muted">{{ copy.emptyStateBody }}</p>
        </div>
      </div>

      <footer class="composer-shell">
        <div class="composer-card" :class="{ 'is-busy': loading }">
          <el-input
            v-model="question"
            type="textarea"
            resize="none"
            :rows="4"
            :placeholder="copy.placeholder"
            :disabled="loading"
            @keyup.ctrl.enter="submitQuestion()"
          />
          <div class="composer-foot">
            <span class="composer-status muted" :class="{ 'is-live': streaming }">
              <span v-if="streaming" class="status-dots" aria-hidden="true">
                <i></i>
                <i></i>
                <i></i>
              </span>
              {{ composerStatus }}
            </span>
            <div class="composer-actions">
              <el-button v-if="streaming" plain @click="stopStreaming()">{{ copy.stop }}</el-button>
              <el-button class="send-btn" type="primary" :disabled="loading" @click="submitQuestion()">
                <span class="send-btn-content">
                  <span>{{ loading ? copy.asking : copy.ask }}</span>
                  <span class="send-btn-arrow" aria-hidden="true">-></span>
                </span>
              </el-button>
            </div>
          </div>
        </div>
      </footer>
    </main>

    <aside class="context-rail">
      <section class="section-card context-panel">
        <div class="section-heading compact-heading">
          <h2>{{ copy.sourcesTitle }}</h2>
        </div>
        <p class="muted rail-hint">{{ copy.sourceHint }}</p>
        <div v-if="latestSources.length" class="source-list">
          <router-link
            v-for="source in latestSources"
            :key="`${source.slug}-${source.citationIndex}`"
            :to="`/posts/${source.slug}`"
            class="source-card"
          >
            <strong>[{{ source.citationIndex }}] {{ source.title }}</strong>
            <p>{{ source.excerpt }}</p>
            <span class="muted">score {{ source.score }}</span>
          </router-link>
        </div>
        <p v-else class="muted rail-empty">{{ copy.noSources }}</p>
      </section>

      <section class="section-card context-panel">
        <div class="section-heading compact-heading">
          <h2>{{ copy.followUpsTitle }}</h2>
        </div>
        <div v-if="latestFollowUps.length" class="prompt-list stacked">
          <button
            v-for="item in latestFollowUps"
            :key="item"
            type="button"
            class="prompt-chip prompt-chip-block"
            :disabled="loading"
            @click="submitQuestion(item)"
          >
            {{ item }}
          </button>
        </div>
        <p v-else class="muted rail-empty">{{ copy.noFollowUps }}</p>
      </section>

      <section class="section-card context-panel">
        <div class="section-heading compact-heading">
          <h2>{{ copy.threadMapTitle }}</h2>
        </div>
        <div v-if="threadMap.length" class="thread-map">
          <button
            v-for="item in threadMap"
            :key="item.id"
            type="button"
            class="thread-item"
            @click="jumpToMessage(item.messageIndex)"
          >
            <span class="thread-index">{{ copy.turnLabel }} {{ item.order }}</span>
            <span class="thread-preview">{{ item.preview }}</span>
          </button>
        </div>
        <p v-else class="muted rail-empty">{{ copy.noThreadMap }}</p>
      </section>
    </aside>
  </div>
</template>

<style scoped>
.knowledge-shell {
  position: relative;
  width: 100%;
  min-height: calc(100vh - 122px);
  margin: 0;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr) 320px;
  gap: 14px;
  align-items: stretch;
}

.knowledge-shell.sidebar-collapsed {
  grid-template-columns: 88px minmax(0, 1fr) 320px;
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  z-index: 24;
  border: 0;
  background: rgba(0, 0, 0, 0.52);
}

.knowledge-sidebar,
.chat-stage,
.context-rail,
.context-panel {
  min-height: 0;
}

.knowledge-sidebar {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  padding: 18px;
  overflow: hidden;
}

.sidebar-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sidebar-toggle,
.mini-action,
.rail-toggle,
.ghost-action {
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: var(--text-main);
  transition: 0.2s ease;
}

.sidebar-toggle,
.mini-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  cursor: pointer;
}

.sidebar-toggle:hover,
.mini-action:hover,
.rail-toggle:hover,
.ghost-action:hover {
  border-color: var(--line-strong);
  background: var(--bg-panel-hover);
}

.sidebar-toggle {
  flex-direction: column;
  gap: 4px;
}

.sidebar-toggle span {
  width: 16px;
  height: 1px;
  background: currentColor;
}

.new-chat-btn {
  width: 100%;
  min-height: 46px;
}

.sidebar-scroll {
  margin-top: 18px;
  padding-right: 6px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.sidebar-collapsed-actions {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-head {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-title {
  margin: 0;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 0.94;
  letter-spacing: -0.06em;
  text-transform: uppercase;
}

.sidebar-copy,
.sidebar-empty {
  margin: 0;
  line-height: 1.8;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.compact-heading {
  margin-bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.compact-heading h2 {
  margin: 0;
  font-size: 0.92rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.capability-list,
.prompt-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.stacked {
  flex-direction: column;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-item {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  cursor: pointer;
  transition: 0.2s ease;
}

.session-item:hover {
  transform: translateY(-1px);
  border-color: var(--line-strong);
  background: var(--bg-panel-hover);
}

.session-item.active {
  border-color: rgba(255, 255, 255, 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.04)),
    var(--bg-panel);
}

.session-item.deleted {
  opacity: 0.82;
}

.session-copy strong {
  display: block;
  margin-bottom: 8px;
  line-height: 1.45;
}

.session-copy p {
  margin: 0;
  line-height: 1.65;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.session-meta {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 0.78rem;
}

.session-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ghost-action {
  min-height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 0.76rem;
}

.ghost-action.danger:hover {
  border-color: rgba(255, 104, 104, 0.28);
  color: #ff9f9f;
}

.capability-pill,
.prompt-chip {
  border-radius: 18px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: var(--text-secondary);
  transition: 0.2s ease;
}

.capability-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  font-size: 0.82rem;
}

.prompt-chip {
  min-height: 40px;
  padding: 10px 14px;
  cursor: pointer;
  text-align: left;
}

.prompt-chip:hover:not(:disabled) {
  color: var(--text-main);
  border-color: var(--line-strong);
  background: var(--bg-panel-hover);
}

.prompt-chip:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.prompt-chip-block {
  width: 100%;
}

.chat-stage {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
}

.chat-stage-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 28px 18px;
  border-bottom: 1px solid var(--line);
}

.chat-stage-label,
.stage-subtitle {
  margin: 0;
}

.chat-stage-label {
  margin-bottom: 8px;
  font-size: 0.8rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.chat-stage-title {
  margin: 0;
  font-size: 1.8rem;
  letter-spacing: -0.04em;
}

.stage-subtitle {
  margin-top: 8px;
  line-height: 1.7;
  max-width: 720px;
}

.stage-actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.stage-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.rail-toggle {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stage-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: var(--text-secondary);
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.conversation-viewport {
  overflow: auto;
  padding: 28px;
  scroll-padding-bottom: 200px;
}

.conversation-viewport.is-loading {
  overflow: hidden;
}

.conversation-timeline {
  width: min(980px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.chat-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.avatar-ring {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03));
  color: var(--text-secondary);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.chat-row.is-user .avatar-ring {
  background: rgba(255, 255, 255, 0.05);
}

.bubble-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bubble-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bubble-meta strong {
  font-size: 0.92rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.bubble-citations {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.citation-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: var(--text-secondary);
  font-size: 0.76rem;
}

.chat-bubble {
  width: 100%;
  padding: 20px 22px;
  border-radius: 24px;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, var(--bg-panel-strong), var(--bg-panel)),
    var(--bg-elevated);
  line-height: 1.9;
}

.chat-bubble.pending {
  opacity: 0.88;
}

.user-bubble {
  width: fit-content;
  max-width: min(82%, 760px);
  margin-left: auto;
  background:
    linear-gradient(180deg, var(--bg-panel-hover), var(--bg-panel)),
    var(--bg-main);
}

.pending-bubble {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.placeholder-line {
  display: block;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.06));
  background-size: 220% 100%;
  animation: shimmer 1.3s linear infinite;
}

.placeholder-line.long {
  width: 100%;
}

.placeholder-line.medium {
  width: 78%;
}

.placeholder-line.short {
  width: 54%;
}

.empty-state {
  width: min(760px, 100%);
  margin: 8vh auto 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.empty-state h3 {
  margin: 0;
  font-size: clamp(2rem, 5vw, 3.4rem);
  line-height: 0.96;
  letter-spacing: -0.06em;
  text-transform: uppercase;
}

.empty-state p {
  margin: 0;
  max-width: 620px;
  line-height: 1.9;
}

.composer-shell {
  position: sticky;
  bottom: 0;
  z-index: 3;
  padding: 18px 20px 20px;
  background: linear-gradient(180deg, rgba(5, 5, 5, 0), rgba(5, 5, 5, 0.72) 20%, rgba(5, 5, 5, 0.94));
  backdrop-filter: blur(16px);
}

.composer-card {
  position: relative;
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 14px 14px 12px;
  border-radius: 28px;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, var(--bg-panel-hover), var(--bg-panel)),
    var(--bg-main);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.32);
}

.composer-card::after {
  content: '';
  position: absolute;
  inset: auto 18px -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.22), transparent);
  opacity: 0.34;
}

.composer-card.is-busy {
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.composer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 12px;
  padding: 0 6px;
}

.composer-status {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.composer-status.is-live {
  color: var(--text-main);
}

.status-dots {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.status-dots i {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.35;
  animation: status-pulse 1s infinite ease-in-out;
}

.status-dots i:nth-child(2) {
  animation-delay: 0.12s;
}

.status-dots i:nth-child(3) {
  animation-delay: 0.24s;
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.send-btn-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.send-btn-arrow {
  display: inline-block;
  transition: transform 0.2s ease;
}

.send-btn:hover .send-btn-arrow {
  transform: translate(2px, -2px);
}

.context-rail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.context-panel {
  padding: 22px;
}

.rail-hint,
.rail-empty {
  margin: 0 0 14px;
  line-height: 1.7;
}

.source-list,
.thread-map {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.source-card,
.thread-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 15px 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  transition: 0.2s ease;
}

.source-card:hover,
.thread-item:hover {
  border-color: var(--line-strong);
  background: var(--bg-panel-hover);
}

.source-card p {
  margin: 0;
  line-height: 1.75;
}

.thread-item {
  cursor: pointer;
  text-align: left;
}

.thread-index {
  color: var(--text-secondary);
  font-size: 0.76rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.thread-preview {
  line-height: 1.65;
  color: var(--text-main);
}

html[data-theme='light'] .composer-shell {
  background: linear-gradient(180deg, rgba(239, 239, 236, 0), rgba(239, 239, 236, 0.74) 20%, rgba(239, 239, 236, 0.96));
}

@keyframes status-pulse {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.3;
  }

  40% {
    transform: translateY(-3px);
    opacity: 1;
  }
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: -100% 0;
  }
}

@media (min-width: 1800px) {
  .knowledge-shell {
    grid-template-columns: 390px minmax(0, 1fr) 360px;
    min-height: calc(100vh - 126px);
  }

  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 96px minmax(0, 1fr) 360px;
  }

  .conversation-timeline,
  .composer-card {
    width: min(1100px, 100%);
  }
}

@media (max-width: 1500px) {
  .knowledge-shell {
    grid-template-columns: 320px minmax(0, 1fr) 300px;
  }

  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 84px minmax(0, 1fr) 300px;
  }
}

@media (max-width: 1320px) {
  .knowledge-shell {
    grid-template-columns: 300px minmax(0, 1fr);
  }

  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 84px minmax(0, 1fr);
  }

  .context-rail {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1120px) {
  .knowledge-shell,
  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 1fr;
    min-height: calc(100vh - 108px);
  }

  .knowledge-sidebar {
    position: fixed;
    top: 90px;
    left: 12px;
    bottom: 12px;
    width: min(380px, calc(100vw - 24px));
    z-index: 30;
    transform: translateX(calc(-100% - 20px));
    transition: transform 0.24s ease;
    box-shadow: 0 28px 80px rgba(0, 0, 0, 0.44);
  }

  .knowledge-shell.sidebar-open .knowledge-sidebar {
    transform: translateX(0);
  }

  .context-rail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .knowledge-shell {
    gap: 12px;
    min-height: calc(100vh - 88px);
  }

  .knowledge-sidebar {
    top: 82px;
    left: 8px;
    bottom: 8px;
    width: min(348px, calc(100vw - 16px));
  }

  .knowledge-sidebar,
  .context-panel {
    padding: 16px;
  }

  .chat-stage-head,
  .conversation-viewport {
    padding-left: 18px;
    padding-right: 18px;
  }

  .chat-stage-head,
  .stage-actions,
  .composer-foot,
  .bubble-meta,
  .session-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .rail-toggle {
    width: 100%;
    justify-content: center;
  }

  .conversation-timeline {
    gap: 20px;
  }

  .chat-row {
    grid-template-columns: 40px minmax(0, 1fr);
  }

  .avatar-ring {
    width: 40px;
    height: 40px;
    border-radius: 14px;
  }

  .composer-shell {
    padding: 14px 12px 12px;
  }

  .user-bubble {
    max-width: 100%;
  }
}

@media (max-width: 560px) {
  .sidebar-title {
    font-size: 1.7rem;
  }

  .chat-stage-title {
    font-size: 1.45rem;
  }

  .conversation-viewport {
    padding: 16px 14px;
  }

  .chat-bubble {
    padding: 16px 16px 18px;
    border-radius: 20px;
  }

  .composer-card {
    padding: 12px 12px 10px;
    border-radius: 22px;
  }

  .session-item {
    padding: 12px;
  }

  .context-panel {
    padding: 14px;
  }
}

@media (max-width: 420px) {
  .knowledge-shell {
    min-height: calc(100vh - 76px);
  }

  .chat-stage-head {
    padding: 16px 14px 12px;
  }

  .conversation-viewport {
    padding: 14px 12px;
  }

  .chat-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .avatar-ring {
    width: 36px;
    height: 36px;
    border-radius: 12px;
  }

  .bubble-meta strong {
    font-size: 0.86rem;
  }
}
</style>
