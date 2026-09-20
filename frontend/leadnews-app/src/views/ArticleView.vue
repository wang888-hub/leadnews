<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ApiError, useAuthStore, type Article, type ArticleComment } from '@leadnews/shared'
import { articleApi, behaviorApi, commentApi } from '../api'

const route = useRoute()
const auth = useAuthStore()
const article = ref<Article>()
const loading = ref(true)
const acting = ref(false)
const viewSent = ref(false)
const comments = ref<ArticleComment[]>([])
const commentTotal = ref(0)
const commentContent = ref('')
const commenting = ref(false)

async function load() {
  try {
    const id = Number(route.params.id)
    article.value = await articleApi.detail(id)
    await loadComments(id)
    if (auth.isAuthenticated && !viewSent.value) {
      viewSent.value = true
      const behavior = await behaviorApi.view(id)
      if (article.value) article.value.viewCount = behavior.viewCount
    }
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '文章加载失败')
  } finally {
    loading.value = false
  }
}

async function loadComments(articleId: number) {
  const page = await commentApi.list(articleId)
  comments.value = page.records
  commentTotal.value = page.total
}

async function submitComment() {
  if (!auth.isAuthenticated) return void ElMessage.warning('请先登录后评论')
  if (!article.value) return
  const content = commentContent.value.trim()
  if (!content) return void ElMessage.warning('请输入评论内容')
  commenting.value = true
  try {
    await commentApi.create(article.value.id, content)
    commentContent.value = ''
    await loadComments(article.value.id)
    ElMessage.success('评论发布成功')
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '评论发布失败')
  } finally {
    commenting.value = false
  }
}

async function toggle() {
  if (!auth.isAuthenticated) return void ElMessage.warning('请先登录')
  if (!article.value) return
  acting.value = true
  try {
    const behavior = article.value.liked
      ? await behaviorApi.unlike(article.value.id)
      : await behaviorApi.like(article.value.id)
    article.value.liked = behavior.liked
    article.value.likeCount = behavior.likeCount
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '操作失败')
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page" v-loading="loading">
    <article v-if="article" class="panel">
      <p class="meta">
        {{ article.channelName }} · {{ article.authorName }} ·
        {{ article.publishTime?.replace('T', ' ') }}
      </p>
      <h1 style="font-size: 38px">{{ article.title }}</h1>

      <template v-for="(block, index) in article.content" :key="index">
        <p v-if="block.type === 'text'" class="content-block">{{ block.value }}</p>
        <img v-else class="content-image" :src="block.value" alt="正文图片">
      </template>

      <section v-if="article.summaryStatus === 'SUCCESS'" class="ai-summary">
        <strong>AI 摘要 · AI 生成</strong>
        <p>{{ article.summary }}</p>
      </section>
      <section
        v-else-if="['PENDING', 'GENERATING'].includes(article.summaryStatus || '')"
        class="ai-summary"
      >
        <strong>AI 摘要</strong>
        <p>摘要正在异步生成，不影响正文阅读。</p>
      </section>
      <section v-else-if="article.summaryStatus === 'FAILED'" class="ai-summary">
        <strong>AI 摘要暂不可用</strong>
        <p>本篇文章的摘要生成失败，正文和文章发布状态不受影响。</p>
      </section>
      <section v-else class="ai-summary">
        <strong>AI 摘要</strong>
        <p>本篇文章暂未生成摘要。</p>
      </section>

      <div class="toolbar">
        <el-button :type="article.liked ? 'danger' : 'primary'" :loading="acting" @click="toggle">
          {{ article.liked ? '取消点赞' : '点赞' }} · {{ article.likeCount }}
        </el-button>
        <span class="meta">评论 {{ commentTotal }}</span>
        <span class="meta">浏览 {{ article.viewCount }}</span>
      </div>

      <section style="margin-top: 32px">
        <h2>评论（{{ commentTotal }}）</h2>
        <div class="toolbar" style="align-items: flex-start">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="说说你的看法"
          />
          <el-button type="primary" :loading="commenting" @click="submitComment">发表评论</el-button>
        </div>
        <article v-for="comment in comments" :key="comment.id" class="article-card">
          <div>
            <strong>{{ comment.authorName }}</strong>
            <p>{{ comment.content }}</p>
            <p class="meta">{{ comment.createdTime?.replace('T', ' ') }}</p>
          </div>
        </article>
        <div v-if="!comments.length" class="empty">还没有评论，来发表第一条吧</div>
      </section>
    </article>
    <div v-else-if="!loading" class="empty">文章不存在</div>
  </main>
</template>
