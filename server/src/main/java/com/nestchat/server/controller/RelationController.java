package com.nestchat.server.controller;

import com.nestchat.server.common.Result;
import com.nestchat.server.dto.request.CreateBindRequest;
import com.nestchat.server.dto.request.UpdateRemarkRequest;
import com.nestchat.server.dto.response.RelationApplicationResponse;
import com.nestchat.server.dto.response.RelationStatusResponse;
import com.nestchat.server.security.UserContext;
import com.nestchat.server.service.RelationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/relations")
public class RelationController {

    private final RelationService relationService;

    public RelationController(RelationService relationService) {
        this.relationService = relationService;
    }

    @GetMapping("/current")
    public Result<RelationStatusResponse> getCurrentRelation() {
        return Result.ok(relationService.getCurrentRelation(UserContext.get()));
    }

    @PostMapping("/applications")
    public Result<RelationStatusResponse> createApplication(@Valid @RequestBody CreateBindRequest req) {
        return Result.ok(relationService.createApplication(UserContext.get(), req));
    }

    @GetMapping("/applications")
    public Result<Map<String, List<RelationApplicationResponse>>> getApplications() {
        List<RelationApplicationResponse> items = relationService.getApplications(UserContext.get());
        return Result.ok(Map.of("items", items));
    }

    @PostMapping("/applications/{applicationId}/accept")
    public Result<RelationStatusResponse> acceptApplication(@PathVariable String applicationId) {
        return Result.ok(relationService.acceptApplication(UserContext.get(), applicationId));
    }

    @PostMapping("/applications/{applicationId}/reject")
    public Result<Void> rejectApplication(@PathVariable String applicationId) {
        relationService.rejectApplication(UserContext.get(), applicationId);
        return Result.ok();
    }

    @PutMapping("/current/remark")
    public Result<RelationStatusResponse> updateRemark(@Valid @RequestBody UpdateRemarkRequest req) {
        return Result.ok(relationService.updateRemark(UserContext.get(), req));
    }

    @PostMapping("/current/unbind-request")
    public Result<RelationStatusResponse> requestUnbind() {
        return Result.ok(relationService.requestUnbind(UserContext.get()));
    }

    @DeleteMapping("/current")
    public Result<Void> unbind() {
        relationService.requestUnbind(UserContext.get());
        return Result.ok();
    }
}
