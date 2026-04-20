<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox, ElMessage } from 'element-plus';
import { deleteAdminPostApi, fetchAdminPostsApi } from '../../api/admin';
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
  keyword: '',
  status: ''
});

const loadData = async (page = pageData.value.page || 1) => {
  loading.value = true;
  try {
    const res = await fetchAdminPostsApi({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page,
      pageSize: 10
    });
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
  filters.status = '';
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

const removePost = async (id) => {
  await ElMessageBox.confirm(
    preferences.t('postManage.deleteConfirmBody'),
    preferences.t('postManage.deleteConfirmTitle'),
    { type: 'warning' }
  );
  await deleteAdminPostApi(id);
  ElMessage.success(preferences.t('postManage.postDeleted'));

  const targetPage = pageData.value.records.length === 1 && pageData.value.page > 1
    ? pageData.value.page - 1
    : pageData.value.page;

  await loadData(targetPage);
};

const statusLabel = (status) => {
  return status === 'PUBLISHED'
    ? preferences.t('postManage.published')
    : preferences.t('postManage.draft');
};

onMounted(async () => {
  await loadData(1);
});
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack" v-loading="loading">
    <div class="admin-page-head">
      <div>
        <span class="admin-kicker">Publishing</span>
        <h2>{{ preferences.t('postManage.title') }}</h2>
        <p class="muted">{{ preferences.t('postManage.hint') }}</p>
      </div>
      <div class="admin-page-actions">
        <span class="admin-badge">{{ preferences.t('postManage.totalPageInfo', { total: pageData.total, page: pageData.page, pages: pageData.totalPages || 1 }) }}</span>
        <el-button type="primary" @click="router.push('/admin/posts/new')">{{ preferences.t('postManage.newPost') }}</el-button>
      </div>
    </div>

    <div class="toolbar admin-toolbar manage-toolbar">
      <el-input
        v-model="filters.keyword"
        :placeholder="preferences.t('postManage.searchPlaceholder')"
        @keyup.enter="search"
      />
      <el-select v-model="filters.status" :placeholder="preferences.t('postManage.allStatus')" clearable>
        <el-option :label="preferences.t('postManage.published')" value="PUBLISHED" />
        <el-option :label="preferences.t('postManage.draft')" value="DRAFT" />
      </el-select>
      <el-button type="primary" @click="search">{{ preferences.t('postManage.apply') }}</el-button>
      <el-button @click="resetFilters">{{ preferences.t('postManage.reset') }}</el-button>
    </div>

    <div class="admin-meta-row table-meta muted">
      <span>{{ preferences.t('postManage.totalPageInfo', { total: pageData.total, page: pageData.page, pages: pageData.totalPages || 1 }) }}</span>
    </div>

    <div class="admin-table-wrap">
      <el-table :data="pageData.records">
        <el-table-column prop="title" :label="preferences.t('postManage.colTitle')" min-width="220" />
        <el-table-column :label="preferences.t('postManage.colTags')" min-width="220">
          <template #default="{ row }">
            <div class="tag-cell">
              <span v-for="tag in row.tags || []" :key="`${row.id}-${tag}`" class="chip"># {{ tag }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="preferences.t('postManage.colStatus')" width="120">
          <template #default="{ row }">
            <span class="admin-status-pill">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" :label="preferences.t('postManage.colViews')" width="100" />
        <el-table-column :label="preferences.t('postManage.colUpdatedAt')" width="180">
          <template #default="{ row }">
            {{ preferences.formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="preferences.t('postManage.colActions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/admin/posts/${row.id}/edit`)">{{ preferences.t('postManage.edit') }}</el-button>
            <el-button link type="danger" @click="removePost(row.id)">{{ preferences.t('postManage.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="pageData.total > 0" class="pager">
      <el-button :disabled="pageData.page <= 1" @click="prevPage">{{ preferences.t('postManage.previous') }}</el-button>
      <el-button :disabled="!pageData.hasNext" type="primary" @click="nextPage">{{ preferences.t('postManage.next') }}</el-button>
    </div>
  </section>
</template>

<style scoped>
.manage-toolbar {
  grid-template-columns: minmax(0, 1.8fr) 180px auto auto;
}

.admin-surface :deep(.el-input__wrapper),
.admin-surface :deep(.el-select__wrapper),
.admin-surface :deep(.el-textarea__inner) {
  background: var(--input-bg) !important;
  box-shadow: 0 0 0 1px var(--input-line) inset !important;
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

.tag-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 960px) {
  .manage-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
