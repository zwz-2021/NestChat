package com.example.nestchat.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    private static final String TAG = "ApiClient";
    public static final String BASE_URL = "http://117.72.190.135:9090/api/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new Gson();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ========== GET ==========
    public static <T> void get(String path, Type dataType, ApiCallback<T> callback) {
        Request request = newRequestBuilder(path).get().build();
        execute(request, dataType, callback);
    }

    // ========== POST ==========
    public static <T> void post(String path, Object body, Type dataType, ApiCallback<T> callback) {
        String json = body != null ? gson.toJson(body) : "{}";
        RequestBody reqBody = RequestBody.create(json, JSON);
        Request request = newRequestBuilder(path).post(reqBody).build();
        execute(request, dataType, callback);
    }

    // ========== PUT ==========
    public static <T> void put(String path, Object body, Type dataType, ApiCallback<T> callback) {
        String json = body != null ? gson.toJson(body) : "{}";
        RequestBody reqBody = RequestBody.create(json, JSON);
        Request request = newRequestBuilder(path).put(reqBody).build();
        execute(request, dataType, callback);
    }

    // ========== DELETE ==========
    public static <T> void delete(String path, Type dataType, ApiCallback<T> callback) {
        Request request = newRequestBuilder(path).delete().build();
        execute(request, dataType, callback);
    }

    // ========== Multipart Upload ==========
    public static <T> void uploadFile(String path, File file, String fieldName,
                                       String mimeType, String bizType,
                                       Type dataType, ApiCallback<T> callback) {
        MediaType mediaType = MediaType.parse(mimeType);
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(fieldName, file.getName(),
                        RequestBody.create(file, mediaType))
                .addFormDataPart("bizType", bizType)
                .build();
        Request request = newRequestBuilder(path).post(body).build();
        execute(request, dataType, callback);
    }

    // ========== Internal ==========

    private static Request.Builder newRequestBuilder(String path) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + path);
        String token = TokenManager.getAccessToken();
        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        return builder;
    }

    @SuppressWarnings("unchecked")
    private static <T> void execute(Request request, Type dataType, ApiCallback<T> callback) {
        Log.d(TAG, request.method() + " " + request.url());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
                ApiError error = new ApiError();
                error.code = 50000;
                error.message = "网络连接失败: " + e.getMessage();
                mainHandler.post(() -> callback.onError(error));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bodyStr = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Response: " + bodyStr.substring(0, Math.min(bodyStr.length(), 500)));

                try {
                    JsonObject json = JsonParser.parseString(bodyStr).getAsJsonObject();
                    int code = json.get("code").getAsInt();
                    String message = json.has("message") ? json.get("message").getAsString() : "";

                    if (code == 0) {
                        // 成功
                        JsonElement dataElement = json.get("data");
                        T data = null;
                        if (dataType != null && dataElement != null && !dataElement.isJsonNull()) {
                            data = gson.fromJson(dataElement, dataType);
                        }
                        T finalData = data;
                        mainHandler.post(() -> callback.onSuccess(finalData));
                    } else {
                        // 业务错误
                        ApiError error = new ApiError();
                        error.code = code;
                        error.message = message;
                        error.traceId = json.has("traceId") ? json.get("traceId").getAsString() : "";
                        mainHandler.post(() -> callback.onError(error));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                    ApiError error = new ApiError();
                    error.code = 50000;
                    error.message = "数据解析失败";
                    mainHandler.post(() -> callback.onError(error));
                }
            }
        });
    }

    public static Gson getGson() {
        return gson;
    }
}
