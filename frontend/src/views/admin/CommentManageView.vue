<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchAdminCommentsApi, reviewCommentApi } from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const comments = ref([]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchAdminCommentsApi();
    comments.value = res.data;
  } finally {
    loading.value = false;
  }
};

const review = async (id, status) => {
  await reviewCommentApi(id, status);
  ElMessage.success(preferences.t('commentManage.statusUpdated'));
  await loadData();
};

const statusTone = (status) => {
  if (status === 'APPROVED') {
    return 'status-pill status-approved';
  }
  if (status === 'REJECTED') {
    return 'status-pill status-rejected';
  }
  return 'status-pill';
};

const statusLabel = (status) => {
  if (status === 'APPROVED') {
    return preferences.t('commentManage.approve');
  }
  if (status === 'REJECTED') {
    return preferences.t('commentManage.reject');
  }
  return status;
};

onMounted(loadData);
</script>

<template>
  <section class="section-card panel admin-surface" v-loading="loading">
    <div class="section-heading">
      <div>
        <h2>{{ preferences.t('commentManage.title') }}</h2>
        <p class="muted panel-hint">{{ preferences.t('commentManage.reviewHint') }}</p>
      </div>
      <span class="muted">{{ preferences.t('commentManage.records', { count: comments.length }) }}</span>
    </div>

    <el-table :data="comments">
      <el-table-column prop="nickname" :label="preferences.t('commentManage.nickname')" width="120" />
      <el-table-column prop="postTitle" :label="preferences.t('commentManage.post')" min-width="200" />
      <el-table-column prop="content" :label="preferences.t('commentManage.comment')" min-width="260" />
      <el-table-column :label="preferences.t('commentManage.status')" width="140">
        <template #default="{ row }">
          <span :class="statusTone(row.status)">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="preferences.t('commentManage.submittedAt')" width="180">
        <template #default="{ row }">
          {{ preferences.formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="preferences.t('commentManage.actions')" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="review(row.id, 'APPROVED')">{{ preferences.t('commentManage.approve') }}</el-button>
          <el-button link type="danger" @click="review(row.id, 'REJECTED')">{{ preferences.t('commentManage.reject') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.panel {
  padding: 24px;
}

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

.admin-surface :deep(.el-table) {
  border-radius: 18px;
  overflow: hidden;
}

.panel-hint {
  margin: 8px 0 0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.04);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.72rem;
}

.status-approved {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.22);
}

.status-rejected {
  color: #c2c2c2;
}
</style>
