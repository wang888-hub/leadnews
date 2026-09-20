local factor, minScore = tonumber(ARGV[1]), tonumber(ARGV[2])
local values = redis.call('ZRANGE',KEYS[1],0,-1,'WITHSCORES')
local removed = 0
for i=1,#values,2 do
 local score=tonumber(values[i+1])*factor
 if score<minScore then redis.call('ZREM',KEYS[1],values[i]); removed=removed+1
 else redis.call('ZADD',KEYS[1],score,values[i]) end
end
return removed
