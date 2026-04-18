package com.example.nestchat.api;

/**
 * 认证相关接口占位。
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
}
