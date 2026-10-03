package com.citymall.entity;

/** Login audit outcome (mirrors user-management user_login_status_id 1/2/3/4). */
public enum LoginStatus {
    SUCCESS,
    LOGOUT,
    FAILED,
    LOCKED
}
