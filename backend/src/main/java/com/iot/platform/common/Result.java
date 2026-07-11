package com.iot.platform.common;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    public static <T> Result<T> success() { return new Result<>(200, "操作成功", null, System.currentTimeMillis()); }
    public static <T> Result<T> success(T data) { return new Result<>(200, "操作成功", data, System.currentTimeMillis()); }
    public static <T> Result<T> success(String msg, T data) { return new Result<>(200, msg, data, System.currentTimeMillis()); }
    public static <T> Result<T> fail(int code, String msg) { return new Result<>(code, msg, null, System.currentTimeMillis()); }
    public static <T> Result<T> fail(String msg) { return new Result<>(500, msg, null, System.currentTimeMillis()); }
}
