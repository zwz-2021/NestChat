package com.example.nestchat;

import android.net.Uri;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_diary_detail);

        applyWindowInsets();
        initViews();
        bindEvents();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        TextView tvDate = findViewById(R.id.tvDiaryDate);
        TextView tvAuthor = findViewById(R.id.tvDiaryAuthor);
        TextView tvMood = findViewById(R.id.tvDiaryMood);
        TextView tvContent = findViewById(R.id.tvDiaryContent);
        TextView tvImageCount = findViewById(R.id.tvImageCount);
        View layoutPhotoPanel = findViewById(R.id.layoutPhotoPanel);
        LinearLayout layoutDiaryPhotos = findViewById(R.id.layoutDiaryPhotos);
        TextView tvPhotoPlaceholder = findViewById(R.id.tvPhotoPlaceholder);

        String date = getIntent().getStringExtra(EXTRA_DATE);
        String author = getIntent().getStringExtra(EXTRA_AUTHOR);
        String mood = getIntent().getStringExtra(EXTRA_MOOD);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);
        String imageCount = getIntent().getStringExtra(EXTRA_IMAGE_COUNT);
        String imageUri = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        ArrayList<String> imageUris = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URIS);

        if (imageUris == null) {
            imageUris = new ArrayList<>();
        }
        if (imageUris.isEmpty() && imageUri != null && !imageUri.isEmpty()) {
            imageUris.add(imageUri);
        }

        tvDate.setText(date == null ? "2026.04.18" : date);
        tvAuthor.setText(author == null ? "我" : author);
        tvMood.setText(mood == null ? "开心 🙂" : mood);
        tvContent.setText(content == null ? "今天我们聊了很久，感觉轻松了很多。" : content);
        tvImageCount.setText(imageCount == null ? "1 张图片" : imageCount);

        if (imageUris.isEmpty()) {
            layoutPhotoPanel.setVisibility(View.GONE);
            tvPhotoPlaceholder.setVisibility(View.VISIBLE);
        } else {
            layoutPhotoPanel.setVisibility(View.VISIBLE);
            tvPhotoPlaceholder.setVisibility(View.GONE);
            bindImageStrip(layoutDiaryPhotos, imageUris, 180, 180);
        }
    }

    private void bindImageStrip(LinearLayout container, List<String> imageUris,
                                int widthDp, int heightDp) {
        container.removeAllViews();

        for (int i = 0; i < imageUris.size(); i++) {
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
            imageView.setImageURI(Uri.parse(imageUris.get(i)));
            container.addView(imageView);
        }
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
