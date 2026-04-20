import { ref, watch } from 'vue';
import { defineStore } from 'pinia';

const STORAGE_THEME_KEY = 'blog-ui-theme';
const STORAGE_LOCALE_KEY = 'blog-ui-locale';

const messages = {
  'zh-CN': {
    controls: {
      switchTheme: '切换主题',
      switchLanguage: '切换语言',
      themeCaption: '主题',
      languageCaption: '语言',
      themeDark: '深色',
      themeLight: '浅色',
      localeZh: '中文',
      localeEn: 'EN'
    },
    main: {
      navHome: '首页',
      navKnowledge: '知识问答',
      navArchive: '归档',
      navCategories: '分类',
      navConsole: '后台',
      brandSubtitle: '围绕产品、工程与系统实践的长期写作空间。',
      footerProduct: '基于 Vue 3 与 Spring Boot 的个人博客系统',
      footerOps: '支持文章发布、标签分类、评论审核与后台管理'
    },
    admin: {
      consoleTitle: '管理后台',
      controlLayer: '控制层',
      blogAdmin: '博客运营台',
      operator: '当前用户',
      menuOverview: '总览',
      menuPosts: '文章',
      menuTaxonomy: '标签管理',
      menuComments: '评论',
      menuRagFeedback: 'RAG 反馈',
      backToSite: '返回前台',
      signOut: '退出登录'
    },
    home: {
      posts: '文章',
      categories: '分类',
      tags: '标签',
      comments: '评论',
      featuredPosts: '精选文章',
      browseAll: '查看全部',
      latestReleases: '最新发布',
      signalTags: '热门标签'
    },
    login: {
      adminAccess: '后台入口',
      enterConsole: '进入管理后台',
      defaultAccount: '默认演示账号：admin / Admin123!',
      username: '用户名',
      password: '密码',
      signIn: '登录',
      backToSite: '返回前台',
      loginSuccess: '登录成功'
    },
    archive: {
      title: '文章归档',
      summary: '支持按关键词、分类、标签与分页筛选文章。',
      postsIndexed: '共收录 {count} 篇文章',
      searchPlaceholder: '搜索标题、摘要或关键词',
      keyword: '关键词',
      category: '分类',
      tag: '标签',
      noPosts: '当前筛选条件下没有匹配文章。',
      pageInfo: '第 {page} / {total} 页'
    },
    categories: {
      title: '分类索引',
      tagCloud: '标签云',
      noDescription: '暂无描述',
      postsSuffix: '篇'
    },
    post: {
      uncategorized: '未分类',
      views: '{count} 次阅读',
      comments: '{count} 条评论',
      commentsTitle: '评论区',
      commentsDisabled: '本文已关闭评论',
      nickname: '昵称',
      email: '邮箱',
      comment: '评论',
      nicknamePlaceholder: '输入你的昵称',
      emailPlaceholder: '用于接收回复通知',
      commentPlaceholder: '说说你的看法',
      submitComment: '提交评论',
      commentSubmitted: '评论已提交，等待审核'
    },
    dashboard: {
      totalPosts: '文章总数',
      categories: '分类数量',
      tags: '标签数量',
      pendingComments: '待审核评论',
      recentPosts: '最近更新',
      latestComments: '最新评论'
    },
    taxonomy: {
      categoryDesign: '分类设计',
      tagDesign: '标签设计',
      name: '名称',
      slug: 'Slug',
      description: '描述',
      saveCategory: '保存分类',
      saveTag: '保存标签',
      clear: '清空',
      edit: '编辑',
      delete: '删除',
      categorySaved: '分类已保存',
      tagSaved: '标签已保存',
      categoryDeleted: '分类已删除',
      tagDeleted: '标签已删除'
    },
    commentManage: {
      title: '评论审核',
      reviewHint: '审核前台用户提交的评论内容。',
      records: '{count} 条记录',
      nickname: '昵称',
      post: '文章',
      comment: '评论内容',
      status: '状态',
      submittedAt: '提交时间',
      actions: '操作',
      approve: '通过',
      reject: '驳回',
      statusUpdated: '评论状态已更新'
    },
    postManage: {
      deleteConfirmTitle: '删除文章',
      deleteConfirmBody: '删除后将无法恢复，确认继续吗？',
      postDeleted: '文章已删除',
      title: '文章管理',
      hint: '按关键词和状态筛选文章，并快速进入编辑。',
      newPost: '新建文章',
      searchPlaceholder: '搜索标题、摘要或 slug',
      allStatus: '全部状态',
      published: '已发布',
      draft: '草稿',
      allCategories: '全部分类',
      apply: '应用',
      reset: '重置',
      totalPageInfo: '共 {total} 篇，当前第 {page} / {pages} 页',
      colTitle: '标题',
      colCategory: '分类',
      colTags: '标签',
      colStatus: '状态',
      colViews: '阅读',
      colUpdatedAt: '更新时间',
      colActions: '操作',
      edit: '编辑',
      delete: '删除',
      previous: '上一页',
      next: '下一页'
    },
    postEditor: {
      postSaved: '文章已保存',
      editPost: '编辑文章',
      newPost: '新建文章',
      backToList: '返回列表',
      title: '标题',
      slug: 'Slug',
      slugPlaceholder: '留空自动生成',
      summary: '摘要',
      coverImageUrl: '封面图地址',
      content: '正文',
      livePreview: '实时预览',
      bold: '加粗',
      italic: '斜体',
      link: '链接',
      list: '列表',
      quote: '引用',
      code: '代码',
      image: '图片',
      markdown: 'Markdown',
      preview: '预览',
      writeMarkdown: '在这里输入 Markdown 内容',
      category: '分类',
      tags: '标签',
      status: '状态',
      published: '已发布',
      draft: '草稿',
      featured: '精选推荐',
      allowComments: '允许评论',
      savePost: '保存文章',
      defaultContent: `## 开始写作

这里支持 Markdown 实时预览。
`
    }
  },
  'en-US': {
    controls: {
      switchTheme: 'Switch theme',
      switchLanguage: 'Switch language',
      themeCaption: 'Theme',
      languageCaption: 'Language',
      themeDark: 'Dark',
      themeLight: 'Light',
      localeZh: '中文',
      localeEn: 'EN'
    },
    main: {
      navHome: 'Home',
      navKnowledge: 'Knowledge',
      navArchive: 'Archive',
      navCategories: 'Categories',
      navConsole: 'Admin',
      brandSubtitle: 'Long-form notes on product, engineering, and system practice.',
      footerProduct: 'Personal publishing system powered by Vue 3 and Spring Boot',
      footerOps: 'Articles, taxonomy, moderation, and admin operations'
    },
    admin: {
      consoleTitle: 'Admin Console',
      controlLayer: 'Control Layer',
      blogAdmin: 'Blog Operations',
      operator: 'Operator',
      menuOverview: 'Overview',
      menuPosts: 'Posts',
      menuTaxonomy: 'Tag Moderation',
      menuComments: 'Comments',
      menuRagFeedback: 'RAG Feedback',
      backToSite: 'Back to Site',
      signOut: 'Sign Out'
    },
    home: {
      posts: 'Posts',
      categories: 'Categories',
      tags: 'Tags',
      comments: 'Comments',
      featuredPosts: 'Featured Posts',
      browseAll: 'Browse All',
      latestReleases: 'Latest Releases',
      signalTags: 'Popular Tags'
    },
    login: {
      adminAccess: 'Admin Access',
      enterConsole: 'Enter Admin Console',
      defaultAccount: 'Default demo account: admin / Admin123!',
      username: 'Username',
      password: 'Password',
      signIn: 'Sign In',
      backToSite: 'Back to Site',
      loginSuccess: 'Login successful'
    },
    archive: {
      title: 'Archive',
      summary: 'Filter articles by keyword, category, tag, and pagination.',
      postsIndexed: '{count} posts indexed',
      searchPlaceholder: 'Search title, summary, or keyword',
      keyword: 'Keyword',
      category: 'Category',
      tag: 'Tag',
      noPosts: 'No posts matched the current filters.',
      pageInfo: 'Page {page} / {total}'
    },
    categories: {
      title: 'Category Index',
      tagCloud: 'Tag Cloud',
      noDescription: 'No description yet.',
      postsSuffix: 'posts'
    },
    post: {
      uncategorized: 'Uncategorized',
      views: '{count} views',
      comments: '{count} comments',
      commentsTitle: 'Comments',
      commentsDisabled: 'Comments are disabled for this post',
      nickname: 'Nickname',
      email: 'Email',
      comment: 'Comment',
      nicknamePlaceholder: 'Enter your nickname',
      emailPlaceholder: 'Used for reply notifications',
      commentPlaceholder: 'Share your thoughts',
      submitComment: 'Submit Comment',
      commentSubmitted: 'Comment submitted for moderation'
    },
    dashboard: {
      totalPosts: 'Total Posts',
      categories: 'Categories',
      tags: 'Tags',
      pendingComments: 'Pending Comments',
      recentPosts: 'Recent Posts',
      latestComments: 'Latest Comments'
    },
    taxonomy: {
      categoryDesign: 'Category Design',
      tagDesign: 'Tag Design',
      name: 'Name',
      slug: 'Slug',
      description: 'Description',
      saveCategory: 'Save Category',
      saveTag: 'Save Tag',
      clear: 'Clear',
      edit: 'Edit',
      delete: 'Delete',
      categorySaved: 'Category saved',
      tagSaved: 'Tag saved',
      categoryDeleted: 'Category deleted',
      tagDeleted: 'Tag deleted'
    },
    commentManage: {
      title: 'Comment Moderation',
      reviewHint: 'Review public comments before they are visible on the site.',
      records: '{count} records',
      nickname: 'Nickname',
      post: 'Post',
      comment: 'Comment',
      status: 'Status',
      submittedAt: 'Submitted At',
      actions: 'Actions',
      approve: 'Approve',
      reject: 'Reject',
      statusUpdated: 'Comment status updated'
    },
    postManage: {
      deleteConfirmTitle: 'Delete Post',
      deleteConfirmBody: 'This action permanently deletes the post. Continue?',
      postDeleted: 'Post deleted',
      title: 'Post Management',
      hint: 'Filter posts by keyword and status, then jump into editing quickly.',
      newPost: 'New Post',
      searchPlaceholder: 'Search title, summary, or slug',
      allStatus: 'All status',
      published: 'Published',
      draft: 'Draft',
      allCategories: 'All categories',
      apply: 'Apply',
      reset: 'Reset',
      totalPageInfo: '{total} posts total, page {page} / {pages}',
      colTitle: 'Title',
      colCategory: 'Category',
      colTags: 'Tags',
      colStatus: 'Status',
      colViews: 'Views',
      colUpdatedAt: 'Updated At',
      colActions: 'Actions',
      edit: 'Edit',
      delete: 'Delete',
      previous: 'Previous',
      next: 'Next'
    },
    postEditor: {
      postSaved: 'Post saved',
      editPost: 'Edit Post',
      newPost: 'New Post',
      backToList: 'Back to List',
      title: 'Title',
      slug: 'Slug',
      slugPlaceholder: 'Auto generated when left empty',
      summary: 'Summary',
      coverImageUrl: 'Cover Image URL',
      content: 'Content',
      livePreview: 'Live Preview',
      bold: 'Bold',
      italic: 'Italic',
      link: 'Link',
      list: 'List',
      quote: 'Quote',
      code: 'Code',
      image: 'Image',
      markdown: 'Markdown',
      preview: 'Preview',
      writeMarkdown: 'Write Markdown content here',
      category: 'Category',
      tags: 'Tags',
      status: 'Status',
      published: 'Published',
      draft: 'Draft',
      featured: 'Featured',
      allowComments: 'Allow Comments',
      savePost: 'Save Post',
      defaultContent: `## Start writing

This editor supports Markdown with live preview.
`
    }
  }
};

function readStorage(key, fallback) {
  const value = localStorage.getItem(key);
  return value || fallback;
}

export const usePreferencesStore = defineStore('preferences', () => {
  const theme = ref(readStorage(STORAGE_THEME_KEY, 'dark'));
  const locale = ref(readStorage(STORAGE_LOCALE_KEY, 'zh-CN'));

  const t = (key, params = {}) => {
    const dictionary = messages[locale.value] || messages['en-US'];
    const value = key.split('.').reduce((acc, part) => acc?.[part], dictionary);
    if (typeof value !== 'string') {
      return key;
    }

    return Object.entries(params).reduce(
      (result, [paramKey, paramValue]) => result.replaceAll(`{${paramKey}}`, String(paramValue)),
      value
    );
  };

  const applyDocumentState = () => {
    document.documentElement.dataset.theme = theme.value;
    document.documentElement.lang = locale.value;
  };

  const initialize = () => {
    if (!['dark', 'light'].includes(theme.value)) {
      theme.value = 'dark';
    }
    if (!Object.keys(messages).includes(locale.value)) {
      locale.value = 'zh-CN';
    }
    applyDocumentState();
  };

  const toggleTheme = () => {
    theme.value = theme.value === 'dark' ? 'light' : 'dark';
  };

  const toggleLocale = () => {
    locale.value = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN';
  };

  const setTheme = (value) => {
    if (['dark', 'light'].includes(value)) {
      theme.value = value;
    }
  };

  const setLocale = (value) => {
    if (Object.keys(messages).includes(value)) {
      locale.value = value;
    }
  };

  const formatDate = (value, options = {}) => {
    if (!value) {
      return '';
    }
    return new Date(value).toLocaleDateString(locale.value, options);
  };

  const formatDateTime = (value, options = {}) => {
    if (!value) {
      return '';
    }
    return new Date(value).toLocaleString(locale.value, options);
  };

  const formatMonth = (value) => {
    if (!value) {
      return '';
    }
    return new Date(value).toLocaleDateString(locale.value, {
      year: 'numeric',
      month: 'long'
    });
  };

  watch(theme, (value) => {
    localStorage.setItem(STORAGE_THEME_KEY, value);
    applyDocumentState();
  });

  watch(locale, (value) => {
    localStorage.setItem(STORAGE_LOCALE_KEY, value);
    applyDocumentState();
  });

  return {
    theme,
    locale,
    initialize,
    toggleTheme,
    toggleLocale,
    setTheme,
    setLocale,
    t,
    formatDate,
    formatDateTime,
    formatMonth
  };
});
