<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  deleteCategoryApi,
  deleteTagApi,
  fetchCategoriesApi,
  fetchTagsApi,
  saveCategoryApi,
  saveTagApi
} from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const categories = ref([]);
const tags = ref([]);
const categoryForm = reactive({ id: null, name: '', slug: '', description: '' });
const tagForm = reactive({ id: null, name: '', slug: '' });

const resetCategory = () => Object.assign(categoryForm, { id: null, name: '', slug: '', description: '' });
const resetTag = () => Object.assign(tagForm, { id: null, name: '', slug: '' });

const loadData = async () => {
  loading.value = true;
  try {
    const [categoryRes, tagRes] = await Promise.all([fetchCategoriesApi(), fetchTagsApi()]);
    categories.value = categoryRes.data;
    tags.value = tagRes.data;
  } finally {
    loading.value = false;
  }
};

const saveCategory = async () => {
  await saveCategoryApi(categoryForm);
  ElMessage.success(preferences.t('taxonomy.categorySaved'));
  resetCategory();
  await loadData();
};

const saveTag = async () => {
  await saveTagApi(tagForm);
  ElMessage.success(preferences.t('taxonomy.tagSaved'));
  resetTag();
  await loadData();
};

const removeCategory = async (id) => {
  await deleteCategoryApi(id);
  ElMessage.success(preferences.t('taxonomy.categoryDeleted'));
  await loadData();
};

const removeTag = async (id) => {
  await deleteTagApi(id);
  ElMessage.success(preferences.t('taxonomy.tagDeleted'));
  await loadData();
};

onMounted(loadData);
</script>

<template>
  <div class="split-grid" v-loading="loading">
    <section class="section-card panel admin-surface">
      <div class="section-heading">
        <h2>{{ preferences.t('taxonomy.categoryDesign') }}</h2>
      </div>

      <el-form label-position="top">
        <el-form-item :label="preferences.t('taxonomy.name')">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item :label="preferences.t('taxonomy.slug')">
          <el-input v-model="categoryForm.slug" />
        </el-form-item>
        <el-form-item :label="preferences.t('taxonomy.description')">
          <el-input v-model="categoryForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <div class="action-row">
          <el-button type="primary" @click="saveCategory">{{ preferences.t('taxonomy.saveCategory') }}</el-button>
          <el-button @click="resetCategory">{{ preferences.t('taxonomy.clear') }}</el-button>
        </div>
      </el-form>

      <div class="stack">
        <div v-for="item in categories" :key="item.id" class="inline-item">
          <div>
            <strong>{{ item.name }}</strong>
            <p class="muted">{{ item.description || item.slug }}</p>
          </div>
          <div class="chip-list">
            <el-button link type="primary" @click="Object.assign(categoryForm, item)">{{ preferences.t('taxonomy.edit') }}</el-button>
            <el-button link type="danger" @click="removeCategory(item.id)">{{ preferences.t('taxonomy.delete') }}</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="section-card panel admin-surface">
      <div class="section-heading">
        <h2>{{ preferences.t('taxonomy.tagDesign') }}</h2>
      </div>

      <el-form label-position="top">
        <el-form-item :label="preferences.t('taxonomy.name')">
          <el-input v-model="tagForm.name" />
        </el-form-item>
        <el-form-item :label="preferences.t('taxonomy.slug')">
          <el-input v-model="tagForm.slug" />
        </el-form-item>
        <div class="action-row">
          <el-button type="primary" @click="saveTag">{{ preferences.t('taxonomy.saveTag') }}</el-button>
          <el-button @click="resetTag">{{ preferences.t('taxonomy.clear') }}</el-button>
        </div>
      </el-form>

      <div class="stack">
        <div v-for="item in tags" :key="item.id" class="inline-item">
          <div>
            <strong>{{ item.name }}</strong>
            <p class="muted">{{ item.slug }}</p>
          </div>
          <div class="chip-list">
            <el-button link type="primary" @click="Object.assign(tagForm, item)">{{ preferences.t('taxonomy.edit') }}</el-button>
            <el-button link type="danger" @click="removeTag(item.id)">{{ preferences.t('taxonomy.delete') }}</el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.panel {
  padding: 22px;
}

.admin-surface :deep(.el-input__wrapper),
.admin-surface :deep(.el-select__wrapper),
.admin-surface :deep(.el-textarea__inner) {
  background: var(--input-bg) !important;
  box-shadow: 0 0 0 1px var(--input-line) inset !important;
}

.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 22px;
}

.action-row {
  display: flex;
  gap: 10px;
}

.inline-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.inline-item p {
  margin: 6px 0 0;
}
</style>
