package com.chida.sampriti.protal.backend_ws.constant;

public class Authority {
    public static final String[] USER_AUTHORITIES = { "user:read" };
    public static final String[] MEMBER_AUTHORITIES = { "user:read", "user:update" };
    public static final String[] EC_AUTHORITIES = { "user:read", "user:update", "user:create" };
    public static final String[] ADMIN_AUTHORITIES = { "user:read", "user:update", "user:create", "user:delete" };
}
