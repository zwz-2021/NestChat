package com.example.nestchat;

import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.nestchat.api.FileApi;
import com.example.nestchat.api.TokenManager;
import com.example.nestchat.api.UserApi;
import com.example.nestchat.util.AvatarImageLoader;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

    private static final int AVATAR_PLACEHOLDER_PADDING_DP = 18;

    private ImageView ivAvatar;
    private EditText etNickname;
    private MaterialButton btnSave;
    private ActivityResultLauncher<String> avatarPicker;

    private String currentAvatarUrl = "";
    private String pendingAvatarUrl = "";
    private boolean avatarUploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        initAvatarPicker();
        applyWindowInsets();
        initViews();
        bindEvents();
        loadProfile();
    }

    private void initAvatarPicker() {
        avatarPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleAvatarSelected);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        etNickname = findViewById(R.id.etNickname);
        btnSave = findViewById(R.id.btnSaveProfile);
        AvatarImageLoader.load(ivAvatar, "", AVATAR_PLACEHOLDER_PADDING_DP);
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());
        ivAvatar.setOnClickListener(v -> avatarPicker.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        UserApi.Impl.getMineProfile(new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                if (data == null || isFinishing()) {
                    return;
                }
                if (data.nickname != null) {
                    etNickname.setText(data.nickname);
                }
                currentAvatarUrl = safeTrim(data.avatarUrl);
                pendingAvatarUrl = currentAvatarUrl;
                AvatarImageLoader.load(ivAvatar, currentAvatarUrl, AVATAR_PLACEHOLDER_PADDING_DP);
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(EditProfileActivity.this, "加载资料失败: " + error.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleAvatarSelected(Uri uri) {
        if (uri == null) {
            return;
        }
        AvatarImageLoader.showLocal(ivAvatar, uri);
        avatarUploading = true;
        ivAvatar.setEnabled(false);
        btnSave.setEnabled(false);
        btnSave.setText("头像上传中...");

        new Thread(() -> {
            try {
                File tempFile = copyUriToTempFile(uri);
                String mimeType = resolveMimeType(uri);
                FileApi.UploadFileRequest req = new FileApi.UploadFileRequest();
                req.localPath = tempFile.getAbsolutePath();
                req.fileSize = tempFile.length();
                req.mimeType = mimeType;
                req.bizType = "avatar";

                runOnUiThread(() -> FileApi.Impl.uploadImage(req, new ApiCallback<FileApi.UploadFileResponse>() {
                    @Override
                    public void onSuccess(FileApi.UploadFileResponse data) {
                        avatarUploading = false;
                        ivAvatar.setEnabled(true);
                        btnSave.setEnabled(true);
                        btnSave.setText("保存个人信息");
                        pendingAvatarUrl = data != null ? safeTrim(data.fileUrl) : "";
                        if (pendingAvatarUrl.isEmpty()) {
                            pendingAvatarUrl = currentAvatarUrl;
                            AvatarImageLoader.load(ivAvatar, currentAvatarUrl, AVATAR_PLACEHOLDER_PADDING_DP);
                            Toast.makeText(EditProfileActivity.this, "头像上传失败", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(EditProfileActivity.this, "头像已上传，保存后生效", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(ApiError error) {
                        avatarUploading = false;
                        ivAvatar.setEnabled(true);
                        btnSave.setEnabled(true);
                        btnSave.setText("保存个人信息");
                        pendingAvatarUrl = currentAvatarUrl;
                        AvatarImageLoader.load(ivAvatar, currentAvatarUrl, AVATAR_PLACEHOLDER_PADDING_DP);
                        Toast.makeText(EditProfileActivity.this, "头像上传失败: " + error.message, Toast.LENGTH_SHORT).show();
                    }
                }));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    avatarUploading = false;
                    ivAvatar.setEnabled(true);
                    btnSave.setEnabled(true);
                    btnSave.setText("保存个人信息");
                    pendingAvatarUrl = currentAvatarUrl;
                    AvatarImageLoader.load(ivAvatar, currentAvatarUrl, AVATAR_PLACEHOLDER_PADDING_DP);
                    Toast.makeText(EditProfileActivity.this, "读取图片失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveProfile() {
        if (avatarUploading) {
            Toast.makeText(this, "头像仍在上传，请稍后", Toast.LENGTH_SHORT).show();
            return;
        }

        String nickname = etNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            etNickname.setError("请输入昵称");
            etNickname.requestFocus();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("保存中...");

        UserApi.UpdateProfileRequest req = new UserApi.UpdateProfileRequest();
        req.nickname = nickname;
        req.avatarUrl = safeTrim(pendingAvatarUrl);

        UserApi.Impl.updateProfile(req, new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                String savedNickname = data != null ? safeTrim(data.nickname) : nickname;
                String savedAvatarUrl = data != null ? safeTrim(data.avatarUrl) : safeTrim(pendingAvatarUrl);
                TokenManager.updateProfile(savedNickname, savedAvatarUrl);
                Toast.makeText(EditProfileActivity.this, "个人信息已保存", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(EditProfileActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("保存个人信息");
            }
        });
    }

    private File copyUriToTempFile(Uri uri) throws IOException {
        String extension = resolveExtension(uri);
        File tempFile = File.createTempFile("avatar_", extension, getCacheDir());
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream == null) {
                throw new IOException("Input stream is null");
            }
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
        return tempFile;
    }

    private String resolveMimeType(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        return safeTrim(mimeType).isEmpty() ? "image/jpeg" : mimeType;
    }

    private String resolveExtension(Uri uri) {
        String fileName = queryDisplayName(uri);
        if (!safeTrim(fileName).isEmpty()) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                return fileName.substring(dotIndex);
            }
        }
        String mimeType = resolveMimeType(uri);
        if ("image/png".equalsIgnoreCase(mimeType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(mimeType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return "";
            }
            int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (columnIndex < 0) {
                return "";
            }
            return safeTrim(cursor.getString(columnIndex));
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
