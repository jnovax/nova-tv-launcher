package com.appletv.contract;

public final class StatusProviderContract {
    private StatusProviderContract() {}

    public static final String AUTHORITY = "vn.familytv.parentalcontrol.provider";
    public static final String STATUS_PATH = "status";
    public static final String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + STATUS_PATH;

    public static final String COLUMN_CURRENT_MODE = "current_mode";
    public static final String COLUMN_REMAINING_SECONDS = "remaining_seconds";
    public static final String COLUMN_SESSION_LIMIT_SECONDS = "session_limit_seconds";
    public static final String COLUMN_WHITELIST_PACKAGES = "whitelist_packages";
}
