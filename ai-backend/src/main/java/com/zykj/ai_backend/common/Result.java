package com.zykj.ai_backend.common;

import lombok.Getter;

@Getter
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> Result<T> ok(int code, String message, T data) {
        return new Result<T>(code, message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message);
    }
}
