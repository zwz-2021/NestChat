package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRemarkRequest {

    private String relationId;
    @NotBlank
    private String remark;
}
