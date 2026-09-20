-- KEYS: global rank, channel rank, active channels, applied windows, recovery flag.
-- ARGV: windowEventId, articleId, deltaScore, windowEnd, minScore, topN, channelId.
local id, member = ARGV[1], ARGV[2]
local delta, windowEnd = tonumber(ARGV[3]), tonumber(ARGV[4])
local minScore, topN = tonumber(ARGV[5]), tonumber(ARGV[6])
if not delta or not windowEnd or not minScore or not topN or topN<1 then
 return redis.error_reply('invalid hot score arguments')
end
for i=1,4 do
 local kind=redis.call('TYPE',KEYS[i]).ok
 local expected=i==3 and 'set' or 'zset'
 if kind~='none' and kind~=expected then
  return redis.error_reply('wrong hot score key type')
 end
end
if redis.call('ZSCORE', KEYS[4], id) then return 0 end
local recoveryId=redis.call('GET',KEYS[5])
if recoveryId then
 local deltaGlobal='hot:article:recovery:'..recoveryId..':delta:global'
 local deltaChannel='hot:article:recovery:'..recoveryId..':delta:channel:'..ARGV[7]
 redis.call('ZINCRBY',deltaGlobal,delta,member)
 redis.call('ZINCRBY',deltaChannel,delta,member)
 redis.call('PEXPIRE',deltaGlobal,900000)
 redis.call('PEXPIRE',deltaChannel,900000)
 redis.call('SADD',KEYS[3],ARGV[7])
 redis.call('ZADD',KEYS[4],windowEnd,id)
 return 1
end
for i=1,2 do
 local score=tonumber(redis.call('ZINCRBY',KEYS[i],delta,member))
 if score<minScore then redis.call('ZREM',KEYS[i],member) end
 local size=redis.call('ZCARD',KEYS[i])
 if size>topN then redis.call('ZREMRANGEBYRANK',KEYS[i],0,size-topN-1) end
end
redis.call('SADD',KEYS[3],ARGV[7])
redis.call('ZADD',KEYS[4],windowEnd,id)
return 1
