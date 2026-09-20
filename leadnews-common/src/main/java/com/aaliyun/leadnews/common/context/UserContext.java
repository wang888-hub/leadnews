package com.aaliyun.leadnews.common.context;

public final class UserContext {
    private static final ThreadLocal<Long> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> TYPE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> INTERNAL = new ThreadLocal<>();
    private UserContext() {}
    public static void setUserId(Long userId) { USER.set(userId); }
    public static Long getUserId() { return USER.get(); }
    public static void setUserType(String type) { TYPE.set(type); }
    public static String getUserType() { return TYPE.get(); }
    public static boolean isType(String type) { return type.equals(TYPE.get()); }
    public static void setInternal(boolean value) { INTERNAL.set(value); }
    public static boolean isInternal() { return Boolean.TRUE.equals(INTERNAL.get()); }
    public static void clear() { USER.remove(); TYPE.remove(); INTERNAL.remove(); }
}
