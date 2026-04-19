package com.example.nestchat.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
import com.example.nestchat.api.ApiClient;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.DiaryApi;

import java.util.ArrayList;
import java.util.List;

public class DiaryFragment extends Fragment {

    private TextView tvTodayStatus;
    private TextView tvWeeklyCount;
    private TextView tvDiaryHint;
    private LinearLayout layoutMineDiaryList;
    private View itemDiaryTa;

    private final List<DiaryApi.DiarySummary> diaryItems = new ArrayList<>();

    private final ActivityResultLauncher<Intent> writeDiaryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadDiaryList();
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
        loadDiaryList();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDiaryList();
    }

    private void initViews(View view) {
        tvTodayStatus = view.findViewById(R.id.tvTodayStatus);
        tvWeeklyCount = view.findViewById(R.id.tvWeeklyCount);
        tvDiaryHint = view.findViewById(R.id.tvDiaryHint);
        layoutMineDiaryList = view.findViewById(R.id.layoutMineDiaryList);
        itemDiaryTa = view.findViewById(R.id.itemDiaryTa);
    }

    private void bindClicks(View view) {
        itemDiaryTa.setOnClickListener(v -> {
            // TA的日记区域 — 暂时不做特殊处理
        });

        view.findViewById(R.id.fabWriteDiary).setOnClickListener(v ->
                writeDiaryLauncher.launch(new Intent(requireContext(), WriteDiaryActivity.class)));
    }

    private void loadDiaryList() {
        DiaryApi.Impl.getDiaryList(1, 20, new ApiCallback<DiaryApi.DiaryListResponse>() {
            @Override
            public void onSuccess(DiaryApi.DiaryListResponse data) {
                if (!isAdded()) return;
                diaryItems.clear();
                if (data != null && data.items != null) {
                    diaryItems.addAll(data.items);
                }
                renderDiaryData();
            }

            @Override
            public void onError(ApiError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "加载日记失败: " + error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void renderDiaryData() {
        int count = diaryItems.size();
        if (count > 0) {
            tvTodayStatus.setText("共记录 " + count + " 篇日记");
            tvDiaryHint.setText("点击查看日记详情");
        } else {
            tvTodayStatus.setText("今天还没有写下你们的故事");
            tvDiaryHint.setText("把今天的心情和想说的话轻轻写下来");
        }
        tvWeeklyCount.setText("共 " + count + " 篇");

        renderMineDiaryList();
    }

    private void renderMineDiaryList() {
        layoutMineDiaryList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (DiaryApi.DiarySummary item : diaryItems) {
            View itemView = inflater.inflate(R.layout.item_diary_entry, layoutMineDiaryList, false);
            bindDiaryItem(itemView, item);
            layoutMineDiaryList.addView(itemView);
        }
    }

    private void bindDiaryItem(View itemView, DiaryApi.DiarySummary item) {
        TextView tvDate = itemView.findViewById(R.id.tvDiaryItemDate);
        TextView tvAuthor = itemView.findViewById(R.id.tvDiaryItemAuthor);
        TextView tvMood = itemView.findViewById(R.id.tvDiaryItemMood);
        TextView tvContent = itemView.findViewById(R.id.tvDiaryItemContent);
        TextView tvImageCount = itemView.findViewById(R.id.tvDiaryItemImageCount);
        View layoutImagePanel = itemView.findViewById(R.id.layoutDiaryItemImagePanel);
        ImageView ivPhoto = itemView.findViewById(R.id.ivDiaryItemPhoto);

        tvDate.setText(item.date != null ? item.date : "");
        tvAuthor.setText("self".equals(item.authorType) ? "我" : "TA");
        tvMood.setText("心情：" + (item.moodText != null ? item.moodText : ""));
        tvContent.setText(item.contentSummary != null ? item.contentSummary : "");
        tvImageCount.setText(item.imageCount + " 张图片");

        if (item.imageCount > 0 && item.coverUrl != null && !item.coverUrl.isEmpty()) {
            layoutImagePanel.setVisibility(View.VISIBLE);
            String fullUrl = item.coverUrl.startsWith("http") ? item.coverUrl
                    : ApiClient.BASE_URL.replace("/api/v1", "") + item.coverUrl;
            // For network images, we just show placeholder; full Glide integration is out of scope
            ivPhoto.setImageDrawable(null);
        } else {
            layoutImagePanel.setVisibility(item.imageCount > 0 ? View.VISIBLE : View.GONE);
            ivPhoto.setImageDrawable(null);
        }

        itemView.setOnClickListener(v -> openDiaryDetail(item.diaryId));
    }

    private void openDiaryDetail(String diaryId) {
        DiaryApi.Impl.getDiaryDetail(diaryId, new ApiCallback<DiaryApi.DiaryDetailResponse>() {
            @Override
            public void onSuccess(DiaryApi.DiaryDetailResponse data) {
                if (!isAdded() || data == null) return;

                Intent intent = new Intent(requireContext(), DiaryDetailActivity.class);
                intent.putExtra(DiaryDetailActivity.EXTRA_DATE, data.date);
                intent.putExtra(DiaryDetailActivity.EXTRA_AUTHOR, "self".equals(data.authorType) ? "我" : "TA");
                intent.putExtra(DiaryDetailActivity.EXTRA_MOOD, data.moodText != null ? data.moodText : "");
                intent.putExtra(DiaryDetailActivity.EXTRA_CONTENT, data.content != null ? data.content : "");

                ArrayList<String> imageUrls = new ArrayList<>();
                if (data.imageUrls != null) {
                    for (String url : data.imageUrls) {
                        String fullUrl = url.startsWith("http") ? url
                                : ApiClient.BASE_URL.replace("/api/v1", "") + url;
                        imageUrls.add(fullUrl);
                    }
                }
                intent.putExtra(DiaryDetailActivity.EXTRA_IMAGE_COUNT, imageUrls.size() + " 张图片");
                intent.putStringArrayListExtra(DiaryDetailActivity.EXTRA_IMAGE_URIS, imageUrls);
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
}
