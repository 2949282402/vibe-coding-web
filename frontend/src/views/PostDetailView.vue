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
      <p class="eyebrow muted">{{ post.categoryName }} / {{ preferences.formatDateTime(post.publishedAt) }}</p>
      <h1>{{ post.title }}</h1>
      <p class="lead">{{ post.summary }}</p>
      <div class="article-meta muted">
        <span>{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
        <span>{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
      </div>
      <div class="chip-list">
        <span v-for="tag in post.tags" :key="tag" class="chip"># {{ tag }}</span>
      </div>
      <div class="content-html markdown-body" v-html="renderedContent"></div>
    </article>

    <section class="section-card comment-panel">
      <div class="section-heading">
        <h2>{{ preferences.t('post.commentsTitle') }}</h2>
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
        <el-button type="primary" @click="submitComment">{{ preferences.t('post.submitComment') }}</el-button>
      </el-form>

      <div class="comment-list">
        <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
          <div class="comment-head">
            <strong>{{ comment.nickname }}</strong>
            <span class="muted">{{ preferences.formatDateTime(comment.createdAt) }}</span>
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
  gap: 24px;
}

.article-panel,
.comment-panel {
  padding: 32px;
}

.eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.8rem;
}

h1 {
  margin: 10px 0 14px;
  font-size: clamp(2rem, 4vw, 3.2rem);
}

.lead {
  color: var(--text-secondary);
  font-size: 1.05rem;
  margin-bottom: 16px;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-bottom: 18px;
}

.comment-form {
  margin: 16px 0 28px;
  padding: 22px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.015)),
    rgba(6, 6, 6, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.comment-alert {
  margin: 16px 0 28px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 8px;
}

.comment-item {
  padding: 18px 20px;
  border-radius: var(--radius-lg);
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

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 1.2em 0 0.6em;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  line-height: 1.85;
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
  padding: 0.75rem 1rem;
  border-left: 4px solid rgba(255, 255, 255, 0.3);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.04);
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 12px;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .comment-head {
    flex-direction: column;
  }
}
</style>
