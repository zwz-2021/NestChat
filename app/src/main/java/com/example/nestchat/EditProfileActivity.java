package com.example.nestchat;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.UserApi;
import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private EditText etNickname;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        applyWindowInsets();
        initViews();
        bindEvents();
        loadProfile();
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
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());
        ivAvatar.setOnClickListener(v ->
                Toast.makeText(this, "更换头像（暂未实现）", Toast.LENGTH_SHORT).show()
        );
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        UserApi.Impl.getMineProfile(new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                if (data == null) return;
                if (data.nickname != null) etNickname.setText(data.nickname);
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(EditProfileActivity.this, "加载资料失败: " + error.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
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

        UserApi.Impl.updateProfile(req, new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                Toast.makeText(EditProfileActivity.this, "个人信息已保存", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(EditProfileActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("保存");
            }
        });
    }
}
