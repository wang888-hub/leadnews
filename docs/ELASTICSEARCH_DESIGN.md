# Elasticsearch 设计

服务端和官方 Java API Client 均为 `8.17.10`，地址 `http://localhost:9201`。未使用已淘汰的 `RestHighLevelClient`。

## 索引与 Alias

- `leadnews_article_v1` → alias `leadnews_article`
- `leadnews_suggestion_v1` → alias `leadnews_suggestion`

initializer 只在索引不存在时创建，绝不在启动时删除。文章 mapping 开启 `dynamic: strict`：articleId/authorId/channelId 为 long；title/content/summary 为 text；authorName/channelName/labels/staticUrl 为 keyword（staticUrl 不索引）；publishTime/createdTime 为 date。

联想索引保存 articleId、title、labels 和 suggestion。suggestion 使用 `standard tokenizer + lowercase + edge_ngram(1..20)`，查询使用 `match_phrase_prefix`，由文章 UPSERT/DELETE 同步维护。

当前 Elasticsearch 容器未安装 IK。文章全文字段采用内置 `standard` analyzer，中文通常按单字切分，召回可用但词义边界与相关性不如 IK。阶段 7 不临时安装来源不明的插件；后续若安装与 ES 8.17.10 精确匹配的 IK，应新建 `leadnews_article_v2` 重建并原子切 alias，不能在线修改既有 analyzer。

## 运维命令

检查 alias：`curl http://localhost:9201/_cat/aliases/leadnews_*?v`。检查文档：`curl http://localhost:9201/leadnews_article/_count`。重建通过 Search 内部接口触发，直接调用必须带可信内部标记，Gateway 永不暴露 `/internal/**`。
