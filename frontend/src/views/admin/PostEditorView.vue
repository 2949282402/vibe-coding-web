<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { QuillEditor } from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import {
  fetchAdminPostApi,
  saveAdminPostApi,
  uploadAdminImageApi
} from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const HASH_TAG_PATTERN = /(^|[^\p{L}\p{N}_-])#([\p{L}\p{N}][\p{L}\p{N}_-]{0,31})/gu;
const TAG_SPLIT_PATTERN = /[,，;；\n]+/g;

const preferences = usePreferencesStore();
const route = useRoute();
const router = useRouter();

const loading = ref(false);
const coverUploading = ref(false);
const inlineImageUploading = ref(false);
const settingsExpanded = ref(false);
const quillEditorRef = ref(null);
const editorSinglePaneRef = ref(null);
const coverInputRef = ref(null);
const inlineImageInputRef = ref(null);
const quillInstance = shallowRef(null);
const toolbarPinned = ref(false);
const toolbarLeft = ref(0);
const toolbarWidth = ref(0);

const copy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      kicker: '编辑器',
      composeKicker: '富文本正文',
      settingsKicker: '发布设置',
      basicInfoKicker: '文章信息',
      editPost: '编辑文章',
      newPost: '新建文章',
      backToList: '返回列表',
      livePreview: '实时预览',
      workspaceLabel: '写作工作台',
      basicInfoHint: '先整理标题和摘要；封面图是可选的，不上传也可以直接发布。',
      settingsHint: '标签可手动输入，也会自动识别正文中的 #标签。',
      contentHint: '左侧使用富文本编辑正文，右侧同步预览渲染结果，并可直接上传本地图片插入正文。',
      title: '标题',
      slug: 'Slug',
      slugPlaceholder: '留空则自动生成',
      summary: '摘要',
      coverImage: '封面图片（可选）',
      coverHint: '不上传封面也可以发布；如需封面，图片会上传到服务器并返回可访问地址。',
      uploadCover: '上传封面',
      replaceCover: '重新上传',
      removeCover: '移除封面',
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
      toolbarHint: '工具栏支持标题、加粗、列表、引用、代码块和图片上传',
      uploadInlineImage: '上传并插入图片',
      inlineImageHint: '点击工具栏图片按钮或这里的按钮，选择本地图片后会先上传到服务器，再插入正文。',
      inlineImageUploaded: '正文图片已插入',
      richText: '富文本编辑',
      editorNote: '支持本地图片直传、标题层级、列表、引用和代码块。',
      writeRichText: '在这里输入正文内容',
      savePost: '保存文章',
      postSaved: '文章已保存',
      publishPanel: '发布控制',
      collapseSettings: '展开次级设置',
      expandSettings: '收起次级设置',
      settingsSummary: '封面、状态、标签与评论开关',
      statusDraftShort: '草稿',
      statusPublishedShort: '已发布',
      commentsOn: '评论开',
      commentsOff: '评论关',
      coverOn: '有封面',
      coverOff: '无封面',
      defaultContent: `<h2>开始写作</h2><p>这个编辑器支持富文本排版，也支持把本地图片直接上传到服务器后插入正文。</p><ul><li>可直接编辑标题、段落、列表、引用和代码块</li><li>正文里的 #标签 仍会自动加入标签列表</li><li>封面图可选，不上传也可以直接发布</li></ul><blockquote><p>先写清楚主题，再补充示例与截图。</p></blockquote>`,
      uploadFailed: '图片上传失败'
    };
  }

  return {
    kicker: 'Editor',
    composeKicker: 'Rich Content',
    settingsKicker: 'Settings',
    basicInfoKicker: 'Post Details',
    editPost: 'Edit Post',
    newPost: 'New Post',
    backToList: 'Back to List',
    livePreview: 'Live Preview',
    workspaceLabel: 'Writing Desk',
    basicInfoHint: 'Set the title and summary first; the cover image is optional and the post can be published without it.',
    settingsHint: 'Tags can be typed manually and are also detected from #hashtags in the article body.',
    contentHint: 'Use the rich text editor on the left, review the rendered result on the right, and upload local images directly into the article body.',
    title: 'Title',
    slug: 'Slug',
    slugPlaceholder: 'Auto generated when left empty',
    summary: 'Summary',
    coverImage: 'Cover Image (Optional)',
    coverHint: 'You can publish without a cover image. If you add one, it will be uploaded to the server and stored as a reusable URL.',
    uploadCover: 'Upload Cover',
    replaceCover: 'Replace Cover',
    removeCover: 'Remove Cover',
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
    toolbarHint: 'The toolbar supports headings, formatting, lists, quotes, code blocks, and direct image upload',
    uploadInlineImage: 'Upload and Insert Image',
    inlineImageHint: 'Use the image button in the toolbar or this button to upload a local image to the server and insert it into the article body.',
    inlineImageUploaded: 'Inline image inserted',
    richText: 'Rich Text',
    editorNote: 'Supports direct local image upload, heading levels, lists, quotes, and code blocks.',
    writeRichText: 'Write your post here',
    savePost: 'Save Post',
    postSaved: 'Post saved',
    publishPanel: 'Publishing',
    collapseSettings: 'Expand secondary settings',
    expandSettings: 'Collapse secondary settings',
    settingsSummary: 'Cover, status, tags, and comment controls',
    statusDraftShort: 'Draft',
    statusPublishedShort: 'Published',
    commentsOn: 'Comments on',
    commentsOff: 'Comments off',
    coverOn: 'Cover on',
    coverOff: 'No cover',
    defaultContent: `<h2>Start writing</h2><p>This editor supports rich text formatting and uploads local images to the server before inserting them into the article.</p><ul><li>Edit headings, paragraphs, lists, quotes, and code blocks directly</li><li>#hashtags in the content still flow into the tag list</li><li>Publishing without a cover image is supported</li></ul><blockquote><p>Start with the main idea, then add examples and screenshots.</p></blockquote>`,
    uploadFailed: 'Upload failed'
  };
});

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
const manualTags = computed(() => parseManualTags(form.tagsText));
const contentTags = computed(() => extractHashTags(stripHtml(form.content)));
const mergedTags = computed(() => mergeTags(manualTags.value, contentTags.value));
const detectedOnlyTags = computed(() => {
  const manualSet = new Set(manualTags.value.map((item) => item.toLowerCase()));
  return contentTags.value.filter((item) => !manualSet.has(item.toLowerCase()));
});
const settingsBadges = computed(() => [
  form.status === 'PUBLISHED' ? copy.value.statusPublishedShort : copy.value.statusDraftShort,
  form.allowComment ? copy.value.commentsOn : copy.value.commentsOff,
  form.coverImage ? copy.value.coverOn : copy.value.coverOff
]);

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

function stripHtml(value) {
  return String(value || '').replace(/<[^>]+>/g, ' ');
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

function clearCoverImage() {
  form.coverImage = '';
}

function handleEditorReady(quill) {
  quillInstance.value = quill;
  const toolbar = quill.getModule('toolbar');
  toolbar?.addHandler('image', () => openFileDialog('inline'));
  nextTick(updateFloatingToolbar);
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

  const quill = quillInstance.value || quillEditorRef.value?.getQuill?.();
  if (quill) {
    const range = quill.getSelection(true);
    const index = range?.index ?? quill.getLength();
    quill.insertEmbed(index, 'image', url, 'user');
    quill.setSelection(index + 1, 0, 'user');
  } else {
    form.content += `<p><img src="${url}" alt="" /></p>`;
  }
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

function updateFloatingToolbar() {
  const wrapper = editorSinglePaneRef.value;
  const toolbar = wrapper?.querySelector('.ql-toolbar.ql-snow');
  const editor = wrapper?.querySelector('.ql-editor');
  if (!wrapper || !toolbar || !editor) {
    toolbarPinned.value = false;
    return;
  }

  const wrapperRect = wrapper.getBoundingClientRect();
  const toolbarHeight = toolbar.offsetHeight || 0;
  const topOffset = 12;
  const editorHeight = editor.scrollHeight || editor.offsetHeight || 0;
  const shouldPin =
    wrapperRect.top <= topOffset &&
    wrapperRect.top + toolbarHeight + editorHeight > topOffset + toolbarHeight + 24;

  toolbarPinned.value = shouldPin;
  if (shouldPin) {
    toolbarLeft.value = wrapperRect.left;
    toolbarWidth.value = wrapperRect.width;
  }
}

onMounted(() => {
  loadDetail();
  window.addEventListener('scroll', updateFloatingToolbar, { passive: true });
  window.addEventListener('resize', updateFloatingToolbar);
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateFloatingToolbar);
  window.removeEventListener('resize', updateFloatingToolbar);
});

const editorToolbar = [
  [{ header: [1, 2, 3, false] }],
  ['bold', 'italic', 'underline', 'strike'],
  [{ list: 'ordered' }, { list: 'bullet' }],
  ['blockquote', 'code-block'],
  ['link', 'image'],
  [{ align: [] }],
  ['clean']
];

const editorOptions = {
  theme: 'snow',
  modules: {
    toolbar: editorToolbar
  },
  placeholder: ''
};
</script>

<template>
  <section class="section-card admin-surface admin-panel admin-page-stack editor-page" v-loading="loading">
    <div class="admin-page-head editor-shell-head">
      <div class="editor-head-copy">
        <span class="admin-kicker">{{ copy.workspaceLabel }}</span>
        <h2>{{ isEdit ? copy.editPost : copy.newPost }}</h2>
      </div>
      <div class="admin-page-actions editor-head-actions">
        <el-button @click="router.push('/admin/posts')">{{ copy.backToList }}</el-button>
        <el-button type="primary" @click="save">{{ copy.savePost }}</el-button>
      </div>
    </div>

    <el-form label-position="top" class="editor-stack">
      <section class="editor-intro-grid">
        <section class="admin-side-card editor-card editor-primary-card">
          <div class="editor-card-head">
            <div>
              <span class="admin-eyebrow">{{ copy.basicInfoKicker }}</span>
              <p class="muted editor-copy">{{ copy.basicInfoHint }}</p>
            </div>
          </div>

          <div class="admin-form-grid editor-basic-grid">
            <el-form-item :label="copy.title" class="title-field">
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
                <el-button v-if="form.coverImage" text @click="clearCoverImage">
                  {{ copy.removeCover }}
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

        <aside class="admin-side-card editor-card settings-card editor-settings-card">
          <button type="button" class="settings-toggle" @click="settingsExpanded = !settingsExpanded">
            <div class="settings-toggle-copy">
              <span class="admin-eyebrow">{{ copy.publishPanel }}</span>
              <strong>{{ settingsExpanded ? copy.expandSettings : copy.collapseSettings }}</strong>
              <p class="muted settings-summary">{{ copy.settingsSummary }}</p>
            </div>
            <div class="settings-toggle-meta">
              <div class="chip-list settings-badges">
                <span v-for="badge in settingsBadges" :key="badge" class="chip settings-chip">{{ badge }}</span>
              </div>
              <span class="settings-caret" :class="{ 'settings-caret--open': settingsExpanded }">+</span>
            </div>
          </button>

          <div v-show="settingsExpanded" class="settings-grid settings-grid-compact">
            <el-form-item :label="copy.status">
              <el-radio-group v-model="form.status">
                <el-radio-button value="PUBLISHED">{{ copy.published }}</el-radio-button>
                <el-radio-button value="DRAFT">{{ copy.draft }}</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <div class="switch-group compact-switch-group">
              <div class="switch-row">
                <span>{{ copy.featured }}</span>
                <el-switch v-model="form.featured" />
              </div>
              <div class="switch-row">
                <span>{{ copy.allowComments }}</span>
                <el-switch v-model="form.allowComment" />
              </div>
            </div>

            <el-form-item :label="copy.tags" class="tags-field">
              <el-input
                v-model="form.tagsText"
                type="textarea"
                :rows="4"
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
          </div>
        </aside>
      </section>

      <section class="admin-side-card editor-card content-card">
        <div class="editor-card-head split-head">
          <div>
            <span class="admin-eyebrow">{{ copy.composeKicker }}</span>
            <p class="muted editor-copy">{{ copy.contentHint }}</p>
          </div>
          <div class="editor-meta-inline">
            <span class="editor-toolbar-hint">{{ copy.toolbarHint }}</span>
            <span class="editor-note">{{ copy.editorNote }}</span>
          </div>
        </div>

        <input
          ref="inlineImageInputRef"
          type="file"
          accept="image/*"
          class="hidden-file-input"
          @change="handleInlineImageChange"
        />

        <div
          ref="editorSinglePaneRef"
          class="editor-single-pane"
          :class="{ 'editor-single-pane--toolbar-pinned': toolbarPinned }"
          :style="{
            '--editor-toolbar-left': `${toolbarLeft}px`,
            '--editor-toolbar-width': `${toolbarWidth}px`
          }"
        >
          <div class="pane-title">{{ copy.richText }}</div>
          <QuillEditor
            ref="quillEditorRef"
            v-model:content="form.content"
            contentType="html"
            class="rich-editor"
            :options="editorOptions"
            @ready="handleEditorReady"
          />
        </div>
      </section>
    </el-form>
  </section>
</template>

<style scoped>
.editor-shell-head {
  align-items: center;
  padding-bottom: 8px;
}

.editor-head-copy h2 {
  font-size: clamp(1.35rem, 2vw, 1.8rem);
  letter-spacing: -0.04em;
}

.editor-head-actions {
  flex-shrink: 0;
}

.admin-surface :deep(.el-input__wrapper),
.admin-surface :deep(.el-select__wrapper),
.admin-surface :deep(.el-textarea__inner),
.admin-surface :deep(.ql-toolbar),
.admin-surface :deep(.ql-container) {
  background: var(--input-bg) !important;
  box-shadow: 0 0 0 1px var(--input-line) inset !important;
}

.editor-page {
  max-width: 1520px;
  margin: 0 auto;
  min-height: 100%;
  overflow: visible;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.04), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.015), rgba(255, 255, 255, 0));
}

.editor-stack {
  display: grid;
  gap: 16px;
  min-height: 0;
  overflow: visible;
}

.editor-intro-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  gap: 18px;
  align-items: start;
}

.editor-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02)),
    rgba(255, 255, 255, 0.015);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.editor-primary-card,
.editor-settings-card {
  height: 100%;
}

.editor-settings-card {
  gap: 12px;
}

.content-card {
  min-height: min(72vh, 900px);
  overflow: visible;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0.03));
}

.editor-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.editor-copy {
  margin: 6px 0 0;
  max-width: 760px;
  line-height: 1.65;
  font-size: 0.95rem;
}

.editor-basic-grid {
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.7fr);
  gap: 14px;
}

.title-field :deep(.el-input__inner) {
  font-size: 1.05rem;
}

.cover-upload-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cover-upload-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.cover-preview-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02)),
    var(--admin-soft-bg);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
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
  grid-template-columns: minmax(0, 1.5fr) minmax(250px, 0.85fr) minmax(220px, 0.7fr);
  gap: 14px;
  align-items: start;
}

.settings-grid-compact {
  grid-template-columns: 1fr;
  gap: 12px;
}

.settings-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 2px 0 4px;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.settings-toggle-copy {
  min-width: 0;
}

.settings-toggle-copy strong {
  display: block;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-main);
}

.settings-summary {
  margin: 6px 0 0;
  font-size: 0.84rem;
}

.settings-toggle-meta {
  display: grid;
  justify-items: end;
  gap: 10px;
  flex-shrink: 0;
}

.settings-badges {
  justify-content: flex-end;
  gap: 8px;
}

.settings-chip {
  padding: 6px 10px;
  font-size: 0.72rem;
  letter-spacing: 0.04em;
}

.settings-caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-secondary);
  font-size: 1rem;
  line-height: 1;
  transform: rotate(0deg);
  transition: transform 0.28s var(--ease-liquid), color 0.28s var(--ease-soft), border-color 0.28s var(--ease-soft), background 0.28s var(--ease-soft), box-shadow 0.28s var(--ease-liquid);
}

.settings-caret--open {
  transform: rotate(45deg);
  color: var(--text-main);
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.08);
}

.tags-field {
  margin-bottom: 0;
}

.field-hint {
  margin-top: 6px;
  font-size: 0.82rem;
  color: var(--text-secondary);
  line-height: 1.55;
}

.tags-chip-list {
  margin-top: 10px;
}

.detected-tags {
  margin-top: 8px;
  line-height: 1.6;
}

.detected-tag-item {
  margin-right: 8px;
}

.switch-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact-switch-group {
  gap: 8px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02)),
    var(--admin-soft-bg);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.split-head {
  align-items: end;
}

.compact-settings-head {
  padding-bottom: 2px;
}

.editor-meta-inline {
  display: grid;
  gap: 4px;
  justify-items: end;
}

.editor-toolbar-hint {
  font-size: 0.82rem;
  color: var(--text-secondary);
  max-width: 360px;
  text-align: right;
}

.editor-note {
  font-size: 0.76rem;
  color: var(--text-muted);
  letter-spacing: 0.04em;
  text-align: right;
}

.editor-single-pane {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  min-height: 0;
  position: relative;
  border-radius: 24px;
}

.editor-single-pane--toolbar-pinned {
  padding-top: 62px;
}

.pane-title {
  margin-bottom: 10px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-secondary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

:deep(.rich-editor) {
  min-height: 620px;
  border: 1px solid var(--line);
  border-radius: 24px;
  overflow: visible;
  background:
    linear-gradient(180deg, rgba(18, 19, 23, 0.98), rgba(13, 14, 17, 0.98)),
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.06), transparent 28%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04), 0 24px 44px rgba(0, 0, 0, 0.16);
}

:deep(.rich-editor .ql-toolbar.ql-snow) {
  border: 0;
  border-bottom: 1px solid var(--line);
  padding: 12px 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(16px);
  position: sticky;
  top: 12px;
  z-index: 14;
  border-radius: 22px 22px 0 0;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.12);
}

.editor-single-pane--toolbar-pinned :deep(.rich-editor .ql-toolbar.ql-snow) {
  position: fixed;
  top: 12px;
  left: var(--editor-toolbar-left);
  width: var(--editor-toolbar-width);
  z-index: 40;
}

:deep(.rich-editor .ql-container.ql-snow) {
  border: 0;
  font-family: inherit;
  min-height: 560px;
  border-radius: 0 0 22px 22px;
}

:deep(.rich-editor .ql-editor) {
  min-height: 560px;
  padding: 26px 28px 32px;
  font-size: 1.04rem;
  line-height: 1.9;
  color: var(--text-main);
  font-family: var(--font-body);
}

:deep(.rich-editor .ql-editor.ql-blank::before) {
  color: var(--text-secondary);
  font-style: normal;
  left: 30px;
  right: 30px;
}

:deep(.rich-editor .ql-toolbar .ql-picker),
:deep(.rich-editor .ql-toolbar button) {
  color: var(--text-secondary);
}

:deep(.rich-editor .ql-snow .ql-stroke) {
  stroke: currentColor;
}

:deep(.rich-editor .ql-snow .ql-fill) {
  fill: currentColor;
}

:deep(.rich-editor .ql-toolbar button:hover),
:deep(.rich-editor .ql-toolbar button.ql-active),
:deep(.rich-editor .ql-toolbar .ql-picker-label:hover),
:deep(.rich-editor .ql-toolbar .ql-picker-label.ql-active) {
  color: var(--text-main);
}

:deep(.rich-editor .ql-toolbar button:hover) {
  background: rgba(255, 255, 255, 0.06);
  border-radius: 10px;
}

:deep(.rich-editor .ql-editor h1),
:deep(.rich-editor .ql-editor h2),
:deep(.rich-editor .ql-editor h3),
:deep(.rich-editor .ql-editor h4),
:deep(.rich-editor .ql-editor p) {
  margin: 0 0 0.95rem;
}

:deep(.rich-editor .ql-editor ul),
:deep(.rich-editor .ql-editor ol) {
  padding-left: 1.35rem;
}

:deep(.rich-editor .ql-editor li + li) {
  margin-top: 0.3rem;
}

:deep(.rich-editor .ql-editor blockquote) {
  margin: 1.2rem 0;
  padding: 0.9rem 1rem 0.9rem 1.15rem;
  border-left: 4px solid rgba(255, 255, 255, 0.34);
  border-radius: 0 14px 14px 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.02));
  color: rgba(235, 235, 238, 0.82);
}

:deep(.rich-editor .ql-editor .ql-code-block-container) {
  margin: 1.4rem 0;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

:deep(.rich-editor .ql-editor img) {
  display: block;
  max-width: 100%;
  margin: 1.5rem auto;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.2);
}

html[data-theme='light'] .settings-caret--open {
  background: rgba(17, 17, 17, 0.04);
}

html[data-theme='light'] :deep(.rich-editor .ql-editor blockquote) {
  color: rgba(22, 22, 24, 0.72);
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.08), rgba(17, 17, 17, 0.02));
}

html[data-theme='light'] :deep(.rich-editor .ql-editor img) {
  border-color: rgba(17, 17, 17, 0.08);
  box-shadow: 0 18px 34px rgba(30, 25, 20, 0.08);
}

html[data-theme='light'] :deep(.rich-editor .ql-editor .ql-code-block-container) {
  border-color: rgba(17, 17, 17, 0.08);
}

:deep(.rich-editor .ql-editor h1),
:deep(.rich-editor .ql-editor h2),
:deep(.rich-editor .ql-editor h3),
:deep(.rich-editor .ql-editor h4) {
  margin: 1.3em 0 0.55em;
}

:deep(.rich-editor .ql-editor h1),
:deep(.rich-editor .ql-editor h2),
:deep(.rich-editor .ql-editor h3),
:deep(.rich-editor .ql-editor h4) {
  font-family: var(--font-display);
  letter-spacing: -0.04em;
  line-height: 1.12;
}

html[data-theme='light'] .content-card {
  background:
    radial-gradient(circle at top right, rgba(0, 0, 0, 0.06), transparent 26%),
    linear-gradient(180deg, rgba(17, 17, 17, 0.012), rgba(17, 17, 17, 0.02));
}

html[data-theme='light'] :deep(.rich-editor) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.985), rgba(248, 248, 245, 0.985)),
    radial-gradient(circle at top left, rgba(0, 0, 0, 0.05), transparent 28%);
}

html[data-theme='light'] :deep(.rich-editor .ql-toolbar.ql-snow) {
  background: rgba(17, 17, 17, 0.025);
}

@media (max-width: 1180px) {
  .editor-page,
  .editor-stack,
  .content-card {
    height: auto;
    overflow: visible;
  }

  .editor-intro-grid,
  .settings-grid {
    grid-template-columns: 1fr;
  }

  :deep(.rich-editor .ql-editor) {
    min-height: 420px;
  }
}

@media (max-width: 720px) {
  .editor-card-head,
  .cover-upload-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .settings-toggle,
  .settings-toggle-meta {
    width: 100%;
  }

  .settings-toggle-meta {
    justify-items: start;
  }

  .editor-meta-inline {
    justify-items: start;
  }

  .editor-head-actions {
    width: 100%;
  }

  .editor-toolbar-hint {
    text-align: left;
  }

  .editor-note {
    text-align: left;
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
