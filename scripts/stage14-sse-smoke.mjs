const [baseUrl, username, password, newsId = '22'] = process.argv.slice(2)
if (!baseUrl || !username || !password) {
  throw new Error('Usage: node scripts/stage14-sse-smoke.mjs <baseUrl> <username> <password> [newsId]')
}

const loginResponse = await fetch(`${baseUrl}/api/wemedia/login`, {
  method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({name: username, password})
})
if (!loginResponse.ok) throw new Error(`Login failed: HTTP ${loginResponse.status}`)
const token = (await loginResponse.json()).data.token
const headers = {Authorization: `Bearer ${token}`}
const loadNews = async () => (await (await fetch(`${baseUrl}/api/wemedia/news/${newsId}`, {headers})).json()).data
const before = JSON.stringify((await loadNews()).content)

async function consume(signal) {
  const started = performance.now()
  const response = await fetch(`${baseUrl}/api/wemedia/news/${newsId}/ai/continue`, {
    method: 'POST', signal,
    headers: {...headers, 'Content-Type': 'application/json', Accept: 'text/event-stream'},
    body: JSON.stringify({instruction:'继续当前主题并给出简洁结尾', targetLength:120, currentContent:'人工智能正在改变新闻生产流程，但编辑判断与事实核验仍然不可替代。'})
  })
  if (!response.ok) throw new Error(`SSE failed: HTTP ${response.status}`)
  const reader = response.body.getReader(), decoder = new TextDecoder()
  let buffer = '', chunks = 0, done = 0, firstChunkMs = null
  while (true) {
    const item = await reader.read()
    buffer += decoder.decode(item.value || new Uint8Array(), {stream: !item.done})
    const blocks = buffer.split(/\r?\n\r?\n/); buffer = blocks.pop() || ''
    for (const block of blocks) {
      if (/^event:chunk/m.test(block)) { chunks++; firstChunkMs ??= Math.round(performance.now() - started) }
      if (/^event:done/m.test(block)) done++
    }
    if (item.done) break
  }
  return {chunks, done, firstChunkMs, totalMs:Math.round(performance.now() - started)}
}

const completed = await consume(undefined)
const controller = new AbortController()
let cancelled = false
const cancelRequest = fetch(`${baseUrl}/api/wemedia/news/${newsId}/ai/continue`, {
  method:'POST', signal:controller.signal,
  headers:{...headers,'Content-Type':'application/json',Accept:'text/event-stream'},
  body:JSON.stringify({instruction:'续写并用于取消验证',targetLength:300,currentContent:'人工智能正在改变新闻生产流程。'})
}).then(async response => { const reader=response.body.getReader(); await reader.read(); controller.abort(); await reader.read() })
try { await cancelRequest } catch (error) { cancelled = error.name === 'AbortError' }
const after = JSON.stringify((await loadNews()).content)
console.log(JSON.stringify({baseUrl, ...completed, cancelled, databaseUnchanged:before === after}))
