<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
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
const readingProgress = ref(0);
const commentForm = ref({
  postId: null,
  content: ''
});

const copy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        discussion: '评论交流',
        replyTitle: '登录后发表评论',
        replyBody: '文章公开可读，但评论需要先登录账号。',
        goLogin: '去登录',
        currentUser: '当前账号',
        submitSuccess: '评论已提交，等待审核',
        backToList: '返回文章列表',
        publishedAt: '发布时间',
        readTime: '阅读时长',
        category: '分类',
        comments: '评论数',
        articleNote: '阅读说明',
        articleNoteBody: '正文区采用更宽松的阅读节奏，建议在桌面端配合右侧信息栏使用。',
        openArchives: '查看更多文章',
        shareHint: '如果这篇文章有帮助，可以继续浏览同类主题或参与评论。'
      }
    : {
        discussion: 'Discussion',
        replyTitle: 'Sign in to comment',
        replyBody: 'Reading is public, but posting comments requires an account.',
        goLogin: 'Sign In',
        currentUser: 'Current Account',
        submitSuccess: 'Comment submitted for moderation',
        backToList: 'Back to articles',
        publishedAt: 'Published',
        readTime: 'Reading time',
        category: 'Category',
        comments: 'Comments',
        articleNote: 'Reading note',
        articleNoteBody: 'The article body uses a wider editorial rhythm. On desktop, the side rail is meant to stay visible while reading.',
        openArchives: 'View more articles',
        shareHint: 'If this article was useful, continue with similar topics or join the discussion.'
      }
);

const renderedContent = computed(() => renderMarkdown(post.value?.content || ''));
const plainArticle = computed(() => String(post.value?.content || '').replace(/[#>*_`\-[\]()]/g, ' ').replace(/\s+/g, ' ').trim());
const readingMinutes = computed(() => {
  const length = plainArticle.value.length;
  if (!length) {
    return 1;
  }
  return Math.max(1, Math.ceil(length / 420));
});
const articleMeta = computed(() => [
  { label: copy.value.publishedAt, value: preferences.formatDateTime(post.value?.publishedAt) || '-' },
  { label: copy.value.readTime, value: `${readingMinutes.value} min` },
  { label: copy.value.category, value: post.value?.categoryName || preferences.t('post.uncategorized') },
  { label: copy.value.comments, value: String(post.value?.comments?.length || 0) }
]);
const hasCoverImage = computed(() => Boolean(post.value?.coverImage));
const coverStyle = computed(() => ({
  backgroundImage: hasCoverImage.value
    ? `linear-gradient(180deg, rgba(8, 10, 14, 0.06), rgba(8, 10, 14, 0.76)), url(${post.value.coverImage})`
    : 'none'
}));

const getViewCacheKey = (slug) => `blog:viewed:${slug}`;

const updateReadingProgress = () => {
  const body = document.querySelector('.article-reading-body');
  if (!body) {
    readingProgress.value = 0;
    return;
  }
  const rect = body.getBoundingClientRect();
  const viewport = window.innerHeight || 1;
  const total = Math.max(body.offsetHeight - viewport * 0.45, 1);
  const consumed = Math.min(Math.max(-rect.top + viewport * 0.18, 0), total);
  readingProgress.value = Math.max(0, Math.min(consumed / total, 1));
};

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
    requestAnimationFrame(updateReadingProgress);
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

onMounted(() => {
  loadPost();
  window.addEventListener('scroll', updateReadingProgress, { passive: true });
  window.addEventListener('resize', updateReadingProgress);
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateReadingProgress);
  window.removeEventListener('resize', updateReadingProgress);
});

watch(() => route.params.slug, loadPost);
</script>

<template>
  <div v-if="post" v-loading="loading" class="detail-page">
    <div class="reading-progress-bar" aria-hidden="true">
      <span :style="{ transform: `scaleX(${readingProgress})` }"></span>
    </div>

    <section class="section-card article-stage" :class="{ 'article-stage--no-cover': !hasCoverImage }">
      <div class="article-stage-copy">
        <router-link to="/archives" class="back-link">{{ copy.backToList }}</router-link>
        <p class="article-category muted">{{ post.categoryName }} / {{ preferences.formatDateTime(post.publishedAt) }}</p>
        <h1>{{ post.title }}</h1>
        <p class="article-summary">{{ post.summary }}</p>

        <div class="article-tag-row chip-list">
          <router-link
            v-for="tag in post.tags"
            :key="tag"
            class="chip"
            :to="{ path: '/archives', query: { tag } }"
          >
            # {{ tag }}
          </router-link>
        </div>
      </div>

      <div v-if="hasCoverImage" class="article-stage-cover" :style="coverStyle"></div>
    </section>

    <section class="article-layout">
      <article class="section-card article-reading-body">
        <div class="article-body-shell">
          <div class="article-meta-strip">
            <div v-for="item in articleMeta" :key="item.label" class="meta-pill">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <div class="content-html markdown-body article-body" v-html="renderedContent"></div>

          <div class="article-endnote">
            <strong>{{ copy.articleNote }}</strong>
            <p class="muted">{{ copy.articleNoteBody }}</p>
          </div>
        </div>
      </article>

      <aside class="article-rail">
        <section class="section-card rail-card rail-summary-card">
          <span class="rail-kicker">Overview</span>
          <h2>{{ post.title }}</h2>
          <p class="muted">{{ copy.shareHint }}</p>
          <div class="rail-metrics">
            <div>
              <span>{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
              <strong>{{ post.viewCount }}</strong>
            </div>
            <div>
              <span>{{ preferences.t('post.comments', { count: post.comments.length }) }}</span>
              <strong>{{ post.comments.length }}</strong>
            </div>
          </div>
          <router-link to="/archives" class="rail-link">{{ copy.openArchives }}</router-link>
        </section>

        <section class="section-card rail-card">
          <span class="rail-kicker">Tags</span>
          <div class="chip-list rail-chip-list">
            <router-link
              v-for="tag in post.tags"
              :key="`rail-${tag}`"
              class="chip"
              :to="{ path: '/archives', query: { tag } }"
            >
              # {{ tag }}
            </router-link>
          </div>
        </section>
      </aside>
    </section>

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
.detail-page {
  display: grid;
  gap: 24px;
}

.reading-progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  z-index: 30;
  background: rgba(255, 255, 255, 0.05);
}

.reading-progress-bar span {
  display: block;
  width: 100%;
  height: 100%;
  transform-origin: left center;
  background: linear-gradient(90deg, rgba(217, 167, 76, 0.98), rgba(255, 236, 201, 0.98));
}

html[data-theme='light'] .reading-progress-bar span {
  background: linear-gradient(90deg, rgba(20, 20, 22, 0.98), rgba(90, 90, 95, 0.98));
}

.article-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 24px;
  padding: 30px;
  background:
    radial-gradient(circle at top right, rgba(214, 165, 88, 0.09), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.015), rgba(255, 255, 255, 0.02));
}

.article-stage--no-cover {
  grid-template-columns: 1fr;
}

.article-stage-copy {
  display: grid;
  align-content: start;
  gap: 14px;
}

.back-link {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: var(--text-main);
}

.article-category,
.comment-kicker,
.rail-kicker {
  margin: 0;
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.article-stage-copy h1,
.rail-summary-card h2 {
  margin: 0;
  font-family: var(--font-display);
  line-height: 1;
  letter-spacing: -0.05em;
}

.article-stage-copy h1 {
  max-width: 860px;
  font-size: clamp(2.4rem, 4.8vw, 4.6rem);
}

.article-summary {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 1.02rem;
  line-height: 1.92;
}

.article-stage-cover {
  min-height: 380px;
  border-radius: 26px;
  border: 1px solid var(--line);
  background-size: cover;
  background-position: center;
}

.article-tag-row {
  margin-top: 6px;
}

.article-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  align-items: start;
}

.article-reading-body,
.comment-panel,
.rail-card {
  padding: 28px;
}

.article-body-shell {
  max-width: 860px;
  margin: 0 auto;
  display: grid;
  gap: 28px;
}

.article-meta-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meta-pill {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.meta-pill span {
  color: var(--text-secondary);
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.meta-pill strong {
  font-family: var(--font-display);
  font-size: 1.06rem;
  line-height: 1.2;
}

.article-body {
  font-size: 1.07rem;
  color: var(--text-main);
}

.article-body :deep(h1),
.article-body :deep(h2),
.article-body :deep(h3),
.article-body :deep(h4) {
  margin: 1.45em 0 0.72em;
  font-family: var(--font-display);
  line-height: 1.12;
  letter-spacing: -0.045em;
  text-wrap: balance;
}

.article-body :deep(h1) {
  font-size: clamp(2.3rem, 3.6vw, 3.1rem);
}

.article-body :deep(h2) {
  position: relative;
  padding-top: 0.25rem;
}

.article-body :deep(h2) {
  font-size: clamp(1.9rem, 2.6vw, 2.5rem);
}

.article-body :deep(h3) {
  font-size: clamp(1.45rem, 2vw, 1.9rem);
}

.article-body :deep(p),
.article-body :deep(ul),
.article-body :deep(ol),
.article-body :deep(blockquote) {
  line-height: 2;
}

.article-body :deep(p),
.article-body :deep(ul),
.article-body :deep(ol),
.article-body :deep(pre),
.article-body :deep(blockquote),
.article-body :deep(table),
.article-body :deep(figure) {
  margin: 0 0 1.45rem;
}

.article-body :deep(p) {
  color: rgba(245, 245, 246, 0.95);
}

.article-body :deep(ul),
.article-body :deep(ol) {
  padding-left: 1.45rem;
}

.article-body :deep(li + li) {
  margin-top: 0.5rem;
}

.article-body :deep(a) {
  color: var(--text-main);
  text-decoration-color: rgba(217, 167, 76, 0.56);
  text-decoration-thickness: 1.5px;
  text-underline-offset: 0.2em;
}

.article-body :deep(blockquote) {
  margin-left: 0;
  padding: 1.1rem 1.2rem 1.1rem 1.3rem;
  border-left: 4px solid rgba(217, 167, 76, 0.82);
  border-radius: 0 18px 18px 0;
  color: rgba(235, 235, 238, 0.78);
  background: linear-gradient(90deg, rgba(217, 167, 76, 0.12), rgba(255, 255, 255, 0.03));
}

.article-body :deep(pre) {
  overflow: auto;
  padding: 20px 22px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(6, 7, 11, 0.98), rgba(2, 2, 4, 0.98));
  color: var(--text-primary);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04), 0 20px 34px rgba(0, 0, 0, 0.18);
}

.article-body :deep(code) {
  font-size: 0.92em;
  word-break: break-word;
  font-family: var(--font-mono);
}

.article-body :deep(p code),
.article-body :deep(li code),
.article-body :deep(blockquote code),
.article-body :deep(td code) {
  padding: 0.16rem 0.46rem;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #f8e9bf;
}

.article-body :deep(pre code) {
  color: inherit;
  word-break: normal;
  padding: 0;
  background: transparent;
}

.article-body :deep(img) {
  display: block;
  max-width: 100%;
  margin: 1.9rem auto;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24px 46px rgba(0, 0, 0, 0.24);
}

.article-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid var(--line);
  table-layout: fixed;
  background: rgba(255, 255, 255, 0.02);
}

.article-body :deep(th),
.article-body :deep(td) {
  padding: 13px 15px;
  border-bottom: 1px solid var(--line);
  text-align: left;
  word-break: break-word;
}

.article-body :deep(th) {
  background: rgba(255, 255, 255, 0.055);
  color: var(--text-secondary);
  font-size: 0.78rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.article-body :deep(hr) {
  margin: 2rem 0;
  border: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
}

.article-body :deep(tr:last-child td) {
  border-bottom: 0;
}

.article-endnote {
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

.article-endnote strong {
  display: block;
  margin-bottom: 8px;
  font-family: var(--font-display);
  font-size: 1.08rem;
}

.article-endnote p {
  margin: 0;
  line-height: 1.82;
}

.article-rail {
  display: grid;
  gap: 18px;
  position: sticky;
  top: 92px;
}

.rail-card {
  display: grid;
  gap: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.02);
}

.rail-summary-card p {
  margin: 0;
  line-height: 1.78;
}

.rail-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.rail-metrics div {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.rail-metrics span {
  color: var(--text-secondary);
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.rail-metrics strong {
  font-family: var(--font-display);
  font-size: 1.4rem;
}

.rail-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(192, 192, 192, 0.92));
  color: #111214;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

html[data-theme='light'] .rail-link {
  background: linear-gradient(135deg, rgba(29, 29, 31, 0.95), rgba(68, 68, 74, 0.94));
  color: #ffffff;
  border-color: rgba(20, 20, 20, 0.14);
}

.rail-chip-list {
  gap: 10px;
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

html[data-theme='light'] .article-stage {
  background:
    radial-gradient(circle at top right, rgba(196, 145, 61, 0.1), transparent 26%),
    linear-gradient(180deg, rgba(17, 17, 17, 0.012), rgba(17, 17, 17, 0.02));
}

html[data-theme='light'] .article-body :deep(p) {
  color: rgba(22, 22, 24, 0.92);
}

html[data-theme='light'] .article-body :deep(blockquote) {
  color: rgba(22, 22, 24, 0.72);
  background: linear-gradient(90deg, rgba(196, 145, 61, 0.12), rgba(17, 17, 17, 0.02));
}

html[data-theme='light'] .article-body :deep(p code),
html[data-theme='light'] .article-body :deep(li code),
html[data-theme='light'] .article-body :deep(blockquote code),
html[data-theme='light'] .article-body :deep(td code) {
  background: rgba(17, 17, 17, 0.06);
  color: #7b4c00;
}

html[data-theme='light'] .article-body :deep(img) {
  border-color: rgba(17, 17, 17, 0.08);
  box-shadow: 0 22px 42px rgba(30, 25, 20, 0.08);
}

html[data-theme='light'] .article-body :deep(table) {
  background: rgba(17, 17, 17, 0.02);
}

html[data-theme='light'] .article-body :deep(th) {
  background: rgba(17, 17, 17, 0.04);
  color: rgba(17, 17, 17, 0.64);
}

html[data-theme='light'] .article-body :deep(hr) {
  border-top-color: rgba(17, 17, 17, 0.1);
}

@media (max-width: 1100px) {
  .article-stage,
  .article-layout {
    grid-template-columns: 1fr;
  }

  .article-rail {
    position: static;
    grid-template-columns: 1fr 1fr;
  }

  .article-meta-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .article-stage,
  .article-reading-body,
  .rail-card,
  .comment-panel {
    padding: 22px;
  }

  .article-stage-cover {
    min-height: 260px;
  }

  .article-meta-strip,
  .article-rail,
  .rail-metrics {
    grid-template-columns: 1fr;
  }

  .comment-heading,
  .comment-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
