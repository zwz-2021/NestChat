package com.nestchat.server.controller;

import com.nestchat.server.common.Result;
import com.nestchat.server.dto.request.LoginRequest;
import com.nestchat.server.dto.request.RegisterRequest;
import com.nestchat.server.dto.request.ResetPasswordRequest;
import com.nestchat.server.dto.request.SendResetCodeRequest;
import com.nestchat.server.dto.response.CaptchaResponse;
import com.nestchat.server.dto.response.LoginResponse;
import com.nestchat.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/captcha/login")
    public Result<CaptchaResponse> getLoginCaptcha() {
        return Result.ok(authService.generateCaptcha("login"));
    }

    @GetMapping("/captcha/register")
    public Result<CaptchaResponse> getRegisterCaptcha() {
        return Result.ok(authService.generateCaptcha("register"));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.ok();
    }

    @PostMapping("/password/code/send")
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeRequest req) {
        authService.sendResetCode(req);
        return Result.ok();
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        authService.logout(token);
        return Result.ok();
    }
}
