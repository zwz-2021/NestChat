package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMoodRequest {

    @NotBlank
    private String moodCode;
}
