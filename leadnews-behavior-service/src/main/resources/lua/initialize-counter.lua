if redis.call('EXISTS',KEYS[1])==0 then redis.call('HSET',KEYS[1],'likeCount',ARGV[1],'viewCount',ARGV[2],'commentCount',ARGV[3],'collectCount',ARGV[4]) end
redis.call('SETNX',KEYS[2],ARGV[5]);redis.call('SETNX',KEYS[3],ARGV[6]);return 1
