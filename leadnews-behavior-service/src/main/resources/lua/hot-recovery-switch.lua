-- KEYS[1] recovery flag. Remaining keys are official,temp,delta triplets.
-- ARGV[1] recoveryId, ARGV[2] topN, ARGV[3] minScore.
if redis.call('GET',KEYS[1])~=ARGV[1] then return redis.error_reply('recovery ownership lost') end
local topN=tonumber(ARGV[2])
local minScore=tonumber(ARGV[3])
for i=2,#KEYS,3 do
 local official,temp,delta=KEYS[i],KEYS[i+1],KEYS[i+2]
 local increments=redis.call('ZRANGE',delta,0,-1,'WITHSCORES')
 for j=1,#increments,2 do redis.call('ZINCRBY',temp,tonumber(increments[j+1]),increments[j]) end
 redis.call('ZREMRANGEBYSCORE',temp,'-inf','('..minScore)
 local size=redis.call('ZCARD',temp)
 if size>topN then redis.call('ZREMRANGEBYRANK',temp,0,size-topN-1) end
 redis.call('DEL',official)
 if redis.call('EXISTS',temp)==1 then redis.call('RENAME',temp,official) end
 redis.call('DEL',delta)
end
redis.call('DEL',KEYS[1])
return 1
