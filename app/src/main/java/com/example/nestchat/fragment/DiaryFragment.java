package com.example.nestchat.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nestchat.DiaryDetailActivity;
import com.example.nestchat.R;
import com.example.nestchat.WriteDiaryActivity;
import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.DiaryApi;
import com.example.nestchat.api.RelationApi;

import java.util.ArrayList;
import java.util.List;

public class DiaryFragment extends Fragment {

    private static final int TREND_DAYS = 7;

    private TextView tvTodayStatus;
    private TextView tvWeeklyCount;
    private TextView tvDiaryHint;
    private TextView tvDiaryEmptyTitle;
    private TextView tvDiaryEmptySubtitle;
    private TextView tvPartnerDiaryTitle;
    private TextView tvPartnerDiaryDate;
    private TextView tvPartnerDiaryMood;
    private TextView tvPartnerDiaryContent;
    private TextView tvPartnerDiaryImageCount;
    private ImageView ivPartnerDiaryPhoto;
    private TextView tvTrendSectionTitle;
    private TextView tvTrendHint;
    private TextView tvTrendEmpty;
    private LinearLayout layoutMineDiaryList;
    private LinearLayout layoutTrendBars;
    private LinearLayout layoutTrendDates;
    private View cardDiaryEmpty;
    private View cardPartnerDiary;
    private View layoutPartnerDiaryImagePanel;
    private View layoutTrendPanel;

    private boolean isBound;
    private String partnerName = "TA";
    private int totalDiaryCount;
    private final List<DiaryApi.DiarySummary> myDiaryItems = new ArrayList<>();
    private DiaryApi.DiarySummary latestPartnerDiary;
    private final List<DiaryApi.MoodTrendPoint> trendPoints = new ArrayList<>();

    private final ActivityResultLauncher<Intent> writeDiaryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadPageData();
                }
            });

    public DiaryFragment() {
        // Required empty public constructor.
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        bindClicks(view);
        loadPageData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPageData();
    }

    private void initViews(View view) {
        tvTodayStatus = view.findViewById(R.id.tvTodayStatus);
        tvWeeklyCount = view.findViewById(R.id.tvWeeklyCount);
        tvDiaryHint = view.findViewById(R.id.tvDiaryHint);
        tvDiaryEmptyTitle = view.findViewById(R.id.tvDiaryEmptyTitle);
        tvDiaryEmptySubtitle = view.findViewById(R.id.tvDiaryEmptySubtitle);
        tvPartnerDiaryTitle = view.findViewById(R.id.tvPartnerDiaryTitle);
        tvPartnerDiaryDate = view.findViewById(R.id.tvPartnerDiaryDate);
        tvPartnerDiaryMood = view.findViewById(R.id.tvPartnerDiaryMood);
        tvPartnerDiaryContent = view.findViewById(R.id.tvPartnerDiaryContent);
        tvPartnerDiaryImageCount = view.findViewById(R.id.tvPartnerDiaryImageCount);
        ivPartnerDiaryPhoto = view.findViewById(R.id.ivPartnerDiaryPhoto);
        tvTrendSectionTitle = view.findViewById(R.id.tvTrendSectionTitle);
        tvTrendHint = view.findViewById(R.id.tvTrendHint);
        tvTrendEmpty = view.findViewById(R.id.tvTrendEmpty);
        layoutMineDiaryList = view.findViewById(R.id.layoutMineDiaryList);
        layoutTrendBars = view.findViewById(R.id.layoutTrendBars);
        layoutTrendDates = view.findViewById(R.id.layoutTrendDates);
        cardDiaryEmpty = view.findViewById(R.id.cardDiaryEmpty);
        cardPartnerDiary = view.findViewById(R.id.cardPartnerDiary);
        layoutPartnerDiaryImagePanel = view.findViewById(R.id.layoutPartnerDiaryImagePanel);
        layoutTrendPanel = view.findViewById(R.id.layoutTrendPanel);
    }

    private void bindClicks(View view) {
        cardPartnerDiary.setOnClickListener(v -> {
            if (latestPartnerDiary != null && !TextUtils.isEmpty(latestPartnerDiary.diaryId)) {
                openDiaryDetail(latestPartnerDiary.diaryId);
            }
        });

        view.findViewById(R.id.fabWriteDiary).setOnClickListener(v ->
                writeDiaryLauncher.launch(new Intent(requireContext(), WriteDiaryActivity.class)));
    }

    private void loadPageData() {
        resetDiaryState();
        renderLoadingState();

        RelationApi.Impl.getCurrentRelation(new ApiCallback<RelationApi.RelationStatusResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationStatusResponse data) {
                if (!isAdded()) {
                    return;
                }
                isBound = data != null && "bound".equals(data.status);
                partnerName = resolvePartnerName(data);
                if (!isBound) {
                    renderUnboundState();
                    return;
                }
                loadDiaryList();
                loadPartnerTrend();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) {
                    return;
                }
                isBound = false;
                partnerName = "TA";
                renderUnboundState();
            }
        });
    }

    private void resetDiaryState() {
        totalDiaryCount = 0;
        partnerName = "TA";
        latestPartnerDiary = null;
        myDiaryItems.clear();
        trendPoints.clear();
    }

    private void renderLoadingState() {
        tvTodayStatus.setText("正在加载日记数据");
        tvWeeklyCount.setText("请稍候");
        tvDiaryHint.setText("正在同步关系状态、日记记录和情绪趋势");
        renderDiaryEmptyCard("正在加载", "请稍候片刻。");
        cardPartnerDiary.setVisibility(View.GONE);
        renderTrendEmptyState("正在加载情绪趋势...");
    }

    private void loadDiaryList() {
        DiaryApi.Impl.getDiaryList(1, 20, new ApiCallback<DiaryApi.DiaryListResponse>() {
            @Override
            public void onSuccess(DiaryApi.DiaryListResponse data) {
                if (!isAdded()) {
                    return;
                }

                totalDiaryCount = 0;
                myDiaryItems.clear();
                latestPartnerDiary = null;

                List<DiaryApi.DiarySummary> items = data != null ? data.items : null;
                if (items != null) {
                    totalDiaryCount = items.size();
                    android.util.Log.d("DiaryFragment", "Total items from server: " + items.size());
                    for (DiaryApi.DiarySummary item : items) {
                        String authorType = safeTrim(item.authorType);
                        android.util.Log.d("DiaryFragment", "Item: " + item.date + " by " + authorType);
                        if ("ta".equals(authorType) && latestPartnerDiary == null) {
                            latestPartnerDiary = item;
                            android.util.Log.d("DiaryFragment", "Added as partner diary");
                        } else if ("me".equals(authorType)) {
                            myDiaryItems.add(item);
                            android.util.Log.d("DiaryFragment", "Added as my diary");
                        }
                    }
                    android.util.Log.d("DiaryFragment", "Final count - Total: " + totalDiaryCount + ", Mine: " + myDiaryItems.size());
                }

                renderBoundState();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "加载日记失败: " + error.message, Toast.LENGTH_SHORT).show();
                renderBoundState();
            }
        });
    }

    private void loadPartnerTrend() {
        DiaryApi.Impl.getPartnerMoodTrend(TREND_DAYS, new ApiCallback<DiaryApi.MoodTrendResponse>() {
            @Override
            public void onSuccess(DiaryApi.MoodTrendResponse data) {
                if (!isAdded()) {
                    return;
                }
                trendPoints.clear();
                if (data != null && data.points != null) {
                    trendPoints.addAll(data.points);
                }
                renderBoundState();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "加载趋势失败: " + error.message, Toast.LENGTH_SHORT).show();
                renderBoundState();
            }
        });
    }

    private void renderUnboundState() {
        tvTodayStatus.setText("暂未绑定关系");
        tvWeeklyCount.setText("当前没有可展示的共享日记");
        tvDiaryHint.setText("绑定后可查看双方日记和 TA 的情绪趋势");
        renderDiaryEmptyCard("当前没有可展示的共享日记", "未绑定时这里不会展示共享日记，也不会显示 TA 的情绪趋势。");
        cardPartnerDiary.setVisibility(View.GONE);
        tvTrendSectionTitle.setText("TA 最近 " + TREND_DAYS + " 天情绪趋势");
        tvTrendHint.setText("趋势严格按数据库中的日记记录渲染。");
        renderTrendEmptyState("暂未绑定关系，无法查看 TA 的情绪趋势。");
    }

    private void renderBoundState() {
        if (!isBound) {
            renderUnboundState();
            return;
        }

        int myCount = myDiaryItems.size();
        if (myCount > 0) {
            tvTodayStatus.setText("共记录 " + myCount + " 篇日记");
            tvDiaryHint.setText("当前页面只展示数据库中的真实日记与趋势数据");
        } else {
            tvTodayStatus.setText("你还没有写日记");
            tvDiaryHint.setText("写下今天的心情和故事，慢慢积累属于你们的记录");
        }
        tvWeeklyCount.setText("我的日记 " + myCount + " 篇");

        renderMineDiaryList();
        renderPartnerDiaryCard();
        tvTrendSectionTitle.setText(partnerName + " 最近 " + TREND_DAYS + " 天情绪趋势");
        tvTrendHint.setText("趋势严格按数据库中的日记记录渲染。");
        if (hasTrendData()) {
            renderTrendChart();
        } else {
            renderTrendEmptyState(partnerName + " 最近 " + TREND_DAYS + " 天还没有留下情绪记录。");
        }
    }

    private boolean hasTrendData() {
        for (DiaryApi.MoodTrendPoint point : trendPoints) {
            if (point != null && point.score > 0) {
                return true;
            }
        }
        return false;
    }

    private void renderDiaryEmptyCard(String title, String subtitle) {
        cardDiaryEmpty.setVisibility(View.VISIBLE);
        layoutMineDiaryList.setVisibility(View.GONE);
        layoutMineDiaryList.removeAllViews();
        tvDiaryEmptyTitle.setText(title);
        tvDiaryEmptySubtitle.setText(subtitle);
    }

    private void hideDiaryEmptyCard() {
        cardDiaryEmpty.setVisibility(View.GONE);
        layoutMineDiaryList.setVisibility(View.VISIBLE);
    }

    private void renderMineDiaryList() {
        layoutMineDiaryList.removeAllViews();
        if (myDiaryItems.isEmpty()) {
            renderDiaryEmptyCard("你还没有写日记", "点击右下角按钮，先写下今天的第一篇日记。");
            return;
        }

        hideDiaryEmptyCard();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (DiaryApi.DiarySummary item : myDiaryItems) {
            View itemView = inflater.inflate(R.layout.item_diary_entry, layoutMineDiaryList, false);
            bindDiaryItem(itemView, item);
            layoutMineDiaryList.addView(itemView);
        }
    }

    private void renderPartnerDiaryCard() {
        cardPartnerDiary.setVisibility(View.VISIBLE);
        if (latestPartnerDiary == null) {
            tvPartnerDiaryTitle.setText(partnerName + " 还没有写日记");
            tvPartnerDiaryDate.setText("暂无最新记录");
            tvPartnerDiaryMood.setText("等待第一篇日记");
            tvPartnerDiaryContent.setText("当 " + partnerName + " 写下新的日记，这里会显示最近一篇。");
            layoutPartnerDiaryImagePanel.setVisibility(View.GONE);
            ivPartnerDiaryPhoto.setImageDrawable(null);
            cardPartnerDiary.setClickable(false);
            cardPartnerDiary.setFocusable(false);
            return;
        }

        cardPartnerDiary.setClickable(true);
        cardPartnerDiary.setFocusable(true);
        tvPartnerDiaryTitle.setText(partnerName + " 的最新日记");
        tvPartnerDiaryDate.setText(nonEmpty(latestPartnerDiary.date, "暂无日期"));
        tvPartnerDiaryMood.setText(buildMoodLabel(latestPartnerDiary.moodText));
        tvPartnerDiaryContent.setText(nonEmpty(latestPartnerDiary.contentSummary, "这篇日记还没有摘要。"));
        if (latestPartnerDiary.imageCount > 0) {
            layoutPartnerDiaryImagePanel.setVisibility(View.VISIBLE);
            com.example.nestchat.util.AvatarImageLoader.loadContent(ivPartnerDiaryPhoto, latestPartnerDiary.coverUrl);
            tvPartnerDiaryImageCount.setText(latestPartnerDiary.imageCount + " 张图片");
        } else {
            layoutPartnerDiaryImagePanel.setVisibility(View.GONE);
            ivPartnerDiaryPhoto.setImageDrawable(null);
        }
    }

    private void renderTrendChart() {
        tvTrendEmpty.setVisibility(View.GONE);
        layoutTrendPanel.setVisibility(View.VISIBLE);
        layoutTrendBars.removeAllViews();
        layoutTrendDates.removeAllViews();

        for (DiaryApi.MoodTrendPoint point : trendPoints) {
            layoutTrendBars.addView(buildTrendBarItem(point));
            layoutTrendDates.addView(buildTrendDateItem(point));
        }
    }

    private void renderTrendEmptyState(String message) {
        layoutTrendPanel.setVisibility(View.GONE);
        layoutTrendBars.removeAllViews();
        layoutTrendDates.removeAllViews();
        tvTrendEmpty.setVisibility(View.VISIBLE);
        tvTrendEmpty.setText(message);
    }

    private View buildTrendBarItem(DiaryApi.MoodTrendPoint point) {
        LinearLayout column = new LinearLayout(requireContext());
        column.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        TextView tvEmoji = new TextView(requireContext());
        tvEmoji.setText(resolveTrendEmoji(point));
        tvEmoji.setTextSize(24f);
        column.addView(tvEmoji);

        View bar = new View(requireContext());
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(16), dp(resolveTrendBarHeight(point)));
        barParams.topMargin = dp(6);
        bar.setLayoutParams(barParams);
        applyTrendBarStyle(bar, point != null ? point.score : 0);
        column.addView(bar);

        TextView tvScore = new TextView(requireContext());
        LinearLayout.LayoutParams scoreParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scoreParams.topMargin = dp(6);
        tvScore.setLayoutParams(scoreParams);
        int score = point != null ? point.score : 0;
        tvScore.setText(score > 0 ? String.valueOf(score) : "-");
        tvScore.setTextSize(14f);
        tvScore.setTextColor(requireContext().getColor(
                score >= 5 ? R.color.brand_primary_dark : R.color.text_secondary));
        column.addView(tvScore);

        return column;
    }

    private View buildTrendDateItem(DiaryApi.MoodTrendPoint point) {
        TextView tvDate = new TextView(requireContext());
        tvDate.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvDate.setGravity(android.view.Gravity.CENTER);
        tvDate.setText(resolveTrendDateLabel(point != null ? point.date : null));
        tvDate.setTextSize(12f);
        tvDate.setTextColor(requireContext().getColor(R.color.text_secondary));
        return tvDate;
    }

    private void applyTrendBarStyle(View bar, int score) {
        if (score >= 5) {
            bar.setBackgroundResource(R.drawable.bg_diary_bar_high);
            return;
        }
        if (score >= 3) {
            bar.setBackgroundResource(R.drawable.bg_diary_bar_mid);
            return;
        }
        if (score > 0) {
            bar.setBackgroundResource(R.drawable.bg_diary_bar_low);
            return;
        }
        bar.setBackgroundColor(requireContext().getColor(R.color.divider_tint));
    }

    private int resolveTrendBarHeight(DiaryApi.MoodTrendPoint point) {
        int score = point != null ? point.score : 0;
        if (score <= 0) {
            return 8;
        }
        return 12 + (score * 10);
    }

    private String resolveTrendEmoji(DiaryApi.MoodTrendPoint point) {
        if (point == null || point.score <= 0) {
            return "·";
        }
        String mood = safeTrim(point.moodText);
        if ("平静".equals(mood)) {
            return "😌";
        }
        if ("心动".equals(mood)) {
            return "🥰";
        }
        if ("难过".equals(mood)) {
            return "😢";
        }
        if ("委屈".equals(mood)) {
            return "🥺";
        }
        if ("生气".equals(mood)) {
            return "😤";
        }
        if ("疲惫".equals(mood)) {
            return "😣";
        }
        if ("焦虑".equals(mood)) {
            return "😰";
        }
        return "🙂";
    }

    private String resolveTrendDateLabel(String rawDate) {
        String value = safeTrim(rawDate);
        if (value.length() >= 5) {
            return value.substring(value.length() - 5);
        }
        return TextUtils.isEmpty(value) ? "--/--" : value;
    }

    private void bindDiaryItem(View itemView, DiaryApi.DiarySummary item) {
        TextView tvDate = itemView.findViewById(R.id.tvDiaryItemDate);
        TextView tvAuthor = itemView.findViewById(R.id.tvDiaryItemAuthor);
        TextView tvMood = itemView.findViewById(R.id.tvDiaryItemMood);
        TextView tvContent = itemView.findViewById(R.id.tvDiaryItemContent);
        TextView tvImageCount = itemView.findViewById(R.id.tvDiaryItemImageCount);
        View layoutImagePanel = itemView.findViewById(R.id.layoutDiaryItemImagePanel);
        ImageView ivDiaryItemPhoto = itemView.findViewById(R.id.ivDiaryItemPhoto);

        tvDate.setText(nonEmpty(item.date, ""));
        tvAuthor.setText("me".equals(safeTrim(item.authorType)) ? "我" : "TA");
        tvMood.setText(buildMoodLabel(item.moodText));
        tvContent.setText(nonEmpty(item.contentSummary, ""));
        tvImageCount.setText(item.imageCount + " 张图片");
        layoutImagePanel.setVisibility(item.imageCount > 0 ? View.VISIBLE : View.GONE);

        if (item.imageCount > 0 && !TextUtils.isEmpty(item.coverUrl)) {
            com.example.nestchat.util.AvatarImageLoader.loadContent(ivDiaryItemPhoto, item.coverUrl);
        } else {
            ivDiaryItemPhoto.setImageDrawable(null);
        }

        itemView.setOnClickListener(v -> openDiaryDetail(item.diaryId));
    }

    private void openDiaryDetail(String diaryId) {
        DiaryApi.Impl.getDiaryDetail(diaryId, new ApiCallback<DiaryApi.DiaryDetailResponse>() {
            @Override
            public void onSuccess(DiaryApi.DiaryDetailResponse data) {
                if (!isAdded() || data == null) {
                    return;
                }

                Intent intent = new Intent(requireContext(), DiaryDetailActivity.class);
                intent.putExtra(DiaryDetailActivity.EXTRA_DATE, data.date);
                intent.putExtra(DiaryDetailActivity.EXTRA_AUTHOR, "me".equals(safeTrim(data.authorType)) ? "我" : "TA");
                intent.putExtra(DiaryDetailActivity.EXTRA_MOOD, buildMoodLabel(data.moodText));
                intent.putExtra(DiaryDetailActivity.EXTRA_CONTENT, nonEmpty(data.content, ""));
                intent.putExtra(DiaryDetailActivity.EXTRA_EMOTION_SUMMARY, nonEmpty(data.emotionSummary, ""));
                intent.putExtra(DiaryDetailActivity.EXTRA_TRIGGER_EVENT, nonEmpty(data.triggerEvent, ""));
                intent.putExtra(DiaryDetailActivity.EXTRA_MESSAGE_TO_PARTNER, nonEmpty(data.messageToPartner, ""));

                ArrayList<String> imageUrls = new ArrayList<>();
                if (data.imageUrls != null) {
                    imageUrls.addAll(data.imageUrls);
                }
                intent.putExtra(DiaryDetailActivity.EXTRA_IMAGE_COUNT, imageUrls.size() + " 张图片");
                intent.putStringArrayListExtra(DiaryDetailActivity.EXTRA_IMAGE_URIS, imageUrls);

                // Only pass diaryId for own diaries to enable delete
                if ("me".equals(safeTrim(data.authorType))) {
                    intent.putExtra(DiaryDetailActivity.EXTRA_DIARY_ID, diaryId);
                }

                startActivity(intent);
            }

            @Override
            public void onError(ApiError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String buildMoodLabel(String moodText) {
        String value = safeTrim(moodText);
        return TextUtils.isEmpty(value) ? "心情：未记录" : "心情：" + value;
    }

    private String resolvePartnerName(RelationApi.RelationStatusResponse relation) {
        if (relation == null) {
            return "TA";
        }
        String remark = safeTrim(relation.partnerRemark);
        if (!TextUtils.isEmpty(remark)) {
            return remark;
        }
        String nickname = safeTrim(relation.partnerNickname);
        if (!TextUtils.isEmpty(nickname)) {
            return nickname;
        }
        String phone = safeTrim(relation.partnerPhone);
        if (!TextUtils.isEmpty(phone)) {
            return phone;
        }
        return "TA";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nonEmpty(String value, String fallback) {
        String trimmed = safeTrim(value);
        return TextUtils.isEmpty(trimmed) ? fallback : trimmed;
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
