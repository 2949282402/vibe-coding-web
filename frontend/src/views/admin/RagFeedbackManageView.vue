<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { exportAdminRagFeedbackCsvApi, fetchAdminRagFeedbackApi } from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const router = useRouter();
const loading = ref(true);
const exportLoading = ref(false);
const pageData = ref({
  records: [],
  page: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0,
  hasNext: false
});
const filters = reactive({
  keyword: '',
  helpful: '',
  dateRange: []
});

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      title: 'RAG 反馈管理',
      hint: '筛选用户对 RAG 回答的反馈，查看备注与聊天内容，并快速定位到对应会话。',
      recordCount: '{count} 条反馈',
      searchPlaceholder: '搜索备注、回答内容或会话 ID',
      allFeedback: '全部反馈',
      helpful: '有帮助',
      needsWork: '需改进',
      dateRange: '反馈日期',
      dateStart: '开始日期',
      dateEnd: '结束日期',
      apply: '应用',
      reset: '重置',
      export: '导出 CSV',
      totalPageInfo: '共 {total} 条，第 {page} / {pages} 页',
      answerPreview: '回答摘要',
      feedbackType: '反馈',
      note: '备注',
      mode: '模式',
      session: '会话',
      answerAt: '回答时间',
      feedbackAt: '反馈时间',
      actions: '操作',
      copyFeedback: '复制反馈',
      copyChat: '复制聊天',
      copySession: '复制会话',
      openSession: '查看会话',
      noAnswerContent: '暂无聊天内容',
      copied: '会话 ID 已复制',
      feedbackCopied: '反馈内容已复制',
      chatCopied: '聊天内容已复制',
      exportSuccess: 'CSV 已开始下载',
      previous: '上一页',
      next: '下一页',
      noNote: '未填写备注'
    };
  }

  return {
    title: 'RAG Feedback',
    hint: 'Filter answer ratings, inspect notes, export CSV, and jump directly to the exact chat session.',
    recordCount: '{count} feedback entries',
    searchPlaceholder: 'Search notes, answer text, or session id',
    allFeedback: 'All feedback',
    helpful: 'Helpful',
    needsWork: 'Needs Work',
    dateRange: 'Feedback Date',
    dateStart: 'Start date',
    dateEnd: 'End date',
    apply: 'Apply',
    reset: 'Reset',
    export: 'Export CSV',
    totalPageInfo: '{total} total, page {page} / {pages}',
    answerPreview: 'Answer Preview',
    feedbackType: 'Feedback',
    note: 'Note',
    mode: 'Mode',
    session: 'Session',
    answerAt: 'Answer At',
    feedbackAt: 'Feedback At',
    actions: 'Actions',
    copyFeedback: 'Copy Feedback',
    copyChat: 'Copy Chat',
    copySession: 'Copy Session',
    openSession: 'Open Session',
    noAnswerContent: 'No chat content available',
    copied: 'Session id copied',
    feedbackCopied: 'Feedback copied',
    chatCopied: 'Chat content copied',
    exportSuccess: 'CSV download started',
    previous: 'Previous',
    next: 'Next',
    noNote: 'No note provided'
  };
});

const buildQueryParams = (page = pageData.value.page || 1) => ({
  keyword: filters.keyword || undefined,
  helpful: filters.helpful === '' ? undefined : filters.helpful === 'true',
  feedbackDateFrom: filters.dateRange?.[0] || undefined,
  feedbackDateTo: filters.dateRange?.[1] || undefined,
  page,
  pageSize: 10
});

const loadData = async (page = pageData.value.page || 1) => {
  loading.value = true;
  try {
    const res = await fetchAdminRagFeedbackApi(buildQueryParams(page));
    pageData.value = res.data;
  } finally {
    loading.value = false;
  }
};

const search = async () => {
  await loadData(1);
};

const resetFilters = async () => {
  filters.keyword = '';
  filters.helpful = '';
  filters.dateRange = [];
  await loadData(1);
};

const prevPage = async () => {
  if (pageData.value.page > 1) {
    await loadData(pageData.value.page - 1);
  }
};

const nextPage = async () => {
  if (pageData.value.hasNext) {
    await loadData(pageData.value.page + 1);
  }
};

const statusLabel = (helpful) => (helpful ? copy.value.helpful : copy.value.needsWork);
const statusClass = (helpful) => (helpful ? 'admin-status-pill is-positive' : 'admin-status-pill is-negative');

const copyText = async (value, successMessage) => {
  const normalized = String(value || '');
  if (!normalized) {
    return;
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(normalized);
    } else {
      const input = document.createElement('textarea');
      input.value = normalized;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
    }
    ElMessage.success(successMessage);
  } catch (error) {
    ElMessage.error(error?.message || 'Copy failed');
  }
};

const copySessionId = async (sessionId) => {
  await copyText(sessionId, copy.value.copied);
};

const copyFeedbackContent = async (row) => {
  const payload = [
    `${copy.value.feedbackType}: ${statusLabel(row.helpful)}`,
    `${copy.value.note}: ${row.note || copy.value.noNote}`,
    `${copy.value.session}: ${row.sessionId}`
  ].join('\n');
  await copyText(payload, copy.value.feedbackCopied);
};

const copyChatContent = async (row) => {
  await copyText(row.answerContent || copy.value.noAnswerContent, copy.value.chatCopied);
};

const openSession = (sessionId) => {
  const target = router.resolve({
    path: '/knowledge',
    query: { sessionId }
  });
  window.open(target.href, '_blank', 'noopener');
};

const exportCsv = async () => {
  exportLoading.value = true;
  try {
    const blobData = await exportAdminRagFeedbackCsvApi({
      keyword: filters.keyword || undefined,
      helpful: filters.helpful === '' ? undefined : filters.helpful === 'true',
      feedbackDateFrom: filters.dateRange?.[0] || undefined,
      feedbackDateTo: filters.dateRange?.[1] || undefined
    });

    const blob = new Blob([blobData], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'rag-feedback-export.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    ElMessage.success(copy.value.exportSuccess);
  } catch (error) {
    ElMessage.error(error?.message || 'Export failed');
  } finally {
    exportLoading.value = false;
  }
};

onMounted(async () => {
  await loadData(1);
});
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack feedback-page" v-loading="loading">
    <div class="admin-page-head">
      <div>
        <span class="admin-kicker">Signal Review</span>
        <h2>{{ copy.title }}</h2>
        <p class="muted">{{ copy.hint }}</p>
      </div>
      <div class="admin-page-actions">
        <span class="admin-badge">{{ copy.recordCount.replace('{count}', pageData.total || 0) }}</span>
        <el-button :loading="exportLoading" @click="exportCsv">{{ copy.export }}</el-button>
      </div>
    </div>

    <div class="toolbar admin-toolbar manage-toolbar">
      <el-input
        v-model="filters.keyword"
        :placeholder="copy.searchPlaceholder"
        @keyup.enter="search"
      />
      <el-select v-model="filters.helpful" :placeholder="copy.allFeedback" clearable>
        <el-option :label="copy.helpful" value="true" />
        <el-option :label="copy.needsWork" value="false" />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        unlink-panels
        range-separator="-"
        :start-placeholder="copy.dateStart"
        :end-placeholder="copy.dateEnd"
        value-format="YYYY-MM-DD"
      />
      <el-button type="primary" @click="search">{{ copy.apply }}</el-button>
      <el-button @click="resetFilters">{{ copy.reset }}</el-button>
    </div>

    <div class="admin-meta-row table-meta muted">
      <span>
        {{ copy.totalPageInfo.replace('{total}', pageData.total || 0).replace('{page}', pageData.page || 1).replace('{pages}', pageData.totalPages || 1) }}
      </span>
    </div>

    <div class="admin-table-wrap">
      <el-table :data="pageData.records">
        <el-table-column :label="copy.feedbackType" width="130">
          <template #default="{ row }">
            <span :class="statusClass(row.helpful)">{{ statusLabel(row.helpful) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="answerPreview" :label="copy.answerPreview" min-width="300" />
        <el-table-column :label="copy.note" min-width="220">
          <template #default="{ row }">
            {{ row.note || copy.noNote }}
          </template>
        </el-table-column>
        <el-table-column prop="mode" :label="copy.mode" width="120" />
        <el-table-column prop="sessionId" :label="copy.session" min-width="180" />
        <el-table-column :label="copy.answerAt" width="180">
          <template #default="{ row }">
            {{ preferences.formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="copy.feedbackAt" width="180">
          <template #default="{ row }">
            {{ preferences.formatDateTime(row.feedbackAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="copy.actions" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyFeedbackContent(row)">{{ copy.copyFeedback }}</el-button>
            <el-button link @click="copyChatContent(row)">{{ copy.copyChat }}</el-button>
            <el-button link type="primary" @click="copySessionId(row.sessionId)">{{ copy.copySession }}</el-button>
            <el-button link @click="openSession(row.sessionId)">{{ copy.openSession }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="pageData.total > 0" class="pager">
      <el-button :disabled="pageData.page <= 1" @click="prevPage">{{ copy.previous }}</el-button>
      <el-button :disabled="!pageData.hasNext" type="primary" @click="nextPage">{{ copy.next }}</el-button>
    </div>
  </section>
</template>

<style scoped>
.feedback-page {
  height: 100%;
  overflow: hidden;
}

.manage-toolbar {
  grid-template-columns: minmax(0, 1.3fr) 180px 260px auto auto;
}

.admin-table-wrap {
  flex: 1 1 auto;
  overflow: auto;
}

.admin-table-wrap :deep(.el-table) {
  height: 100%;
}

.admin-surface :deep(.el-input__wrapper),
.admin-surface :deep(.el-select__wrapper),
.admin-surface :deep(.el-date-editor.el-input__wrapper),
.admin-surface :deep(.el-table),
.admin-surface :deep(.el-table__inner-wrapper),
.admin-surface :deep(.el-table__header-wrapper),
.admin-surface :deep(.el-table__body-wrapper),
.admin-surface :deep(.el-table__body),
.admin-surface :deep(.el-table__header),
.admin-surface :deep(.el-scrollbar__wrap),
.admin-surface :deep(.el-scrollbar__view),
.admin-surface :deep(.el-table th.el-table__cell),
.admin-surface :deep(.el-table td.el-table__cell),
.admin-surface :deep(.el-table__fixed),
.admin-surface :deep(.el-table__fixed-right),
.admin-surface :deep(.el-table__fixed-body-wrapper),
.admin-surface :deep(.el-table__fixed-header-wrapper),
.admin-surface :deep(.el-table-fixed-column--left),
.admin-surface :deep(.el-table-fixed-column--right) {
  background: var(--table-surface) !important;
}

.admin-surface :deep(.el-input__wrapper),
.admin-surface :deep(.el-select__wrapper),
.admin-surface :deep(.el-date-editor.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--input-line) inset !important;
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

.is-positive {
  color: var(--text-main);
  border-color: var(--line-strong);
}

.is-negative {
  color: var(--text-secondary);
}

@media (max-width: 960px) {
  .feedback-page {
    height: auto;
    overflow: visible;
  }

  .admin-table-wrap {
    flex: none;
    overflow: hidden;
  }

  .manage-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
