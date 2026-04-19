package com.nestchat.server.controller;

import com.nestchat.server.common.Result;
import com.nestchat.server.dto.response.UploadFileResponse;
import com.nestchat.server.security.UserContext;
import com.nestchat.server.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload/image")
    public Result<UploadFileResponse> uploadImage(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(defaultValue = "chat") String bizType) {
        return Result.ok(fileService.uploadImage(file, bizType, UserContext.get()));
    }

    @PostMapping("/upload/voice")
    public Result<UploadFileResponse> uploadVoice(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(defaultValue = "chat") String bizType) {
        return Result.ok(fileService.uploadVoice(file, bizType, UserContext.get()));
    }
}
