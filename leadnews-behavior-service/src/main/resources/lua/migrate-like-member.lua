-- KEYS: legacy like set, new state hash; ARGV: userId.
if redis.call('SISMEMBER',KEYS[1],ARGV[1])==0 then return 0 end
redis.call('HSETNX',KEYS[2],ARGV[1],'1')
redis.call('SREM',KEYS[1],ARGV[1])
return 1
