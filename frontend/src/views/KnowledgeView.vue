<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchQwenConfigApi, saveQwenConfigApi } from '../api/auth';
import {
  askKnowledgeApi,
  askKnowledgeStreamApi,
  deleteKnowledgeSessionApi,
  fetchKnowledgeHistoryApi,
  fetchKnowledgeSessionsApi,
  purgeKnowledgeSessionApi,
  replayKnowledgeApi,
  renameKnowledgeSessionApi,
  restoreKnowledgeSessionApi,
  submitKnowledgeFeedbackApi
} from '../api/blog';
import { renderMarkdown } from '../utils/markdown';
import { useAuthStore } from '../stores/auth';
import { usePreferencesStore } from '../stores/preferences';

const STORAGE_SESSION_KEY = 'blog-rag-session-id';
const STORAGE_SEARCH_MODE_KEY = 'blog-rag-search-mode';
const STORAGE_SESSION_LIST_CACHE_PREFIX = 'blog-rag-session-list-cache';
const STORAGE_HISTORY_CACHE_PREFIX = 'blog-rag-history-cache';
const DEFAULT_TOP_K = 4;
const SCROLL_STICKY_THRESHOLD = 96;
const SOURCE_PREVIEW_LIMIT = 4;
const LOCAL_CACHE_TTL = 5 * 60 * 1000;
const SEARCH_MODE_LOCAL_ONLY = 'LOCAL_ONLY';
const SEARCH_MODE_LOCAL_AND_WEB = 'LOCAL_AND_WEB';

const preferences = usePreferencesStore();
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const sessions = ref([]);
const loading = ref(false);
const historyLoading = ref(false);
const sessionsLoading = ref(false);
const streaming = ref(false);
const question = ref('');
const result = ref(null);
const history = ref([]);
const pendingTurn = ref(null);
const feedbackSubmittingId = ref(null);
const editingMessageId = ref(null);
const editingMessageContent = ref('');
const answerVariantSelections = ref({});
const searchMode = ref(readSearchMode());
const activeSessionId = ref(readSessionId());
const activeSourceMessageId = ref(null);
const blogSourcesExpanded = ref(false);
const webSourcesExpanded = ref(false);
const drawerMode = ref(false);
const sidebarCollapsed = ref(typeof window !== 'undefined' ? window.innerWidth < 1480 : false);
const sidebarDrawerOpen = ref(false);
const conversationViewport = ref(null);
const shouldStickToBottom = ref(true);
const qwenConfigLoading = ref(false);
const qwenSaving = ref(false);
const qwenConfig = ref({
  hasApiKey: false,
  selectedModel: '',
  webSearchEnabled: false,
  models: []
});
const qwenApiKeyInput = ref('');
const qwenApiKeyEditorVisible = ref(false);
const contextRailCollapsed = ref(false);
const chatStageRef = ref(null);
const isChatStageFullscreen = ref(false);

let activeController = null;

function normalizeSessionId(value) {
  const normalized = String(value || '').trim();
  return normalized || '';
}

function normalizeSearchMode(value) {
  return String(value || '').trim().toUpperCase() === SEARCH_MODE_LOCAL_AND_WEB
    ? SEARCH_MODE_LOCAL_AND_WEB
    : SEARCH_MODE_LOCAL_ONLY;
}

function readSearchMode() {
  return normalizeSearchMode(localStorage.getItem(STORAGE_SEARCH_MODE_KEY));
}

function persistSearchMode() {
  localStorage.setItem(STORAGE_SEARCH_MODE_KEY, searchMode.value);
}

function readSessionId() {
  const fromQuery = normalizeSessionId(route.query.sessionId);
  if (fromQuery) {
    localStorage.setItem(STORAGE_SESSION_KEY, fromQuery);
    return fromQuery;
  }

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

function getCacheUserScope() {
  return String(authStore.user?.id || authStore.user?.username || '').trim();
}

function buildSessionListCacheKey() {
  const scope = getCacheUserScope();
  return scope ? `${STORAGE_SESSION_LIST_CACHE_PREFIX}:${scope}` : '';
}

function buildHistoryCacheKey(sessionId) {
  const scope = getCacheUserScope();
  const normalizedSessionId = normalizeSessionId(sessionId);
  return scope && normalizedSessionId ? `${STORAGE_HISTORY_CACHE_PREFIX}:${scope}:${normalizedSessionId}` : '';
}

function readLocalCache(key) {
  if (!key) {
    return null;
  }
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || 'null');
    if (!parsed || typeof parsed !== 'object') {
      return null;
    }
    const savedAt = Number(parsed.savedAt || 0);
    if (!savedAt || Date.now() - savedAt > LOCAL_CACHE_TTL) {
      localStorage.removeItem(key);
      return null;
    }
    return parsed;
  } catch {
    localStorage.removeItem(key);
    return null;
  }
}

function writeLocalCache(key, value) {
  if (!key) {
    return;
  }
  localStorage.setItem(
    key,
    JSON.stringify({
      savedAt: Date.now(),
      value
    })
  );
}

function persistSessionsCache() {
  if (!authStore.isAuthenticated) {
    return;
  }
  writeLocalCache(buildSessionListCacheKey(), Array.isArray(sessions.value) ? sessions.value : []);
}

function restoreSessionsCache() {
  if (!authStore.isAuthenticated) {
    return false;
  }
  const cached = readLocalCache(buildSessionListCacheKey());
  if (!Array.isArray(cached?.value) || !cached.value.length) {
    return false;
  }
  sessions.value = cached.value;
  return true;
}

function persistHistoryCache(sessionId, messages = history.value) {
  if (!authStore.isAuthenticated) {
    return;
  }
  const normalizedSessionId = normalizeSessionId(sessionId);
  if (!normalizedSessionId) {
    return;
  }
  writeLocalCache(buildHistoryCacheKey(normalizedSessionId), Array.isArray(messages) ? messages : []);
}

function restoreHistoryCache(sessionId) {
  if (!authStore.isAuthenticated) {
    return false;
  }
  const normalizedSessionId = normalizeSessionId(sessionId);
  if (!normalizedSessionId) {
    return false;
  }
  const cached = readLocalCache(buildHistoryCacheKey(normalizedSessionId));
  if (!Array.isArray(cached?.value) || !cached.value.length) {
    return false;
  }
  history.value = cached.value;
  result.value = null;
  return true;
}

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      sidebarLabel: 'RAG 工作台',
      title: '博客知识助手',
      description: '基于已发布文章进行检索增强问答，保留会话历史，并给出带引用的回答。',
      newChat: '新建会话',
      ask: '发送',
      asking: '生成中',
      stop: '停止',
      placeholder: '输入你的问题',
      streaming: '正在生成带引用的回答...',
      sessionsTitle: '会话列表',
      deletedTitle: '最近删除',
      sourcesTitle: '参考来源',
      followUpsTitle: '延伸问题',
      capabilityTitle: '能力边界',
      capabilityCitations: '严格引用',
      capabilityHistory: '多轮会话',
      capabilityRag: '发布后自动切片索引',
      sessionEmpty: '还没有历史会话，发起第一个问题后会显示在这里。',
      deletedEmpty: '暂无已删除会话。',
      historyTitle: '当前对话',
      modeLlm: '模型回答',
      modeRetrieval: '检索回答',
      strictCitation: '严格引用',
      userRole: '用户',
      assistantRole: '助手',
      emptyStateTitle: '开始一次新的知识问答',
      emptyStateBody: '输入问题后，系统会先检索相关文章片段，再生成带引用的回答。',
      composerHint: 'Enter 发送，Ctrl + Enter 换行',
      searchModeTitle: '检索范围',
      searchModeLocal: '仅站内',
      searchModeHybrid: '站内 + 联网',
      searchModeLocalShort: '站内',
      searchModeHybridShort: '联网',
      sourceHint: '回答里的 [1]、[2] 会映射到这里的参考来源。',
      noSources: '本轮还没有来源卡片。',
      noFollowUps: '回答完成后，这里会给出下一步追问建议。',
      noThreadMap: '当前会话还没有用户提问节点。',
      fallbackWarning: '流式通道不可用，已切换为普通请求。',
      historyFailed: '加载会话历史失败',
      sessionsFailed: '加载会话列表失败',
      rename: '重命名',
      remove: '删除',
      restore: '恢复',
      purge: '永久删除',
      renameTitle: '重命名会话',
      renamePrompt: '输入新的会话标题',
      renamePlaceholder: '请输入会话标题',
      renameSuccess: '会话已重命名',
      deleteConfirmTitle: '删除会话',
      deleteConfirmBody: '该会话会进入最近删除列表，之后仍可恢复。',
      deleteSuccess: '会话已删除',
      restoreSuccess: '会话已恢复',
      purgeConfirmTitle: '永久删除会话',
      purgeConfirmBody: '永久删除后，该会话以及聊天记录都无法恢复，确认继续吗？',
      purgeSuccess: '会话已永久删除',
      activeSession: '当前会话',
      untitledSession: '未命名会话',
      turnLabel: '问题',
      messageCount: '{count} 条消息',
      indexed: '{posts} 篇文章 / {chunks} 个切片',
      sessionLoading: '正在同步会话...',
      workingTitle: '正在整理回答',
      blogSourceLabel: '站内文章',
      webSourceLabel: '联网来源',
      webSourceFallback: '外部网页',
      scoreLabel: '相关度',
      viewMoreSources: '查看更多（+{count}）',
      collapseSources: '收起'
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
    placeholder: 'Ask the knowledge base something',
    streaming: 'Generating cited answer...',
    sessionsTitle: 'Sessions',
    deletedTitle: 'Recently Deleted',
    sourcesTitle: 'Sources',
    followUpsTitle: 'Follow-ups',
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
    composerHint: 'Press Enter to send, Ctrl + Enter for newline',
    searchModeTitle: 'Search Scope',
    searchModeLocal: 'Local Only',
    searchModeHybrid: 'Local + Web',
    searchModeLocalShort: 'Local',
    searchModeHybridShort: 'Web',
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
    purge: 'Permanently Delete',
    renameTitle: 'Rename Session',
    renamePrompt: 'Enter a new title for this conversation',
    renamePlaceholder: 'Conversation title',
    renameSuccess: 'Session renamed',
    deleteConfirmTitle: 'Delete Session',
    deleteConfirmBody: 'The conversation will move to Recently Deleted and can be restored later.',
    deleteSuccess: 'Session deleted',
    restoreSuccess: 'Session restored',
    purgeConfirmTitle: 'Permanently Delete Session',
    purgeConfirmBody: 'This permanently removes the session and its chat history. Continue?',
    purgeSuccess: 'Session permanently deleted',
    activeSession: 'Active Session',
    untitledSession: 'Untitled Session',
    turnLabel: 'Turn',
    messageCount: '{count} messages',
    indexed: '{posts} posts / {chunks} chunks indexed',
    sessionLoading: 'Refreshing sessions...',
    workingTitle: 'Preparing the answer',
    blogSourceLabel: 'On-site',
    webSourceLabel: 'Web',
    webSourceFallback: 'External page',
    scoreLabel: 'Score',
    viewMoreSources: 'View more (+{count})',
    collapseSources: 'Show less'
  };
});

const feedbackCopy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      title: '回答反馈',
      prompt: '你希望这条回答如何改进？',
      placeholder: '可选备注。提交后，后台管理员可以查看这条反馈以及关联的聊天记录。',
      confirm: '提交',
      cancel: '取消',
      helpfulSaved: '感谢反馈，后台管理员可以查看这条反馈以及关联的聊天记录。',
      needsWorkSaved: '反馈已提交，后台管理员可以查看这条反馈以及关联的聊天记录。',
      disclosure: '提交反馈即表示你知晓：后台管理员可以查看这条反馈以及关联的聊天记录。'
    };
  }

  return {
    title: 'Answer feedback',
    prompt: 'What should be improved in this answer?',
    placeholder: 'Optional note. Admins can review this feedback together with the related chat history.',
    confirm: 'Send',
    cancel: 'Cancel',
    helpfulSaved: 'Thanks - admins can review this feedback together with the related chat history.',
    needsWorkSaved: 'Feedback saved - admins can review it together with the related chat history.',
    disclosure: 'Submitting feedback lets admins review this feedback together with the related chat history.'
  };
});

function messageSourceAnchorPrefix(messageId) {
  return `rag-source-${messageId || 'message'}`;
}

function citationAnchorId(messageId, index) {
  return `${messageSourceAnchorPrefix(messageId)}-${index}`;
}

function renderAssistantContent(message) {
  const sources = Array.isArray(message?.sources) ? message.sources : [];
  return renderMarkdown(message?.content || '', {
    citationCount: sources.length,
    anchorPrefix: messageSourceAnchorPrefix(message?.id)
  });
}

function messageVariants(message) {
  if (message?.role !== 'assistant') {
    return [];
  }
  const baseVariant = {
    question: '',
    answer: message.content || '',
    mode: message.mode || null,
    citations: Array.isArray(message.citations) ? message.citations : [],
    sources: Array.isArray(message.sources) ? message.sources : [],
    createdAt: message.createdAt || null
  };
  const extraVariants = Array.isArray(message?.variants) ? message.variants : [];
  return [baseVariant, ...extraVariants];
}

function getSelectedVariantIndex(message) {
  const variants = messageVariants(message);
  if (!variants.length) {
    return 0;
  }
  const selected = Number(answerVariantSelections.value[message.id]);
  if (Number.isInteger(selected) && selected >= 0 && selected < variants.length) {
    return selected;
  }
  return variants.length - 1;
}

function getSelectedAssistantVariant(message) {
  const variants = messageVariants(message);
  if (!variants.length) {
    return null;
  }
  return variants[getSelectedVariantIndex(message)] || null;
}

function selectAnswerVariant(messageId, index) {
  answerVariantSelections.value = {
    ...answerVariantSelections.value,
    [messageId]: index
  };
}

const activeSession = computed(() =>
  sessions.value.find((item) => item.sessionId === activeSessionId.value && !item.deleted) || null
);

const visibleSessions = computed(() => sessions.value.filter((item) => !item.deleted));
const deletedSessions = computed(() => sessions.value.filter((item) => item.deleted));
const latestFollowUps = computed(() => result.value?.followUpQuestions || []);
const searchModeOptions = computed(() => [
  {
    value: SEARCH_MODE_LOCAL_ONLY,
    label: copy.value.searchModeLocal
  },
  {
    value: SEARCH_MODE_LOCAL_AND_WEB,
    label: copy.value.searchModeHybrid
  }
]);
const canUseHybridSearch = computed(() => Boolean(qwenConfig.value.webSearchEnabled));
const qwenCopy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        loginRequired: '登录后才能发起对话',
        loginHint: '当前 RAG 聊天需要账号登录，未登录时仍可浏览文章。',
        goLogin: '去登录',
        configTitle: '配置千问 API Key',
        configHint: '需要先填写你的千问 API Key，系统会自动解析可用模型，并判断是否支持联网搜索。',
        apiKey: '千问 API Key',
        apiKeyPlaceholder: '请输入你的千问 API Key',
        saveConfig: '保存并解析',
        model: '对话模型',
        localOnlyLocked: '当前模型不支持联网搜索，已强制使用仅站内检索',
        noModels: '保存 Key 后会显示可用模型列表',
        saved: '配置已保存',
        editApiKey: '修改 API Key',
        cancelEditApiKey: '取消修改'
      }
    : {
        loginRequired: 'Sign in required',
        loginHint: 'RAG chat requires an account, while articles remain public.',
        goLogin: 'Sign In',
        configTitle: 'Configure Qwen API Key',
        configHint: 'Enter your Qwen API key to detect available models and web-search support.',
        apiKey: 'Qwen API Key',
        apiKeyPlaceholder: 'Enter your Qwen API key',
        saveConfig: 'Save and Detect',
        model: 'Chat Model',
        localOnlyLocked: 'This model does not support web search, so local retrieval is enforced.',
        noModels: 'Available models will appear after the key is saved',
        saved: 'Configuration saved',
        editApiKey: 'Update API Key',
        cancelEditApiKey: 'Cancel'
      }
);
const uiCopy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        composerSubmitHint: 'Enter 发送',
        composerNewlineHint: 'Ctrl + Enter 换行',
        fullscreenEnter: '全屏聊天',
        fullscreenExit: '退出全屏',
        sourcesCollapse: '收起参考栏',
        sourcesExpand: '展开参考栏',
        configToolsOpen: '展开模型设置'
      }
    : {
        composerSubmitHint: 'Enter to send',
        composerNewlineHint: 'Ctrl + Enter for newline',
        fullscreenEnter: 'Enter fullscreen',
        fullscreenExit: 'Exit fullscreen',
        sourcesCollapse: 'Collapse sources',
        sourcesExpand: 'Expand sources',
        configToolsOpen: 'Open model settings'
      }
);
const hasQwenConfig = computed(() => Boolean(qwenConfig.value.hasApiKey && qwenConfig.value.selectedModel));
const hasConversation = computed(
  () => history.value.length > 0 || Boolean(pendingTurn.value?.question) || streaming.value || Boolean(result.value?.answer)
);
const sidebarExpanded = computed(() => (drawerMode.value ? sidebarDrawerOpen.value : !sidebarCollapsed.value));
const shellClasses = computed(() => ({
  'sidebar-collapsed': !drawerMode.value && sidebarCollapsed.value,
  'sidebar-open': drawerMode.value && sidebarDrawerOpen.value,
  'context-rail-collapsed': contextRailCollapsed.value,
  'stage-fullscreen': isChatStageFullscreen.value
}));

const draftAssistantMessage = computed(() => {
  const pending = pendingTurn.value;
  const assistantAnswer = pending?.answer || result.value?.answer || '';
  if (!assistantAnswer) {
    return null;
  }

  const lastAssistant = [...history.value].reverse().find((message) => message.role === 'assistant');
  if (!streaming.value && lastAssistant?.content === assistantAnswer) {
    return null;
  }

  return {
    id: pending?.assistantId || 'draft-assistant',
    role: 'assistant',
    content: assistantAnswer,
    sources: result.value?.sources || [],
    citations: [],
    createdAt: pending?.createdAt || new Date().toISOString(),
    pending: streaming.value,
    feedbackHelpful: null,
    feedbackNote: null,
    feedbackAt: null
  };
});

const pendingAssistantMessage = computed(() => {
  if (!streaming.value || pendingTurn.value?.answer || result.value?.answer) {
    return null;
  }

  return {
    id: pendingTurn.value?.assistantId || 'pending-assistant',
    role: 'assistant',
    content: '',
    renderedContent: '',
    sources: [],
    citations: [],
    createdAt: pendingTurn.value?.createdAt || new Date().toISOString(),
    pending: true,
    skeleton: true,
    feedbackHelpful: null,
    feedbackNote: null,
    feedbackAt: null
  };
});

const timelineMessages = computed(() => {
  const messages = history.value.map((message, index, rawMessages) => {
    if (message.role === 'assistant') {
      const variant = getSelectedAssistantVariant(message);
      const content = variant?.answer || message.content || '';
      const sources = Array.isArray(variant?.sources) ? variant.sources : Array.isArray(message.sources) ? message.sources : [];
      const citations = Array.isArray(variant?.citations) ? variant.citations : Array.isArray(message.citations) ? message.citations : [];
      return {
        ...message,
        content,
        mode: variant?.mode || message.mode || null,
        sources,
        citations,
        selectedVariantIndex: getSelectedVariantIndex(message),
        variantCount: messageVariants(message).length,
        renderedContent: renderAssistantContent({
          ...message,
          content,
          sources
        }),
        pending: false,
        skeleton: false
      };
    }

    const pairedAssistant = rawMessages[index + 1]?.role === 'assistant' ? rawMessages[index + 1] : null;
    const pairedVariant = pairedAssistant ? getSelectedAssistantVariant(pairedAssistant) : null;
    const content = pairedVariant?.question || message.content || '';
    return {
      ...message,
      content,
      renderedContent: content,
      pending: false,
      skeleton: false
    };
  });

  const pending = pendingTurn.value;
  const lastUser = [...messages].reverse().find((message) => message.role === 'user');
  if (pending?.question && lastUser?.content !== pending.question) {
    messages.push({
      id: pending.userId,
      role: 'user',
      content: pending.question,
      mode: null,
      sources: [],
      citations: [],
      createdAt: pending.createdAt,
      renderedContent: pending.question,
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
    renderedContent:
      message.role === 'assistant' && !message.skeleton ? renderAssistantContent(message) : message.renderedContent,
    messageIndex
  }));
});

const messageActionCopy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      copy: '复制',
      edit: '修改提问',
      retry: '重新回复',
      save: '发送修改后提问',
      cancel: '取消',
      copied: '消息已复制',
      copyFailed: '复制失败',
      emptyEdit: '请输入要修改的内容'
    };
  }

  return {
    copy: 'Copy',
    edit: 'Edit',
    retry: 'Retry',
    save: 'Send edited message',
    cancel: 'Cancel',
    copied: 'Message copied',
    copyFailed: 'Copy failed',
    emptyEdit: 'Please enter updated content'
  };
});

const answerVariantCopy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        title: '回答版本',
        label: '结果 {index}'
      }
    : {
        title: 'Answer versions',
        label: 'Result {index}'
      }
);

const assistantMessagesWithSources = computed(() =>
  timelineMessages.value.filter(
    (message) => message.role === 'assistant' && !message.skeleton && Array.isArray(message.sources) && message.sources.length
  )
);

const latestAssistantTimelineMessageId = computed(
  () => [...timelineMessages.value].reverse().find((message) => message.role === 'assistant' && !message.skeleton)?.id || null
);

const latestSourceMessageId = computed(() => assistantMessagesWithSources.value.at(-1)?.id || null);

const latestSourceMessage = computed(() => assistantMessagesWithSources.value.at(-1) || null);
const activeSourceMessage = computed(() => latestSourceMessage.value);

const latestSources = computed(() => activeSourceMessage.value?.sources || []);
const blogSources = computed(() => latestSources.value.filter((source) => !isWebSource(source)));
const webSources = computed(() => latestSources.value.filter((source) => isWebSource(source)));
const visibleBlogSources = computed(() =>
  blogSourcesExpanded.value ? blogSources.value : blogSources.value.slice(0, SOURCE_PREVIEW_LIMIT)
);
const visibleWebSources = computed(() =>
  webSourcesExpanded.value ? webSources.value : webSources.value.slice(0, SOURCE_PREVIEW_LIMIT)
);

function formatSessionTime(value) {
  if (!value) {
    return '';
  }
  return preferences.formatDateTime(value);
}

function formatMessageCount(count) {
  return copy.value.messageCount.replace('{count}', count || 0);
}

function sourceKey(source) {
  return `${source.sourceType || 'blog'}-${source.url || source.slug || source.citationIndex}`;
}

function isWebSource(source) {
  return String(source?.sourceType || '').toLowerCase() === 'web';
}

function sourceComponent(source) {
  return isWebSource(source) ? 'a' : 'router-link';
}

function sourceLinkProps(source) {
  if (isWebSource(source)) {
    return {
      href: source.url || '#',
      target: '_blank',
      rel: 'noopener noreferrer'
    };
  }

  return {
    to: source.slug ? `/posts/${source.slug}` : route.fullPath
  };
}

function sourceTypeLabel(source) {
  return isWebSource(source) ? copy.value.webSourceLabel : copy.value.blogSourceLabel;
}

function sourceMetaLabel(source) {
  if (isWebSource(source)) {
    return source.domain || source.url || copy.value.webSourceFallback;
  }
  return source.slug || copy.value.blogSourceLabel;
}

function shouldShowInlineSources(message) {
  if (
    message?.role !== 'assistant' ||
    message.pending ||
    message.skeleton ||
    !Array.isArray(message.sources) ||
    !message.sources.length
  ) {
    return false;
  }

  return timelineMessages.value.some(
    (item) => item.messageIndex > message.messageIndex && item.role === 'user'
  );
}

function shouldShowInlineFollowUps(message) {
  return (
    message?.role === 'assistant' &&
    !message.pending &&
    !message.skeleton &&
    latestFollowUps.value.length > 0 &&
    message.id === latestAssistantTimelineMessageId.value
  );
}

function shouldShowMessageActions(message) {
  return !message?.pending && !message?.skeleton;
}

async function copyMessageContent(message) {
  const content = String(message?.content || '').trim();
  if (!content) {
    return;
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(content);
    } else {
      const input = document.createElement('textarea');
      input.value = content;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
    }
    ElMessage.success(messageActionCopy.value.copied);
  } catch (error) {
    ElMessage.error(error?.message || messageActionCopy.value.copyFailed);
  }
}

function startEditMessage(message) {
  editingMessageId.value = message?.id || null;
  editingMessageContent.value = message?.content || '';
}

function cancelEditMessage() {
  editingMessageId.value = null;
  editingMessageContent.value = '';
}

async function submitEditedMessage() {
  const normalized = String(editingMessageContent.value || '').trim();
  if (!normalized) {
    ElMessage.warning(messageActionCopy.value.emptyEdit);
    return;
  }

  const messageId = editingMessageId.value;
  cancelEditMessage();
  await replayConversation(messageId, normalized);
}

async function retryAssistantMessage(message) {
  await replayConversation(message?.id);
}

async function replayConversation(messageId, questionOverride = '') {
  if (!authStore.isAuthenticated || !hasQwenConfig.value) {
    return;
  }
  if (!messageId) {
    return;
  }

  stopStreaming();
  closeSidebar();
  cancelEditMessage();
  loading.value = true;
  streaming.value = false;

  try {
    const res = await replayKnowledgeApi({
      sessionId: activeSessionId.value,
      messageId,
      question: questionOverride || undefined,
      topK: DEFAULT_TOP_K,
      searchMode: searchMode.value
    });
    applyResponse(res.data, { clearPending: true });
    await loadSessions();
    await scrollConversationToBottom(true);
  } catch (error) {
    ElMessage.error(error?.message || 'Request failed');
  } finally {
    loading.value = false;
    streaming.value = false;
  }
}

function setActiveSourceMessage(messageId) {
  if (!messageId) {
    return;
  }
  activeSourceMessageId.value = messageId;
}

function sourceToggleLabel(total, expanded) {
  if (expanded) {
    return copy.value.collapseSources;
  }

  return copy.value.viewMoreSources.replace('{count}', Math.max(total - SOURCE_PREVIEW_LIMIT, 0));
}

function toggleSourceExpansion(type) {
  if (type === 'web') {
    webSourcesExpanded.value = !webSourcesExpanded.value;
    return;
  }

  blogSourcesExpanded.value = !blogSourcesExpanded.value;
}

async function jumpToCitationSource(messageId, citationIndex) {
  if (!messageId || !citationIndex) {
    return;
  }

  if (messageId !== latestSourceMessageId.value) {
    const source = resolveMessageSource(messageId, citationIndex);
    openSourceInNewWindow(source);
    return;
  }

  await nextTick();
  const target = document.getElementById(citationAnchorId(messageId, citationIndex));
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
}

function resolveMessageSource(messageId, citationIndex) {
  const ownerMessage = assistantMessagesWithSources.value.find((message) => message.id === messageId);
  return ownerMessage?.sources?.find((source) => source.citationIndex === citationIndex) || null;
}

function openSourceInNewWindow(source) {
  if (!source || typeof window === 'undefined') {
    return;
  }

  if (isWebSource(source)) {
    const href = source.url || '';
    if (href) {
      window.open(href, '_blank', 'noopener,noreferrer');
    }
    return;
  }

  const resolved = router.resolve(source.slug ? `/posts/${source.slug}` : '/archives');
  window.open(resolved.href, '_blank', 'noopener,noreferrer');
}

function handleComposerKeydown(event) {
  if (event.key !== 'Enter' || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
    return;
  }

  event.preventDefault();
  submitQuestion();
}

function persistActiveSession() {
  localStorage.setItem(STORAGE_SESSION_KEY, activeSessionId.value);
}

function syncRouteSession(sessionId) {
  const nextSessionId = normalizeSessionId(sessionId);
  const currentSessionId = normalizeSessionId(route.query.sessionId);
  if (currentSessionId === nextSessionId) {
    return;
  }

  const query = { ...route.query };
  if (nextSessionId) {
    query.sessionId = nextSessionId;
  } else {
    delete query.sessionId;
  }

  router.replace({ query });
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

function handleConversationClick(event) {
  if (!(event.target instanceof Element)) {
    return;
  }

  const citationLink = event.target.closest('.citation-link');
  if (!citationLink) {
    const sourceOwner = event.target.closest('[data-source-owner]');
    if (sourceOwner?.dataset.sourceOwner === String(latestSourceMessageId.value || '')) {
      setActiveSourceMessage(sourceOwner.dataset.sourceOwner);
    }
    return;
  }

  event.preventDefault();
  const sourceOwner = citationLink.closest('[data-source-owner]');
  const messageId = sourceOwner?.dataset.sourceOwner;
  const citationIndex = Number(citationLink.dataset.citation);
  if (!messageId || !citationIndex) {
    return;
  }

  jumpToCitationSource(messageId, citationIndex);
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

function clearPendingTurn() {
  pendingTurn.value = null;
}

function createPendingTurn(normalizedQuestion) {
  const createdAt = new Date().toISOString();
  return {
    userId: `pending-user-${Date.now()}`,
    assistantId: `pending-assistant-${Date.now()}`,
    question: normalizedQuestion,
    answer: '',
    createdAt
  };
}

function applyResponse(response, options = {}) {
  const { clearPending = false } = options;
  result.value = response;
  history.value = response.history || [];
  persistHistoryCache(response?.sessionId || activeSessionId.value, history.value);
  if (response?.searchMode) {
    searchMode.value = normalizeSearchMode(response.searchMode);
    persistSearchMode();
  }
  if (clearPending) {
    clearPendingTurn();
  }

  if (response.sessionId && response.sessionId !== activeSessionId.value) {
    activeSessionId.value = response.sessionId;
    persistActiveSession();
    syncRouteSession(response.sessionId);
  }
}

function patchMessageFeedback(messageId, feedback) {
  const patch = (message) => {
    if (message.id !== messageId) {
      return message;
    }

    return {
      ...message,
      ...feedback
    };
  };

  history.value = history.value.map(patch);
  persistHistoryCache(activeSessionId.value, history.value);
  if (result.value?.history?.length) {
    result.value = {
      ...result.value,
      history: result.value.history.map(patch)
    };
  }
}

async function submitAnswerFeedback(message, helpful) {
  if (!message?.id || message.role !== 'assistant' || feedbackSubmittingId.value) {
    return;
  }

  let note = '';
  if (!helpful) {
    try {
      const response = await ElMessageBox.prompt(
        feedbackCopy.value.prompt,
        feedbackCopy.value.title,
        {
          inputType: 'textarea',
          inputPlaceholder: feedbackCopy.value.placeholder,
          confirmButtonText: feedbackCopy.value.confirm,
          cancelButtonText: feedbackCopy.value.cancel,
          closeOnClickModal: false,
          closeOnPressEscape: true,
          distinguishCancelAndClose: true
        }
      );
      note = String(response.value || '').trim();
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return;
      }
      ElMessage.error(error?.message || 'Request failed');
      return;
    }
  }

  feedbackSubmittingId.value = message.id;
  try {
    const res = await submitKnowledgeFeedbackApi({
      sessionId: activeSessionId.value,
      messageId: message.id,
      helpful,
      note
    });

    patchMessageFeedback(message.id, {
      feedbackHelpful: res.data.feedbackHelpful,
      feedbackNote: res.data.feedbackNote,
      feedbackAt: res.data.feedbackAt
    });
    ElMessage.success(helpful ? feedbackCopy.value.helpfulSaved : feedbackCopy.value.needsWorkSaved);
  } catch (error) {
    ElMessage.error(error?.message || 'Request failed');
  } finally {
    feedbackSubmittingId.value = null;
  }
}

async function loadSessions() {
  if (!authStore.isAuthenticated) {
    sessions.value = [];
    return;
  }
  sessionsLoading.value = true;
  try {
    const res = await fetchKnowledgeSessionsApi(true);
    sessions.value = res.data.sessions || [];
    persistSessionsCache();
  } catch (error) {
    ElMessage.warning(error?.message || copy.value.sessionsFailed);
  } finally {
    sessionsLoading.value = false;
  }
}

async function loadHistory(sessionToLoad = activeSessionId.value) {
  if (!authStore.isAuthenticated) {
    history.value = [];
    result.value = null;
    return;
  }
  const restoredFromCache = restoreHistoryCache(sessionToLoad);
  historyLoading.value = true;
  result.value = null;
  clearPendingTurn();
  cancelEditMessage();
  let loaded = false;

  try {
    const res = await fetchKnowledgeHistoryApi(sessionToLoad);
    history.value = res.data.messages || [];
    persistHistoryCache(sessionToLoad, history.value);
    loaded = true;
  } catch (error) {
    if (!restoredFromCache) {
      history.value = [];
      ElMessage.warning(error?.message || copy.value.historyFailed);
    }
  } finally {
    historyLoading.value = false;
    if (loaded || restoredFromCache) {
      await scrollConversationToBottom(true);
    }
  }
}

async function loadQwenConfig() {
  if (!authStore.isAuthenticated) {
    qwenConfig.value = {
      hasApiKey: false,
      selectedModel: '',
      webSearchEnabled: false,
      models: []
    };
    return;
  }

  qwenConfigLoading.value = true;
  try {
    const res = await fetchQwenConfigApi();
    qwenConfig.value = res.data || {
      hasApiKey: false,
      selectedModel: '',
      webSearchEnabled: false,
      models: []
    };
    if (!qwenConfig.value.webSearchEnabled && searchMode.value === SEARCH_MODE_LOCAL_AND_WEB) {
      searchMode.value = SEARCH_MODE_LOCAL_ONLY;
    }
  } catch (error) {
    qwenConfig.value = {
      hasApiKey: false,
      selectedModel: '',
      webSearchEnabled: false,
      models: []
    };
  } finally {
    qwenConfigLoading.value = false;
  }
}

async function saveQwenConfig(selectedModel = '') {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }

  qwenSaving.value = true;
  try {
    const hasNewApiKey = Boolean(qwenApiKeyInput.value.trim());
    const res = await saveQwenConfigApi({
      apiKey: qwenApiKeyInput.value.trim() || undefined,
      selectedModel: selectedModel || (hasNewApiKey ? undefined : qwenConfig.value.selectedModel || undefined)
    });
    qwenConfig.value = res.data;
    authStore.user = {
      ...authStore.user,
      hasQwenApiKey: res.data.hasApiKey,
      qwenChatModel: res.data.selectedModel,
      qwenWebSearchEnabled: res.data.webSearchEnabled
    };
    localStorage.setItem('blog-auth-user', JSON.stringify(authStore.user));
    qwenApiKeyInput.value = '';
    qwenApiKeyEditorVisible.value = false;
    if (!res.data.webSearchEnabled) {
      searchMode.value = SEARCH_MODE_LOCAL_ONLY;
    }
    ElMessage.success(qwenCopy.value.saved);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'Request failed');
  } finally {
    qwenSaving.value = false;
  }
}

function showQwenApiKeyEditor() {
  qwenApiKeyEditorVisible.value = true;
}

function hideQwenApiKeyEditor() {
  qwenApiKeyEditorVisible.value = false;
  qwenApiKeyInput.value = '';
}

function toggleContextRail() {
  contextRailCollapsed.value = !contextRailCollapsed.value;
}

async function toggleChatStageFullscreen() {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
      return;
    }
    await chatStageRef.value?.requestFullscreen?.();
  } catch (error) {
    ElMessage.error(error?.message || 'Fullscreen unavailable');
  }
}

function syncFullscreenState() {
  isChatStageFullscreen.value = Boolean(document.fullscreenElement && chatStageRef.value && document.fullscreenElement === chatStageRef.value);
}

function createEmptyResult(normalizedQuestion) {
  return {
    sessionId: activeSessionId.value,
    question: normalizedQuestion,
    answer: '',
    mode: 'llm',
    searchMode: searchMode.value,
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
  pendingTurn.value = createPendingTurn(normalizedQuestion);
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
        topK: DEFAULT_TOP_K,
        searchMode: searchMode.value
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
            const nextAnswer = `${current.answer || ''}${payload.delta || ''}`;
            if (pendingTurn.value) {
              pendingTurn.value = {
                ...pendingTurn.value,
                answer: nextAnswer
              };
            }
            result.value = {
              ...current,
              answer: nextAnswer
            };
            await scrollConversationToBottom();
            return;
          }

          if (type === 'done' && payload.response) {
            applyResponse(payload.response, { clearPending: true });
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
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (!hasQwenConfig.value) {
    ElMessage.warning(qwenCopy.value.configHint);
    return;
  }

  const normalizedQuestion = String(presetQuestion || question.value).trim();
  if (!normalizedQuestion) {
    return;
  }

  stopStreaming();
  closeSidebar();
  cancelEditMessage();
  question.value = '';

  try {
    await runStreamRequest(normalizedQuestion);
  } catch (error) {
    if (error?.name === 'AbortError') {
      return;
    }

    try {
      pendingTurn.value = createPendingTurn(normalizedQuestion);
      const res = await askKnowledgeApi({
        sessionId: activeSessionId.value,
        question: normalizedQuestion,
        topK: DEFAULT_TOP_K,
        searchMode: searchMode.value
      });
      applyResponse(res.data, { clearPending: true });
      await loadSessions();
      ElMessage.warning(error?.message || copy.value.fallbackWarning);
      await scrollConversationToBottom(true);
    } catch (fallbackError) {
      clearPendingTurn();
      question.value = normalizedQuestion;
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
  syncRouteSession(sessionToLoad);
  await loadHistory(sessionToLoad);
  closeSidebar();
}

function startNewChat() {
  stopStreaming();
  question.value = '';
  result.value = null;
  history.value = [];
  clearPendingTurn();
  cancelEditMessage();
  activeSessionId.value = createSessionId();
  persistActiveSession();
  syncRouteSession(activeSessionId.value);
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

async function purgeSession(session) {
  try {
    await ElMessageBox.confirm(copy.value.purgeConfirmBody, copy.value.purgeConfirmTitle, {
      confirmButtonText: copy.value.purge,
      cancelButtonText: 'Cancel',
      type: 'warning'
    });

    await purgeKnowledgeSessionApi(session.sessionId);
    await loadSessions();
    ElMessage.success(copy.value.purgeSuccess);

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

watch(
  () => timelineMessages.value.length,
  async () => {
    await scrollConversationToBottom();
  }
);

watch(
  latestSourceMessageId,
  (value) => {
    activeSourceMessageId.value = value || null;
  },
  { immediate: true }
);

watch(
  () => latestSources.value.map((source) => sourceKey(source)).join('|'),
  () => {
    blogSourcesExpanded.value = false;
    webSourcesExpanded.value = false;
  }
);

watch(searchMode, () => {
  persistSearchMode();
});

watch(
  () => qwenConfig.value.webSearchEnabled,
  (enabled) => {
    if (!enabled && searchMode.value === SEARCH_MODE_LOCAL_AND_WEB) {
      searchMode.value = SEARCH_MODE_LOCAL_ONLY;
    }
  }
);

watch(
  () => route.query.sessionId,
  async (value) => {
    const nextSessionId = normalizeSessionId(value);
    if (!nextSessionId || nextSessionId === activeSessionId.value) {
      return;
    }

    stopStreaming();
    activeSessionId.value = nextSessionId;
    persistActiveSession();
    await loadHistory(nextSessionId);
  }
);

onMounted(async () => {
  syncResponsiveLayout();
  window.addEventListener('resize', syncResponsiveLayout);
  document.addEventListener('fullscreenchange', syncFullscreenState);
  if (!authStore.isAuthenticated) {
    await loadQwenConfig();
    return;
  }
  restoreSessionsCache();
  const qwenConfigPromise = loadQwenConfig();
  await loadSessions();
  if (!sessions.value.some((item) => item.sessionId === activeSessionId.value && !item.deleted) && visibleSessions.value.length) {
    activeSessionId.value = visibleSessions.value[0].sessionId;
    persistActiveSession();
  }
  syncRouteSession(activeSessionId.value);
  await Promise.all([
    loadHistory(activeSessionId.value),
    qwenConfigPromise
  ]);
});

onBeforeUnmount(() => {
  stopStreaming();
  window.removeEventListener('resize', syncResponsiveLayout);
  document.removeEventListener('fullscreenchange', syncFullscreenState);
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
      </div>

      <div v-if="sidebarExpanded" class="sidebar-scroll">
        <div class="sidebar-head">
          <span class="sidebar-eyebrow muted">{{ copy.sidebarLabel }}</span>
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

        <section class="sidebar-section sidebar-section-muted">
          <div class="section-heading compact-heading">
            <h2>{{ copy.capabilityTitle }}</h2>
          </div>
          <div class="capability-list">
            <span class="capability-pill">{{ copy.capabilityHistory }}</span>
            <span class="capability-pill">{{ copy.capabilityRag }}</span>
          </div>
        </section>

        <section class="sidebar-section sidebar-section-muted">
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
                <button type="button" class="ghost-action danger danger-strong" @click="purgeSession(session)">
                  {{ copy.purge }}
                </button>
              </div>
            </article>
          </div>
          <p v-else class="muted sidebar-empty">{{ copy.deletedEmpty }}</p>
        </section>
      </div>
    </aside>

    <main ref="chatStageRef" class="chat-stage">
      <header class="chat-stage-head">
        <div class="stage-copy">
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
            class="mini-action"
            :aria-label="isChatStageFullscreen ? uiCopy.fullscreenExit : uiCopy.fullscreenEnter"
            @click="toggleChatStageFullscreen"
          >
            <span class="fullscreen-icon" :class="{ active: isChatStageFullscreen }" aria-hidden="true"></span>
          </button>
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
          </div>
        </div>
      </header>

      <div
        ref="conversationViewport"
        class="conversation-viewport"
        :class="{ 'is-loading': historyLoading }"
        @scroll="handleConversationScroll"
        @click="handleConversationClick"
      >
        <div v-if="historyLoading" class="conversation-timeline skeleton-timeline">
          <article v-for="index in 4" :key="`skeleton-${index}`" class="chat-row is-assistant">
            <div class="avatar-ring avatar-ring-assistant">
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
            :data-source-owner="message.role === 'assistant' && message.sources?.length ? String(message.id) : null"
          >
            <div class="avatar-ring" :class="message.role === 'assistant' ? 'avatar-ring-assistant' : 'avatar-ring-user'">
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

              <div v-if="message.role === 'assistant' && !message.pending && !message.skeleton" class="bubble-feedback">
                <button
                  type="button"
                  class="feedback-pill"
                  :class="{ active: message.feedbackHelpful === true }"
                  :disabled="feedbackSubmittingId === message.id"
                  @click="submitAnswerFeedback(message, true)"
                >
                  Helpful
                </button>
                <button
                  type="button"
                  class="feedback-pill"
                  :class="{ active: message.feedbackHelpful === false }"
                  :disabled="feedbackSubmittingId === message.id"
                  @click="submitAnswerFeedback(message, false)"
                >
                  Needs work
                </button>
                <span v-if="message.feedbackAt" class="feedback-meta muted">
                  {{ message.feedbackHelpful === true ? 'Marked helpful' : 'Needs work' }}
                  <template v-if="message.feedbackNote">: {{ message.feedbackNote }}</template>
                </span>
                <span class="feedback-disclosure muted">
                  {{ feedbackCopy.disclosure }}
                </span>
              </div>

              <div v-if="message.skeleton" class="chat-bubble pending-bubble">
                <span class="placeholder-line long"></span>
                <span class="placeholder-line medium"></span>
                <span class="placeholder-line short"></span>
              </div>

              <template v-else-if="message.role === 'assistant'">
                <div
                  class="chat-bubble assistant-bubble content-html markdown-body"
                  :class="{ pending: message.pending }"
                  v-html="message.renderedContent"
                ></div>

                <div v-if="shouldShowMessageActions(message)" class="message-actions">
                  <button type="button" class="ghost-action message-action-btn" @click="copyMessageContent(message)">
                    {{ messageActionCopy.copy }}
                  </button>
                  <button type="button" class="ghost-action message-action-btn" @click="retryAssistantMessage(message)">
                    {{ messageActionCopy.retry }}
                  </button>
                </div>

                <div v-if="message.variantCount > 1" class="answer-variants">
                  <span class="answer-variants-label muted">{{ answerVariantCopy.title }}</span>
                  <div class="answer-variants-list">
                    <button
                      v-for="index in message.variantCount"
                      :key="`${message.id}-variant-${index - 1}`"
                      type="button"
                      class="answer-variant-chip"
                      :class="{ active: message.selectedVariantIndex === index - 1 }"
                      @click="selectAnswerVariant(message.id, index - 1)"
                    >
                      {{ answerVariantCopy.label.replace('{index}', index) }}
                    </button>
                  </div>
                </div>

                <div v-if="shouldShowInlineSources(message)" class="inline-sources">
                  <span class="inline-sources-label muted">{{ copy.sourcesTitle }}</span>
                  <div class="inline-source-list">
                    <button
                      v-for="source in message.sources"
                      :key="sourceKey(source)"
                      type="button"
                      class="inline-source-chip"
                      @click="jumpToCitationSource(message.id, source.citationIndex)"
                    >
                      <span class="inline-source-index">[{{ source.citationIndex }}]</span>
                      <span class="inline-source-title">{{ source.title || sourceMetaLabel(source) }}</span>
                      <span class="inline-source-type" :class="{ web: isWebSource(source) }">
                        {{ sourceTypeLabel(source) }}
                      </span>
                    </button>
                  </div>
                </div>

                <div v-if="shouldShowInlineFollowUps(message)" class="inline-followups">
                  <span class="inline-followups-label muted">{{ copy.followUpsTitle }}</span>
                  <div class="prompt-list inline-followups-list">
                    <button
                      v-for="item in latestFollowUps"
                      :key="`${message.id}-${item}`"
                      type="button"
                      class="prompt-chip"
                      :disabled="loading"
                      @click="submitQuestion(item)"
                    >
                      {{ item }}
                    </button>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="chat-bubble user-bubble" :class="{ pending: message.pending }">
                  {{ message.content }}
                </div>

                <div v-if="shouldShowMessageActions(message)" class="message-actions">
                  <button type="button" class="ghost-action message-action-btn" @click="copyMessageContent(message)">
                    {{ messageActionCopy.copy }}
                  </button>
                  <button type="button" class="ghost-action message-action-btn" @click="startEditMessage(message)">
                    {{ messageActionCopy.edit }}
                  </button>
                </div>

                <div v-if="editingMessageId === message.id" class="message-edit-shell">
                  <el-input
                    v-model="editingMessageContent"
                    type="textarea"
                    resize="none"
                    :rows="3"
                  />
                  <div class="message-edit-actions">
                    <el-button type="primary" :disabled="loading" @click="submitEditedMessage">
                      {{ messageActionCopy.save }}
                    </el-button>
                    <el-button :disabled="loading" @click="cancelEditMessage">
                      {{ messageActionCopy.cancel }}
                    </el-button>
                  </div>
                </div>
              </template>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <h3>{{ copy.emptyStateTitle }}</h3>
          <p class="muted">{{ copy.emptyStateBody }}</p>
        </div>
      </div>

      <footer class="composer-shell">
        <div class="composer-card" :class="{ 'is-busy': loading }">
          <div v-if="!authStore.isAuthenticated" class="qwen-config-panel">
            <strong>{{ qwenCopy.loginRequired }}</strong>
            <p class="muted">{{ qwenCopy.loginHint }}</p>
            <el-button type="primary" @click="router.push({ name: 'login', query: { redirect: route.fullPath } })">
              {{ qwenCopy.goLogin }}
            </el-button>
          </div>

          <div v-else-if="!hasQwenConfig" class="qwen-config-panel">
            <strong>{{ qwenCopy.configTitle }}</strong>
            <p class="muted">{{ qwenCopy.configHint }}</p>
            <el-input
              v-model="qwenApiKeyInput"
              type="textarea"
              resize="none"
              :rows="3"
              :placeholder="qwenCopy.apiKeyPlaceholder"
            />
            <div class="qwen-config-actions">
              <el-button type="primary" :loading="qwenSaving || qwenConfigLoading" @click="saveQwenConfig()">
                {{ qwenCopy.saveConfig }}
              </el-button>
            </div>
            <p class="muted">{{ qwenCopy.noModels }}</p>
          </div>

          <div v-else class="qwen-config-panel compact">
            <div class="compact-config-note" v-if="!canUseHybridSearch">
              <p class="muted">{{ qwenCopy.localOnlyLocked }}</p>
            </div>
          </div>

          <div class="composer-input-shell">
            <el-input
              v-model="question"
              type="textarea"
              resize="none"
              :rows="3"
              :placeholder="copy.placeholder"
              :disabled="loading || !authStore.isAuthenticated || !hasQwenConfig"
              @keydown="handleComposerKeydown"
            />
            <div class="composer-inline-hints">
              <div class="search-mode-group">
                <span class="toolbar-label">{{ copy.searchModeTitle }}</span>
                <div class="search-mode-picker" :aria-label="copy.searchModeTitle" role="group">
                  <button
                    v-for="option in searchModeOptions"
                    :key="option.value"
                    type="button"
                    class="search-mode-option"
                    :data-mode="option.value"
                    :class="{ active: searchMode === option.value }"
                    :aria-pressed="searchMode === option.value"
                    :disabled="loading || (option.value === SEARCH_MODE_LOCAL_AND_WEB && !canUseHybridSearch)"
                    @click="searchMode = option.value"
                  >
                    <span class="search-mode-option-dot" aria-hidden="true"></span>
                    {{ option.label }}
                  </button>
                </div>
              </div>
              <div
                class="qwen-tools"
                @mouseenter="openQwenTools"
                @mouseleave="scheduleHideQwenTools"
                @focusin="openQwenTools"
                @focusout="scheduleHideQwenTools"
              >
                <button
                  type="button"
                  class="mini-action qwen-tools-toggle"
                  :aria-label="uiCopy.configToolsOpen"
                  :aria-expanded="qwenToolsVisible"
                  @click="toggleQwenTools"
                >
                  <span class="chevron-icon" :class="{ active: qwenToolsVisible }" aria-hidden="true"></span>
                </button>
                <transition name="fade-slide">
                  <div v-if="qwenToolsVisible" class="qwen-tools-popover section-card">
                    <div class="toolbar-group qwen-tools-group">
                      <span class="toolbar-label">{{ qwenCopy.model }}</span>
                      <el-select
                        :model-value="qwenConfig.selectedModel"
                        class="qwen-model-select"
                        @change="saveQwenConfig"
                      >
                        <el-option
                          v-for="model in qwenConfig.models"
                          :key="model.model"
                          :label="model.model"
                          :value="model.model"
                        />
                      </el-select>
                    </div>
                    <el-button
                      v-if="!qwenApiKeyEditorVisible"
                      type="primary"
                      plain
                      class="search-mode-key-btn"
                      :loading="qwenSaving"
                      @click="showQwenApiKeyEditor"
                    >
                      {{ qwenCopy.editApiKey }}
                    </el-button>
                    <template v-if="qwenApiKeyEditorVisible">
                      <el-input
                        v-model="qwenApiKeyInput"
                        type="password"
                        :placeholder="qwenCopy.apiKeyPlaceholder"
                        show-password
                      />
                      <div class="qwen-config-actions">
                        <el-button type="primary" plain :loading="qwenSaving" @click="saveQwenConfig()">
                          {{ qwenCopy.saveConfig }}
                        </el-button>
                        <el-button :disabled="qwenSaving" @click="hideQwenApiKeyEditor">
                          {{ qwenCopy.cancelEditApiKey }}
                        </el-button>
                      </div>
                    </template>
                  </div>
                </transition>
              </div>
              <span class="composer-hint-chip muted">{{ uiCopy.composerSubmitHint }}</span>
              <span class="composer-hint-chip muted">{{ uiCopy.composerNewlineHint }}</span>
              <el-button v-if="streaming" plain class="composer-inline-btn" @click="stopStreaming()">{{ copy.stop }}</el-button>
              <el-button
                class="send-btn composer-inline-btn"
                type="primary"
                :disabled="loading || !authStore.isAuthenticated || !hasQwenConfig"
                @click="submitQuestion()"
              >
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
      <section class="section-card context-panel sources-panel">
        <div class="section-heading compact-heading rail-heading">
          <h2 v-if="!contextRailCollapsed">{{ copy.sourcesTitle }}</h2>
          <button
            type="button"
            class="mini-action rail-collapse-toggle"
            :aria-label="contextRailCollapsed ? uiCopy.sourcesExpand : uiCopy.sourcesCollapse"
            @click="toggleContextRail"
          >
            <span class="chevron-icon rail-chevron" :class="{ active: !contextRailCollapsed }" aria-hidden="true"></span>
          </button>
        </div>
        <template v-if="!contextRailCollapsed">
          <p class="muted rail-hint">{{ copy.sourceHint }}</p>
          <div v-if="latestSources.length" class="source-groups">
          <section v-if="blogSources.length" class="source-group is-blog">
            <div class="source-group-head">
              <div class="source-group-label">
                <span class="source-group-marker" aria-hidden="true"></span>
                <span class="source-group-title">{{ copy.blogSourceLabel }}</span>
                <span class="source-group-count muted">{{ blogSources.length }}</span>
              </div>
              <button
                v-if="blogSources.length > SOURCE_PREVIEW_LIMIT"
                type="button"
                class="ghost-action source-more-btn"
                @click="toggleSourceExpansion('blog')"
              >
                {{ sourceToggleLabel(blogSources.length, blogSourcesExpanded) }}
              </button>
            </div>
            <transition-group name="source-expand" tag="div" class="source-list">
              <component
                v-for="source in visibleBlogSources"
                :is="sourceComponent(source)"
                :key="sourceKey(source)"
                v-bind="sourceLinkProps(source)"
                class="source-card"
                :id="citationAnchorId(activeSourceMessage?.id, source.citationIndex)"
              >
                <div class="source-card-head">
                  <strong class="source-title">[{{ source.citationIndex }}] {{ source.title }}</strong>
                  <span class="source-type-badge" :class="{ web: isWebSource(source) }">
                    {{ sourceTypeLabel(source) }}
                  </span>
                </div>
                <p class="source-excerpt">{{ source.excerpt }}</p>
                <div class="source-meta-row muted">
                  <span>{{ sourceMetaLabel(source) }}</span>
                  <span>{{ copy.scoreLabel }} {{ source.score }}</span>
                </div>
              </component>
            </transition-group>
          </section>

          <section v-if="webSources.length" class="source-group is-web">
            <div class="source-group-head">
              <div class="source-group-label">
                <span class="source-group-marker" aria-hidden="true"></span>
                <span class="source-group-title">{{ copy.webSourceLabel }}</span>
                <span class="source-group-count muted">{{ webSources.length }}</span>
              </div>
              <button
                v-if="webSources.length > SOURCE_PREVIEW_LIMIT"
                type="button"
                class="ghost-action source-more-btn"
                @click="toggleSourceExpansion('web')"
              >
                {{ sourceToggleLabel(webSources.length, webSourcesExpanded) }}
              </button>
            </div>
            <transition-group name="source-expand" tag="div" class="source-list">
              <component
                v-for="source in visibleWebSources"
                :is="sourceComponent(source)"
                :key="sourceKey(source)"
                v-bind="sourceLinkProps(source)"
                class="source-card"
                :id="citationAnchorId(activeSourceMessage?.id, source.citationIndex)"
              >
                <div class="source-card-head">
                  <strong class="source-title">[{{ source.citationIndex }}] {{ source.title }}</strong>
                  <span class="source-type-badge" :class="{ web: isWebSource(source) }">
                    {{ sourceTypeLabel(source) }}
                  </span>
                </div>
                <p class="source-excerpt">{{ source.excerpt }}</p>
                <div class="source-meta-row muted">
                  <span>{{ sourceMetaLabel(source) }}</span>
                  <span>{{ copy.scoreLabel }} {{ source.score }}</span>
                </div>
              </component>
            </transition-group>
          </section>
          </div>
          <p v-else class="muted rail-empty">{{ copy.noSources }}</p>
        </template>
      </section>
    </aside>
  </div>
</template>

<style scoped>
.knowledge-shell {
  position: relative;
  width: 100%;
  min-height: 0;
  height: 100%;
  max-height: 100%;
  margin: 0;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: stretch;
}

.knowledge-shell.sidebar-collapsed {
  grid-template-columns: 72px minmax(0, 1fr) 300px;
}

.knowledge-shell.context-rail-collapsed {
  grid-template-columns: 300px minmax(0, 1fr) 72px;
}

.knowledge-shell.sidebar-collapsed.context-rail-collapsed {
  grid-template-columns: 72px minmax(0, 1fr) 72px;
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
  height: 100%;
  max-height: 100%;
  padding: 14px;
  overflow: hidden;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(224, 208, 180, 0.1), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.015)),
    rgba(9, 9, 9, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 24px 56px rgba(0, 0, 0, 0.22);
}

.sidebar-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar-toggle,
.mini-action,
.rail-toggle,
.ghost-action {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-main);
  transition: 0.2s ease;
}

.sidebar-toggle,
.mini-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  cursor: pointer;
}

.sidebar-toggle:hover,
.mini-action:hover,
.rail-toggle:hover,
.ghost-action:hover {
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.06);
}

.sidebar-toggle {
  flex-direction: column;
  gap: 4px;
}

.sidebar-toggle span {
  width: 15px;
  height: 1px;
  background: currentColor;
}

.new-chat-btn {
  width: 100%;
  min-height: 42px;
}

.sidebar-scroll {
  margin-top: 14px;
  padding-right: 4px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sidebar-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 8px 4px;
}

.sidebar-eyebrow {
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.sidebar-title {
  margin: 0;
  font-size: clamp(1.55rem, 2vw, 2.1rem);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.sidebar-copy,
.sidebar-empty {
  margin: 0;
  line-height: 1.7;
  font-size: 0.92rem;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-section-muted {
  padding-top: 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
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
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.capability-list,
.prompt-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stacked {
  flex-direction: column;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  padding: 12px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.028);
  cursor: pointer;
  transition: 0.2s ease;
}

.session-item:hover {
  border-color: rgba(215, 199, 173, 0.22);
  background: rgba(255, 255, 255, 0.05);
}

.session-item.active {
  border-color: rgba(219, 203, 178, 0.2);
  background:
    linear-gradient(180deg, rgba(216, 196, 164, 0.1), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.04);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.session-item.deleted {
  opacity: 0.78;
}

.session-copy strong {
  display: block;
  margin-bottom: 6px;
  line-height: 1.4;
  font-size: 0.94rem;
}

.session-copy p {
  margin: 0;
  line-height: 1.55;
  font-size: 0.84rem;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.session-meta {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 0.72rem;
}

.session-actions {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ghost-action {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 0.72rem;
}

.ghost-action.danger {
  border-color: rgba(255, 118, 118, 0.16);
}

.ghost-action.danger-strong {
  color: #ffd6d6;
  border-color: rgba(255, 110, 110, 0.28);
  background: linear-gradient(180deg, rgba(120, 28, 28, 0.28), rgba(72, 16, 16, 0.18));
  box-shadow: inset 0 1px 0 rgba(255, 214, 214, 0.08);
}

.ghost-action.danger-strong:hover {
  border-color: rgba(255, 126, 126, 0.42);
  background: linear-gradient(180deg, rgba(150, 34, 34, 0.34), rgba(88, 18, 18, 0.24));
  color: #fff1f1;
}

.ghost-action.danger:hover {
  border-color: rgba(255, 104, 104, 0.28);
  color: #ff9f9f;
}

.capability-pill,
.prompt-chip {
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-secondary);
  transition: 0.2s ease;
}

.capability-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  font-size: 0.76rem;
}

.prompt-chip {
  min-height: 38px;
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
  line-height: 1.45;
}

.prompt-chip:hover:not(:disabled) {
  color: var(--text-main);
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.06);
}

.prompt-chip:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.prompt-chip-block {
  width: 100%;
}

.chat-stage {
  min-height: 0;
  height: 100%;
  max-height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top center, rgba(222, 205, 173, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.025), rgba(255, 255, 255, 0.015)),
    rgba(12, 12, 12, 0.92);
  box-shadow: 0 26px 72px rgba(0, 0, 0, 0.26);
}

.chat-stage:fullscreen {
  width: 100vw;
  height: 100vh;
  max-height: none;
  border-radius: 0;
}

.chat-stage-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.028), rgba(255, 255, 255, 0.012));
}

.stage-copy {
  min-width: 0;
}

.chat-stage-label,
.stage-subtitle {
  margin: 0;
}

.chat-stage-label {
  margin-bottom: 6px;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.chat-stage-title {
  margin: 0;
  font-size: 1.2rem;
  letter-spacing: -0.02em;
}

.stage-subtitle {
  margin-top: 6px;
  line-height: 1.6;
  max-width: 720px;
  font-size: 0.9rem;
}

.stage-actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.fullscreen-icon,
.chevron-icon {
  display: inline-block;
  width: 12px;
  height: 12px;
  position: relative;
}

.fullscreen-icon::before,
.fullscreen-icon::after {
  content: '';
  position: absolute;
  border: 1.5px solid currentColor;
  border-radius: 2px;
}

.fullscreen-icon::before {
  inset: 0;
}

.fullscreen-icon::after {
  inset: 3px;
  opacity: 0.55;
}

.fullscreen-icon.active::after {
  inset: 1px;
  opacity: 1;
}

.chevron-icon::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 7px;
  height: 7px;
  border-right: 1.5px solid currentColor;
  border-bottom: 1.5px solid currentColor;
  transform: rotate(45deg);
  transition: transform 0.2s ease;
}

.chevron-icon.active::before {
  transform: rotate(225deg);
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
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stage-badge,
.composer-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-secondary);
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.conversation-viewport {
  overflow: auto;
  padding: 18px 20px 12px;
  scroll-padding-bottom: 200px;
}

.conversation-viewport.is-loading {
  overflow: hidden;
}

.conversation-timeline {
  width: min(860px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.chat-row {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.chat-row.is-user {
  grid-template-columns: minmax(0, 1fr) 40px;
}

.chat-row.is-user .avatar-ring {
  order: 2;
}

.chat-row.is-user .bubble-stack {
  order: 1;
  align-items: flex-end;
}

.avatar-ring {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--text-secondary);
  font-size: 0.68rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.avatar-ring-assistant {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.04));
}

.avatar-ring-user {
  background: rgba(255, 255, 255, 0.04);
}

.bubble-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.answer-variants {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.answer-variants-label {
  font-size: 0.74rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.answer-variants-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.answer-variant-chip {
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-secondary);
  cursor: pointer;
  transition: 0.18s ease;
}

.answer-variant-chip:hover,
.answer-variant-chip.active {
  color: var(--text-main);
  border-color: var(--line-strong);
  background: rgba(255, 248, 233, 0.12);
}

.message-action-btn {
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 248, 233, 0.04);
  color: var(--text-secondary);
  cursor: pointer;
  transition: 0.18s ease;
}

.message-action-btn:hover {
  color: var(--text-main);
  border-color: var(--line-strong);
  background: rgba(255, 248, 233, 0.08);
}

.message-edit-shell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-edit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.inline-followups {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 2px;
}

.inline-sources {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 2px;
}

.inline-sources-label,
.inline-followups-label {
  font-size: 0.74rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.inline-source-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.inline-source-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: min(100%, 360px);
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-secondary);
  cursor: pointer;
  transition: 0.18s ease;
  min-width: 0;
}

.inline-source-chip:hover {
  border-color: rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-main);
}

.inline-source-index {
  flex: none;
  font-size: 0.72rem;
  color: var(--text-main);
}

.inline-source-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.78rem;
}

.inline-source-type {
  flex: none;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 0.68rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.inline-source-type.web {
  background: rgba(205, 186, 146, 0.14);
  color: #dbc290;
}

.inline-followups-list {
  gap: 10px;
}

.bubble-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bubble-meta strong {
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.bubble-citations {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.bubble-feedback {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.citation-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-secondary);
  font-size: 0.72rem;
}

.feedback-pill {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-secondary);
  cursor: pointer;
  transition: 0.2s ease;
  font-size: 0.74rem;
}

.feedback-pill:hover:not(:disabled) {
  border-color: var(--line-strong);
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.06);
}

.feedback-pill.active {
  border-color: rgba(255, 255, 255, 0.16);
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.08);
}

.feedback-pill:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.feedback-meta {
  font-size: 0.78rem;
  line-height: 1.5;
}

.feedback-disclosure {
  width: 100%;
  font-size: 0.76rem;
  line-height: 1.55;
}

.citation-link {
  display: inline-flex;
  align-items: center;
  padding: 0 0.25em;
  border-radius: 999px;
  color: var(--brand-accent, #d8d8d8);
  text-decoration: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
  transition: 0.18s ease;
}

.citation-link:hover {
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.06);
}

.chat-bubble {
  width: 100%;
  padding: 18px 20px;
  border-radius: 22px;
  line-height: 1.85;
}

.assistant-bubble {
  border: 1px solid transparent;
  background: transparent;
  padding-left: 0;
  padding-right: 0;
}

.chat-bubble.pending {
  opacity: 0.88;
}

.user-bubble {
  width: fit-content;
  max-width: min(78%, 720px);
  margin-left: auto;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.pending-bubble {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.03);
}

.placeholder-line {
  display: block;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.05));
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
  margin: min(12vh, 100px) auto 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 14px;
}

.empty-eyebrow {
  font-size: 0.76rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.empty-state h3 {
  margin: 0;
  font-size: clamp(2rem, 5vw, 3.3rem);
  line-height: 1;
  letter-spacing: -0.05em;
}

.empty-state p {
  margin: 0;
  max-width: 640px;
  line-height: 1.8;
}

.empty-prompt-grid {
  width: min(760px, 100%);
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.composer-shell {
  position: sticky;
  bottom: 0;
  z-index: 3;
  padding: 14px 18px 18px;
  background: linear-gradient(180deg, rgba(11, 11, 11, 0), rgba(11, 11, 11, 0.72) 28%, rgba(11, 11, 11, 0.92));
  backdrop-filter: blur(18px);
}

.composer-card {
  position: relative;
  width: min(860px, 100%);
  margin: 0 auto;
  padding: 10px 10px 12px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(17, 17, 17, 0.92);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.22);
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;
}

.toolbar-label {
  color: var(--text-secondary);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
}

.search-mode-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;
}

.qwen-tools {
  position: relative;
}

.qwen-tools-toggle {
  width: 36px;
  height: 36px;
  border-radius: 12px;
}

.qwen-tools-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 6;
  width: min(340px, calc(100vw - 56px));
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(20, 20, 20, 0.96), rgba(12, 12, 12, 0.94)),
    rgba(12, 12, 12, 0.96);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.32);
}

.qwen-tools-group {
  display: grid;
  gap: 8px;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.search-mode-label {
  color: var(--text-secondary);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
}

.search-mode-picker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.search-mode-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s ease;
  white-space: nowrap;
}

.search-mode-option-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.4;
}

.search-mode-option:hover:not(:disabled) {
  color: var(--text-main);
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
}

.search-mode-option.active {
  color: var(--text-main);
  border-color: rgba(255, 255, 255, 0.18);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.18);
}

.search-mode-option.active .search-mode-option-dot {
  opacity: 1;
}

.search-mode-option[data-mode='LOCAL_ONLY'].active {
  background: linear-gradient(135deg, rgba(255, 195, 87, 0.28), rgba(255, 141, 87, 0.2));
  border-color: rgba(255, 197, 92, 0.34);
}

.search-mode-option[data-mode='LOCAL_AND_WEB'].active {
  background: linear-gradient(135deg, rgba(72, 163, 255, 0.28), rgba(88, 211, 187, 0.22));
  border-color: rgba(96, 183, 255, 0.34);
}

.search-mode-option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.composer-card::after {
  content: '';
  position: absolute;
  inset: auto 18px -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.14), transparent);
  opacity: 0.28;
}

.composer-card.is-busy {
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.34);
}

.composer-input-shell {
  position: relative;
}

.composer-input-shell :deep(.el-textarea__inner) {
  min-height: 108px !important;
  padding: 14px 16px 54px;
  border-radius: 18px;
}

.composer-inline-hints {
  position: absolute;
  right: 10px;
  bottom: 10px;
  left: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.composer-hint-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  font-size: 0.74rem;
  white-space: nowrap;
}

.composer-inline-btn,
.search-mode-group,
.search-mode-picker,
.search-mode-option {
  pointer-events: auto;
}

.composer-inline-hints > .search-mode-group {
  margin-right: 0;
}

.compact-config-note {
  margin-bottom: 8px;
}

.compact-config-note p {
  margin: 0;
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

.qwen-config-panel {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 248, 233, 0.04);
}

.qwen-config-panel.compact {
  gap: 10px;
}

.qwen-model-select {
  width: 220px;
}

.qwen-config-actions {
  display: flex;
  justify-content: flex-end;
}

.qwen-config-actions.start {
  justify-content: flex-start;
}

.search-mode-key-btn {
  align-self: flex-end;
}

.context-rail {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

.rail-heading {
  align-items: center;
}

.rail-collapse-toggle {
  margin-left: auto;
}

.rail-chevron::before {
  transform: rotate(135deg);
}

.rail-chevron.active::before {
  transform: rotate(-45deg);
}

.knowledge-shell.context-rail-collapsed .context-panel {
  padding: 12px 10px;
}

.knowledge-shell.context-rail-collapsed .rail-heading {
  justify-content: center;
}

.knowledge-shell.context-rail-collapsed .rail-collapse-toggle {
  margin-left: 0;
}

.sources-panel {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
}

.context-panel {
  padding: 16px;
  border-radius: 24px;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(218, 201, 170, 0.08), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.018)),
    rgba(10, 10, 10, 0.9);
}

.rail-hint,
.rail-empty {
  margin: 0 0 12px;
  line-height: 1.6;
  font-size: 0.88rem;
}

.source-groups,
.source-group,
.source-list,
.thread-map {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.source-group {
  padding: 12px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.025);
}

.source-groups {
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.thread-map {
  max-height: 260px;
  overflow: auto;
  padding-right: 4px;
}

.source-group.is-blog {
  background:
    linear-gradient(180deg, rgba(255, 181, 72, 0.08), rgba(255, 255, 255, 0.02) 42%),
    rgba(255, 255, 255, 0.025);
  border-color: rgba(255, 184, 88, 0.16);
}

.source-group.is-web {
  background:
    linear-gradient(180deg, rgba(88, 171, 255, 0.1), rgba(255, 255, 255, 0.02) 42%),
    rgba(255, 255, 255, 0.025);
  border-color: rgba(92, 177, 255, 0.16);
}

.source-group-head,
.source-group-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.source-group-label {
  justify-content: flex-start;
}

.source-group-marker {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  flex-shrink: 0;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.04);
}

.source-group.is-blog .source-group-marker {
  background: linear-gradient(135deg, #ffcf72, #ff9d5c);
}

.source-group.is-web .source-group-marker {
  background: linear-gradient(135deg, #69b8ff, #5de1bf);
}

.source-group-title {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-main);
}

.source-group-count {
  font-size: 0.78rem;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.source-group.is-blog .source-group-count {
  color: #ffd39b;
}

.source-group.is-web .source-group-count {
  color: #b8deff;
}

.source-more-btn {
  align-self: flex-start;
  min-height: 30px;
  font-size: 0.72rem;
}

.source-card,
.thread-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 13px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.028);
  transition: 0.2s ease;
  scroll-margin-top: 18px;
}

.source-card:hover,
.thread-item:hover {
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.05);
}

.source-card:target {
  border-color: rgba(255, 255, 255, 0.24);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.source-title {
  display: block;
  font-size: 0.88rem;
  line-height: 1.45;
}

.source-excerpt {
  margin: 0;
  font-size: 0.82rem;
  line-height: 1.55;
  color: var(--text-secondary);
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.source-expand-enter-active,
.source-expand-leave-active,
.source-expand-move {
  transition: transform 0.24s ease, opacity 0.24s ease;
}

.source-expand-enter-from,
.source-expand-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.source-card-head,
.source-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.source-card-head {
  align-items: flex-start;
}

.source-type-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  font-size: 0.72rem;
}

.source-type-badge.web {
  background: rgba(108, 160, 255, 0.12);
  border-color: rgba(108, 160, 255, 0.22);
  color: #cfe0ff;
}

.source-meta-row {
  flex-wrap: wrap;
  font-size: 0.8rem;
  line-height: 1.5;
}

.thread-item {
  cursor: pointer;
  text-align: left;
}

.thread-index {
  color: var(--text-secondary);
  font-size: 0.72rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.thread-preview {
  line-height: 1.6;
  color: var(--text-main);
}

html[data-theme='light'] .knowledge-sidebar {
  background:
    radial-gradient(circle at top left, rgba(196, 171, 130, 0.16), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(244, 241, 236, 0.96)),
    rgba(248, 248, 245, 0.96);
}

html[data-theme='light'] .chat-stage {
  background:
    radial-gradient(circle at top center, rgba(196, 171, 130, 0.14), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(244, 241, 236, 0.96)),
    rgba(248, 248, 245, 0.95);
}

html[data-theme='light'] .composer-shell {
  background: linear-gradient(180deg, rgba(239, 239, 236, 0), rgba(239, 239, 236, 0.8) 28%, rgba(239, 239, 236, 0.96));
}

html[data-theme='light'] .composer-card,
html[data-theme='light'] .session-item,
html[data-theme='light'] .source-group,
html[data-theme='light'] .source-card,
html[data-theme='light'] .thread-item,
html[data-theme='light'] .prompt-chip,
html[data-theme='light'] .capability-pill,
html[data-theme='light'] .user-bubble,
html[data-theme='light'] .pending-bubble,
html[data-theme='light'] .stage-badge,
html[data-theme='light'] .composer-badge,
html[data-theme='light'] .citation-pill,
html[data-theme='light'] .search-mode-picker {
  background: rgba(0, 0, 0, 0.035);
}

html[data-theme='light'] .search-mode-option:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.05);
}

html[data-theme='light'] .search-mode-option[data-mode='LOCAL_ONLY'].active {
  background: linear-gradient(135deg, rgba(255, 190, 72, 0.26), rgba(255, 128, 72, 0.18));
}

html[data-theme='light'] .search-mode-option[data-mode='LOCAL_AND_WEB'].active {
  background: linear-gradient(135deg, rgba(72, 163, 255, 0.2), rgba(63, 200, 170, 0.16));
}

html[data-theme='light'] .source-group.is-blog {
  background:
    linear-gradient(180deg, rgba(255, 190, 88, 0.18), rgba(0, 0, 0, 0.02) 42%),
    rgba(0, 0, 0, 0.03);
}

html[data-theme='light'] .source-group.is-web {
  background:
    linear-gradient(180deg, rgba(95, 177, 255, 0.18), rgba(0, 0, 0, 0.02) 42%),
    rgba(0, 0, 0, 0.03);
}

html[data-theme='light'] .context-panel {
  background:
    radial-gradient(circle at top left, rgba(196, 171, 130, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 241, 236, 0.96)),
    rgba(248, 248, 245, 0.96);
}

html[data-theme='light'] .ghost-action.danger-strong {
  color: #842828;
  border-color: rgba(176, 52, 52, 0.24);
  background: linear-gradient(180deg, rgba(255, 218, 218, 0.96), rgba(248, 231, 231, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55);
}

html[data-theme='light'] .ghost-action.danger-strong:hover {
  color: #6f1c1c;
  border-color: rgba(176, 52, 52, 0.34);
  background: linear-gradient(180deg, rgba(255, 208, 208, 0.98), rgba(245, 222, 222, 0.98));
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
    grid-template-columns: 320px minmax(0, 1fr) 320px;
    min-height: 0;
    height: 100%;
    max-height: 100%;
  }

  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 92px minmax(0, 1fr) 320px;
  }

  .conversation-timeline,
  .composer-card {
    width: min(920px, 100%);
  }

}

@media (max-width: 1500px) {
  .knowledge-shell {
    grid-template-columns: 280px minmax(0, 1fr) 280px;
  }

  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 80px minmax(0, 1fr) 280px;
  }

  .knowledge-shell.context-rail-collapsed {
    grid-template-columns: 280px minmax(0, 1fr) 72px;
  }

  .knowledge-shell.sidebar-collapsed.context-rail-collapsed {
    grid-template-columns: 80px minmax(0, 1fr) 72px;
  }
}

@media (max-width: 1320px) {
  .knowledge-shell {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 80px minmax(0, 1fr);
  }

  .context-rail {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .knowledge-shell.context-rail-collapsed .context-rail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1120px) {
  .knowledge-shell,
  .knowledge-shell.sidebar-collapsed {
    grid-template-columns: 1fr;
    min-height: 0;
    height: auto;
    max-height: none;
  }

  .knowledge-sidebar {
    position: fixed;
    top: 90px;
    left: 12px;
    bottom: 12px;
    width: min(360px, calc(100vw - 24px));
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
    overflow: visible;
    height: auto;
    max-height: none;
  }

}

@media (max-width: 720px) {
  .knowledge-shell {
    gap: 10px;
    min-height: 0;
    height: auto;
    max-height: none;
  }

  .knowledge-sidebar {
    top: 82px;
    left: 8px;
    bottom: 8px;
    width: min(340px, calc(100vw - 16px));
  }

  .knowledge-sidebar,
  .context-panel {
    padding: 14px;
  }

  .chat-stage-head,
  .conversation-viewport {
    padding-left: 14px;
    padding-right: 14px;
  }

  .chat-stage-head,
  .stage-actions,
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
    gap: 22px;
  }

  .composer-shell {
    padding: 12px 10px 12px;
  }

  .composer-inline-hints {
    position: static;
    margin-top: 10px;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .composer-input-shell :deep(.el-textarea__inner) {
    min-height: 96px !important;
    padding-bottom: 14px;
  }

  .user-bubble {
    max-width: 100%;
  }

}

@media (max-width: 560px) {
  .sidebar-title {
    font-size: 1.45rem;
  }

  .chat-stage-title {
    font-size: 1.05rem;
  }

  .conversation-viewport {
    padding: 14px 12px 10px;
  }

  .chat-bubble {
    padding: 14px 16px;
    border-radius: 18px;
  }

  .assistant-bubble {
    padding-left: 0;
    padding-right: 0;
  }

  .composer-card {
    padding: 10px;
    border-radius: 20px;
  }

  .session-item,
  .context-panel {
    padding: 12px;
  }

  .source-group-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 420px) {
  .knowledge-shell {
    min-height: 0;
    height: auto;
    max-height: none;
  }

  .chat-stage-head {
    padding: 14px 12px 12px;
  }

  .conversation-viewport {
    padding: 12px 10px 8px;
  }

  .chat-row,
  .chat-row.is-user {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .chat-row.is-user .avatar-ring,
  .chat-row.is-user .bubble-stack {
    order: initial;
  }

  .avatar-ring {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .bubble-meta strong {
    font-size: 0.72rem;
  }

}
</style>
