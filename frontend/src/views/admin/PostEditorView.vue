<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  fetchAdminPostApi,
  fetchCategoriesApi,
  fetchTagsApi,
  saveAdminPostApi
} from '../../api/admin';
import { renderMarkdown } from '../../utils/markdown';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const categories = ref([]);
const tags = ref([]);
const editorRef = ref(null);

const markdownSamples = computed(() => ({
  heading1: '# Title',
  heading2: '## Subtitle',
  list: '- Item 1\n- Item 2',
  quote: '> Quote text',
  code: '```java\nSystem.out.println("Hello");\n```',
  image: '![Image description](https://example.com/image.jpg)'
}));

const form = reactive({
  id: null,
  title: '',
  slug: '',
  summary: '',
  coverImage: '',
  content: '',
  status: 'PUBLISHED',
  featured: true,
  allowComment: true,
  categoryId: null,
  tagIds: []
});

const isEdit = computed(() => Boolean(route.params.id));
const renderedContent = computed(() => renderMarkdown(form.content));

const applyDefaultContent = () => {
  if (!form.content) {
    form.content = preferences.t('postEditor.defaultContent');
  }
};

const loadOptions = async () => {
  const [categoryRes, tagRes] = await Promise.all([fetchCategoriesApi(), fetchTagsApi()]);
  categories.value = categoryRes.data;
  tags.value = tagRes.data;
  if (!form.categoryId && categories.value.length) {
    form.categoryId = categories.value[0].id;
  }
};

const loadDetail = async () => {
  if (!isEdit.value) {
    applyDefaultContent();
    return;
  }
  const res = await fetchAdminPostApi(route.params.id);
  Object.assign(form, res.data);
};

const focusEditor = async (selectionStart, selectionEnd) => {
  await nextTick();
  const textarea = editorRef.value?.textarea;
  if (!textarea) {
    return;
  }
  textarea.focus();
  if (typeof selectionStart === 'number' && typeof selectionEnd === 'number') {
    textarea.setSelectionRange(selectionStart, selectionEnd);
  }
};

const wrapSelection = async (prefix, suffix = '', fallback = '') => {
  const textarea = editorRef.value?.textarea;
  if (!textarea) {
    form.content += `${prefix}${fallback}${suffix}`;
    return;
  }

  const start = textarea.selectionStart ?? form.content.length;
  const end = textarea.selectionEnd ?? form.content.length;
  const selected = form.content.slice(start, end) || fallback;
  form.content = `${form.content.slice(0, start)}${prefix}${selected}${suffix}${form.content.slice(end)}`;

  const nextStart = start + prefix.length;
  const nextEnd = nextStart + selected.length;
  await focusEditor(nextStart, nextEnd);
};

const insertBlock = async (snippet) => {
  const textarea = editorRef.value?.textarea;
  if (!textarea) {
    form.content += `\n${snippet}\n`;
    return;
  }

  const start = textarea.selectionStart ?? form.content.length;
  const end = textarea.selectionEnd ?? form.content.length;
  const needsLeadingBreak = start > 0 && !form.content.slice(0, start).endsWith('\n') ? '\n' : '';
  const needsTrailingBreak = end < form.content.length && !form.content.slice(end).startsWith('\n') ? '\n' : '';
  const insertion = `${needsLeadingBreak}${snippet}${needsTrailingBreak}`;

  form.content = `${form.content.slice(0, start)}${insertion}${form.content.slice(end)}`;
  const cursor = start + insertion.length;
  await focusEditor(cursor, cursor);
};

const save = async () => {
  loading.value = true;
  try {
    await saveAdminPostApi(form);
    ElMessage.success(preferences.t('postEditor.postSaved'));
    router.push('/admin/posts');
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await loadOptions();
  await loadDetail();
});
</script>

<template>
  <section class="section-card panel admin-surface" v-loading="loading">
    <div class="section-heading">
      <h2>{{ isEdit ? preferences.t('postEditor.editPost') : preferences.t('postEditor.newPost') }}</h2>
      <el-button @click="router.push('/admin/posts')">{{ preferences.t('postEditor.backToList') }}</el-button>
    </div>

    <el-form label-position="top">
      <div class="editor-grid">
        <div class="editor-main">
          <el-form-item :label="preferences.t('postEditor.title')">
            <el-input v-model="form.title" />
          </el-form-item>

          <el-form-item :label="preferences.t('postEditor.slug')">
            <el-input v-model="form.slug" :placeholder="preferences.t('postEditor.slugPlaceholder')" />
          </el-form-item>

          <el-form-item :label="preferences.t('postEditor.summary')">
            <el-input v-model="form.summary" type="textarea" :rows="3" />
          </el-form-item>

          <el-form-item :label="preferences.t('postEditor.coverImageUrl')">
            <el-input v-model="form.coverImage" />
          </el-form-item>

          <div class="markdown-header">
            <span class="editor-label">{{ preferences.t('postEditor.content') }}</span>
            <span class="editor-hint">{{ preferences.t('postEditor.livePreview') }}</span>
          </div>

          <div class="toolbar">
            <el-button text @click="insertBlock(markdownSamples.heading1)">H1</el-button>
            <el-button text @click="insertBlock(markdownSamples.heading2)">H2</el-button>
            <el-button text @click="wrapSelection('**', '**', preferences.t('postEditor.bold'))">{{ preferences.t('postEditor.bold') }}</el-button>
            <el-button text @click="wrapSelection('*', '*', preferences.t('postEditor.italic'))">{{ preferences.t('postEditor.italic') }}</el-button>
            <el-button text @click="wrapSelection('[', '](https://example.com)', preferences.t('postEditor.link'))">{{ preferences.t('postEditor.link') }}</el-button>
            <el-button text @click="insertBlock(markdownSamples.list)">{{ preferences.t('postEditor.list') }}</el-button>
            <el-button text @click="insertBlock(markdownSamples.quote)">{{ preferences.t('postEditor.quote') }}</el-button>
            <el-button text @click="insertBlock(markdownSamples.code)">{{ preferences.t('postEditor.code') }}</el-button>
            <el-button text @click="insertBlock(markdownSamples.image)">{{ preferences.t('postEditor.image') }}</el-button>
          </div>

          <div class="markdown-layout">
            <div class="markdown-pane">
              <div class="pane-title">{{ preferences.t('postEditor.markdown') }}</div>
              <el-input
                ref="editorRef"
                v-model="form.content"
                type="textarea"
                :rows="22"
                resize="none"
                class="markdown-input"
                :placeholder="preferences.t('postEditor.writeMarkdown')"
              />
            </div>

            <div class="markdown-pane preview-pane">
              <div class="pane-title">{{ preferences.t('postEditor.preview') }}</div>
              <div class="markdown-preview markdown-body" v-html="renderedContent"></div>
            </div>
          </div>
        </div>

        <aside class="editor-side">
          <el-form-item :label="preferences.t('postEditor.category')">
            <el-select v-model="form.categoryId" class="w-full">
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="preferences.t('postEditor.tags')">
            <el-select v-model="form.tagIds" multiple class="w-full">
              <el-option
                v-for="item in tags"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="preferences.t('postEditor.status')">
            <el-radio-group v-model="form.status">
              <el-radio-button value="PUBLISHED">{{ preferences.t('postEditor.published') }}</el-radio-button>
              <el-radio-button value="DRAFT">{{ preferences.t('postEditor.draft') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item :label="preferences.t('postEditor.featured')">
            <el-switch v-model="form.featured" />
          </el-form-item>

          <el-form-item :label="preferences.t('postEditor.allowComments')">
            <el-switch v-model="form.allowComment" />
          </el-form-item>

          <el-button type="primary" @click="save">{{ preferences.t('postEditor.savePost') }}</el-button>
        </aside>
      </div>
    </el-form>
  </section>
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

.editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.9fr);
  gap: 24px;
}

.editor-main {
  min-width: 0;
}

.editor-side {
  align-self: start;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.025), rgba(255, 255, 255, 0.01)),
    rgba(6, 6, 6, 0.96);
}

.editor-side :deep(.el-form-item:last-of-type) {
  margin-bottom: 0;
}

.w-full {
  width: 100%;
}

.markdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.editor-label {
  font-weight: 600;
  color: var(--text-primary);
}

.editor-hint {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.03);
}

.markdown-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

.markdown-pane {
  min-width: 0;
}

.pane-title {
  margin-bottom: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.preview-pane {
  display: flex;
  flex-direction: column;
}

.markdown-preview {
  min-height: 520px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(8, 8, 8, 0.96);
  overflow: auto;
}

:deep(.markdown-input textarea) {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  line-height: 1.7;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 1.2em 0 0.6em;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol),
.markdown-body :deep(blockquote) {
  line-height: 1.8;
}

.markdown-body :deep(pre) {
  overflow: auto;
  padding: 14px;
  border-radius: 12px;
  background: #000;
  color: #f5f5f5;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.markdown-body :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
}

.markdown-body :deep(blockquote) {
  margin: 0;
  padding: 0.5rem 0 0.5rem 1rem;
  border-left: 4px solid rgba(255, 255, 255, 0.3);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.05);
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 12px;
}

@media (max-width: 1200px) {
  .markdown-layout {
    grid-template-columns: 1fr;
  }

  .markdown-preview {
    min-height: 340px;
  }
}

@media (max-width: 960px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
