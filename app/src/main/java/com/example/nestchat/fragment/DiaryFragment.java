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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nestchat.DiaryDetailActivity;
import com.example.nestchat.R;
import com.example.nestchat.WriteDiaryActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DiaryFragment extends Fragment {

    private static final String STATE_MINE_ENTRIES = "state_mine_entries";

    private static final String TA_DATE = "2026.04.17";
    private static final String TA_AUTHOR = "TA";
    private static final String TA_MOOD = "难过 😢";
    private static final String TA_CONTENT =
            "今天心情不太好，感觉有些疲惫。希望明天会轻一点，也希望能早点收到你的消息。";
    private static final String TA_IMAGE_COUNT = "1 张图片";

    private TextView tvTodayStatus;
    private TextView tvWeeklyCount;
    private TextView tvDiaryHint;
    private LinearLayout layoutMineDiaryList;
    private final ArrayList<DiaryEntry> mineEntries = new ArrayList<>();

    private final ActivityResultLauncher<Intent> writeDiaryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    return;
                }

                DiaryEntry newEntry = DiaryEntry.fromIntent(result.getData());
                mineEntries.add(0, newEntry);
                renderDiaryData();
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
        restoreState(savedInstanceState);
        initViews(view);
        bindClicks(view);
        renderDiaryData();
    }

    private void restoreState(Bundle savedInstanceState) {
        mineEntries.clear();

        if (savedInstanceState == null) {
            mineEntries.add(createDefaultMineEntry());
            return;
        }

        ArrayList<Bundle> savedEntries =
                savedInstanceState.getParcelableArrayList(STATE_MINE_ENTRIES);
        if (savedEntries == null || savedEntries.isEmpty()) {
            mineEntries.add(createDefaultMineEntry());
            return;
        }

        for (Bundle bundle : savedEntries) {
            mineEntries.add(DiaryEntry.fromBundle(bundle));
        }
    }

    private void initViews(View view) {
        tvTodayStatus = view.findViewById(R.id.tvTodayStatus);
        tvWeeklyCount = view.findViewById(R.id.tvWeeklyCount);
        tvDiaryHint = view.findViewById(R.id.tvDiaryHint);
        layoutMineDiaryList = view.findViewById(R.id.layoutMineDiaryList);
    }

    private void bindClicks(View view) {
        view.findViewById(R.id.itemDiaryTa).setOnClickListener(v ->
                openDiaryDetail(
                        TA_DATE,
                        TA_AUTHOR,
                        TA_MOOD,
                        TA_CONTENT,
                        TA_IMAGE_COUNT,
                        "",
                        new ArrayList<>()
                ));

        view.findViewById(R.id.fabWriteDiary).setOnClickListener(v ->
                writeDiaryLauncher.launch(new Intent(requireContext(), WriteDiaryActivity.class)));
    }

    private void renderDiaryData() {
        renderOverview();
        renderMineDiaryList();
    }

    private void renderOverview() {
        int todayRecordCount = countTodayRecords();
        int weeklyRecordCount = mineEntries.size() + 1;

        if (todayRecordCount > 0) {
            tvTodayStatus.setText("今天已记录 " + todayRecordCount + " 次");
            tvDiaryHint.setText("刚刚保存的日记已经同步到列表中");
        } else {
            tvTodayStatus.setText("今天还没有写下你们的故事");
            tvDiaryHint.setText("把今天的心情和想说的话轻轻写下来");
        }

        tvWeeklyCount.setText("本周已记录 " + weeklyRecordCount + " 次");
    }

    private int countTodayRecords() {
        String today = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date());
        int count = 0;
        for (DiaryEntry entry : mineEntries) {
            if (today.equals(entry.date)) {
                count++;
            }
        }
        return count;
    }

    private void renderMineDiaryList() {
        layoutMineDiaryList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (DiaryEntry entry : mineEntries) {
            View itemView = inflater.inflate(R.layout.item_diary_entry, layoutMineDiaryList, false);
            bindDiaryItem(itemView, entry);
            layoutMineDiaryList.addView(itemView);
        }
    }

    private void bindDiaryItem(View itemView, DiaryEntry entry) {
        TextView tvDate = itemView.findViewById(R.id.tvDiaryItemDate);
        TextView tvAuthor = itemView.findViewById(R.id.tvDiaryItemAuthor);
        TextView tvMood = itemView.findViewById(R.id.tvDiaryItemMood);
        TextView tvContent = itemView.findViewById(R.id.tvDiaryItemContent);
        TextView tvImageCount = itemView.findViewById(R.id.tvDiaryItemImageCount);
        View layoutImagePanel = itemView.findViewById(R.id.layoutDiaryItemImagePanel);
        ImageView ivPhoto = itemView.findViewById(R.id.ivDiaryItemPhoto);

        tvDate.setText(entry.date);
        tvAuthor.setText(entry.author);
        tvMood.setText("心情：" + entry.mood);
        tvContent.setText(entry.content);
        tvImageCount.setText(entry.imageCount);

        if (!entry.imageUris.isEmpty()) {
            layoutImagePanel.setVisibility(View.VISIBLE);
            ivPhoto.setImageURI(Uri.parse(entry.imageUris.get(0)));
        } else if (!TextUtils.isEmpty(entry.imageUri)) {
            layoutImagePanel.setVisibility(View.VISIBLE);
            ivPhoto.setImageURI(Uri.parse(entry.imageUri));
        } else if (entry.imageCount.startsWith("0")) {
            layoutImagePanel.setVisibility(View.GONE);
            ivPhoto.setImageDrawable(null);
        } else {
            layoutImagePanel.setVisibility(View.VISIBLE);
            ivPhoto.setImageDrawable(null);
        }

        itemView.setOnClickListener(v ->
                openDiaryDetail(
                        entry.date,
                        entry.author,
                        entry.mood,
                        entry.content,
                        entry.imageCount,
                        entry.imageUri,
                        entry.imageUris
                ));
    }

    private void openDiaryDetail(String date, String author, String mood, String content,
                                 String imageCount, String imageUri, ArrayList<String> imageUris) {
        Intent intent = new Intent(requireContext(), DiaryDetailActivity.class);
        intent.putExtra(DiaryDetailActivity.EXTRA_DATE, date);
        intent.putExtra(DiaryDetailActivity.EXTRA_AUTHOR, author);
        intent.putExtra(DiaryDetailActivity.EXTRA_MOOD, mood);
        intent.putExtra(DiaryDetailActivity.EXTRA_CONTENT, content);
        intent.putExtra(DiaryDetailActivity.EXTRA_IMAGE_COUNT, imageCount);
        intent.putExtra(DiaryDetailActivity.EXTRA_IMAGE_URI, imageUri == null ? "" : imageUri);
        intent.putStringArrayListExtra(
                DiaryDetailActivity.EXTRA_IMAGE_URIS,
                imageUris == null ? new ArrayList<>() : new ArrayList<>(imageUris)
        );
        if ((imageUri != null && !imageUri.isEmpty()) || (imageUris != null && !imageUris.isEmpty())) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivity(intent);
    }

    private DiaryEntry createDefaultMineEntry() {
        return new DiaryEntry(
                "2026.04.16",
                "我",
                "开心 🙂",
                "今天我们聊了很久，感觉轻松了很多，想把这份平静好好记下来。",
                "1 张图片",
                "",
                new ArrayList<>()
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<Bundle> entryBundles = new ArrayList<>();
        for (DiaryEntry entry : mineEntries) {
            entryBundles.add(entry.toBundle());
        }
        outState.putParcelableArrayList(STATE_MINE_ENTRIES, entryBundles);
    }

    private static class DiaryEntry {
        final String date;
        final String author;
        final String mood;
        final String content;
        final String imageCount;
        final String imageUri;
        final ArrayList<String> imageUris;

        DiaryEntry(String date, String author, String mood, String content,
                   String imageCount, String imageUri, ArrayList<String> imageUris) {
            this.date = date;
            this.author = author;
            this.mood = mood;
            this.content = content;
            this.imageCount = imageCount;
            this.imageUri = imageUri == null ? "" : imageUri;
            this.imageUris = imageUris == null ? new ArrayList<>() : imageUris;
        }

        Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(DiaryDetailActivity.EXTRA_DATE, date);
            bundle.putString(DiaryDetailActivity.EXTRA_AUTHOR, author);
            bundle.putString(DiaryDetailActivity.EXTRA_MOOD, mood);
            bundle.putString(DiaryDetailActivity.EXTRA_CONTENT, content);
            bundle.putString(DiaryDetailActivity.EXTRA_IMAGE_COUNT, imageCount);
            bundle.putString(DiaryDetailActivity.EXTRA_IMAGE_URI, imageUri);
            bundle.putStringArrayList(DiaryDetailActivity.EXTRA_IMAGE_URIS, imageUris);
            return bundle;
        }

        static DiaryEntry fromBundle(Bundle bundle) {
            return new DiaryEntry(
                    bundle.getString(DiaryDetailActivity.EXTRA_DATE, ""),
                    bundle.getString(DiaryDetailActivity.EXTRA_AUTHOR, "我"),
                    bundle.getString(DiaryDetailActivity.EXTRA_MOOD, "开心 🙂"),
                    bundle.getString(DiaryDetailActivity.EXTRA_CONTENT, ""),
                    bundle.getString(DiaryDetailActivity.EXTRA_IMAGE_COUNT, "0 张图片"),
                    bundle.getString(DiaryDetailActivity.EXTRA_IMAGE_URI, ""),
                    bundle.getStringArrayList(DiaryDetailActivity.EXTRA_IMAGE_URIS)
            );
        }

        static DiaryEntry fromIntent(Intent intent) {
            return new DiaryEntry(
                    intent.getStringExtra(DiaryDetailActivity.EXTRA_DATE),
                    intent.getStringExtra(DiaryDetailActivity.EXTRA_AUTHOR),
                    intent.getStringExtra(DiaryDetailActivity.EXTRA_MOOD),
                    intent.getStringExtra(DiaryDetailActivity.EXTRA_CONTENT),
                    intent.getStringExtra(DiaryDetailActivity.EXTRA_IMAGE_COUNT),
                    intent.getStringExtra(DiaryDetailActivity.EXTRA_IMAGE_URI),
                    intent.getStringArrayListExtra(DiaryDetailActivity.EXTRA_IMAGE_URIS)
            );
        }
    }
}
