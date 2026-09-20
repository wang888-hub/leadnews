-- Same KEYS/ARGV layout as like.lua; all operations are per user and O(1).
local member,eventId=ARGV[1],ARGV[2]
local kind=ARGV[9]
local countField=kind=='COLLECT' and 'collectCount' or 'likeCount'
local state=redis.call('HGET',KEYS[1],member)
if not state and redis.call('SISMEMBER',KEYS[6],member)==1 then
 state='1';redis.call('HSET',KEYS[1],member,'1');redis.call('SREM',KEYS[6],member)
end
local count=tonumber(redis.call('HGET',KEYS[3],countField) or '0')
local version=tonumber(redis.call('GET',KEYS[5]) or '0')
local countVersion=tonumber(redis.call('GET',KEYS[8]) or '0')
if state~='1' then return {0,0,0,count,version,0,countVersion} end
local expireAt=tonumber(ARGV[7])+tonumber(ARGV[8])
redis.call('HSET',KEYS[1],member,'2')
redis.call('ZADD',KEYS[2],expireAt,member)
local nextDue=tonumber(redis.call('ZSCORE',KEYS[7],ARGV[3]) or '0')
if nextDue==0 or expireAt<nextDue then redis.call('ZADD',KEYS[7],expireAt,ARGV[3]) end
count=math.max(0,count-1)
redis.call('HSET',KEYS[3],countField,count)
version=redis.call('INCR',KEYS[5])
countVersion=redis.call('INCR',KEYS[8])
local heatDelta=-tonumber(ARGV[12])
local event={eventId=eventId,articleId=tonumber(ARGV[3]),userId=tonumber(member),
 channelId=tonumber(ARGV[6]),behaviorType=kind,delta=-1,heatDelta=heatDelta,
 relationVersion=version,countVersion=countVersion,occurredAt=ARGV[4],traceId=ARGV[5]}
if kind=='COLLECT' then event.collected=false;event.collectCount=count
else event.liked=false;event.likeCount=count end
redis.call('HSET',KEYS[4],eventId,cjson.encode(event))
return {1,0,-1,count,version,heatDelta,countVersion}
