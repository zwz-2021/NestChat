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

import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private EditText etNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

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
        ivAvatar = findViewById(R.id.ivAvatar);
        etNickname = findViewById(R.id.etNickname);
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);
        MaterialButton btnSave = findViewById(R.id.btnSaveProfile);

        ivBack.setOnClickListener(v -> finish());
        ivAvatar.setOnClickListener(v ->
                Toast.makeText(this, "更换头像（演示）", Toast.LENGTH_SHORT).show()
        );
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();

        if (nickname.isEmpty()) {
            etNickname.setError("请输入昵称");
            etNickname.requestFocus();
            return;
        }

        Toast.makeText(this, "个人信息已保存（演示）", Toast.LENGTH_SHORT).show();
        finish();
    }
}
