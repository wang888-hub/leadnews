-- KEYS: state, cooldown, counters, pending, relation version, legacy set, active index, count version.
-- ARGV: user, eventId, articleId, occurredAt, traceId, channelId, nowMillis, cooldownMillis.
local member,eventId=ARGV[1],ARGV[2]
local kind=ARGV[9]
local countField=kind=='COLLECT' and 'collectCount' or 'likeCount'
local state=redis.call('HGET',KEYS[1],member)
if not state and redis.call('SISMEMBER',KEYS[6],member)==1 then
 state='1'; redis.call('HSET',KEYS[1],member,'1'); redis.call('SREM',KEYS[6],member)
end
local now=tonumber(ARGV[7]); local cooldown=tonumber(ARGV[8])
if state=='2' then
 local expireAt=tonumber(redis.call('ZSCORE',KEYS[2],member) or '0')
 if expireAt<=now then
  redis.call('HDEL',KEYS[1],member);redis.call('ZREM',KEYS[2],member);state=nil
 end
end
local count=tonumber(redis.call('HGET',KEYS[3],countField) or '0')
local version=tonumber(redis.call('GET',KEYS[5]) or '0')
local countVersion=tonumber(redis.call('GET',KEYS[8]) or '0')
if state=='1' then return {0,1,0,count,version,0,countVersion} end
local heatDelta=state=='2' and tonumber(ARGV[11]) or tonumber(ARGV[10])
redis.call('HSET',KEYS[1],member,'1')
redis.call('ZREM',KEYS[2],member)
count=redis.call('HINCRBY',KEYS[3],countField,1)
version=redis.call('INCR',KEYS[5])
countVersion=redis.call('INCR',KEYS[8])
local event={eventId=eventId,articleId=tonumber(ARGV[3]),userId=tonumber(member),
 channelId=tonumber(ARGV[6]),behaviorType=kind,delta=1,heatDelta=heatDelta,
 relationVersion=version,countVersion=countVersion,occurredAt=ARGV[4],traceId=ARGV[5]}
if kind=='COLLECT' then event.collected=true;event.collectCount=count
else event.liked=true;event.likeCount=count end
redis.call('HSET',KEYS[4],eventId,cjson.encode(event))
return {1,1,1,count,version,heatDelta,countVersion}
