package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBindRequest {

    @NotBlank
    private String targetPhone;
}
