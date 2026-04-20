package com.nestchat.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nestchat.server.common.BusinessException;
import com.nestchat.server.common.IdGenerator;
import com.nestchat.server.common.MediaUrlHelper;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.dto.request.CreateDiaryRequest;
import com.nestchat.server.dto.response.DiaryDetailResponse;
import com.nestchat.server.dto.response.DiaryInsightResponse;
import com.nestchat.server.dto.response.DiaryListResponse;
import com.nestchat.server.dto.response.DiarySummaryResponse;
import com.nestchat.server.dto.response.MoodTrendResponse;
import com.nestchat.server.entity.Diary;
import com.nestchat.server.entity.DiaryImage;
import com.nestchat.server.entity.FileRecord;
import com.nestchat.server.entity.Relation;
import com.nestchat.server.entity.User;
import com.nestchat.server.mapper.DiaryImageMapper;
import com.nestchat.server.mapper.DiaryMapper;
import com.nestchat.server.mapper.FileRecordMapper;
import com.nestchat.server.mapper.RelationMapper;
import com.nestchat.server.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiaryService {

    private static final Map<String, Integer> MOOD_SCORE = Map.ofEntries(
            Map.entry("happy", 5),
            Map.entry("calm", 4),
            Map.entry("love", 5),
            Map.entry("sad", 2),
            Map.entry("wronged", 2),
            Map.entry("angry", 1),
            Map.entry("tired", 3),
            Map.entry("anxious", 2)
    );
    private static final Map<String, String> MOOD_TEXT = Map.ofEntries(
            Map.entry("happy", "开心"),
            Map.entry("calm", "平静"),
            Map.entry("love", "心动"),
            Map.entry("sad", "难过"),
            Map.entry("wronged", "委屈"),
            Map.entry("angry", "生气"),
            Map.entry("tired", "疲惫"),
            Map.entry("anxious", "焦虑")
    );

    private final DiaryMapper diaryMapper;
    private final DiaryImageMapper diaryImageMapper;
    private final FileRecordMapper fileRecordMapper;
    private final RelationMapper relationMapper;
    private final UserMapper userMapper;
    private final MediaUrlHelper mediaUrlHelper;
    private final DiaryInsightService diaryInsightService;

    public DiaryService(DiaryMapper diaryMapper, DiaryImageMapper diaryImageMapper,
                        FileRecordMapper fileRecordMapper, RelationMapper relationMapper,
                        UserMapper userMapper, MediaUrlHelper mediaUrlHelper,
                        DiaryInsightService diaryInsightService) {
        this.diaryMapper = diaryMapper;
        this.diaryImageMapper = diaryImageMapper;
        this.fileRecordMapper = fileRecordMapper;
        this.relationMapper = relationMapper;
        this.userMapper = userMapper;
        this.mediaUrlHelper = mediaUrlHelper;
        this.diaryInsightService = diaryInsightService;
    }

    public DiaryListResponse listDiaries(String userId, int pageNo, int pageSize) {
        List<String> userIds = new ArrayList<>();
        userIds.add(userId);
        Relation relation = relationMapper.selectBoundByUserId(userId);
        if (relation != null) {
            String partnerId = relation.getUserIdA().equals(userId) ? relation.getUserIdB() : relation.getUserIdA();
            userIds.add(partnerId);
        }

        Page<Diary> page = diaryMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Diary>()
                        .in(Diary::getUserId, userIds)
                        .orderByDesc(Diary::getDate, Diary::getCreatedAt));

        List<DiarySummaryResponse> items = new ArrayList<>();
        for (Diary diary : page.getRecords()) {
            DiarySummaryResponse item = new DiarySummaryResponse();
            item.setDiaryId(diary.getDiaryId());
            item.setDate(diary.getDate());
            item.setAuthorType(diary.getUserId().equals(userId) ? "me" : "ta");
            item.setMoodText(diary.getMoodText());

            String content = diary.getContent() != null ? diary.getContent() : "";
            item.setContentSummary(content.length() > 50 ? content.substring(0, 50) + "..." : content);

            List<DiaryImage> images = diaryImageMapper.selectList(
                    new LambdaQueryWrapper<DiaryImage>()
                            .eq(DiaryImage::getDiaryId, diary.getDiaryId())
                            .orderByAsc(DiaryImage::getSortOrder));
            item.setImageCount(images.size());
            item.setCoverUrl(images.isEmpty() ? "" : mediaUrlHelper.toPublicUrl(images.get(0).getImageUrl()));

            items.add(item);
        }

        DiaryListResponse resp = new DiaryListResponse();
        resp.setItems(items);
        resp.setHasMore(page.getCurrent() < page.getPages());
        return resp;
    }

    public DiaryDetailResponse getDiaryDetail(String userId, String diaryId) {
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日记不存在");
        }

        if (!diary.getUserId().equals(userId)) {
            Relation relation = relationMapper.selectBoundByUserId(userId);
            if (relation == null) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            String partnerId = relation.getUserIdA().equals(userId) ? relation.getUserIdB() : relation.getUserIdA();
            if (!diary.getUserId().equals(partnerId)) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }

        List<DiaryImage> images = diaryImageMapper.selectList(
                new LambdaQueryWrapper<DiaryImage>()
                        .eq(DiaryImage::getDiaryId, diaryId)
                        .orderByAsc(DiaryImage::getSortOrder));

        DiaryDetailResponse resp = new DiaryDetailResponse();
        resp.setDiaryId(diary.getDiaryId());
        resp.setDate(diary.getDate());
        resp.setAuthorType(diary.getUserId().equals(userId) ? "me" : "ta");
        resp.setMoodCode(diary.getMoodCode());
        resp.setMoodText(diary.getMoodText());
        resp.setContent(diary.getContent());
        resp.setImageUrls(images.stream()
                .map(DiaryImage::getImageUrl)
                .map(mediaUrlHelper::toPublicUrl)
                .collect(Collectors.toList()));
        applyDiaryInsight(resp);
        return resp;
    }

    @Transactional
    public DiaryDetailResponse createDiary(String userId, CreateDiaryRequest req) {
        String moodText = req.getMoodText();
        if (moodText == null || moodText.isEmpty()) {
            moodText = MOOD_TEXT.getOrDefault(req.getMoodCode(), req.getMoodCode());
        }

        Diary diary = new Diary();
        diary.setDiaryId(IdGenerator.generate("d"));
        diary.setUserId(userId);
        diary.setDate(req.getDate());
        diary.setMoodCode(req.getMoodCode());
        diary.setMoodText(moodText);
        diary.setContent(req.getContent());
        diary.setCreatedAt(LocalDateTime.now());
        diaryMapper.insert(diary);

        List<String> imageUrls = new ArrayList<>();
        if (req.getImageFileIds() != null) {
            int order = 0;
            for (String fileId : req.getImageFileIds()) {
                FileRecord file = fileRecordMapper.selectById(fileId);
                if (file != null) {
                    String storedPath = mediaUrlHelper.toStoredPath(file.getFileUrl());
                    DiaryImage img = new DiaryImage();
                    img.setDiaryId(diary.getDiaryId());
                    img.setImageUrl(storedPath);
                    img.setSortOrder(order++);
                    diaryImageMapper.insert(img);
                    imageUrls.add(mediaUrlHelper.toPublicUrl(storedPath));
                }
            }
        }

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setMoodCode(req.getMoodCode());
            user.setMoodText(moodText);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        DiaryDetailResponse resp = new DiaryDetailResponse();
        resp.setDiaryId(diary.getDiaryId());
        resp.setDate(diary.getDate());
        resp.setAuthorType("me");
        resp.setMoodCode(diary.getMoodCode());
        resp.setMoodText(diary.getMoodText());
        resp.setContent(diary.getContent());
        resp.setImageUrls(imageUrls);
        applyDiaryInsight(resp);
        return resp;
    }

    public MoodTrendResponse getPartnerMoodTrend(String userId, int days) {
        Relation relation = relationMapper.selectBoundByUserId(userId);
        if (relation == null) {
            MoodTrendResponse resp = new MoodTrendResponse();
            resp.setPoints(Collections.emptyList());
            return resp;
        }

        String partnerId = relation.getUserIdA().equals(userId) ? relation.getUserIdB() : relation.getUserIdA();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Diary> diaries = diaryMapper.selectList(
                new LambdaQueryWrapper<Diary>()
                        .eq(Diary::getUserId, partnerId)
                        .ge(Diary::getDate, startDate.format(fmt))
                        .le(Diary::getDate, endDate.format(fmt))
                        .orderByAsc(Diary::getDate, Diary::getCreatedAt));

        Map<String, Diary> dateMap = new LinkedHashMap<>();
        for (Diary diary : diaries) {
            dateMap.put(diary.getDate(), diary);
        }

        List<MoodTrendResponse.MoodPoint> points = new ArrayList<>();
        DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateKey = date.format(fmt);
            String dateOut = date.format(outFmt);

            MoodTrendResponse.MoodPoint point = new MoodTrendResponse.MoodPoint();
            point.setDate(dateOut);

            Diary diary = dateMap.get(dateKey);
            if (diary != null) {
                point.setScore(MOOD_SCORE.getOrDefault(diary.getMoodCode(), 3));
                point.setMoodText(diary.getMoodText());
            } else {
                point.setScore(0);
                point.setMoodText("");
            }
            points.add(point);
        }

        MoodTrendResponse resp = new MoodTrendResponse();
        resp.setPoints(points);
        return resp;
    }

    @Transactional
    public void deleteDiary(String userId, String diaryId) {
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日记不存在");
        }

        // Only allow deleting own diaries
        if (!diary.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的日记");
        }

        // Delete diary images first
        diaryImageMapper.delete(
                new LambdaQueryWrapper<DiaryImage>()
                        .eq(DiaryImage::getDiaryId, diaryId)
        );

        // Delete diary
        diaryMapper.deleteById(diaryId);
    }

    private void applyDiaryInsight(DiaryDetailResponse resp) {
        DiaryInsightResponse insight = diaryInsightService.summarize(
                resp.getMoodCode(),
                resp.getMoodText(),
                resp.getContent(),
                resp.getImageUrls()
        );
        resp.setEmotionSummary(insight.getEmotionSummary());
        resp.setTriggerEvent(insight.getTriggerEvent());
        resp.setMessageToPartner(insight.getMessageToPartner());
    }
}
