export type UserType = 'APP_USER' | 'WEMEDIA' | 'ADMIN'
export interface ResponseResult<T> { code:number; message:string; data:T; timestamp:number; traceId?:string }
export interface PageResult<T> { total:number; page:number; size:number; records:T[] }
export interface User { id:number; name:string; phone?:string; image?:string; sex?:number; flag?:string }
export interface LoginResult<U=User> { token:string; expiresIn:number; user?:U; userId?:number; nickname?:string }
export interface Channel { id?:number; name:string; description?:string; status:'ENABLED'|'DISABLED'; ord:number; createdTime?:string; updatedTime?:string }
export interface ContentBlock { type:'text'|'image'; value:string }
export interface Article { id:number; wmNewsId?:number; title:string; authorId:number; authorName:string; channelId:number; channelName:string; content:ContentBlock[]; coverImages:string[]; labels?:string; publishTime:string; summary?:string; summaryStatus?:'PENDING'|'GENERATING'|'SUCCESS'|'FAILED'; summaryGenerated?:boolean; summaryGeneratedTime?:string; staticUrl?:string; publishStatus:string; publishedTime?:string; likeCount:number; viewCount:number; commentCount:number; collectCount:number; liked:boolean; behaviorRealtime:boolean }
export interface ArticleComment { id:number; articleId:number; userId:number; authorName:string; content:string; createdTime:string }
export interface BehaviorInfo { liked:boolean; likeCount:number; viewCount:number; changed:boolean }
export interface HotArticle { article:{articleId:number;channelId:number;title:string;summary?:string;staticUrl?:string;publishTime:string}; score:number }
export interface HotResponse { degraded:boolean; items:HotArticle[] }
export interface SearchDocument { articleId:number; title:string; content:string; summary?:string; channelId:number; channelName?:string; authorName?:string; labels?:string; publishTime:string; staticUrl?:string }
export interface SearchResult { article:SearchDocument; highlightedTitle:string[]; highlightedContent:string[]; score?:number }
export interface SearchPage { total:number;page:number;size:number;items:SearchResult[] }
export interface SearchHistory { id:number; keyword:string; createdTime?:string; updatedTime?:string }
export interface Material { id:number; userId?:number; url:string; objectKey?:string; type?:string; createdTime?:string }
export interface AuditRecord { auditVersion:number;auditStage:string;decision:string;riskLevel?:string;reason?:string;confidence?:number;riskTags?:string[];model?:string;reviewerId?:number;createdTime:string }
export interface WmNews { id:number;userId:number;authorName:string;title:string;content:ContentBlock[];layout:number;channelId:number;labels?:string;coverImages:string[];status:string;submittedTime?:string;publishTime?:string;reason?:string;auditVersion?:number;auditSource?:string;auditTrail?:AuditRecord[] }
export type SseEvent = {event:string;data:Record<string,unknown>}
