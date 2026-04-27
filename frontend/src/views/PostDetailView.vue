<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchPostDetailApi, submitCommentApi } from '../api/blog';
import { renderMarkdown } from '../utils/markdown';
import { useAuthStore } from '../stores/auth';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const loading = ref(true);
const post = ref(null);
const commentForm = ref({
  postId: null,
  content: ''
});

const copy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        discussion: '评论交流',
        replyTitle: '登录后发表评论',
        replyBody: '文章可以直接查看，但发表评论需要先登录账号。',
        goLogin: '去登录',
        currentUser: '当前账号',
        submitSuccess: '评论已提交，等待审核'
      }
    : {
        discussion: 'Discussion',
        replyTitle: 'Sign in to comment',
        replyBody: 'Reading is public, but posting comments requires an account.',
        goLogin: 'Sign In',
        currentUser: 'Current Account',
        submitSuccess: 'Comment submitted for moderation'
      }
);

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
  ElMessage.success(copy.value.submitSuccess);
  commentForm.value.content = '';
};

const goLogin = () => {
  router.push({ name: 'login', query: { redirect: route.fullPath } });
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

          <div class="article-context">
            <div class="article-meta muted">
              <span>{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
              <span>{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
            </div>

            <div class="chip-list article-tags">
              <span v-for="tag in post.tags" :key="tag" class="chip"># {{ tag }}</span>
            </div>
          </div>
        </div>

        <aside class="article-overview glass-panel">
          <div class="overview-item">
            <span class="overview-label muted">{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
            <strong>{{ post.viewCount }}</strong>
          </div>
          <div class="overview-item">
            <span class="overview-label muted">{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
            <strong>{{ post.comments.length }}</strong>
          </div>
        </aside>
      </div>

      <div v-if="post.coverImage" class="article-cover-wrap">
        <img :src="post.coverImage" :alt="post.title" class="article-cover" />
      </div>

      <div class="article-body-wrap">
        <div class="content-html markdown-body article-body" v-html="renderedContent"></div>
      </div>
    </article>

    <section class="section-card comment-panel">
      <div class="section-heading comment-heading">
        <div>
          <span class="comment-kicker muted">{{ copy.discussion }}</span>
          <h2>{{ preferences.t('post.commentsTitle') }}</h2>
        </div>
        <span class="comment-total muted">{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
      </div>

      <el-alert
        v-if="!post.allowComment"
        :title="preferences.t('post.commentsDisabled')"
        type="info"
        :closable="false"
        class="comment-alert"
      />

      <template v-else-if="!authStore.isAuthenticated">
        <div class="comment-login-card">
          <strong>{{ copy.replyTitle }}</strong>
          <p class="muted">{{ copy.replyBody }}</p>
          <el-button type="primary" @click="goLogin">{{ copy.goLogin }}</el-button>
        </div>
      </template>

      <el-form v-else class="comment-form" label-position="top" @submit.prevent="submitComment">
        <div class="comment-form-head">
          <div>
            <strong>{{ copy.currentUser }}</strong>
            <p class="muted">{{ authStore.user?.displayName }} / {{ authStore.user?.email }}</p>
          </div>
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
  display: grid;
  gap: 24px;
}

.article-panel,
.comment-panel {
  padding: 34px;
}

.article-panel {
  display: grid;
  gap: 30px;
}

.article-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) 220px;
  gap: 30px;
  align-items: start;
}

.article-hero-copy {
  min-width: 0;
  display: grid;
  gap: 16px;
}

.eyebrow,
.comment-kicker {
  margin: 0;
  letter-spacing: 0.12em;
  font-size: 0.74rem;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  max-width: 900px;
  font-size: clamp(2rem, 4vw, 3.35rem);
  line-height: 1.1;
  letter-spacing: -0.05em;
  text-wrap: balance;
}

.lead {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 1.02rem;
  line-height: 1.88;
}

.article-context {
  display: grid;
  gap: 14px;
  padding-top: 4px;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.article-tags {
  gap: 10px;
}

.article-overview {
  padding: 16px;
  border-radius: 24px;
  display: grid;
  gap: 12px;
  align-self: start;
}

.overview-item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  display: grid;
  gap: 8px;
}

.overview-item strong {
  display: block;
  font-size: 1.9rem;
  line-height: 1;
  letter-spacing: -0.05em;
}

.overview-label {
  display: block;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.article-cover-wrap {
  overflow: hidden;
  border-radius: 26px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
}

.article-cover {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: cover;
}

.article-body-wrap {
  border-top: 1px solid var(--line);
  padding-top: 34px;
}

.article-body {
  max-width: 780px;
  margin: 0 auto;
}

.comment-panel {
  display: grid;
  gap: 22px;
}

.comment-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 18px;
}

.comment-heading h2 {
  margin: 8px 0 0;
}

.comment-total {
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.comment-form,
.comment-login-card {
  padding: 24px;
  border-radius: 22px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
}

.comment-login-card {
  display: grid;
  gap: 10px;
}

.comment-login-card p {
  margin: 0;
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
  margin: 0;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  display: grid;
  gap: 14px;
}

.comment-item {
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
}

.comment-item p {
  margin: 10px 0 0;
  line-height: 1.75;
  word-break: break-word;
}

.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.comment-head p {
  margin: 6px 0 0;
}

.markdown-body {
  color: var(--text-primary);
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 1.4em 0 0.7em;
  line-height: 1.2;
  letter-spacing: -0.03em;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol),
.markdown-body :deep(blockquote) {
  line-height: 1.9;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol),
.markdown-body :deep(pre),
.markdown-body :deep(blockquote),
.markdown-body :deep(table) {
  margin: 0 0 1.2rem;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.3rem;
}

.markdown-body :deep(li + li) {
  margin-top: 0.35rem;
}

.markdown-body :deep(a) {
  color: var(--text-primary);
  text-decoration-color: var(--line-strong);
  text-underline-offset: 0.18em;
}

.markdown-body :deep(blockquote) {
  margin-left: 0;
  padding: 0.9rem 1rem;
  border-left: 3px solid var(--line-strong);
  border-radius: 0 16px 16px 0;
  color: var(--text-secondary);
  background: var(--bg-panel);
}

.markdown-body :deep(pre) {
  overflow: auto;
  padding: 16px 18px;
  border-radius: 18px;
  background: var(--bg-panel);
  color: var(--text-primary);
  border: 1px solid var(--line);
}

.markdown-body :deep(code) {
  font-size: 0.92em;
  word-break: break-word;
}

.markdown-body :deep(pre code) {
  color: inherit;
  word-break: normal;
}

.markdown-body :deep(img) {
  display: block;
  max-width: 100%;
  margin: 1.4rem auto;
  border-radius: 18px;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid var(--line);
  table-layout: fixed;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 12px 14px;
  border-bottom: 1px solid var(--line);
  text-align: left;
  word-break: break-word;
}

.markdown-body :deep(th) {
  background: var(--bg-panel);
  color: var(--text-secondary);
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.markdown-body :deep(tr:last-child td) {
  border-bottom: 0;
}

@media (max-width: 960px) {
  .article-hero {
    grid-template-columns: 1fr;
  }

  .article-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .article-cover {
    max-height: 320px;
  }

  .article-body {
    max-width: 100%;
    margin: 0;
  }
}

@media (max-width: 720px) {
  .article-panel,
  .comment-panel {
    padding: 22px;
  }

  .comment-heading,
  .comment-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .article-overview {
    grid-template-columns: 1fr;
  }

  .article-cover {
    max-height: 240px;
  }
}
</style>
