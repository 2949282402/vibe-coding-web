<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAdminAgentTaskTraceApi, fetchAdminAgentToolCallsApi } from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const router = useRouter();
const loading = ref(true);
const pageData = ref({
  records: [],
  page: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0,
  hasNext: false
});

const filters = reactive({
  taskId: ''
});

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      title: 'Agent 工具调用',
      hint: '管理员可查看所有工具调用记录与执行结果。',
      keyword: '任务 ID',
      apply: '应用',
      reset: '重置',
      totalPageInfo: '{total} 条，当前 {page} / {pages}',
      toolName: '工具名',
      permissionLevel: '权限级别',
      success: '成功',
      fail: '失败',
      taskId: '任务 ID',
      stepId: '步骤 ID',
      latency: '耗时(ms)',
      responseSummary: '返回摘要',
      errorMessage: '错误信息',
      createdAt: '创建时间',
      openSession: '打开会话',
      previous: '上一页',
      next: '下一页'
    };
  }

  return {
    title: 'Agent Tool Calls',
    hint: 'Inspect all tool call records and execution results from admin.',
    keyword: 'Task ID',
    apply: 'Apply',
    reset: 'Reset',
    totalPageInfo: '{total} total, page {page} / {pages}',
    toolName: 'Tool name',
    permissionLevel: 'Permission',
    success: 'Success',
    fail: 'Fail',
    taskId: 'Task ID',
    stepId: 'Step ID',
    latency: 'Latency(ms)',
    responseSummary: 'Response summary',
    errorMessage: 'Error message',
    createdAt: 'Created At',
    openSession: 'Open session',
    previous: 'Previous',
    next: 'Next'
  };
});

const statusBadgeType = (value) => (value ? 'success' : 'danger');
const statusBadgeText = (value) => (value ? copy.value.success : copy.value.fail);

const buildParams = (page = pageData.value.page || 1) => ({
  taskId: filters.taskId || undefined,
  page,
  pageSize: pageData.value.pageSize
});

const loadData = async (page = pageData.value.page || 1) => {
  loading.value = true;
  try {
    const res = await fetchAdminAgentToolCallsApi(buildParams(page));
    pageData.value = res.data;
  } finally {
    loading.value = false;
  }
};

const search = async () => {
  await loadData(1);
};

const resetFilters = async () => {
  filters.taskId = '';
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

const openTaskSession = async (taskId) => {
  const res = await fetchAdminAgentTaskTraceApi(taskId, {
    toolCallPage: 1,
    toolCallPageSize: 1,
    memoryHitPage: 1,
    memoryHitPageSize: 1
  });
  const sessionId = res?.data?.task?.sessionId;
  if (!sessionId) {
    return;
  }
  const target = router.resolve({ path: '/knowledge', query: { sessionId } });
  window.open(target.href, '_blank', 'noopener');
};

onMounted(async () => {
  await loadData(1);
});
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack" v-loading="loading">
    <div class="admin-page-head">
      <div>
        <span class="admin-kicker">Tooling</span>
        <h2>{{ copy.title }}</h2>
        <p class="muted">{{ copy.hint }}</p>
      </div>
    </div>

    <div class="toolbar admin-toolbar manage-toolbar">
      <el-input v-model="filters.taskId" :placeholder="copy.keyword" />
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
        <el-table-column :label="copy.taskId" prop="taskId" width="120" />
        <el-table-column :label="copy.stepId" prop="stepId" width="120" />
        <el-table-column :label="copy.toolName" prop="toolName" min-width="180" />
        <el-table-column :label="copy.permissionLevel" prop="permissionLevel" width="140" />
        <el-table-column :label="copy.success" width="90">
          <template #default="{ row }">
            <el-tag :type="statusBadgeType(row.success)" size="small">{{ statusBadgeText(row.success) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="copy.latency" prop="latencyMs" width="120" />
        <el-table-column :label="copy.responseSummary" prop="responseSummary" min-width="260" show-overflow-tooltip />
        <el-table-column :label="copy.errorMessage" prop="errorMessage" min-width="220" show-overflow-tooltip />
        <el-table-column :label="copy.createdAt" width="180">
          <template #default="{ row }">
            {{ preferences.formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="copy.openSession" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTaskSession(row.taskId)">{{ copy.openSession }}</el-button>
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

@media (max-width: 960px) {
  .admin-table-wrap {
    flex: none;
    overflow: hidden;
  }

  .manage-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
