<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  approveAdminAgentDraftApi,
  fetchAdminAgentDraftApi,
  fetchAdminAgentDraftsApi,
  rejectAdminAgentDraftApi
} from '../../api/admin';
import { renderMarkdown } from '../../utils/markdown';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const router = useRouter();
const route = useRoute();

const loading = ref(true);
const detailLoading = ref(false);
const actionLoading = ref(false);
const selectedTaskId = ref(null);

const pageData = ref({
  records: [],
  page: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0,
  hasNext: false
});

const detailData = ref(null);

const filters = reactive({
  reviewStatus: '',
  keyword: ''
});

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      title: 'Agent 草稿审核',
      hint: '集中查看 Agent 生成的待审草稿，并完成通过或驳回。',
      allReviewStatus: '全部审核状态',
      keywordPlaceholder: '按任务标题或目标搜索',
      apply: '应用',
      reset: '重置',
      taskId: '任务 ID',
      titleCol: '标题',
      goal: '目标',
      taskStatus: '任务状态',
      reviewStatus: '审核状态',
      draftPostId: '草稿文章 ID',
      updatedAt: '更新时间',
      rejectReason: '驳回原因',
      actions: '操作',
      open: '查看',
      approve: '通过',
      reject: '驳回',
      previous: '上一页',
      next: '下一页',
      previousDraft: '上一条',
      nextDraft: '下一条',
      totalPageInfo: '{total} 条，当前 {page} / {pages}',
      taskSummary: '任务信息',
      sessionId: '会话 ID',
      finalOutput: '最终摘要',
      draftSummary: '草稿摘要',
      draftStatus: '草稿状态',
      draftUpdatedAt: '草稿更新时间',
      draftContent: '草稿内容',
      preview: '预览',
      raw: '原文',
      openSession: '打开会话',
      openPostEditor: '编辑草稿',
      reviewPendingHint: '该草稿当前可审核。',
      reviewRejectedHint: '该草稿已被驳回，允许再次查看与重审。',
      reviewPublishedHint: '该草稿已发布，无需重复处理。',
      noData: '暂无草稿记录',
      detailEmpty: '请选择一条草稿查看审核详情。',
      approveConfirmTitle: '确认通过草稿',
      approveConfirmBody: '通过后将正式发布该草稿，并同步公开内容与知识索引。是否继续？',
      approveSuccess: '草稿已发布',
      rejectPromptTitle: '驳回草稿',
      rejectPromptMessage: '请输入驳回原因，便于后续人工修改或重试。',
      rejectInputPlaceholder: '例如：结构不完整、事实依据不足、标题不准确',
      rejectSuccess: '草稿已驳回',
      statusPending: '待执行',
      statusRunning: '执行中',
      statusCompleted: '已完成',
      statusFailed: '失败',
      statusCancelled: '已取消',
      reviewReady: '待审核',
      reviewRejected: '已驳回',
      reviewPublished: '已发布',
      close: '关闭'
    };
  }

  return {
    title: 'Agent Draft Review',
    hint: 'Review agent-generated drafts and decide whether to publish or reject them.',
    allReviewStatus: 'All review status',
    keywordPlaceholder: 'Search by task title or goal',
    apply: 'Apply',
    reset: 'Reset',
    taskId: 'Task ID',
    titleCol: 'Title',
    goal: 'Goal',
    taskStatus: 'Task status',
    reviewStatus: 'Review status',
    draftPostId: 'Draft post ID',
    updatedAt: 'Updated At',
    rejectReason: 'Reject reason',
    actions: 'Actions',
    open: 'Open',
    approve: 'Approve',
    reject: 'Reject',
    previous: 'Previous',
    next: 'Next',
    previousDraft: 'Previous Draft',
    nextDraft: 'Next Draft',
    totalPageInfo: '{total} total, page {page} / {pages}',
    taskSummary: 'Task summary',
    sessionId: 'Session ID',
    finalOutput: 'Final summary',
    draftSummary: 'Draft summary',
    draftStatus: 'Draft status',
    draftUpdatedAt: 'Draft updated at',
    draftContent: 'Draft content',
    preview: 'Preview',
    raw: 'Raw',
    openSession: 'Open session',
    openPostEditor: 'Edit draft',
    reviewPendingHint: 'This draft is ready for review.',
    reviewRejectedHint: 'This draft was rejected and can be reviewed again.',
    reviewPublishedHint: 'This draft has already been published.',
    noData: 'No draft records',
    detailEmpty: 'Select a draft to inspect its review details.',
    approveConfirmTitle: 'Approve draft',
    approveConfirmBody: 'Approving will publish the draft and sync public content plus the knowledge index. Continue?',
    approveSuccess: 'Draft published',
    rejectPromptTitle: 'Reject draft',
    rejectPromptMessage: 'Enter a reject reason for later editing or retry.',
    rejectInputPlaceholder: 'For example: incomplete structure, weak evidence, inaccurate title',
    rejectSuccess: 'Draft rejected',
    statusPending: 'Pending',
    statusRunning: 'Running',
    statusCompleted: 'Completed',
    statusFailed: 'Failed',
    statusCancelled: 'Cancelled',
    reviewReady: 'Ready',
    reviewRejected: 'Rejected',
    reviewPublished: 'Published',
    close: 'Close'
  };
});

const reviewStatusOptions = computed(() => ([
  { label: copy.value.reviewReady, value: 'DRAFT_READY' },
  { label: copy.value.reviewRejected, value: 'REVIEW_REJECTED' },
  { label: copy.value.reviewPublished, value: 'PUBLISHED' }
]));

const renderedDraftContent = computed(() => {
  const content = detailData.value?.draftContent || '';
  return renderMarkdown(content);
});

const currentDraftIndex = computed(() => pageData.value.records.findIndex((item) => item.taskId === selectedTaskId.value));
const hasPreviousDraft = computed(() => currentDraftIndex.value > 0);
const hasNextDraft = computed(() => {
  if (currentDraftIndex.value < 0) {
    return false;
  }
  return currentDraftIndex.value < pageData.value.records.length - 1;
});

const buildParams = (page = pageData.value.page || 1) => ({
  reviewStatus: filters.reviewStatus || undefined,
  keyword: filters.keyword || undefined,
  page,
  pageSize: pageData.value.pageSize
});

const loadDrafts = async (page = pageData.value.page || 1) => {
  loading.value = true;
  try {
    const res = await fetchAdminAgentDraftsApi(buildParams(page));
    pageData.value = res.data;
  } finally {
    loading.value = false;
  }
};

const syncRouteTaskId = (taskId) => {
  const nextQuery = { ...route.query };
  if (taskId) {
    nextQuery.taskId = String(taskId);
  } else {
    delete nextQuery.taskId;
  }
  router.replace({ query: nextQuery });
};

const loadDetail = async (taskId = selectedTaskId.value) => {
  if (!taskId) {
    return;
  }
  detailLoading.value = true;
  try {
    const res = await fetchAdminAgentDraftApi(taskId);
    detailData.value = res.data || null;
    selectedTaskId.value = taskId;
    syncRouteTaskId(taskId);
  } finally {
    detailLoading.value = false;
  }
};

const search = async () => {
  await loadDrafts(1);
};

const resetFilters = async () => {
  filters.reviewStatus = '';
  filters.keyword = '';
  await loadDrafts(1);
};

const prevPage = async () => {
  if (pageData.value.page > 1) {
    await loadDrafts(pageData.value.page - 1);
  }
};

const nextPage = async () => {
  if (pageData.value.hasNext) {
    await loadDrafts(pageData.value.page + 1);
  }
};

const statusText = (status) => {
  const map = {
    PENDING: copy.value.statusPending,
    RUNNING: copy.value.statusRunning,
    COMPLETED: copy.value.statusCompleted,
    FAILED: copy.value.statusFailed,
    CANCELLED: copy.value.statusCancelled
  };
  return map[status] || status || '-';
};

const reviewStatusText = (status) => {
  const map = {
    DRAFT_READY: copy.value.reviewReady,
    REVIEW_REJECTED: copy.value.reviewRejected,
    PUBLISHED: copy.value.reviewPublished
  };
  return map[status] || status || '-';
};

const statusBadgeClass = (status) => {
  if (status === 'COMPLETED' || status === 'PUBLISHED') {
    return 'admin-status-pill is-positive';
  }
  if (status === 'FAILED' || status === 'CANCELLED' || status === 'REVIEW_REJECTED') {
    return 'admin-status-pill is-negative';
  }
  if (status === 'RUNNING') {
    return 'admin-status-pill is-running';
  }
  if (status === 'DRAFT_READY') {
    return 'admin-status-pill is-waiting';
  }
  return 'admin-status-pill';
};

const canApprove = computed(() => detailData.value?.task?.reviewStatus === 'DRAFT_READY');
const canReject = computed(() => {
  const status = detailData.value?.task?.reviewStatus;
  return status === 'DRAFT_READY' || status === 'REVIEW_REJECTED';
});

const openTaskSession = () => {
  const sessionId = detailData.value?.task?.sessionId;
  if (!sessionId) {
    return;
  }
  const target = router.resolve({ path: '/knowledge', query: { sessionId } });
  window.open(target.href, '_blank', 'noopener');
};

const openPostEditor = () => {
  const postId = detailData.value?.postId;
  if (!postId) {
    return;
  }
  const target = router.resolve({ name: 'admin-post-edit', params: { id: postId } });
  window.open(target.href, '_blank', 'noopener');
};

const reviewHint = computed(() => {
  const status = detailData.value?.task?.reviewStatus;
  if (status === 'PUBLISHED') {
    return copy.value.reviewPublishedHint;
  }
  if (status === 'REVIEW_REJECTED') {
    return copy.value.reviewRejectedHint;
  }
  return copy.value.reviewPendingHint;
});

const approveDraft = async () => {
  if (!detailData.value?.task?.id || !canApprove.value) {
    return;
  }
  const taskId = detailData.value.task.id;
  await ElMessageBox.confirm(copy.value.approveConfirmBody, copy.value.approveConfirmTitle, { type: 'warning' });
  actionLoading.value = true;
  try {
    await approveAdminAgentDraftApi(taskId, {});
    ElMessage.success(copy.value.approveSuccess);
    await loadDrafts(pageData.value.page);
    const moved = await jumpToNextPendingDraft(taskId);
    if (!moved) {
      await loadDetail(taskId);
    }
  } finally {
    actionLoading.value = false;
  }
};

const rejectDraft = async () => {
  if (!detailData.value?.task?.id || !canReject.value) {
    return;
  }
  const taskId = detailData.value.task.id;
  const result = await ElMessageBox.prompt(copy.value.rejectPromptMessage, copy.value.rejectPromptTitle, {
    confirmButtonText: copy.value.reject,
    cancelButtonText: copy.value.close,
    inputPlaceholder: copy.value.rejectInputPlaceholder,
    inputValidator: (value) => (String(value || '').trim() ? true : copy.value.rejectPromptMessage)
  });
  actionLoading.value = true;
  try {
    await rejectAdminAgentDraftApi(taskId, { reason: result.value.trim() });
    ElMessage.success(copy.value.rejectSuccess);
    await loadDrafts(pageData.value.page);
    const moved = await jumpToNextPendingDraft(taskId);
    if (!moved) {
      await loadDetail(taskId);
    }
  } finally {
    actionLoading.value = false;
  }
};

const goToSiblingDraft = async (direction) => {
  const nextIndex = currentDraftIndex.value + direction;
  if (nextIndex < 0 || nextIndex >= pageData.value.records.length) {
    return false;
  }
  const nextTaskId = pageData.value.records[nextIndex]?.taskId;
  if (!nextTaskId) {
    return false;
  }
  await loadDetail(nextTaskId);
  return true;
};

const jumpToNextPendingDraft = async (processedTaskId) => {
  const currentIndex = pageData.value.records.findIndex((item) => item.taskId === processedTaskId);
  const nextReady = pageData.value.records.find((item, index) => index > currentIndex && item.reviewStatus === 'DRAFT_READY');
  if (nextReady?.taskId) {
    await loadDetail(nextReady.taskId);
    return true;
  }
  const firstReady = pageData.value.records.find((item) => item.reviewStatus === 'DRAFT_READY');
  if (firstReady?.taskId) {
    await loadDetail(firstReady.taskId);
    return true;
  }
  return false;
};

const closeDetail = () => {
  selectedTaskId.value = null;
  detailData.value = null;
  syncRouteTaskId(null);
};

const formatDatetime = (value) => preferences.formatDateTime(value);

onMounted(async () => {
  await loadDrafts(1);
  const taskId = Number(route.query.taskId);
  if (Number.isFinite(taskId) && taskId > 0) {
    await loadDetail(taskId);
  }
});
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack" v-loading="loading">
    <div class="admin-page-head">
      <div>
        <span class="admin-kicker">Review</span>
        <h2>{{ copy.title }}</h2>
        <p class="muted">{{ copy.hint }}</p>
      </div>
      <div class="admin-page-actions">
        <span class="admin-badge">{{ copy.totalPageInfo.replace('{total}', pageData.total || 0).replace('{page}', pageData.page || 1).replace('{pages}', pageData.totalPages || 1) }}</span>
      </div>
    </div>

    <div class="toolbar admin-toolbar manage-toolbar">
      <el-select v-model="filters.reviewStatus" :placeholder="copy.allReviewStatus" clearable>
        <el-option :label="copy.allReviewStatus" value="" />
        <el-option v-for="item in reviewStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="filters.keyword" :placeholder="copy.keywordPlaceholder" @keyup.enter="search" />
      <el-button type="primary" @click="search">{{ copy.apply }}</el-button>
      <el-button @click="resetFilters">{{ copy.reset }}</el-button>
    </div>

    <div class="admin-meta-row table-meta muted">
      <span>{{ copy.totalPageInfo.replace('{total}', pageData.total || 0).replace('{page}', pageData.page || 1).replace('{pages}', pageData.totalPages || 1) }}</span>
    </div>

    <div class="admin-table-wrap">
      <el-table :data="pageData.records">
        <el-table-column prop="taskId" :label="copy.taskId" width="110" />
        <el-table-column prop="title" :label="copy.titleCol" min-width="220" />
        <el-table-column prop="goal" :label="copy.goal" min-width="260" show-overflow-tooltip />
        <el-table-column :label="copy.taskStatus" width="120">
          <template #default="{ row }">
            <span :class="statusBadgeClass(row.status)">{{ statusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="copy.reviewStatus" width="120">
          <template #default="{ row }">
            <span :class="statusBadgeClass(row.reviewStatus)">{{ reviewStatusText(row.reviewStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="draftPostId" :label="copy.draftPostId" width="120" />
        <el-table-column :label="copy.updatedAt" width="180">
          <template #default="{ row }">
            {{ formatDatetime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="copy.actions" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="loadDetail(row.taskId)">{{ copy.open }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="pageData.total > 0" class="pager">
      <el-button :disabled="pageData.page <= 1" @click="prevPage">{{ copy.previous }}</el-button>
      <el-button :disabled="!pageData.hasNext" type="primary" @click="nextPage">{{ copy.next }}</el-button>
    </div>

    <section v-if="detailData" class="draft-detail-panel" v-loading="detailLoading || actionLoading">
      <div class="draft-detail-head">
        <div>
          <span class="admin-kicker">Detail</span>
          <h3>{{ detailData.draftTitle || detailData.task?.title || '-' }}</h3>
          <p class="muted">{{ reviewHint }}</p>
        </div>
        <div class="draft-detail-actions">
          <el-button size="small" :disabled="!hasPreviousDraft" @click="goToSiblingDraft(-1)">{{ copy.previousDraft }}</el-button>
          <el-button size="small" :disabled="!hasNextDraft" @click="goToSiblingDraft(1)">{{ copy.nextDraft }}</el-button>
          <el-button size="small" @click="openTaskSession">{{ copy.openSession }}</el-button>
          <el-button size="small" @click="openPostEditor">{{ copy.openPostEditor }}</el-button>
          <el-button size="small" type="success" :disabled="!canApprove" @click="approveDraft">{{ copy.approve }}</el-button>
          <el-button size="small" type="danger" :disabled="!canReject" @click="rejectDraft">{{ copy.reject }}</el-button>
          <el-button size="small" @click="closeDetail">{{ copy.close }}</el-button>
        </div>
      </div>

      <div class="draft-info-grid">
        <div class="draft-info-card">
          <span>{{ copy.taskId }}</span>
          <strong>{{ detailData.task?.id || '-' }}</strong>
        </div>
        <div class="draft-info-card">
          <span>{{ copy.reviewStatus }}</span>
          <strong :class="statusBadgeClass(detailData.task?.reviewStatus)">{{ reviewStatusText(detailData.task?.reviewStatus) }}</strong>
        </div>
        <div class="draft-info-card">
          <span>{{ copy.taskStatus }}</span>
          <strong :class="statusBadgeClass(detailData.task?.status)">{{ statusText(detailData.task?.status) }}</strong>
        </div>
        <div class="draft-info-card">
          <span>{{ copy.draftStatus }}</span>
          <strong>{{ detailData.draftStatus || '-' }}</strong>
        </div>
        <div class="draft-info-card">
          <span>{{ copy.draftPostId }}</span>
          <strong>{{ detailData.postId || '-' }}</strong>
        </div>
        <div class="draft-info-card">
          <span>{{ copy.draftUpdatedAt }}</span>
          <strong>{{ formatDatetime(detailData.draftUpdatedAt) }}</strong>
        </div>
      </div>

      <div class="draft-sections">
        <article class="draft-section">
          <h4>{{ copy.taskSummary }}</h4>
          <dl class="draft-meta-list">
            <div>
              <dt>{{ copy.titleCol }}</dt>
              <dd>{{ detailData.task?.title || '-' }}</dd>
            </div>
            <div>
              <dt>{{ copy.sessionId }}</dt>
              <dd>{{ detailData.task?.sessionId || '-' }}</dd>
            </div>
            <div>
              <dt>{{ copy.goal }}</dt>
              <dd>{{ detailData.task?.goal || '-' }}</dd>
            </div>
            <div>
              <dt>{{ copy.rejectReason }}</dt>
              <dd>{{ detailData.task?.rejectReason || '-' }}</dd>
            </div>
            <div>
              <dt>{{ copy.finalOutput }}</dt>
              <dd class="pre-wrap">{{ detailData.task?.finalOutputSummary || '-' }}</dd>
            </div>
            <div>
              <dt>{{ copy.draftSummary }}</dt>
              <dd class="pre-wrap">{{ detailData.draftSummary || '-' }}</dd>
            </div>
          </dl>
        </article>

        <article class="draft-section draft-content-section">
          <div class="draft-section-head">
            <h4>{{ copy.draftContent }}</h4>
            <div class="draft-content-tabs">
              <span class="tab-chip">{{ copy.preview }}</span>
              <span class="tab-chip muted">{{ copy.raw }}</span>
            </div>
          </div>
          <div class="markdown-body draft-markdown" v-html="renderedDraftContent"></div>
          <pre class="draft-raw-content">{{ detailData.draftContent || '' }}</pre>
        </article>
      </div>
    </section>

    <section v-else class="draft-empty-panel">
      <p>{{ copy.detailEmpty }}</p>
    </section>
  </section>
</template>

<style scoped>
.admin-table-wrap {
  flex: 1 1 auto;
  overflow: auto;
}

.admin-table-wrap :deep(.el-table) {
  height: 100%;
}

.table-meta {
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-size: 0.8rem;
}

.pager {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.draft-detail-panel,
.draft-empty-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: var(--admin-soft-bg);
}

.draft-detail-head,
.draft-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.draft-detail-head h3,
.draft-section h4 {
  margin: 0;
}

.draft-detail-actions,
.draft-content-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.draft-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.draft-info-card,
.draft-section {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.draft-info-card span,
.draft-meta-list dt,
.tab-chip {
  display: block;
  color: var(--text-secondary);
  font-size: 0.82rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.draft-info-card strong {
  display: inline-flex;
  margin-top: 8px;
}

.draft-sections {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
}

.draft-meta-list {
  display: grid;
  gap: 14px;
  margin: 0;
}

.draft-meta-list div {
  display: grid;
  gap: 6px;
}

.draft-meta-list dd {
  margin: 0;
  color: var(--text-main);
  line-height: 1.7;
  word-break: break-word;
}

.pre-wrap {
  white-space: pre-wrap;
}

.draft-content-section {
  gap: 14px;
}

.draft-markdown {
  padding: 18px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.18);
  overflow: auto;
}

.draft-raw-content {
  margin: 0;
  max-height: 280px;
  padding: 16px;
  border-radius: 14px;
  border: 1px dashed var(--line);
  background: rgba(255, 255, 255, 0.02);
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  overflow: auto;
}

.tab-chip {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
}

.is-positive {
  background: rgba(22, 163, 74, 0.14);
  border-color: rgba(22, 163, 74, 0.45);
  color: #16a34a;
}

.is-negative {
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.45);
  color: #dc2626;
}

.is-running {
  background: rgba(59, 130, 246, 0.14);
  border-color: rgba(59, 130, 246, 0.45);
  color: #1d4ed8;
}

.is-waiting {
  background: rgba(245, 158, 11, 0.14);
  border-color: rgba(245, 158, 11, 0.4);
  color: #d97706;
}

@media (max-width: 1080px) {
  .draft-sections {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .admin-table-wrap {
    flex: none;
    overflow: hidden;
  }

  .manage-toolbar {
    grid-template-columns: 1fr;
  }

  .draft-detail-head,
  .draft-section-head {
    flex-direction: column;
  }
}
</style>