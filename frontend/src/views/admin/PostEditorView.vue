<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  fetchAdminPostApi,
  saveAdminPostApi,
  uploadAdminImageApi
} from '../../api/admin';
import { renderMarkdown } from '../../utils/markdown';
import { usePreferencesStore } from '../../stores/preferences';

const HASH_TAG_PATTERN = /(^|[^\p{L}\p{N}_-])#([\p{L}\p{N}][\p{L}\p{N}_-]{0,31})/gu;
const TAG_SPLIT_PATTERN = /[,，;；\n]+/g;

const preferences = usePreferencesStore();
const route = useRoute();
const router = useRouter();

const loading = ref(false);
const coverUploading = ref(false);
const inlineImageUploading = ref(false);
const editorRef = ref(null);
const coverInputRef = ref(null);
const inlineImageInputRef = ref(null);

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      kicker: '编辑器',
      composeKicker: '正文创作',
      settingsKicker: '发布设置',
      basicInfoKicker: '文章信息',
      editPost: '编辑文章',
      newPost: '新建文章',
      backToList: '返回列表',
      livePreview: '实时预览',
      basicInfoHint: '先整理标题、摘要和封面，再进入正文编辑。',
      settingsHint: '标签可手动输入，也会自动识别正文中的 #标签。',
      contentHint: '左侧编辑正文，右侧同步预览渲染结果。',
      title: '标题',
      slug: 'Slug',
      slugPlaceholder: '留空则自动生成',
      summary: '摘要',
      coverImage: '封面图片',
      coverHint: '封面图片将上传到服务器并返回可访问地址。',
      uploadCover: '上传封面',
      replaceCover: '重新上传',
      coverUploaded: '封面图片上传完成',
      content: '正文',
      tags: '标签',
      tagsPlaceholder: '例如：Vue 3, Spring Boot, #RAG',
      tagsHint: '支持逗号、分号或换行分隔；正文中的 #标签 会自动加入。',
      detectedTags: '自动识别标签',
      status: '状态',
      published: '已发布',
      draft: '草稿',
      featured: '精选推荐',
      allowComments: '允许评论',
      toolbarHint: '常用 Markdown 快捷插入',
      bold: '加粗',
      italic: '斜体',
      link: '链接',
      list: '列表',
      quote: '引用',
      code: '代码',
      image: '图片模板',
      uploadInlineImage: '上传正文图片',
      inlineImageAlt: '图片描述',
      inlineImageUploaded: '正文图片已插入',
      markdown: 'Markdown',
      preview: '预览',
      writeMarkdown: '在这里输入 Markdown 正文内容',
      savePost: '保存文章',
      postSaved: '文章已保存',
      defaultContent: `## 开始写作

这篇文章支持 **Markdown** 实时预览，也支持上传正文图片。

- 使用工具栏插入常见格式
- 在正文中写入 #标签 会自动加入标签列表
- 封面图和正文图片都会上传到服务器

> 先写清楚主题，再补充示例与截图。
`,
      uploadFailed: '图片上传失败'
    };
  }

  return {
    kicker: 'Editor',
    composeKicker: 'Compose',
    settingsKicker: 'Settings',
    basicInfoKicker: 'Post Details',
    editPost: 'Edit Post',
    newPost: 'New Post',
    backToList: 'Back to List',
    livePreview: 'Live Preview',
    basicInfoHint: 'Set the title, summary, and cover first, then move into the article body.',
    settingsHint: 'Tags can be typed manually and are also detected from #hashtags in the article body.',
    contentHint: 'Write on the left and review the rendered preview on the right.',
    title: 'Title',
    slug: 'Slug',
    slugPlaceholder: 'Auto generated when left empty',
    summary: 'Summary',
    coverImage: 'Cover Image',
    coverHint: 'The cover image is uploaded to the server and stored as a reusable URL.',
    uploadCover: 'Upload Cover',
    replaceCover: 'Replace Cover',
    coverUploaded: 'Cover image uploaded',
    content: 'Content',
    tags: 'Tags',
    tagsPlaceholder: 'Example: Vue 3, Spring Boot, #RAG',
    tagsHint: 'Use commas, semicolons, or new lines; #hashtags in content are added automatically.',
    detectedTags: 'Detected tags',
    status: 'Status',
    published: 'Published',
    draft: 'Draft',
    featured: 'Featured',
    allowComments: 'Allow Comments',
    toolbarHint: 'Common Markdown shortcuts',
    bold: 'Bold',
    italic: 'Italic',
    link: 'Link',
    list: 'List',
    quote: 'Quote',
    code: 'Code',
    image: 'Image Template',
    uploadInlineImage: 'Upload Inline Image',
    inlineImageAlt: 'Image',
    inlineImageUploaded: 'Inline image inserted',
    markdown: 'Markdown',
    preview: 'Preview',
    writeMarkdown: 'Write Markdown content here',
    savePost: 'Save Post',
    postSaved: 'Post saved',
    defaultContent: `## Start writing

This editor supports **Markdown** live preview and inline image uploads.

- Use the toolbar for common formatting
- Write #hashtags in the content to auto-add tags
- Cover and inline images are uploaded to the server

> Start with the main idea, then add examples and screenshots.
`,
    uploadFailed: 'Upload failed'
  };
});

const markdownSamples = computed(() => ({
  heading1: '# Title',
  heading2: '## Subtitle',
  list: '- Item 1\n- Item 2',
  quote: '> Quote text',
  code: '```java\nSystem.out.println("Hello");\n```',
  image: `![${copy.value.inlineImageAlt}](https://example.com/image.jpg)`
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
  tagsText: ''
});

const isEdit = computed(() => Boolean(route.params.id));
const renderedContent = computed(() => renderMarkdown(form.content));
const manualTags = computed(() => parseManualTags(form.tagsText));
const contentTags = computed(() => extractHashTags(form.content));
const mergedTags = computed(() => mergeTags(manualTags.value, contentTags.value));
const detectedOnlyTags = computed(() => {
  const manualSet = new Set(manualTags.value.map((item) => item.toLowerCase()));
  return contentTags.value.filter((item) => !manualSet.has(item.toLowerCase()));
});

function normalizeTag(value) {
  return String(value || '')
    .trim()
    .replace(/^#+/, '')
    .replace(/[`'"“”‘’]+/g, '')
    .replace(/\s{2,}/g, ' ')
    .replace(/^[\p{P}\p{S}\s]+|[\p{P}\p{S}\s]+$/gu, '');
}

function mergeTags(...groups) {
  const tagMap = new Map();
  groups.flat().forEach((item) => {
    const normalized = normalizeTag(item);
    if (!normalized) {
      return;
    }

    const key = normalized.toLowerCase();
    if (!tagMap.has(key)) {
      tagMap.set(key, normalized);
    }
  });
  return Array.from(tagMap.values());
}

function parseManualTags(value) {
  return mergeTags(
    String(value || '')
      .split(TAG_SPLIT_PATTERN)
      .map(normalizeTag)
      .filter(Boolean)
  );
}

function extractHashTags(value) {
  const content = String(value || '');
  const tags = [];
  for (const match of content.matchAll(HASH_TAG_PATTERN)) {
    const normalized = normalizeTag(match[2]);
    if (normalized) {
      tags.push(normalized);
    }
  }
  return mergeTags(tags);
}

function applyDefaultContent() {
  if (!form.content) {
    form.content = copy.value.defaultContent;
  }
}

async function loadDetail() {
  if (!isEdit.value) {
    applyDefaultContent();
    return;
  }

  loading.value = true;
  try {
    const res = await fetchAdminPostApi(route.params.id);
    form.id = res.data.id;
    form.title = res.data.title || '';
    form.slug = res.data.slug || '';
    form.summary = res.data.summary || '';
    form.coverImage = res.data.coverImage || '';
    form.content = res.data.content || '';
    form.status = res.data.status || 'PUBLISHED';
    form.featured = Boolean(res.data.featured);
    form.allowComment = Boolean(res.data.allowComment);
    form.tagsText = (res.data.tags || []).join(', ');
  } finally {
    loading.value = false;
  }
}

async function focusEditor(selectionStart, selectionEnd) {
  await nextTick();
  const textarea = editorRef.value?.textarea;
  if (!textarea) {
    return;
  }

  textarea.focus();
  if (typeof selectionStart === 'number' && typeof selectionEnd === 'number') {
    textarea.setSelectionRange(selectionStart, selectionEnd);
  }
}

async function wrapSelection(prefix, suffix = '', fallback = '') {
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
}

async function insertBlock(snippet) {
  const textarea = editorRef.value?.textarea;
  if (!textarea) {
    form.content += `${form.content ? '\n' : ''}${snippet}\n`;
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
}

function openFileDialog(type) {
  if (type === 'cover') {
    coverInputRef.value?.click();
    return;
  }
  inlineImageInputRef.value?.click();
}

async function uploadImage(file, type) {
  if (!file) {
    return null;
  }

  const formData = new FormData();
  formData.append('file', file);

  if (type === 'cover') {
    coverUploading.value = true;
  } else {
    inlineImageUploading.value = true;
  }

  try {
    const res = await uploadAdminImageApi(formData);
    return res.data?.url || '';
  } finally {
    if (type === 'cover') {
      coverUploading.value = false;
    } else {
      inlineImageUploading.value = false;
    }
  }
}

async function handleCoverFileChange(event) {
  const [file] = event.target.files || [];
  event.target.value = '';
  if (!file) {
    return;
  }

  const url = await uploadImage(file, 'cover');
  if (!url) {
    ElMessage.error(copy.value.uploadFailed);
    return;
  }

  form.coverImage = url;
  ElMessage.success(copy.value.coverUploaded);
}

async function handleInlineImageChange(event) {
  const [file] = event.target.files || [];
  event.target.value = '';
  if (!file) {
    return;
  }

  const url = await uploadImage(file, 'inline');
  if (!url) {
    ElMessage.error(copy.value.uploadFailed);
    return;
  }

  await insertBlock(`![${copy.value.inlineImageAlt}](${url})`);
  ElMessage.success(copy.value.inlineImageUploaded);
}

async function save() {
  loading.value = true;
  try {
    await saveAdminPostApi({
      id: form.id,
      title: form.title,
      slug: form.slug,
      summary: form.summary,
      coverImage: form.coverImage,
      content: form.content,
      status: form.status,
      featured: form.featured,
      allowComment: form.allowComment,
      tags: mergedTags.value
    });
    ElMessage.success(copy.value.postSaved);
    router.push('/admin/posts');
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack editor-page" v-loading="loading">
    <div class="admin-page-head">
      <div>
        <span class="admin-kicker">{{ copy.kicker }}</span>
        <h2>{{ isEdit ? copy.editPost : copy.newPost }}</h2>
      </div>
      <div class="admin-page-actions">
        <span class="admin-badge">{{ copy.livePreview }}</span>
        <el-button @click="router.push('/admin/posts')">{{ copy.backToList }}</el-button>
      </div>
    </div>

    <el-form label-position="top" class="editor-stack">
      <section class="admin-side-card editor-card">
        <div class="editor-card-head">
          <div>
            <span class="admin-eyebrow">{{ copy.basicInfoKicker }}</span>
            <p class="muted editor-copy">{{ copy.basicInfoHint }}</p>
          </div>
        </div>

        <div class="admin-form-grid editor-basic-grid">
          <el-form-item :label="copy.title">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item :label="copy.slug">
            <el-input v-model="form.slug" :placeholder="copy.slugPlaceholder" />
          </el-form-item>
        </div>

        <el-form-item :label="copy.summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" />
        </el-form-item>

        <el-form-item :label="copy.coverImage">
          <div class="cover-upload-row">
            <div class="cover-upload-actions">
              <el-button :loading="coverUploading" @click="openFileDialog('cover')">
                {{ form.coverImage ? copy.replaceCover : copy.uploadCover }}
              </el-button>
              <span class="muted">{{ copy.coverHint }}</span>
            </div>
            <div v-if="form.coverImage" class="cover-preview-card">
              <img :src="form.coverImage" :alt="copy.coverImage" class="cover-preview-image" />
              <code class="cover-preview-url">{{ form.coverImage }}</code>
            </div>
          </div>
          <input
            ref="coverInputRef"
            type="file"
            accept="image/*"
            class="hidden-file-input"
            @change="handleCoverFileChange"
          />
        </el-form-item>
      </section>

      <section class="admin-side-card editor-card settings-card">
        <div class="editor-card-head">
          <div>
            <span class="admin-eyebrow">{{ copy.settingsKicker }}</span>
            <p class="muted editor-copy">{{ copy.settingsHint }}</p>
          </div>
        </div>

        <div class="settings-grid">
          <el-form-item :label="copy.tags" class="tags-field">
            <el-input
              v-model="form.tagsText"
              type="textarea"
              :rows="3"
              resize="none"
              :placeholder="copy.tagsPlaceholder"
            />
            <div class="field-hint">{{ copy.tagsHint }}</div>
            <div v-if="mergedTags.length" class="chip-list tags-chip-list">
              <span v-for="tag in mergedTags" :key="tag" class="chip"># {{ tag }}</span>
            </div>
            <div v-if="detectedOnlyTags.length" class="detected-tags muted">
              {{ copy.detectedTags }}：
              <span v-for="tag in detectedOnlyTags" :key="`detected-${tag}`" class="detected-tag-item">#{{ tag }}</span>
            </div>
          </el-form-item>

          <el-form-item :label="copy.status">
            <el-radio-group v-model="form.status">
              <el-radio-button value="PUBLISHED">{{ copy.published }}</el-radio-button>
              <el-radio-button value="DRAFT">{{ copy.draft }}</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <div class="switch-group">
            <div class="switch-row">
              <span>{{ copy.featured }}</span>
              <el-switch v-model="form.featured" />
            </div>
            <div class="switch-row">
              <span>{{ copy.allowComments }}</span>
              <el-switch v-model="form.allowComment" />
            </div>
          </div>
        </div>
      </section>

      <section class="admin-side-card editor-card content-card">
        <div class="editor-card-head split-head">
          <div>
            <span class="admin-eyebrow">{{ copy.composeKicker }}</span>
            <p class="muted editor-copy">{{ copy.contentHint }}</p>
          </div>
          <span class="editor-toolbar-hint">{{ copy.toolbarHint }}</span>
        </div>

        <div class="toolbar admin-toolbar editor-toolbar">
          <el-button text @click="insertBlock(markdownSamples.heading1)">H1</el-button>
          <el-button text @click="insertBlock(markdownSamples.heading2)">H2</el-button>
          <el-button text @click="wrapSelection('**', '**', copy.bold)">{{ copy.bold }}</el-button>
          <el-button text @click="wrapSelection('*', '*', copy.italic)">{{ copy.italic }}</el-button>
          <el-button text @click="wrapSelection('[', '](https://example.com)', copy.link)">{{ copy.link }}</el-button>
          <el-button text @click="insertBlock(markdownSamples.list)">{{ copy.list }}</el-button>
          <el-button text @click="insertBlock(markdownSamples.quote)">{{ copy.quote }}</el-button>
          <el-button text @click="insertBlock(markdownSamples.code)">{{ copy.code }}</el-button>
          <el-button text @click="insertBlock(markdownSamples.image)">{{ copy.image }}</el-button>
          <el-button text :loading="inlineImageUploading" @click="openFileDialog('inline')">{{ copy.uploadInlineImage }}</el-button>
        </div>

        <input
          ref="inlineImageInputRef"
          type="file"
          accept="image/*"
          class="hidden-file-input"
          @change="handleInlineImageChange"
        />

        <div class="markdown-layout">
          <div class="markdown-pane input-pane">
            <div class="pane-title">{{ copy.markdown }}</div>
            <el-input
              ref="editorRef"
              v-model="form.content"
              type="textarea"
              :rows="24"
              resize="none"
              class="markdown-input"
              :placeholder="copy.writeMarkdown"
            />
          </div>

          <div class="markdown-pane preview-pane">
            <div class="pane-title">{{ copy.preview }}</div>
            <div class="markdown-preview markdown-body" v-html="renderedContent"></div>
          </div>
        </div>
      </section>

      <div class="editor-footer-actions">
        <el-button type="primary" size="large" @click="save">{{ copy.savePost }}</el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped>
.admin-surface :deep(.el-input__wrapper),
.admin-surface :deep(.el-select__wrapper),
.admin-surface :deep(.el-textarea__inner) {
  background: var(--input-bg) !important;
  box-shadow: 0 0 0 1px var(--input-line) inset !important;
}

.editor-page {
  max-width: 1520px;
  margin: 0 auto;
}

.editor-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.editor-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.editor-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.editor-copy {
  margin: 8px 0 0;
  max-width: 720px;
  line-height: 1.7;
}

.editor-basic-grid {
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
}

.cover-upload-row {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cover-upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-preview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--admin-soft-bg);
}

.cover-preview-image {
  width: 100%;
  max-width: 520px;
  max-height: 280px;
  object-fit: cover;
  border-radius: 14px;
}

.cover-preview-url {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.hidden-file-input {
  display: none;
}

.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.8fr) minmax(220px, 0.7fr);
  gap: 18px;
  align-items: start;
}

.tags-field {
  margin-bottom: 0;
}

.field-hint {
  margin-top: 8px;
  font-size: 0.84rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

.tags-chip-list {
  margin-top: 12px;
}

.detected-tags {
  margin-top: 10px;
  line-height: 1.7;
}

.detected-tag-item {
  margin-right: 10px;
}

.switch-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--admin-soft-bg);
}

.split-head {
  align-items: center;
}

.editor-toolbar-hint {
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.editor-toolbar {
  grid-template-columns: repeat(auto-fit, minmax(110px, max-content));
}

.markdown-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.85fr);
  gap: 18px;
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
  min-height: 640px;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(8, 8, 8, 0.96);
  overflow: auto;
}

:deep(.markdown-input textarea) {
  min-height: 640px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  line-height: 1.72;
}

.editor-footer-actions {
  display: flex;
  justify-content: flex-end;
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

@media (max-width: 1180px) {
  .settings-grid,
  .markdown-layout {
    grid-template-columns: 1fr;
  }

  .markdown-preview,
  :deep(.markdown-input textarea) {
    min-height: 420px;
  }
}

@media (max-width: 720px) {
  .editor-card-head,
  .cover-upload-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .editor-basic-grid {
    grid-template-columns: 1fr;
  }

  .editor-footer-actions {
    justify-content: stretch;
  }

  .editor-footer-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
