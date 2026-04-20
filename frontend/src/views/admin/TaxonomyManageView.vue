<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  deleteTagApi,
  fetchTagsApi,
  saveTagApi
} from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const tags = ref([]);
const tagForm = reactive({ id: null, name: '', slug: '' });

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      kicker: '标签管理',
      title: '标签审核与整理',
      hint: '文章标签现在支持自动创建，这里用于修正命名、补充 slug，或删除不合适的标签。',
      formTitle: '标签信息',
      listTitle: '当前标签',
      name: '标签名',
      slug: 'Slug',
      save: '保存标签',
      clear: '清空',
      edit: '编辑',
      delete: '删除',
      deleteTitle: '删除标签',
      deleteConfirm: '删除后会同步移除文章上的该标签关联，确认继续吗？',
      empty: '当前还没有标签。',
      saved: '标签已保存',
      deleted: '标签已删除',
      records: '{count} 个标签',
      posts: '{count} 篇文章',
      slugHint: '留空时会根据标签名自动生成。'
    };
  }

  return {
    kicker: 'Tag Moderation',
    title: 'Review and manage tags',
    hint: 'Tags can now be created automatically from article input. Use this page to rename, refine slugs, or remove unsuitable tags.',
    formTitle: 'Tag Details',
    listTitle: 'Current Tags',
    name: 'Name',
    slug: 'Slug',
    save: 'Save Tag',
    clear: 'Clear',
    edit: 'Edit',
    delete: 'Delete',
    deleteTitle: 'Delete Tag',
    deleteConfirm: 'Deleting a tag also removes its post associations. Continue?',
    empty: 'No tags yet.',
    saved: 'Tag saved',
    deleted: 'Tag deleted',
    records: '{count} tags',
    posts: '{count} posts',
    slugHint: 'Leave blank to generate it from the tag name.'
  };
});

function resetTag() {
  Object.assign(tagForm, { id: null, name: '', slug: '' });
}

async function loadData() {
  loading.value = true;
  try {
    const res = await fetchTagsApi();
    tags.value = res.data || [];
  } finally {
    loading.value = false;
  }
}

async function saveTag() {
  await saveTagApi(tagForm);
  ElMessage.success(copy.value.saved);
  resetTag();
  await loadData();
}

function editTag(tag) {
  Object.assign(tagForm, {
    id: tag.id,
    name: tag.name || '',
    slug: tag.slug || ''
  });
}

async function removeTag(id) {
  await ElMessageBox.confirm(copy.value.deleteConfirm, copy.value.deleteTitle, { type: 'warning' });
  await deleteTagApi(id);
  ElMessage.success(copy.value.deleted);
  if (tagForm.id === id) {
    resetTag();
  }
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack" v-loading="loading">
    <div class="admin-page-head">
      <div>
        <span class="admin-kicker">{{ copy.kicker }}</span>
        <h2>{{ copy.title }}</h2>
        <p class="muted">{{ copy.hint }}</p>
      </div>
      <div class="admin-page-actions">
        <span class="admin-badge">{{ copy.records.replace('{count}', tags.length || 0) }}</span>
      </div>
    </div>

    <div class="taxonomy-grid">
      <section class="admin-side-card taxonomy-form-card">
        <div class="section-heading refined-heading compact-heading">
          <h2>{{ copy.formTitle }}</h2>
        </div>

        <el-form label-position="top">
          <el-form-item :label="copy.name">
            <el-input v-model="tagForm.name" />
          </el-form-item>
          <el-form-item :label="copy.slug">
            <el-input v-model="tagForm.slug" />
            <div class="field-hint">{{ copy.slugHint }}</div>
          </el-form-item>
          <div class="action-row">
            <el-button type="primary" @click="saveTag">{{ copy.save }}</el-button>
            <el-button @click="resetTag">{{ copy.clear }}</el-button>
          </div>
        </el-form>
      </section>

      <section class="admin-side-card taxonomy-list-card">
        <div class="section-heading refined-heading compact-heading">
          <h2>{{ copy.listTitle }}</h2>
        </div>

        <div v-if="tags.length" class="admin-list taxonomy-list">
          <div v-for="item in tags" :key="item.id" class="admin-list-row taxonomy-row">
            <div class="admin-list-copy">
              <div class="taxonomy-row-head">
                <strong># {{ item.name }}</strong>
                <span class="muted taxonomy-meta">{{ copy.posts.replace('{count}', item.postCount || 0) }}</span>
              </div>
              <p class="muted taxonomy-slug">{{ item.slug }}</p>
            </div>
            <div class="chip-list">
              <el-button link type="primary" @click="editTag(item)">{{ copy.edit }}</el-button>
              <el-button link type="danger" @click="removeTag(item.id)">{{ copy.delete }}</el-button>
            </div>
          </div>
        </div>
        <p v-else class="muted">{{ copy.empty }}</p>
      </section>
    </div>
  </section>
</template>

<style scoped>
.admin-surface :deep(.el-input__wrapper),
.admin-surface :deep(.el-select__wrapper),
.admin-surface :deep(.el-textarea__inner) {
  background: var(--input-bg) !important;
  box-shadow: 0 0 0 1px var(--input-line) inset !important;
}

.taxonomy-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.4fr);
  gap: 20px;
  align-items: start;
}

.taxonomy-form-card,
.taxonomy-list-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.compact-heading {
  margin-bottom: 0;
}

.field-hint {
  margin-top: 8px;
  font-size: 0.84rem;
  color: var(--text-secondary);
}

.action-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.taxonomy-list {
  gap: 14px;
}

.taxonomy-row {
  gap: 14px;
}

.taxonomy-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.taxonomy-meta,
.taxonomy-slug {
  margin: 0;
}

@media (max-width: 960px) {
  .taxonomy-grid {
    grid-template-columns: 1fr;
  }
}
</style>
