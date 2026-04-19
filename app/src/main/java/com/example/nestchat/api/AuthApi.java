package com.example.nestchat.api;

/**
 * 认证相关接口。
 */
public interface AuthApi {

    void getLoginCaptcha(ApiCallback<CaptchaResponse> callback);
    void getRegisterCaptcha(ApiCallback<CaptchaResponse> callback);
    void login(LoginRequest request, ApiCallback<LoginResponse> callback);
    void register(RegisterRequest request, ApiCallback<SimpleResponse> callback);
    void sendResetCode(SendResetCodeRequest request, ApiCallback<SimpleResponse> callback);
    void resetPassword(ResetPasswordRequest request, ApiCallback<SimpleResponse> callback);
    void logout(ApiCallback<SimpleResponse> callback);

    class CaptchaResponse {
        public String captchaId;
        public String imageBase64;
        public long expireAt;
    }

    class LoginRequest {
        public String account;
        public String password;
        public String captchaId;
        public String captchaCode;
        public boolean rememberMe;
    }

    class LoginResponse {
        public String accessToken;
        public String refreshToken;
        public long expireAt;
        public UserBrief user;
    }

    class RegisterRequest {
        public String account;
        public String password;
        public String confirmPassword;
        public String captchaId;
        public String captchaCode;
    }

    class SendResetCodeRequest {
        public String account;
    }

    class ResetPasswordRequest {
        public String account;
        public String verifyCode;
        public String newPassword;
        public String confirmPassword;
    }

    class UserBrief {
        public String userId;
        public String account;
        public String nickname;
        public String avatarUrl;
    }

    class SimpleResponse {
        public boolean success;
        public String message;
    }

    // ========== 实现 ==========
    class Impl {
        public static void getLoginCaptcha(ApiCallback<CaptchaResponse> callback) {
            ApiClient.get("/auth/captcha/login", CaptchaResponse.class, callback);
        }

        public static void getRegisterCaptcha(ApiCallback<CaptchaResponse> callback) {
            ApiClient.get("/auth/captcha/register", CaptchaResponse.class, callback);
        }

        public static void login(LoginRequest request, ApiCallback<LoginResponse> callback) {
            ApiClient.post("/auth/login", request, LoginResponse.class, new ApiCallback<LoginResponse>() {
                @Override
                public void onSuccess(LoginResponse data) {
                    if (data != null && data.user != null) {
                        TokenManager.saveLoginInfo(
                                data.accessToken, data.refreshToken,
                                data.user.userId, data.user.account,
                                data.user.nickname, data.user.avatarUrl);
                    }
                    callback.onSuccess(data);
                }

                @Override
                public void onError(ApiError error) {
                    callback.onError(error);
                }
            });
        }

        public static void register(RegisterRequest request, ApiCallback<SimpleResponse> callback) {
            ApiClient.post("/auth/register", request, SimpleResponse.class, new ApiCallback<SimpleResponse>() {
                @Override
                public void onSuccess(SimpleResponse data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onError(ApiError error) {
                    callback.onError(error);
                }
            });
        }

        public static void sendResetCode(SendResetCodeRequest request, ApiCallback<SimpleResponse> callback) {
            ApiClient.post("/auth/password/code/send", request, SimpleResponse.class, new ApiCallback<SimpleResponse>() {
                @Override
                public void onSuccess(SimpleResponse data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onError(ApiError error) {
                    callback.onError(error);
                }
            });
        }

        public static void resetPassword(ResetPasswordRequest request, ApiCallback<SimpleResponse> callback) {
            ApiClient.post("/auth/password/reset", request, SimpleResponse.class, new ApiCallback<SimpleResponse>() {
                @Override
                public void onSuccess(SimpleResponse data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onError(ApiError error) {
                    callback.onError(error);
                }
            });
        }

        public static void logout(ApiCallback<SimpleResponse> callback) {
            ApiClient.post("/auth/logout", null, SimpleResponse.class, new ApiCallback<SimpleResponse>() {
                @Override
                public void onSuccess(SimpleResponse data) {
                    TokenManager.clear();
                    callback.onSuccess(data);
                }

                @Override
                public void onError(ApiError error) {
                    TokenManager.clear();
                    callback.onError(error);
                }
            });
        }
    }
}
