<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchPostDetailApi, submitCommentApi } from '../api/blog';
import { renderMarkdown } from '../utils/markdown';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const route = useRoute();
const loading = ref(true);
const post = ref(null);
const commentForm = ref({
  postId: null,
  nickname: '',
  email: '',
  content: ''
});

const renderedContent = computed(() => renderMarkdown(post.value?.content || ''));

const getViewCacheKey = (slug) => `blog:viewed:${slug}`;

const loadPost = async () => {
  loading.value = true;
  try {
    const slug = String(route.params.slug || '');
    const viewed = window.sessionStorage.getItem(getViewCacheKey(slug)) === '1';
    const res = await fetchPostDetailApi(slug, !viewed);
    post.value = res.data;
    commentForm.value.postId = res.data.id;
    if (!viewed) {
      window.sessionStorage.setItem(getViewCacheKey(slug), '1');
    }
  } finally {
    loading.value = false;
  }
};

const submitComment = async () => {
  await submitCommentApi(commentForm.value);
  ElMessage.success(preferences.t('post.commentSubmitted'));
  commentForm.value.nickname = '';
  commentForm.value.email = '';
  commentForm.value.content = '';
};

watch(() => route.params.slug, loadPost);
onMounted(loadPost);
</script>

<template>
  <div v-if="post" v-loading="loading" class="detail-wrap">
    <article class="section-card article-panel">
      <div class="article-hero">
        <div class="article-hero-copy">
          <p class="eyebrow muted">{{ post.categoryName }} / {{ preferences.formatDateTime(post.publishedAt) }}</p>
          <h1>{{ post.title }}</h1>
          <p class="lead">{{ post.summary }}</p>
        </div>

        <div class="article-overview glass-panel">
          <div class="overview-item">
            <span class="overview-label muted">{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
            <strong>{{ post.viewCount }}</strong>
          </div>
          <div class="overview-item">
            <span class="overview-label muted">{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
            <strong>{{ post.comments.length }}</strong>
          </div>
        </div>
      </div>

      <div class="article-meta muted">
        <span>{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
        <span>{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
      </div>

      <div class="chip-list article-tags">
        <span v-for="tag in post.tags" :key="tag" class="chip"># {{ tag }}</span>
      </div>

      <div class="article-body-wrap">
        <div class="content-html markdown-body article-body" v-html="renderedContent"></div>
      </div>
    </article>

    <section class="section-card comment-panel">
      <div class="section-heading comment-heading">
        <div>
          <span class="comment-kicker muted">Discussion</span>
          <h2>{{ preferences.t('post.commentsTitle') }}</h2>
        </div>
        <span class="muted">{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
      </div>

      <el-alert
        v-if="!post.allowComment"
        :title="preferences.t('post.commentsDisabled')"
        type="info"
        :closable="false"
        class="comment-alert"
      />

      <el-form v-else class="comment-form" label-position="top" @submit.prevent="submitComment">
        <div class="comment-form-head">
          <div>
            <strong>Leave a reply</strong>
            <p class="muted">Share follow-up thoughts without leaving the article context.</p>
          </div>
        </div>
        <div class="form-grid">
          <el-form-item :label="preferences.t('post.nickname')">
            <el-input v-model="commentForm.nickname" :placeholder="preferences.t('post.nicknamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="preferences.t('post.email')">
            <el-input v-model="commentForm.email" :placeholder="preferences.t('post.emailPlaceholder')" />
          </el-form-item>
        </div>
        <el-form-item :label="preferences.t('post.comment')">
          <el-input
            v-model="commentForm.content"
            type="textarea"
            :rows="4"
            :placeholder="preferences.t('post.commentPlaceholder')"
          />
        </el-form-item>
        <div class="comment-actions">
          <el-button type="primary" @click="submitComment">{{ preferences.t('post.submitComment') }}</el-button>
        </div>
      </el-form>

      <div class="comment-list">
        <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
          <div class="comment-head">
            <div>
              <strong>{{ comment.nickname }}</strong>
              <p class="muted">{{ preferences.formatDateTime(comment.createdAt) }}</p>
            </div>
          </div>
          <p>{{ comment.content }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-panel,
.comment-panel {
  padding: 30px;
}

.article-panel {
  background:
    radial-gradient(circle at top center, rgba(255, 255, 255, 0.06), transparent 24%),
    rgba(10, 10, 10, 0.94);
}

.article-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 22px;
  align-items: start;
}

.article-hero-copy {
  min-width: 0;
}

.eyebrow,
.comment-kicker {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.78rem;
}

h1 {
  margin: 12px 0 16px;
  font-size: clamp(2.3rem, 4.8vw, 4.1rem);
  line-height: 0.95;
  letter-spacing: -0.05em;
}

.lead {
  max-width: 760px;
  color: var(--text-secondary);
  font-size: 1.05rem;
  line-height: 1.9;
  margin: 0;
}

.article-overview {
  padding: 18px;
  border-radius: 24px;
  display: grid;
  gap: 12px;
}

.overview-item {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}

.overview-item strong {
  display: block;
  margin-top: 8px;
  font-size: 1.8rem;
  line-height: 1;
}

.overview-label {
  display: block;
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin: 20px 0 14px;
  font-size: 0.86rem;
}

.article-tags {
  margin-bottom: 18px;
}

.article-body-wrap {
  padding: 24px 0 6px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.article-body {
  max-width: 840px;
}

.comment-heading h2 {
  margin: 8px 0 0;
}

.comment-form {
  margin: 18px 0 28px;
  padding: 22px;
  border-radius: 24px;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.015)),
    rgba(8, 8, 8, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.comment-form-head {
  margin-bottom: 12px;
}

.comment-form-head strong {
  display: block;
  font-size: 1rem;
}

.comment-form-head p {
  margin: 6px 0 0;
}

.comment-alert {
  margin: 16px 0 28px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 8px;
}

.comment-item {
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.comment-item p {
  margin: 10px 0 0;
  line-height: 1.75;
}

.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.comment-head p {
  margin: 6px 0 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 1.2em 0 0.6em;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  line-height: 1.9;
}

.markdown-body :deep(pre) {
  overflow: auto;
  padding: 16px;
  border-radius: 16px;
  background: #000;
  color: #f5f5f5;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.markdown-body :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
}

.markdown-body :deep(blockquote) {
  margin: 0;
  padding: 0.85rem 1.1rem;
  border-left: 4px solid rgba(255, 255, 255, 0.3);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.04);
  border-radius: 0 14px 14px 0;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 16px;
}

@media (max-width: 900px) {
  .article-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .article-panel,
  .comment-panel {
    padding: 22px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .comment-head {
    flex-direction: column;
  }

  .comment-actions {
    justify-content: stretch;
  }

  .comment-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
