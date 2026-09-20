-- KEYS: state hash, cooldown zset, active article zset; ARGV: articleId, nowMillis, batchSize.
local members=redis.call('ZRANGEBYSCORE',KEYS[2],'-inf',ARGV[2],'LIMIT',0,tonumber(ARGV[3]))
for _,member in ipairs(members) do
 if redis.call('HGET',KEYS[1],member)=='2' then redis.call('HDEL',KEYS[1],member) end
 redis.call('ZREM',KEYS[2],member)
end
local nextEntry=redis.call('ZRANGE',KEYS[2],0,0,'WITHSCORES')
if #nextEntry==0 then redis.call('ZREM',KEYS[3],ARGV[1]);return #members end
redis.call('ZADD',KEYS[3],tonumber(nextEntry[2]),ARGV[1])
return #members
