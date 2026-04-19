package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateDiaryRequest {

    private String authorType;
    @NotBlank
    private String date;
    @NotBlank
    private String moodCode;
    private String moodText;
    @NotBlank
    private String content;
    private List<String> imageFileIds;
}
