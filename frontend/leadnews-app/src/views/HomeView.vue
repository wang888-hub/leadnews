<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { Article, Channel, HotArticle } from '@leadnews/shared'
import { articleApi } from '../api'

const channels = ref<Channel[]>([])
const articles = ref<Article[]>([])
const hot = ref<HotArticle[]>([])
const channelId = ref<number>()
const loading = ref(true)
const degraded = ref(false)
const showDevelopmentDetails = import.meta.env.DEV

async function load() {
  loading.value = true
  try {
    const [page, hotResponse] = await Promise.all([
      articleApi.list({ page: 1, size: 50, channelId: channelId.value }),
      articleApi.hot(channelId.value)
    ])
    articles.value = page.records
    hot.value = hotResponse.items
    degraded.value = hotResponse.degraded
  } catch {
    ElMessage.error('资讯加载失败')
  } finally {
    loading.value = false
  }
}

function heat(score: number) {
  return Number.isFinite(score) ? Math.max(1, Math.round(score)).toLocaleString('zh-CN') : '—'
}

onMounted(async () => {
  try {
    channels.value = await articleApi.channels()
  } finally {
    await load()
  }
})

watch(channelId, load)
</script>

<template>
  <main class="page">
    <section class="hero">
      <p class="eyebrow">CURATED · REAL TIME</p>
      <h1>把复杂世界，读得更清楚</h1>
      <p>频道资讯、实时热点与 AI 摘要，统一从真实服务获取。</p>
    </section>

    <div class="toolbar">
      <el-button :type="channelId === undefined ? 'primary' : 'default'" @click="channelId = undefined">
        全部
      </el-button>
      <el-button
        v-for="channel in channels"
        :key="channel.id"
        :type="channelId === channel.id ? 'primary' : 'default'"
        @click="channelId = channel.id"
      >
        {{ channel.name }}
      </el-button>
    </div>

    <div class="grid">
      <section class="panel" v-loading="loading">
        <article v-for="item in articles" :key="item.id" class="article-card">
          <div>
            <RouterLink :to="`/article/${item.id}`"><h2>{{ item.title }}</h2></RouterLink>
            <p class="meta">
              <span>{{ item.authorName }}</span>
              <span>{{ item.channelName }}</span>
              <span>{{ item.publishTime?.replace('T', ' ') }}</span>
              <span>♥ {{ item.likeCount }}</span>
              <span>阅 {{ item.viewCount }}</span>
            </p>
          </div>
          <img
            v-if="item.coverImages?.[0]"
            class="cover"
            :src="item.coverImages[0]"
            alt="文章封面"
          >
        </article>
        <div v-if="!loading && !articles.length" class="empty">暂无文章</div>
      </section>

      <aside class="panel">
        <h2>此刻热门</h2>
        <p class="meta">综合点赞、浏览并按发布时间衰减，仅展示前 10 名</p>
        <p v-if="degraded && showDevelopmentDetails" class="meta">当前为降级榜单，热度暂不可用</p>
        <ol>
          <li v-for="(item, index) in hot" :key="item.article.articleId" style="margin: 16px 0">
            <strong class="hot-rank">{{ index + 1 }}</strong>
            <RouterLink :to="`/article/${item.article.articleId}`">
              {{ item.article.title }}
            </RouterLink>
            <span class="meta" style="margin-left: 8px">
              热度指数 {{ degraded ? '—' : heat(item.score) }}
            </span>
          </li>
        </ol>
        <div v-if="!hot.length" class="empty">暂无热点</div>
      </aside>
    </div>
  </main>
</template>
