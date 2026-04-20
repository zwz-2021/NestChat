package com.example.nestchat;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nestchat.util.AvatarImageLoader;

public class ChatImagePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_image_preview);

        ImageView ivPreview = findViewById(R.id.ivPreviewImage);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ivPreview.setOnClickListener(v -> finish());

        String imageUri = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (imageUri != null && !imageUri.isEmpty()) {
            AvatarImageLoader.loadContent(ivPreview, imageUri);
        }
    }
}
