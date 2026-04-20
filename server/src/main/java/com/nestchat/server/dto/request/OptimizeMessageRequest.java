package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OptimizeMessageRequest {

    @NotBlank(message = "原话不能为空")
    private String originalText;

    @Pattern(regexp = "gentle|sincere|comfort", message = "模式必须是 gentle、sincere 或 comfort")
    private String mode = "gentle";
}