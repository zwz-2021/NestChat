package com.example.nestchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WriteDiaryActivity extends AppCompatActivity {

    private static final String STATE_MOOD_LABEL = "state_mood_label";
    private static final String STATE_MOOD_EMOJI = "state_mood_emoji";
    private static final String STATE_IMAGE_URIS = "state_image_uris";
    private static final String STATE_CONTENT = "state_content";
    private static final String DEFAULT_AUTHOR = "我";

    private EditText etDiaryContent;
    private TextView tvCurrentDate;
    private TextView tvCurrentAuthor;
    private TextView tvSelectedImageCount;
    private TextView tvPreviewImageCount;
    private TextView tvPhotoPlaceholder;
    private TextView btnMoodHappy;
    private TextView btnMoodSad;
    private TextView btnMoodTired;
    private LinearLayout layoutSelectedPhotos;
    private MaterialButton btnTogglePhoto;

    private String selectedMoodLabel = "开心";
    private String selectedMoodEmoji = "🙂";
    private String currentDate;
    private final ArrayList<String> selectedImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String[]> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }

                selectedImageUris.clear();
                for (Uri uri : uris) {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    selectedImageUris.add(uri.toString());
                }
                updateImageViews();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_write_diary);

        applyWindowInsets();
        initViews();
        restoreState(savedInstanceState);
        bindEvents();
        renderDraftState();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentAuthor = findViewById(R.id.tvCurrentAuthor);
        etDiaryContent = findViewById(R.id.etDiaryContent);
        tvSelectedImageCount = findViewById(R.id.tvSelectedImageCount);
        tvPreviewImageCount = findViewById(R.id.tvPreviewImageCount);
        tvPhotoPlaceholder = findViewById(R.id.tvPhotoPlaceholder);
        btnMoodHappy = findViewById(R.id.btnMoodHappy);
        btnMoodSad = findViewById(R.id.btnMoodSad);
        btnMoodTired = findViewById(R.id.btnMoodTired);
        layoutSelectedPhotos = findViewById(R.id.layoutSelectedPhotos);
        btnTogglePhoto = findViewById(R.id.btnTogglePhoto);
    }

    private void restoreState(Bundle savedInstanceState) {
        currentDate = getTodayDate();

        if (savedInstanceState == null) {
            return;
        }

        currentDate = savedInstanceState.getString(DiaryDetailActivity.EXTRA_DATE, currentDate);
        selectedMoodLabel = savedInstanceState.getString(STATE_MOOD_LABEL, selectedMoodLabel);
        selectedMoodEmoji = savedInstanceState.getString(STATE_MOOD_EMOJI, selectedMoodEmoji);
        etDiaryContent.setText(savedInstanceState.getString(STATE_CONTENT, ""));

        ArrayList<String> savedImageUris = savedInstanceState.getStringArrayList(STATE_IMAGE_URIS);
        if (savedImageUris != null) {
            selectedImageUris.clear();
            selectedImageUris.addAll(savedImageUris);
        }
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);
        MaterialButton btnSave = findViewById(R.id.btnSaveDiary);

        ivBack.setOnClickListener(v -> finish());
        btnMoodHappy.setOnClickListener(v -> updateMoodSelection("开心", "🙂"));
        btnMoodSad.setOnClickListener(v -> updateMoodSelection("难过", "😢"));
        btnMoodTired.setOnClickListener(v -> updateMoodSelection("疲惫", "😣"));
        btnTogglePhoto.setOnClickListener(v -> handlePhotoAction());
        btnSave.setOnClickListener(v -> saveDiary());
    }

    private void renderDraftState() {
        tvCurrentDate.setText(currentDate);
        tvCurrentAuthor.setText(DEFAULT_AUTHOR);
        updateMoodSelection(selectedMoodLabel, selectedMoodEmoji);
        updateImageViews();
    }

    private void updateMoodSelection(String moodLabel, String moodEmoji) {
        selectedMoodLabel = moodLabel;
        selectedMoodEmoji = moodEmoji;

        btnMoodHappy.setSelected("开心".equals(moodLabel));
        btnMoodSad.setSelected("难过".equals(moodLabel));
        btnMoodTired.setSelected("疲惫".equals(moodLabel));
    }

    private void handlePhotoAction() {
        if (selectedImageUris.isEmpty()) {
            pickImageLauncher.launch(new String[]{"image/*"});
        } else {
            selectedImageUris.clear();
            updateImageViews();
        }
    }

    private void updateImageViews() {
        String imageCountText = selectedImageUris.size() + " 张图片";
        tvSelectedImageCount.setText(imageCountText);
        tvPreviewImageCount.setText(imageCountText);
        tvPhotoPlaceholder.setVisibility(selectedImageUris.isEmpty() ? View.VISIBLE : View.GONE);

        if (selectedImageUris.isEmpty()) {
            btnTogglePhoto.setText("添加图片");
        } else {
            btnTogglePhoto.setText("清空图片");
        }

        bindImageStrip(layoutSelectedPhotos, selectedImageUris, 112, 112);
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

    private void saveDiary() {
        String content = etDiaryContent.getText().toString().trim();

        if (content.isEmpty()) {
            etDiaryContent.setError("请输入内容");
            etDiaryContent.requestFocus();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(DiaryDetailActivity.EXTRA_DATE, currentDate);
        resultIntent.putExtra(DiaryDetailActivity.EXTRA_AUTHOR, DEFAULT_AUTHOR);
        resultIntent.putExtra(DiaryDetailActivity.EXTRA_MOOD, selectedMoodLabel + " " + selectedMoodEmoji);
        resultIntent.putExtra(DiaryDetailActivity.EXTRA_CONTENT, content);
        resultIntent.putExtra(DiaryDetailActivity.EXTRA_IMAGE_COUNT, selectedImageUris.size() + " 张图片");
        resultIntent.putExtra(DiaryDetailActivity.EXTRA_IMAGE_URI,
                selectedImageUris.isEmpty() ? "" : selectedImageUris.get(0));
        resultIntent.putStringArrayListExtra(DiaryDetailActivity.EXTRA_IMAGE_URIS,
                new ArrayList<>(selectedImageUris));
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "日记已保存（演示）", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date());
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DiaryDetailActivity.EXTRA_DATE, currentDate);
        outState.putString(STATE_MOOD_LABEL, selectedMoodLabel);
        outState.putString(STATE_MOOD_EMOJI, selectedMoodEmoji);
        outState.putString(STATE_CONTENT, etDiaryContent.getText().toString());
        outState.putStringArrayList(STATE_IMAGE_URIS, new ArrayList<>(selectedImageUris));
    }
}
