package com.nestchat.server.service;

import com.nestchat.server.common.BusinessException;
import com.nestchat.server.common.MediaUrlHelper;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.dto.request.UpdateMoodRequest;
import com.nestchat.server.dto.request.UpdateProfileRequest;
import com.nestchat.server.dto.response.ProfileResponse;
import com.nestchat.server.entity.User;
import com.nestchat.server.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserService {

    private static final Map<String, String> MOOD_MAP = Map.ofEntries(
            Map.entry("happy", "开心"),
            Map.entry("calm", "平静"),
            Map.entry("love", "心动"),
            Map.entry("sad", "难过"),
            Map.entry("wronged", "委屈"),
            Map.entry("angry", "生气"),
            Map.entry("tired", "疲惫"),
            Map.entry("anxious", "焦虑")
    );

    private final UserMapper userMapper;
    private final MediaUrlHelper mediaUrlHelper;

    public UserService(UserMapper userMapper, MediaUrlHelper mediaUrlHelper) {
        this.userMapper = userMapper;
        this.mediaUrlHelper = mediaUrlHelper;
    }

    public ProfileResponse getProfile(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toProfileResponse(user);
    }

    public ProfileResponse updateProfile(String userId, UpdateProfileRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(mediaUrlHelper.toStoredPath(req.getAvatarUrl()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfileResponse(user);
    }

    public ProfileResponse updateMood(String userId, UpdateMoodRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        user.setMoodCode(req.getMoodCode());
        user.setMoodText(MOOD_MAP.getOrDefault(req.getMoodCode(), req.getMoodCode()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfileResponse(user);
    }

    public void updateLastActive(String userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastActiveAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    private ProfileResponse toProfileResponse(User user) {
        ProfileResponse resp = new ProfileResponse();
        resp.setUserId(user.getUserId());
        resp.setAccount(user.getAccount());
        resp.setNickname(user.getNickname());
        resp.setAvatarUrl(mediaUrlHelper.toPublicUrl(user.getAvatarUrl()));
        resp.setMoodCode(user.getMoodCode());
        resp.setMoodText(user.getMoodText());
        return resp;
    }
}
