if redis.call('ZCARD',KEYS[1])>0 then return 0 end
if not redis.call('SET',KEYS[2],ARGV[1],'NX','PX',ARGV[2]) then return 0 end
return 1
