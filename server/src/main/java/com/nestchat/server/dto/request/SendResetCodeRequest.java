package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendResetCodeRequest {

    @NotBlank
    private String account;
}
