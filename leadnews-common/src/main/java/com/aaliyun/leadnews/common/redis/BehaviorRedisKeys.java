package com.aaliyun.leadnews.common.redis;

public final class BehaviorRedisKeys {
    public static final String PENDING_EVENTS = "behavior:event:pending";
    public static final String COUNTER_INITIALIZED = "article:counter:initialized:";
    public static final String HOT_GLOBAL = "hot:article:global";
    public static final String HOT_CHANNEL_PREFIX = "hot:article:channel:";
    public static final String HOT_META_PREFIX = "article:hot:meta:";
    public static final String HOT_ACTIVE_CHANNELS = "hot:article:channels:active";
    public static final String HOT_DECAY_LOCK_PREFIX = "hot:article:decay:minute:";
    public static final String HOT_APPLIED = "hot:article:applied";
    public static final String HOT_RECOVERY_FLAG = "hot:article:recovery:active";
    public static final String LIKE_COOLDOWN_ARTICLES = "like:cooldown:articles";
    public static final String COLLECT_COOLDOWN_ARTICLES = "collect:cooldown:articles";
    private BehaviorRedisKeys() {}
    public static String articleLikes(long articleId) { return "article:like:user:" + articleId; }
    public static String likeState(long articleId) { return "like:state:" + articleId; }
    public static String likeCooldown(long articleId) { return "like:cooldown:" + articleId; }
    public static String collectState(long articleId) { return "collect:state:" + articleId; }
    public static String collectCooldown(long articleId) { return "collect:cooldown:" + articleId; }
    public static String collectInitialized(long articleId,long userId){return "article:collect:initialized:"+articleId+":"+userId;}
    public static String collectVersion(long articleId,long userId){return "article:collect:version:"+articleId+":"+userId;}
    public static String likeCountVersion(long articleId){return "article:like:count-version:"+articleId;}
    public static String collectCountVersion(long articleId){return "article:collect:count-version:"+articleId;}
    public static String articleCounter(long articleId) { return "article:counter:" + articleId; }
    public static String counterInitialized(long articleId) { return COUNTER_INITIALIZED + articleId; }
    public static String likeInitialized(long articleId, long userId) { return "article:like:initialized:" + articleId + ":" + userId; }
    public static String likeVersion(long articleId, long userId) { return "article:like:version:" + articleId + ":" + userId; }
    public static String hotChannel(long channelId) { return HOT_CHANNEL_PREFIX + channelId; }
    public static String hotMeta(long articleId) { return HOT_META_PREFIX + articleId; }
    public static String hotRecoveryDeltaGlobal(String id){return "hot:article:recovery:"+id+":delta:global";}
    public static String hotRecoveryDeltaChannel(String id,long channel){return "hot:article:recovery:"+id+":delta:channel:"+channel;}
    public static String hotRecoveryTempGlobal(String id){return "hot:article:recovery:"+id+":temp:global";}
    public static String hotRecoveryTempChannel(String id,long channel){return "hot:article:recovery:"+id+":temp:channel:"+channel;}
}
