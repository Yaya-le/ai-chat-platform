package com.zykj.ai_backend.common;

import lombok.Data;

/**
 * 统一返回结果类
 */
@Data
public class Result<T> {
    /** 状态码：200-成功，500-失败 */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 返回数据 */
    private T data;

    /** 成功 */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "成功";
        r.data = data;
        return r;
    }

    /** 失败 */
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}
