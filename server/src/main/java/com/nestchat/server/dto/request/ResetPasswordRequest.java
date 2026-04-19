package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    private String account;
    @NotBlank
    private String verifyCode;
    @NotBlank
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
