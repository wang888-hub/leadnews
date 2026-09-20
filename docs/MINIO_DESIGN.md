# MinIO 设计

- Host/IDEA：`${MINIO_ENDPOINT:http://localhost:9000}`；Bucket：`${MINIO_BUCKET:leadnews}`。
- 静态 HTML：`article/{articleId}/index.html`，同一文章重试覆盖同一键。
- 素材：`material/{wmUserId}/{yyyy/MM/dd}/{uuid}.{detectedExt}`；素材不是幂等发布资源，可使用随机键。
- 数据库以 `static_object_key` 为位置真相，`static_url` 仅为环境兼容字段。
- 凭据只来自环境变量/Nacos 占位符。
- 图片限制大小并检查 JPEG/PNG/GIF/WebP 魔数，不信任 MIME、扩展名或原始路径；被引用时删除返回 409。
- `/static/article/{id}` 只按数字 ID 查已发布记录，客户端不能指定 bucket/objectKey；返回 Cache-Control、ETag，并支持 304。
