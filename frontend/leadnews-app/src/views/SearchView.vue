<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  parseHighlight,
  useAuthStore,
  type HighlightPart,
  type SearchHistory,
  type SearchResult
} from '@leadnews/shared'
import { searchApi } from '../api'

type SuggestionItem = { value: string }
type SuggestionCallback = (items: SuggestionItem[]) => void

const auth = useAuthStore()
const keyword = ref('')
const sort = ref('RELEVANCE')
const results = ref<SearchResult[]>([])
const history = ref<SearchHistory[]>([])
const loading = ref(false)

let suggestionTimer: number | undefined
let suggestionController: AbortController | undefined
let suggestionRequestId = 0

function fetchSuggestions(query: string, callback: SuggestionCallback) {
  window.clearTimeout(suggestionTimer)
  suggestionController?.abort()

  const prefix = query.trim()
  const requestId = ++suggestionRequestId
  if (!prefix) {
    callback([])
    return
  }

  suggestionTimer = window.setTimeout(async () => {
    suggestionController = new AbortController()
    try {
      const values = await searchApi.suggestions(prefix, suggestionController.signal)
      if (requestId === suggestionRequestId) {
        callback(values.map(value => ({ value })))
      }
    } catch (error) {
      if (requestId === suggestionRequestId) callback([])
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        console.debug(error)
      }
    }
  }, 300)
}

async function search() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    results.value = (await searchApi.articles({
      keyword: keyword.value.trim(),
      page: 1,
      size: 20,
      sort: sort.value
    })).items
    if (auth.isAuthenticated) await loadHistory()
  } catch {
    ElMessage.error('搜索暂不可用')
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  try {
    history.value = await searchApi.history()
  } catch {
    history.value = []
  }
}

async function remove(id: number) {
  await searchApi.deleteHistory(id)
  await loadHistory()
}

async function clear() {
  await searchApi.clearHistory()
  history.value = []
}

function choose(value: string) {
  keyword.value = value
  search()
}

function parts(values: string[], fallback: string): HighlightPart[] {
  return parseHighlight(values[0] || fallback)
}

onBeforeUnmount(() => {
  window.clearTimeout(suggestionTimer)
  suggestionController?.abort()
})

if (auth.isAuthenticated) loadHistory()
</script>

<template>
  <main class="page">
    <section class="hero">
      <h1>搜索资讯</h1>
      <p>支持全文检索、关键词高亮与时间排序。</p>
    </section>
    <section class="panel">
      <div class="toolbar">
        <el-autocomplete
          v-model="keyword"
          :fetch-suggestions="fetchSuggestions"
          placeholder="输入关键词"
          style="flex: 1"
          @select="(item: SuggestionItem) => choose(item.value)"
          @keyup.enter="search"
        />
        <el-select v-model="sort" style="width: 130px">
          <el-option label="相关度" value="RELEVANCE" />
          <el-option label="最新" value="TIME" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
      </div>

      <div v-if="auth.isAuthenticated && history.length" class="toolbar">
        <span class="meta">最近搜索</span>
        <el-tag
          v-for="item in history"
          :key="item.id"
          closable
          @close="remove(item.id)"
          @click="choose(item.keyword)"
        >
          {{ item.keyword }}
        </el-tag>
        <el-button text @click="clear">清空</el-button>
      </div>

      <article v-for="item in results" :key="item.article.articleId" class="article-card">
        <div>
          <RouterLink :to="`/article/${item.article.articleId}`">
            <h2>
              <template
                v-for="(part, index) in parts(item.highlightedTitle, item.article.title)"
                :key="index"
              >
                <mark v-if="part.highlighted" class="highlight">{{ part.text }}</mark>
                <template v-else>{{ part.text }}</template>
              </template>
            </h2>
          </RouterLink>
          <p>
            <template
              v-for="(part, index) in parts(item.highlightedContent, item.article.content)"
              :key="index"
            >
              <mark v-if="part.highlighted" class="highlight">{{ part.text }}</mark>
              <template v-else>{{ part.text }}</template>
            </template>
          </p>
          <p class="meta">
            {{ item.article.authorName }} · {{ item.article.publishTime?.replace('T', ' ') }}
          </p>
        </div>
      </article>

      <div v-if="!loading && !results.length" class="empty">输入关键词，发现匹配内容</div>
    </section>
  </main>
</template>
