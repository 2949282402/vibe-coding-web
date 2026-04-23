<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  fetchAdminAgentTaskTraceApi,
  fetchAdminAgentTasksApi
} from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const router = useRouter();
const loading = ref(true);
const traceLoading = ref(false);
const drawerVisible = ref(false);
const activeTraceTab = ref('steps');
const selectedTaskId = ref(null);

const traceData = ref({
  task: null,
  steps: [],
  events: [],
  toolCalls: {
    records: [],
    page: 1,
    pageSize: 8,
    total: 0,
    totalPages: 0,
    hasNext: false
  },
  memoryHits: {
    records: [],
    page: 1,
    pageSize: 8,
    total: 0,
    totalPages: 0,
    hasNext: false
  }
});

const pageData = ref({
  records: [],
  page: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0,
  hasNext: false
});

const filters = reactive({
  status: '',
  keyword: ''
});

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      title: 'Agent 运维',
      hint: '查看后台 Agent 任务列表、步骤、工具调用与记忆命中日志。',
      statusAll: '全部状态',
      searchPlaceholder: '按任务标题/目标搜索',
      apply: '应用',
      reset: '重置',
      taskId: '任务 ID',
      titleCol: '标题',
      goal: '目标',
      status: '状态',
      type: '类型',
      searchMode: '检索方式',
      createdAt: '创建时间',
      updatedAt: '更新时间',
      actions: '操作',
      previous: '上一页',
      next: '下一页',
      totalPageInfo: '{total} 条，当前 {page} / {pages}',
      openTrace: '查看 Trace',
      open: '打开',
      taskSummary: '任务摘要',
      taskIdLabel: '任务 ID',
      sessionId: '会话 ID',
      taskType: '任务类型',
      executionMode: '执行模式',
      searchScope: '检索范围',
      draftWrite: '允许写草稿',
      finalOutput: '最终摘要',
      error: '错误信息',
      stepIndex: '步骤序号',
      agentRole: '角色',
      stepName: '步骤名',
      stepStatus: '状态',
      stepOutput: '输出摘要',
      eventType: '事件类型',
      payloadSummary: '载荷摘要',
      toolCalls: '工具调用',
      memoryHits: '记忆命中',
      noData: '暂无数据',
      toolName: '工具名',
      permissionLevel: '权限',
      success: '成功',
      latency: '耗时(ms)',
      topic: '主题',
      hitReason: '命中原因',
      usedInStep: '使用步骤',
      responseSummary: '返回摘要',
      pass: '成功',
      fail: '失败',
      trace: '任务 Trace',
      steps: '步骤',
      events: '事件',
      statusPending: '待执行',
      statusRunning: '执行中',
      statusCompleted: '已完成',
      statusFailed: '失败',
      statusCancelled: '已取消',
      statusSkipped: '已跳过',
      typeBlogDraft: '博客草稿',
      modeAuto: '自动',
      modeResearchFirst: '先检索',
      modeSinglePass: '单次执行',
      scopeLocalOnly: '仅本地',
      scopeLocalAndWeb: '本地+网络',
      rolePlanner: '规划者',
      roleResearcher: '检索者',
      roleWriter: '写手',
      roleReviewer: '审稿人',
      rolePublisher: '发布者',
      eventTaskCreated: '任务创建',
      eventStepStarted: '步骤开始',
      eventStepCompleted: '步骤完成',
      eventStepFailed: '步骤失败',
      eventToolCall: '工具调用',
      eventTaskCompleted: '任务完成',
      eventTaskCancelled: '任务取消',
      eventTaskRetry: '任务重试',
      eventTaskFailed: '任务失败',
      eventMemoryHit: '记忆命中',
      eventToolFail: '工具失败'
    };
  }

  return {
    title: 'Agent Ops',
    hint: 'Inspect agent tasks, steps, tool calls, and memory hits from admin view.',
    statusAll: 'All status',
    searchPlaceholder: 'Search by task title or goal',
    apply: 'Apply',
    reset: 'Reset',
    taskId: 'Task ID',
    titleCol: 'Title',
    goal: 'Goal',
    status: 'Status',
    type: 'Type',
    searchMode: 'Search mode',
    createdAt: 'Created At',
    updatedAt: 'Updated At',
    actions: 'Actions',
    previous: 'Previous',
    next: 'Next',
    totalPageInfo: '{total} total, page {page} / {pages}',
    openTrace: 'Open Trace',
    open: 'Open',
    taskSummary: 'Task summary',
    taskIdLabel: 'Task ID',
    sessionId: 'Session ID',
    taskType: 'Task type',
    executionMode: 'Execution mode',
    searchScope: 'Search scope',
    draftWrite: 'Allow draft write',
    finalOutput: 'Final summary',
    error: 'Error',
    stepIndex: 'Step',
    agentRole: 'Role',
    stepName: 'Step name',
    stepStatus: 'Status',
    stepOutput: 'Output summary',
    eventType: 'Event type',
    payloadSummary: 'Payload summary',
    toolCalls: 'Tool calls',
    memoryHits: 'Memory hits',
    noData: 'No data',
    toolName: 'Tool name',
    permissionLevel: 'Permission',
    success: 'Success',
    latency: 'Latency(ms)',
    topic: 'Topic',
    hitReason: 'Hit reason',
    usedInStep: 'Used in step',
    responseSummary: 'Response summary',
    pass: 'Pass',
    fail: 'Fail',
    trace: 'Task Trace',
    steps: 'Steps',
    events: 'Events',
    statusPending: 'Pending',
    statusRunning: 'Running',
    statusCompleted: 'Completed',
    statusFailed: 'Failed',
    statusCancelled: 'Cancelled',
    statusSkipped: 'Skipped',
    typeBlogDraft: 'Blog Draft',
    modeAuto: 'AUTO',
    modeResearchFirst: 'Research first',
    modeSinglePass: 'Single pass',
    scopeLocalOnly: 'Local only',
    scopeLocalAndWeb: 'Local + web',
    rolePlanner: 'Planner',
    roleResearcher: 'Researcher',
    roleWriter: 'Writer',
    roleReviewer: 'Reviewer',
    rolePublisher: 'Publisher',
    eventTaskCreated: 'Task created',
    eventStepStarted: 'Step started',
    eventStepCompleted: 'Step completed',
    eventStepFailed: 'Step failed',
    eventToolCall: 'Tool call',
    eventTaskCompleted: 'Task completed',
    eventTaskCancelled: 'Task cancelled',
    eventTaskRetry: 'Task retry',
    eventTaskFailed: 'Task failed',
    eventMemoryHit: 'Memory hit',
    eventToolFail: 'Tool failed'
  };
});

const statusOptions = computed(() => (preferences.locale === 'zh-CN'
  ? [
      { label: copy.value.statusPending, value: 'PENDING' },
      { label: copy.value.statusRunning, value: 'RUNNING' },
      { label: copy.value.statusCompleted, value: 'COMPLETED' },
      { label: copy.value.statusFailed, value: 'FAILED' },
      { label: copy.value.statusCancelled, value: 'CANCELLED' },
      { label: copy.value.statusSkipped, value: 'SKIPPED' }
    ]
  : [
      { label: copy.value.statusPending, value: 'PENDING' },
      { label: copy.value.statusRunning, value: 'RUNNING' },
      { label: copy.value.statusCompleted, value: 'COMPLETED' },
      { label: copy.value.statusFailed, value: 'FAILED' },
      { label: copy.value.statusCancelled, value: 'CANCELLED' },
      { label: copy.value.statusSkipped, value: 'SKIPPED' }
    ]));

const statusBadgeClass = (status) => {
  if (status === 'COMPLETED') {
    return 'admin-status-pill is-positive';
  }
  if (status === 'FAILED' || status === 'CANCELLED') {
    return 'admin-status-pill is-negative';
  }
  if (status === 'RUNNING') {
    return 'admin-status-pill is-running';
  }
  return 'admin-status-pill';
};

const statusText = (status) => {
  const map = {
    PENDING: copy.value.statusPending,
    RUNNING: copy.value.statusRunning,
    COMPLETED: copy.value.statusCompleted,
    FAILED: copy.value.statusFailed,
    CANCELLED: copy.value.statusCancelled,
    SKIPPED: copy.value.statusSkipped
  };
  return map[status] || status || '-';
};

const roleText = (value) => {
  const map = {
    PLANNER: copy.value.rolePlanner,
    RESEARCHER: copy.value.roleResearcher,
    WRITER: copy.value.roleWriter,
    REVIEWER: copy.value.roleReviewer,
    PUBLISHER: copy.value.rolePublisher
  };
  return map[value] || value || '-';
};

const eventText = (value) => {
  const map = {
    TASK_CREATED: copy.value.eventTaskCreated,
    STEP_STARTED: copy.value.eventStepStarted,
    STEP_COMPLETED: copy.value.eventStepCompleted,
    STEP_FAILED: copy.value.eventStepFailed,
    TOOL_CALL: copy.value.eventToolCall,
    TASK_COMPLETED: copy.value.eventTaskCompleted,
    TASK_CANCELLED: copy.value.eventTaskCancelled,
    TASK_RETRY: copy.value.eventTaskRetry,
    TASK_FAILED: copy.value.eventTaskFailed,
    MEMORY_HIT: copy.value.eventMemoryHit,
    TOOL_FAIL: copy.value.eventToolFail
  };
  return map[value] || value || '-';
};

const taskTypeText = (value) => {
  const map = {
    BLOG_DRAFT: copy.value.typeBlogDraft
  };
  return map[value] || value || '-';
};

const executionModeText = (value) => {
  const map = {
    AUTO: copy.value.modeAuto,
    RESEARCH_FIRST: copy.value.modeResearchFirst,
    SINGLE_PASS: copy.value.modeSinglePass
  };
  return map[value] || value || '-';
};

const searchScopeText = (value) => {
  const map = {
    LOCAL_ONLY: copy.value.scopeLocalOnly,
    LOCAL_AND_WEB: copy.value.scopeLocalAndWeb
  };
  return map[value] || value || '-';
};

const buildQueryParams = (page = pageData.value.page || 1) => ({
  status: filters.status || undefined,
  keyword: filters.keyword || undefined,
  page,
  pageSize: pageData.value.pageSize
});

const buildTraceParams = () => ({
  toolCallPage: traceData.value.toolCalls.page,
  toolCallPageSize: traceData.value.toolCalls.pageSize,
  memoryHitPage: traceData.value.memoryHits.page,
  memoryHitPageSize: traceData.value.memoryHits.pageSize
});

const loadTasks = async (page = pageData.value.page || 1) => {
  loading.value = true;
  try {
    const res = await fetchAdminAgentTasksApi(buildQueryParams(page));
    pageData.value = res.data;
  } finally {
    loading.value = false;
  }
};

const loadTrace = async (taskId = selectedTaskId.value) => {
  if (!taskId) {
    return;
  }
  traceLoading.value = true;
  try {
    const res = await fetchAdminAgentTaskTraceApi(taskId, buildTraceParams());
    const payload = res.data || {};
    traceData.value = {
      task: payload.task || null,
      steps: payload.steps || [],
      events: payload.events || [],
      toolCalls: payload.toolCalls || {
        records: [],
        page: 1,
        pageSize: 8,
        total: 0,
        totalPages: 0,
        hasNext: false
      },
      memoryHits: payload.memoryHits || {
        records: [],
        page: 1,
        pageSize: 8,
        total: 0,
        totalPages: 0,
        hasNext: false
      }
    };
  } finally {
    traceLoading.value = false;
  }
};

const search = async () => {
  await loadTasks(1);
};

const resetFilters = async () => {
  filters.status = '';
  filters.keyword = '';
  await loadTasks(1);
};

const prevPage = async () => {
  if (pageData.value.page > 1) {
    await loadTasks(pageData.value.page - 1);
  }
};

const nextPage = async () => {
  if (pageData.value.hasNext) {
    await loadTasks(pageData.value.page + 1);
  }
};

const openTraceDrawer = async (row) => {
  selectedTaskId.value = row.id;
  traceData.value.toolCalls.page = 1;
  traceData.value.memoryHits.page = 1;
  drawerVisible.value = true;
  activeTraceTab.value = 'steps';
  await loadTrace(row.id);
};

const closeTraceDrawer = () => {
  drawerVisible.value = false;
  selectedTaskId.value = null;
};

const beforeCloseTraceDrawer = (done) => {
  closeTraceDrawer();
  done();
};

const loadNextToolCalls = async (direction) => {
  const next = traceData.value.toolCalls.page + direction;
  if (next < 1 || (direction > 0 && !traceData.value.toolCalls.hasNext)) {
    return;
  }
  traceData.value.toolCalls.page = next;
  await loadTrace();
};

const loadNextMemoryHits = async (direction) => {
  const next = traceData.value.memoryHits.page + direction;
  if (next < 1 || (direction > 0 && !traceData.value.memoryHits.hasNext)) {
    return;
  }
  traceData.value.memoryHits.page = next;
  await loadTrace();
};

const openTaskSession = (sessionId) => {
  if (!sessionId) {
    return;
  }
  const target = router.resolve({ path: '/knowledge', query: { sessionId } });
  window.open(target.href, '_blank', 'noopener');
};

const successTagType = (success) => (success ? 'success' : 'danger');
const formatDatetime = (value) => preferences.formatDateTime(value);

onMounted(async () => {
  await loadTasks(1);
});
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack agent-ops-page" v-loading="loading">
    <div class="admin-page-head">
      <div>
        <span class="admin-kicker">Operations</span>
        <h2>{{ copy.title }}</h2>
        <p class="muted">{{ copy.hint }}</p>
      </div>
      <div class="admin-page-actions">
        <span class="admin-badge">{{ copy.totalPageInfo.replace('{total}', pageData.total || 0).replace('{page}', pageData.page || 1).replace('{pages}', pageData.totalPages || 1) }}</span>
      </div>
    </div>

    <div class="toolbar admin-toolbar manage-toolbar">
      <el-select v-model="filters.status" :placeholder="copy.statusAll" clearable>
        <el-option :label="copy.statusAll" value="" />
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="filters.keyword" :placeholder="copy.searchPlaceholder" @keyup.enter="search" />
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
        <el-table-column prop="id" :label="copy.taskId" width="120" />
        <el-table-column prop="title" :label="copy.titleCol" min-width="220" />
        <el-table-column prop="goal" :label="copy.goal" min-width="260" show-overflow-tooltip />
        <el-table-column :label="copy.status" width="130">
          <template #default="{ row }">
            <span :class="statusBadgeClass(row.status)">{{ statusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="copy.type" width="150">
          <template #default="{ row }">
            {{ taskTypeText(row.taskType) }}
          </template>
        </el-table-column>
        <el-table-column :label="copy.searchMode" width="160">
          <template #default="{ row }">
            {{ searchScopeText(row.searchScope) }}
          </template>
        </el-table-column>
        <el-table-column :label="copy.updatedAt" width="180">
          <template #default="{ row }">
            {{ formatDatetime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="copy.actions" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTraceDrawer(row)">{{ copy.openTrace }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="pageData.total > 0" class="pager">
      <el-button :disabled="pageData.page <= 1" @click="prevPage">{{ copy.previous }}</el-button>
      <el-button :disabled="!pageData.hasNext" type="primary" @click="nextPage">{{ copy.next }}</el-button>
    </div>

    <section v-if="drawerVisible" class="trace-inline-panel" v-loading="traceLoading">
      <div class="trace-inline-head">
        <div>
          <span class="admin-kicker">Trace</span>
          <h3>{{ copy.trace }}</h3>
        </div>
        <el-button size="small" @click="closeTraceDrawer">{{ copy.close || '关闭' }}</el-button>
      </div>
      <div v-if="traceData.task" class="trace-panel">
        <div class="trace-head">
          <h3>{{ copy.taskSummary }}</h3>
          <div class="trace-actions">
            <el-button size="small" @click="openTaskSession(traceData.task.sessionId)">{{ copy.open }}</el-button>
            <el-button size="small" @click="loadTrace">{{ copy.openTrace }}</el-button>
          </div>
        </div>

        <el-descriptions :column="2" border size="small" v-loading="traceLoading">
          <el-descriptions-item :label="copy.taskIdLabel">{{ traceData.task.id }}</el-descriptions-item>
          <el-descriptions-item :label="copy.sessionId">{{ traceData.task.sessionId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="copy.titleCol">{{ traceData.task.title || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="copy.goal">{{ traceData.task.goal || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="copy.status">{{ statusText(traceData.task.status) }}</el-descriptions-item>
          <el-descriptions-item :label="copy.taskType">{{ taskTypeText(traceData.task.taskType) }}</el-descriptions-item>
          <el-descriptions-item :label="copy.executionMode">{{ executionModeText(traceData.task.executionMode) }}</el-descriptions-item>
          <el-descriptions-item :label="copy.searchScope">{{ searchScopeText(traceData.task.searchScope) }}</el-descriptions-item>
          <el-descriptions-item :label="copy.draftWrite">{{ traceData.task.allowDraftWrite ? copy.pass : copy.fail }}</el-descriptions-item>
          <el-descriptions-item :label="copy.finalOutput">
            <div class="trace-long-text">{{ traceData.task.finalOutputSummary || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item :label="copy.error">
            <div class="trace-long-text">{{ traceData.task.errorMessage || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item :label="copy.createdAt">{{ formatDatetime(traceData.task.createdAt) }}</el-descriptions-item>
          <el-descriptions-item :label="copy.updatedAt">{{ formatDatetime(traceData.task.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTraceTab" class="agent-trace-tabs">
          <el-tab-pane :label="copy.steps" name="steps">
            <el-table :data="traceData.steps">
              <el-table-column :label="copy.stepIndex" width="90" prop="stepIndex" />
              <el-table-column :label="copy.agentRole" width="130">
                <template #default="{ row }">
                  {{ roleText(row.agentRole) }}
                </template>
              </el-table-column>
              <el-table-column :label="copy.stepName" min-width="220" prop="stepName" />
              <el-table-column :label="copy.stepStatus" width="120">
                <template #default="{ row }">
                  <span :class="statusBadgeClass(row.status)">{{ statusText(row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="copy.latency" width="110" prop="latencyMs" />
              <el-table-column :label="copy.stepOutput" min-width="260" prop="outputSummary" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="copy.events" name="events">
            <el-table :data="traceData.events">
              <el-table-column :label="copy.eventType" width="140">
                <template #default="{ row }">
                  {{ eventText(row.eventType) }}
                </template>
              </el-table-column>
              <el-table-column :label="copy.agentRole" width="120">
                <template #default="{ row }">
                  {{ roleText(row.agentRole) }}
                </template>
              </el-table-column>
              <el-table-column :label="copy.stepStatus" width="120">
                <template #default="{ row }">
                  <span :class="statusBadgeClass(row.status)">{{ statusText(row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="copy.payloadSummary" min-width="280" prop="payloadSummary" show-overflow-tooltip />
              <el-table-column :label="copy.latency" width="110" prop="latencyMs" />
              <el-table-column :label="copy.createdAt" width="180">
                <template #default="{ row }">
                  {{ formatDatetime(row.createdAt) }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="copy.toolCalls" name="tool-calls">
            <div class="admin-table-wrap">
              <el-table :data="traceData.toolCalls.records">
                <el-table-column prop="toolName" :label="copy.toolName" width="170" />
                <el-table-column prop="permissionLevel" :label="copy.permissionLevel" width="140" />
                <el-table-column :label="copy.success" width="90">
                  <template #default="{ row }">
                    <el-tag :type="successTagType(row.success)" size="small">{{ row.success ? copy.pass : copy.fail }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="copy.latency" width="120" prop="latencyMs" />
                <el-table-column :label="copy.responseSummary" min-width="240" prop="responseSummary" show-overflow-tooltip />
              </el-table>
            </div>
            <div class="pager">
              <el-button :disabled="traceData.toolCalls.page <= 1" @click="loadNextToolCalls(-1)">{{ copy.previous }}</el-button>
              <el-button :disabled="!traceData.toolCalls.hasNext" type="primary" @click="loadNextToolCalls(1)">
                {{ copy.next }}
              </el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="copy.memoryHits" name="memory-hits">
            <div class="admin-table-wrap">
              <el-table :data="traceData.memoryHits.records">
                <el-table-column :label="copy.topic" prop="topicKey" min-width="240" />
                <el-table-column :label="copy.hitReason" min-width="240" prop="hitReason" show-overflow-tooltip />
                <el-table-column :label="copy.usedInStep" min-width="170" prop="usedInStep" />
                <el-table-column :label="copy.createdAt" width="180">
                  <template #default="{ row }">
                    {{ formatDatetime(row.createdAt) }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="pager">
              <el-button :disabled="traceData.memoryHits.page <= 1" @click="loadNextMemoryHits(-1)">{{ copy.previous }}</el-button>
              <el-button :disabled="!traceData.memoryHits.hasNext" type="primary" @click="loadNextMemoryHits(1)">
                {{ copy.next }}
              </el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-else class="empty-trace">
        <p>{{ copy.noData }}</p>
      </div>
    </section>
  </section>
</template>

<style scoped>
.agent-ops-page {
  height: auto;
  overflow: visible;
}

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

.trace-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.trace-head h3 {
  margin: 0;
  font-size: 1.2rem;
}

.trace-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.trace-inline-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: min(76vh, 760px);
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: var(--admin-soft-bg);
  overflow: auto;
}

.trace-inline-head {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
  background: var(--admin-soft-bg);
}

.trace-inline-head h3 {
  margin: 4px 0 0;
  font-size: 1.25rem;
}

.trace-long-text {
  max-height: 160px;
  max-width: min(920px, 100%);
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.trace-actions {
  display: flex;
  gap: 8px;
}

.empty-trace {
  margin-top: 24px;
  color: var(--text-secondary);
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

@media (max-width: 960px) {
  .agent-ops-page {
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
