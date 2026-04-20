package com.nestchat.server.controller;

import com.nestchat.server.common.Result;
import com.nestchat.server.dto.request.UpdateMoodRequest;
import com.nestchat.server.dto.request.UpdateProfileRequest;
import com.nestchat.server.dto.response.ProfileResponse;
import com.nestchat.server.security.UserContext;
import com.nestchat.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<ProfileResponse> getMyProfile() {
        return Result.ok(userService.getProfile(UserContext.get()));
    }

    @PutMapping("/me/profile")
    public Result<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return Result.ok(userService.updateProfile(UserContext.get(), req));
    }

    @PutMapping("/me/mood")
    public Result<ProfileResponse> updateMood(@Valid @RequestBody UpdateMoodRequest req) {
        return Result.ok(userService.updateMood(UserContext.get(), req));
    }

    @PostMapping("/me/heartbeat")
    public Result<Void> heartbeat() {
        userService.updateLastActive(UserContext.get());
        return Result.ok();
    }
}
