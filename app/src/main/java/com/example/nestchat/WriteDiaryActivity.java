package com.example.nestchat;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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

import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.DiaryApi;
import com.example.nestchat.api.FileApi;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WriteDiaryActivity extends AppCompatActivity {

    private static final String STATE_MOOD_LABEL = "state_mood_label";
    private static final String STATE_MOOD_CODE = "state_mood_code";
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
    private TextView btnMoodCalm;
    private TextView btnMoodLove;
    private TextView btnMoodSad;
    private TextView btnMoodWronged;
    private TextView btnMoodAngry;
    private TextView btnMoodTired;
    private TextView btnMoodAnxious;
    private LinearLayout layoutSelectedPhotos;
    private MaterialButton btnTogglePhoto;
    private MaterialButton btnSave;

    private String selectedMoodLabel = "开心";
    private String selectedMoodCode = "happy";
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
        btnMoodCalm = findViewById(R.id.btnMoodCalm);
        btnMoodLove = findViewById(R.id.btnMoodLove);
        btnMoodSad = findViewById(R.id.btnMoodSad);
        btnMoodWronged = findViewById(R.id.btnMoodWronged);
        btnMoodAngry = findViewById(R.id.btnMoodAngry);
        btnMoodTired = findViewById(R.id.btnMoodTired);
        btnMoodAnxious = findViewById(R.id.btnMoodAnxious);
        layoutSelectedPhotos = findViewById(R.id.layoutSelectedPhotos);
        btnTogglePhoto = findViewById(R.id.btnTogglePhoto);
        btnSave = findViewById(R.id.btnSaveDiary);
    }

    private void restoreState(Bundle savedInstanceState) {
        currentDate = getTodayDate();

        if (savedInstanceState == null) {
            return;
        }

        currentDate = savedInstanceState.getString(DiaryDetailActivity.EXTRA_DATE, currentDate);
        selectedMoodLabel = savedInstanceState.getString(STATE_MOOD_LABEL, selectedMoodLabel);
        selectedMoodCode = savedInstanceState.getString(STATE_MOOD_CODE, selectedMoodCode);
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

        ivBack.setOnClickListener(v -> finish());
        btnMoodHappy.setOnClickListener(v -> updateMoodSelection("开心", "happy", "🙂"));
        btnMoodCalm.setOnClickListener(v -> updateMoodSelection("平静", "calm", "😌"));
        btnMoodLove.setOnClickListener(v -> updateMoodSelection("心动", "love", "🥰"));
        btnMoodSad.setOnClickListener(v -> updateMoodSelection("难过", "sad", "😢"));
        btnMoodWronged.setOnClickListener(v -> updateMoodSelection("委屈", "wronged", "🥺"));
        btnMoodAngry.setOnClickListener(v -> updateMoodSelection("生气", "angry", "😤"));
        btnMoodTired.setOnClickListener(v -> updateMoodSelection("疲惫", "tired", "😣"));
        btnMoodAnxious.setOnClickListener(v -> updateMoodSelection("焦虑", "anxious", "😰"));
        btnTogglePhoto.setOnClickListener(v -> handlePhotoAction());
        btnSave.setOnClickListener(v -> saveDiary());
    }

    private void renderDraftState() {
        tvCurrentDate.setText(currentDate);
        tvCurrentAuthor.setText(DEFAULT_AUTHOR);
        updateMoodSelection(selectedMoodLabel, selectedMoodCode, selectedMoodEmoji);
        updateImageViews();
    }

    private void updateMoodSelection(String moodLabel, String moodCode, String moodEmoji) {
        selectedMoodLabel = moodLabel;
        selectedMoodCode = moodCode;
        selectedMoodEmoji = moodEmoji;

        btnMoodHappy.setSelected("happy".equals(moodCode));
        btnMoodCalm.setSelected("calm".equals(moodCode));
        btnMoodLove.setSelected("love".equals(moodCode));
        btnMoodSad.setSelected("sad".equals(moodCode));
        btnMoodWronged.setSelected("wronged".equals(moodCode));
        btnMoodAngry.setSelected("angry".equals(moodCode));
        btnMoodTired.setSelected("tired".equals(moodCode));
        btnMoodAnxious.setSelected("anxious".equals(moodCode));
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

        btnSave.setEnabled(false);
        btnSave.setText("保存中...");

        if (selectedImageUris.isEmpty()) {
            createDiary(content, new ArrayList<>());
        } else {
            uploadImagesAndCreateDiary(content);
        }
    }

    private void uploadImagesAndCreateDiary(String content) {
        List<String> fileIds = new ArrayList<>();
        uploadNextImage(content, fileIds, 0);
    }

    private void uploadNextImage(String content, List<String> fileIds, int index) {
        if (index >= selectedImageUris.size()) {
            createDiary(content, fileIds);
            return;
        }

        String uriString = selectedImageUris.get(index);
        File tempFile = copyUriToTempFile(Uri.parse(uriString));
        if (tempFile == null) {
            Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            btnSave.setText("保存日记");
            return;
        }

        FileApi.UploadFileRequest req = new FileApi.UploadFileRequest();
        req.localPath = tempFile.getAbsolutePath();
        req.mimeType = "image/jpeg";
        req.bizType = "diary";

        FileApi.Impl.uploadImage(req, new ApiCallback<FileApi.UploadFileResponse>() {
            @Override
            public void onSuccess(FileApi.UploadFileResponse data) {
                tempFile.delete();
                if (data != null && data.fileId != null) {
                    fileIds.add(data.fileId);
                }
                uploadNextImage(content, fileIds, index + 1);
            }

            @Override
            public void onError(ApiError error) {
                tempFile.delete();
                Toast.makeText(WriteDiaryActivity.this, "图片上传失败: " + error.message, Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("保存日记");
            }
        });
    }

    private void createDiary(String content, List<String> imageFileIds) {
        DiaryApi.CreateDiaryRequest req = new DiaryApi.CreateDiaryRequest();
        req.authorType = "self";
        req.date = currentDate.replace(".", "-");
        req.moodCode = selectedMoodCode;
        req.moodText = selectedMoodLabel;
        req.content = content;
        req.imageFileIds = imageFileIds;

        DiaryApi.Impl.createDiary(req, new ApiCallback<DiaryApi.DiaryDetailResponse>() {
            @Override
            public void onSuccess(DiaryApi.DiaryDetailResponse data) {
                Toast.makeText(WriteDiaryActivity.this, "日记已保存", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(WriteDiaryActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("保存日记");
            }
        });
    }

    private File copyUriToTempFile(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            File temp = new File(getCacheDir(), buildTempFileName(uri));
            FileOutputStream out = new FileOutputStream(temp);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return temp;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildTempFileName(Uri uri) {
        String displayName = queryDisplayName(uri);
        if (displayName == null || displayName.trim().isEmpty()) {
            return "upload_" + System.currentTimeMillis() + ".jpg";
        }
        return "upload_" + System.currentTimeMillis() + "_" + displayName;
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex >= 0) {
                    return cursor.getString(columnIndex);
                }
            }
        } catch (Exception ignored) {
            // Ignore and fall back to default temp filename.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
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
        outState.putString(STATE_MOOD_CODE, selectedMoodCode);
        outState.putString(STATE_MOOD_EMOJI, selectedMoodEmoji);
        outState.putString(STATE_CONTENT, etDiaryContent.getText().toString());
        outState.putStringArrayList(STATE_IMAGE_URIS, new ArrayList<>(selectedImageUris));
    }
}
