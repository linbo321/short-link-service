package com.shortlink.common;

public enum ErrorCode {
    BAD_REQUEST(400, "参数错误"),
    NOT_FOUND(404, "链接不存在或已失效"),
    GONE(410, "该链接已过期"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请1分钟后再试"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
