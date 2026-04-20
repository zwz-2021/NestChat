package com.example.nestchat;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.DiaryApi;
import com.example.nestchat.util.AvatarImageLoader;

import java.util.ArrayList;
import java.util.List;

public class DiaryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_AUTHOR = "extra_author";
    public static final String EXTRA_MOOD = "extra_mood";
    public static final String EXTRA_CONTENT = "extra_content";
    public static final String EXTRA_IMAGE_COUNT = "extra_image_count";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_IMAGE_URIS = "extra_image_uris";
    public static final String EXTRA_DIARY_ID = "extra_diary_id";
    public static final String EXTRA_EMOTION_SUMMARY = "extra_emotion_summary";
    public static final String EXTRA_TRIGGER_EVENT = "extra_trigger_event";
    public static final String EXTRA_MESSAGE_TO_PARTNER = "extra_message_to_partner";

    private String diaryId;
    private TextView tvDate;
    private TextView tvAuthor;
    private TextView tvMood;
    private TextView tvContent;
    private TextView tvImageCount;
    private TextView tvPhotoPlaceholder;
    private TextView tvDelete;
    private TextView tvEmotionSummary;
    private TextView tvTriggerEvent;
    private TextView tvMessageToPartner;
    private View layoutPhotoPanel;
    private View layoutDiaryInsight;
    private LinearLayout layoutDiaryPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_diary_detail);

        diaryId = getIntent().getStringExtra(EXTRA_DIARY_ID);

        applyWindowInsets();
        initViews();
        bindEvents();
        bindIntentSnapshot();
        bindDeleteState();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDiaryDate);
        tvAuthor = findViewById(R.id.tvDiaryAuthor);
        tvMood = findViewById(R.id.tvDiaryMood);
        tvContent = findViewById(R.id.tvDiaryContent);
        tvImageCount = findViewById(R.id.tvImageCount);
        tvPhotoPlaceholder = findViewById(R.id.tvPhotoPlaceholder);
        tvDelete = findViewById(R.id.tvDelete);
        tvEmotionSummary = findViewById(R.id.tvEmotionSummary);
        tvTriggerEvent = findViewById(R.id.tvTriggerEvent);
        tvMessageToPartner = findViewById(R.id.tvMessageToPartner);
        layoutPhotoPanel = findViewById(R.id.layoutPhotoPanel);
        layoutDiaryInsight = findViewById(R.id.layoutDiaryInsight);
        layoutDiaryPhotos = findViewById(R.id.layoutDiaryPhotos);
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());
    }

    private void bindIntentSnapshot() {
        String date = getIntent().getStringExtra(EXTRA_DATE);
        String author = getIntent().getStringExtra(EXTRA_AUTHOR);
        String mood = getIntent().getStringExtra(EXTRA_MOOD);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);
        String imageCount = getIntent().getStringExtra(EXTRA_IMAGE_COUNT);
        String imageUri = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        String emotionSummary = getIntent().getStringExtra(EXTRA_EMOTION_SUMMARY);
        String triggerEvent = getIntent().getStringExtra(EXTRA_TRIGGER_EVENT);
        String messageToPartner = getIntent().getStringExtra(EXTRA_MESSAGE_TO_PARTNER);
        ArrayList<String> imageUris = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URIS);

        if (imageUris == null) {
            imageUris = new ArrayList<>();
        }
        if (imageUris.isEmpty() && !safeTrim(imageUri).isEmpty()) {
            imageUris.add(imageUri);
        }

        bindDiaryDetail(
                date,
                author,
                mood,
                content,
                imageCount,
                imageUris,
                emotionSummary,
                triggerEvent,
                messageToPartner
        );
    }

    private void bindDeleteState() {
        if (safeTrim(diaryId).isEmpty()) {
            tvDelete.setVisibility(View.GONE);
            return;
        }
        tvDelete.setVisibility(View.VISIBLE);
        tvDelete.setOnClickListener(v -> showDeleteDialog());
    }

    private void bindDiaryDetail(String date, String author, String mood, String content,
                                 String imageCount, List<String> imageUris,
                                 String emotionSummary, String triggerEvent,
                                 String messageToPartner) {
        tvDate.setText(nonEmpty(date, "2026.04.18"));
        tvAuthor.setText(nonEmpty(author, "我"));
        tvMood.setText(nonEmpty(mood, "心情：未记录"));
        tvContent.setText(nonEmpty(content, "今天的记录还没写完整。"));
        tvImageCount.setText(nonEmpty(imageCount, imageUris.size() + " 张图片"));

        if (imageUris.isEmpty()) {
            layoutPhotoPanel.setVisibility(View.GONE);
            tvPhotoPlaceholder.setVisibility(View.VISIBLE);
        } else {
            layoutPhotoPanel.setVisibility(View.VISIBLE);
            tvPhotoPlaceholder.setVisibility(View.GONE);
            bindImageStrip(layoutDiaryPhotos, imageUris, 180, 180);
        }

        bindDiaryInsight(emotionSummary, triggerEvent, messageToPartner);
    }

    private void bindDiaryInsight(String emotionSummary, String triggerEvent, String messageToPartner) {
        String safeEmotionSummary = safeTrim(emotionSummary);
        String safeTriggerEvent = safeTrim(triggerEvent);
        String safeMessageToPartner = safeTrim(messageToPartner);
        boolean hasInsight = !safeEmotionSummary.isEmpty()
                || !safeTriggerEvent.isEmpty()
                || !safeMessageToPartner.isEmpty();
        layoutDiaryInsight.setVisibility(hasInsight ? View.VISIBLE : View.GONE);
        if (!hasInsight) {
            return;
        }

        tvEmotionSummary.setText(nonEmpty(
                safeEmotionSummary,
                "今天的情绪已经被记录下来了，只是还需要更多细节来总结。"
        ));
        tvTriggerEvent.setText(nonEmpty(
                safeTriggerEvent,
                "这篇日记里还没有足够明确的触发事件。"
        ));
        tvMessageToPartner.setText(nonEmpty(
                safeMessageToPartner,
                "我还有些感受，想找个合适的时候认真和你聊聊。"
        ));
    }

    private void bindImageStrip(LinearLayout container, List<String> imageUris,
                                int widthDp, int heightDp) {
        container.removeAllViews();

        for (int i = 0; i < imageUris.size(); i++) {
            String imageRef = imageUris.get(i);
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(widthDp), dp(heightDp)
            );
            if (i > 0) {
                params.setMarginStart(dp(10));
            }
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_input_field);
            imageView.setClipToOutline(true);
            AvatarImageLoader.loadContent(imageView, imageRef);
            imageView.setOnClickListener(v -> openImagePreview(imageRef));
            container.addView(imageView);
        }
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除日记")
                .setMessage("确定要删除这篇日记吗？删除后无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> deleteDiary())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteDiary() {
        if (safeTrim(diaryId).isEmpty()) {
            return;
        }

        DiaryApi.Impl.deleteDiary(diaryId, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                finish();
            }

            @Override
            public void onError(ApiError error) {
                new AlertDialog.Builder(DiaryDetailActivity.this)
                        .setTitle("删除失败")
                        .setMessage(error.message)
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }

    private void openImagePreview(String imageRef) {
        if (safeTrim(imageRef).isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, ChatImagePreviewActivity.class);
        intent.putExtra(ChatImagePreviewActivity.EXTRA_IMAGE_URI, imageRef);
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nonEmpty(String value, String fallback) {
        String trimmed = safeTrim(value);
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
