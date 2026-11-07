package com.appletv.contract;

public final class ModeContract {
    private ModeContract() {}

    public static final String MODE_KID = "KID";
    public static final String MODE_PARENT = "PARENT";

    public static final String TARGET_PACKAGE = "vn.familytv.parentalcontrol";
    public static final String ACTION_MODE_CHANGED = "vn.familytv.parentalcontrol.ACTION_MODE_CHANGED";
    public static final String ACTION_AUTHENTICATE = "vn.familytv.parentalcontrol.ACTION_AUTHENTICATE";

    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_ALLOWED_PACKAGES = "extra_allowed_packages";
    public static final String EXTRA_REMAINING_SECONDS = "extra_remaining_seconds";
}
