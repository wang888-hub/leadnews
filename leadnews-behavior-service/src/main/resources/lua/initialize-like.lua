-- Legacy Set migration must run even when the old initialized marker already exists.
if redis.call('SISMEMBER',KEYS[4],ARGV[1])==1 then
    redis.call('HSETNX',KEYS[1],ARGV[1],'1')
    redis.call('SREM',KEYS[4],ARGV[1])
end
if redis.call('EXISTS', KEYS[2]) == 0 then
    if ARGV[2]=='1' and not redis.call('HGET',KEYS[1],ARGV[1]) then redis.call('HSET',KEYS[1],ARGV[1],'1') end
    redis.call('SET', KEYS[2], '1')
    redis.call('SET', KEYS[3], ARGV[3])
    return 1
end
return 0
