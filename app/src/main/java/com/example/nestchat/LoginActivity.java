package com.example.nestchat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.AuthApi;
import com.example.nestchat.api.TokenManager;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etAccount;
    private EditText etPassword;
    private EditText etCaptcha;
    private ImageView ivCaptcha;
    private ImageButton btnTogglePassword;
    private CheckBox cbRememberMe;
    private TextView tvRefreshCaptcha;
    private TextView tvForgotPassword;
    private TextView tvRegister;
    private MaterialButton btnLogin;

    private String currentCaptchaId = "";
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 TokenManager
        TokenManager.init(this);

        // 已登录则直接跳转
        if (TokenManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        applyWindowInsets();
        initViews();
        initRegisterText();
        bindEvents();
        refreshCaptcha();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        etAccount = findViewById(R.id.etAccount);
        etPassword = findViewById(R.id.etPassword);
        etCaptcha = findViewById(R.id.etCaptcha);
        ivCaptcha = findViewById(R.id.ivCaptcha);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvRefreshCaptcha = findViewById(R.id.tvRefreshCaptcha);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void initRegisterText() {
        String fullText = "还没有账号？立即注册";
        String actionText = "立即注册";
        SpannableString spannableString = new SpannableString(fullText);
        int start = fullText.indexOf(actionText);
        int end = start + actionText.length();
        spannableString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_primary_dark)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvRegister.setText(spannableString);
    }

    private void bindEvents() {
        btnTogglePassword.setOnClickListener(view -> togglePasswordVisibility());
        ivCaptcha.setOnClickListener(view -> refreshCaptcha());
        tvRefreshCaptcha.setOnClickListener(view -> refreshCaptcha());

        tvForgotPassword.setOnClickListener(view ->
                startActivity(new Intent(this, ForgetPasswordActivity.class)));

        tvRegister.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));

        btnLogin.setOnClickListener(view -> handleLogin());
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        int selectionStart = etPassword.getSelectionStart();
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        }
        etPassword.setSelection(Math.max(selectionStart, 0));
    }

    private void handleLogin() {
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String inputCaptcha = etCaptcha.getText().toString().trim();

        if (account.isEmpty()) {
            etAccount.setError("请输入账号");
            etAccount.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("请输入密码");
            etPassword.requestFocus();
            return;
        }
        if (inputCaptcha.isEmpty()) {
            etCaptcha.setError("请输入验证码");
            etCaptcha.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");

        AuthApi.LoginRequest req = new AuthApi.LoginRequest();
        req.account = account;
        req.password = password;
        req.captchaId = currentCaptchaId;
        req.captchaCode = inputCaptcha;
        req.rememberMe = cbRememberMe.isChecked();

        AuthApi.Impl.login(req, new ApiCallback<AuthApi.LoginResponse>() {
            @Override
            public void onSuccess(AuthApi.LoginResponse data) {
                Log.i(TAG, "Login success, userId=" + (data != null && data.user != null ? data.user.userId : ""));
                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(ApiError error) {
                Log.w(TAG, "Login failed: " + error.code + " " + error.message);
                Toast.makeText(LoginActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnLogin.setEnabled(true);
                btnLogin.setText("登录");
                refreshCaptcha();
            }
        });
    }

    private void refreshCaptcha() {
        AuthApi.Impl.getLoginCaptcha(new ApiCallback<AuthApi.CaptchaResponse>() {
            @Override
            public void onSuccess(AuthApi.CaptchaResponse data) {
                if (data != null) {
                    currentCaptchaId = data.captchaId;
                    showBase64Image(data.imageBase64);
                    etCaptcha.setText("");
                    Log.d(TAG, "Captcha refreshed, id=" + data.captchaId);
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "Failed to get captcha: " + error.message);
                Toast.makeText(LoginActivity.this, "获取验证码失败: " + error.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showBase64Image(String base64) {
        try {
            String pure = base64;
            if (pure.contains(",")) {
                pure = pure.substring(pure.indexOf(",") + 1);
            }
            byte[] bytes = Base64.decode(pure, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            ivCaptcha.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode captcha image", e);
        }
    }
}
