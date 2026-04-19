package com.example.nestchat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.AuthApi;

public class AccountSecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_security);

        applyWindowInsets();
        bindEvents();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);
        findViewById(R.id.itemChangePassword).setOnClickListener(v ->
                startActivity(new Intent(this, ForgetPasswordActivity.class))
        );
        findViewById(R.id.itemLogout).setOnClickListener(v -> handleLogout());
        ivBack.setOnClickListener(v -> finish());
    }

    private void handleLogout() {
        AuthApi.Impl.logout(new ApiCallback<AuthApi.SimpleResponse>() {
            @Override
            public void onSuccess(AuthApi.SimpleResponse data) {
                goToLogin();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(AccountSecurityActivity.this, error.message, Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
