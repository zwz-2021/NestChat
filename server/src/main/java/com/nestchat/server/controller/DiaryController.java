package com.nestchat.server.controller;

import com.nestchat.server.common.Result;
import com.nestchat.server.dto.request.CreateDiaryRequest;
import com.nestchat.server.dto.response.DiaryDetailResponse;
import com.nestchat.server.dto.response.DiaryListResponse;
import com.nestchat.server.dto.response.MoodTrendResponse;
import com.nestchat.server.security.UserContext;
import com.nestchat.server.service.DiaryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @GetMapping
    public Result<DiaryListResponse> listDiaries(@RequestParam(defaultValue = "1") int pageNo,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(diaryService.listDiaries(UserContext.get(), pageNo, pageSize));
    }

    @GetMapping("/{diaryId}")
    public Result<DiaryDetailResponse> getDiaryDetail(@PathVariable String diaryId) {
        return Result.ok(diaryService.getDiaryDetail(UserContext.get(), diaryId));
    }

    @PostMapping
    public Result<DiaryDetailResponse> createDiary(@Valid @RequestBody CreateDiaryRequest req) {
        return Result.ok(diaryService.createDiary(UserContext.get(), req));
    }

    @GetMapping("/trend/partner")
    public Result<MoodTrendResponse> getPartnerMoodTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(diaryService.getPartnerMoodTrend(UserContext.get(), days));
    }

    @DeleteMapping("/{diaryId}")
    public Result<Void> deleteDiary(@PathVariable String diaryId) {
        diaryService.deleteDiary(UserContext.get(), diaryId);
        return Result.ok();
    }
}
