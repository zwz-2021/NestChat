package com.example.nestchat.api;

/**
 * 纯占位回调接口，用于预留后端接入点。
 * 当前项目尚未接入真实网络层，实现方可由 Retrofit/OkHttp/Volley 等替换。
 */
public interface ApiCallback<T> {

    void onSuccess(T data);

    void onError(ApiError error);
}
