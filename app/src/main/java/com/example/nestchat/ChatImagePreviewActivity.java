package com.example.nestchat;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
            ivPreview.setImageURI(Uri.parse(imageUri));
        }
    }
}
