import { computed, ref, watch } from 'vue';
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
      themeDark: '深黑',
      themeLight: '黑白',
      localeZh: '中文',
      localeEn: 'EN'
    },
    main: {
      navHome: '首页',
      navArchive: '归档',
      navCategories: '分类',
      navConsole: '控制台',
      brandSubtitle: '黑白灰视角下的产品、工程与系统笔记。',
      footerProduct: 'Vue 3 + Spring Boot 个人博客系统',
      footerOps: '文章、分类、评论审核与后台管理'
    },
    admin: {
      consoleTitle: '控制台',
      controlLayer: '控制层',
      blogAdmin: '博客后台',
      operator: '当前用户',
      menuOverview: '总览',
      menuPosts: '文章',
      menuTaxonomy: '分类标签',
      menuComments: '评论',
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
      signalTags: '主题标签'
    },
    login: {
      adminAccess: '后台入口',
      enterConsole: '进入控制台',
      defaultAccount: '默认演示账号：admin / Admin123!',
      username: '用户名',
      password: '密码',
      signIn: '登录',
      backToSite: '返回前台',
      loginSuccess: '登录成功'
    },
    archive: {
      title: '归档矩阵',
      summary: '支持关键字搜索，并按分类、标签和分页组合筛选文章。',
      postsIndexed: '共索引 {count} 篇文章',
      searchPlaceholder: '搜索标题、摘要或关键字',
      keyword: '关键字',
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
      emailPlaceholder: '用于回复通知',
      commentPlaceholder: '说点什么',
      submitComment: '提交评论',
      commentSubmitted: '评论已提交，等待审核'
    },
    dashboard: {
      totalPosts: '文章总数',
      categories: '分类数量',
      tags: '标签数量',
      pendingComments: '待审核评论',
      recentPosts: '最近文章',
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
      deleteConfirmBody: '删除后将无法恢复，是否继续？',
      postDeleted: '文章已删除',
      title: '文章管理',
      hint: '按关键字、状态和分类筛选文章。',
      newPost: '新建文章',
      searchPlaceholder: '搜索标题、摘要或 slug',
      allStatus: '全部状态',
      published: '已发布',
      draft: '草稿',
      allCategories: '全部分类',
      apply: '筛选',
      reset: '重置',
      totalPageInfo: '共 {total} 篇文章，当前第 {page} / {pages} 页',
      colTitle: '标题',
      colCategory: '分类',
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
      slugPlaceholder: '留空则自动生成',
      summary: '摘要',
      coverImageUrl: '封面图 URL',
      content: '正文',
      livePreview: 'Markdown 实时预览',
      bold: '加粗',
      italic: '斜体',
      link: '链接',
      list: '列表',
      quote: '引用',
      code: '代码',
      image: '图片',
      markdown: 'Markdown',
      preview: '预览',
      writeMarkdown: '在这里编写 Markdown 内容',
      category: '分类',
      tags: '标签',
      status: '状态',
      published: '已发布',
      draft: '草稿',
      featured: '精选推荐',
      allowComments: '允许评论',
      savePost: '保存文章',
      defaultContent: `## 开始写作

这个编辑器支持 **Markdown** 实时预览。

- 标题
- 列表
- 引用
- 代码块
- 链接与图片

\`\`\`java
System.out.println("Hello, blog");
\`\`\`
`
    }
  },
  'en-US': {
    controls: {
      switchTheme: 'Switch theme',
      switchLanguage: 'Switch language',
      themeCaption: 'Theme',
      languageCaption: 'Lang',
      themeDark: 'Dark',
      themeLight: 'Monochrome',
      localeZh: '中文',
      localeEn: 'EN'
    },
    main: {
      navHome: 'Home',
      navArchive: 'Archive',
      navCategories: 'Categories',
      navConsole: 'Console',
      brandSubtitle: 'Product, engineering, and system notes in black, white, and grayscale.',
      footerProduct: 'Vue 3 + Spring Boot personal publishing system',
      footerOps: 'Articles, taxonomy, moderation, and admin operations'
    },
    admin: {
      consoleTitle: 'Console',
      controlLayer: 'Control Layer',
      blogAdmin: 'Blog Admin',
      operator: 'Operator',
      menuOverview: 'Overview',
      menuPosts: 'Posts',
      menuTaxonomy: 'Taxonomy',
      menuComments: 'Comments',
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
      signalTags: 'Signal Tags'
    },
    login: {
      adminAccess: 'Admin Access',
      enterConsole: 'Enter Control Console',
      defaultAccount: 'Default demo account: admin / Admin123!',
      username: 'Username',
      password: 'Password',
      signIn: 'Sign In',
      backToSite: 'Back to Site',
      loginSuccess: 'Login successful'
    },
    archive: {
      title: 'Archive Matrix',
      summary: 'Search by keyword and narrow the feed with category, tag, and pagination filters.',
      postsIndexed: '{count} posts indexed',
      searchPlaceholder: 'Search titles, summaries, or keywords',
      keyword: 'Keyword',
      category: 'Category',
      tag: 'Tag',
      noPosts: 'No posts matched the current filter set.',
      pageInfo: 'Page {page} / {total}'
    },
    categories: {
      title: 'Category Index',
      tagCloud: 'Tag Cloud',
      noDescription: 'No description provided yet.',
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
      title: 'Post Operations',
      hint: 'Filter the content inventory by keyword, status, and category.',
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
      livePreview: 'Markdown with live preview',
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

This editor supports **Markdown** with real-time preview.

- Headings
- Lists
- Quotes
- Code blocks
- Links and images

\`\`\`java
System.out.println("Hello, blog");
\`\`\`
`
    }
  }
};

const readStorage = (key, fallback) => {
  const value = localStorage.getItem(key);
  return value || fallback;
};

export const usePreferencesStore = defineStore('preferences', () => {
  const theme = ref(readStorage(STORAGE_THEME_KEY, 'dark'));
  const locale = ref(readStorage(STORAGE_LOCALE_KEY, 'zh-CN'));

  const t = (key, params = {}) => {
    const dictionary = messages[locale.value] || messages['en-US'];
    const value = key.split('.').reduce((acc, part) => acc?.[part], dictionary);
    if (typeof value !== 'string') {
      return key;
    }

    return Object.entries(params).reduce((result, [paramKey, paramValue]) => {
      return result.replaceAll(`{${paramKey}}`, String(paramValue));
    }, value);
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
