<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox, ElMessage } from 'element-plus';
import { deleteAdminPostApi, fetchAdminPostsApi, fetchCategoriesApi } from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const router = useRouter();
const loading = ref(true);
const categories = ref([]);
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
  status: '',
  categoryId: ''
});

const loadCategories = async () => {
  const res = await fetchCategoriesApi();
  categories.value = res.data;
};

const loadData = async (page = pageData.value.page || 1) => {
  loading.value = true;
  try {
    const res = await fetchAdminPostsApi({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      categoryId: filters.categoryId || undefined,
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
  filters.categoryId = '';
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
  await loadCategories();
  await loadData(1);
});
</script>

<template>
  <section class="section-card panel admin-surface" v-loading="loading">
    <div class="section-heading">
      <div>
        <h2>{{ preferences.t('postManage.title') }}</h2>
        <p class="muted subtext">{{ preferences.t('postManage.hint') }}</p>
      </div>
      <el-button type="primary" @click="router.push('/admin/posts/new')">{{ preferences.t('postManage.newPost') }}</el-button>
    </div>

    <div class="toolbar">
      <el-input
        v-model="filters.keyword"
        :placeholder="preferences.t('postManage.searchPlaceholder')"
        @keyup.enter="search"
      />
      <el-select v-model="filters.status" :placeholder="preferences.t('postManage.allStatus')" clearable>
        <el-option :label="preferences.t('postManage.published')" value="PUBLISHED" />
        <el-option :label="preferences.t('postManage.draft')" value="DRAFT" />
      </el-select>
      <el-select v-model="filters.categoryId" :placeholder="preferences.t('postManage.allCategories')" clearable>
        <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="primary" @click="search">{{ preferences.t('postManage.apply') }}</el-button>
      <el-button @click="resetFilters">{{ preferences.t('postManage.reset') }}</el-button>
    </div>

    <div class="table-meta muted">
      {{ preferences.t('postManage.totalPageInfo', { total: pageData.total, page: pageData.page, pages: pageData.totalPages || 1 }) }}
    </div>

    <el-table :data="pageData.records">
      <el-table-column prop="title" :label="preferences.t('postManage.colTitle')" min-width="220" />
      <el-table-column prop="categoryName" :label="preferences.t('postManage.colCategory')" width="140" />
      <el-table-column :label="preferences.t('postManage.colStatus')" width="120">
        <template #default="{ row }">
          {{ statusLabel(row.status) }}
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

    <div v-if="pageData.total > 0" class="pager">
      <el-button :disabled="pageData.page <= 1" @click="prevPage">{{ preferences.t('postManage.previous') }}</el-button>
      <el-button :disabled="!pageData.hasNext" type="primary" @click="nextPage">{{ preferences.t('postManage.next') }}</el-button>
    </div>
  </section>
</template>

<style scoped>
.panel {
  padding: 24px;
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

.admin-surface :deep(.el-table) {
  border-radius: 18px;
  overflow: hidden;
}

.subtext {
  margin: 8px 0 0;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 160px 180px auto auto;
  gap: 12px;
  margin-bottom: 16px;
}

.table-meta {
  margin-bottom: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-size: 0.8rem;
}

.pager {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 960px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
