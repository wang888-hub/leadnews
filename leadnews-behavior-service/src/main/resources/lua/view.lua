local count = redis.call('HINCRBY', KEYS[1], 'viewCount', 1)
redis.call('HSET', KEYS[2], ARGV[1], ARGV[2])
return count
