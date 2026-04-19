package com.nestchat.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nestchat.server.common.BusinessException;
import com.nestchat.server.common.IdGenerator;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.dto.request.CreateDiaryRequest;
import com.nestchat.server.dto.response.*;
import com.nestchat.server.entity.*;
import com.nestchat.server.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiaryService {

    private static final Map<String, Integer> MOOD_SCORE = Map.of(
            "happy", 5,
            "sad", 2,
            "tired", 3
    );
    private static final Map<String, String> MOOD_TEXT = Map.of(
            "happy", "开心",
            "sad", "难过",
            "tired", "疲惫"
    );

    private final DiaryMapper diaryMapper;
    private final DiaryImageMapper diaryImageMapper;
    private final FileRecordMapper fileRecordMapper;
    private final RelationMapper relationMapper;
    private final UserMapper userMapper;

    public DiaryService(DiaryMapper diaryMapper, DiaryImageMapper diaryImageMapper,
                        FileRecordMapper fileRecordMapper, RelationMapper relationMapper,
                        UserMapper userMapper) {
        this.diaryMapper = diaryMapper;
        this.diaryImageMapper = diaryImageMapper;
        this.fileRecordMapper = fileRecordMapper;
        this.relationMapper = relationMapper;
        this.userMapper = userMapper;
    }

    public DiaryListResponse listDiaries(String userId, int pageNo, int pageSize) {
        // 获取自己和伴侣的 userId
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

            // 摘要
            String content = diary.getContent() != null ? diary.getContent() : "";
            item.setContentSummary(content.length() > 50 ? content.substring(0, 50) + "……" : content);

            // 图片信息
            List<DiaryImage> images = diaryImageMapper.selectList(
                    new LambdaQueryWrapper<DiaryImage>()
                            .eq(DiaryImage::getDiaryId, diary.getDiaryId())
                            .orderByAsc(DiaryImage::getSortOrder));
            item.setImageCount(images.size());
            item.setCoverUrl(images.isEmpty() ? "" : images.get(0).getImageUrl());

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

        // 校验权限：只能看自己或伴侣的日记
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
        resp.setImageUrls(images.stream().map(DiaryImage::getImageUrl).collect(Collectors.toList()));
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

        // 处理图片
        List<String> imageUrls = new ArrayList<>();
        if (req.getImageFileIds() != null) {
            int order = 0;
            for (String fileId : req.getImageFileIds()) {
                FileRecord file = fileRecordMapper.selectById(fileId);
                if (file != null) {
                    DiaryImage img = new DiaryImage();
                    img.setDiaryId(diary.getDiaryId());
                    img.setImageUrl(file.getFileUrl());
                    img.setSortOrder(order++);
                    diaryImageMapper.insert(img);
                    imageUrls.add(file.getFileUrl());
                }
            }
        }

        // 同步更新用户情绪
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

        // 查询伴侣最近 N 天的日记
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        List<Diary> diaries = diaryMapper.selectList(
                new LambdaQueryWrapper<Diary>()
                        .eq(Diary::getUserId, partnerId)
                        .ge(Diary::getDate, startDate.format(fmt))
                        .le(Diary::getDate, endDate.format(fmt))
                        .orderByAsc(Diary::getDate));

        // 按日期取最新一条日记的情绪
        Map<String, Diary> dateMap = new LinkedHashMap<>();
        for (Diary d : diaries) {
            dateMap.put(d.getDate(), d);
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
}
