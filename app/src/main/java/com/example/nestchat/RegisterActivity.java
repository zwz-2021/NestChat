package com.example.nestchat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
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
import com.google.android.material.button.MaterialButton;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etAccount;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private EditText etCaptcha;
    private ImageView ivCaptcha;
    private ImageButton btnTogglePassword;
    private ImageButton btnToggleConfirmPassword;
    private TextView tvRefreshCaptcha;
    private TextView tvGoLogin;
    private MaterialButton btnRegister;

    private String currentCaptchaId = "";
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        applyWindowInsets();
        initViews();
        initBottomLoginText();
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
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etCaptcha = findViewById(R.id.etCaptcha);
        ivCaptcha = findViewById(R.id.ivCaptcha);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        tvRefreshCaptcha = findViewById(R.id.tvRefreshCaptcha);
        tvGoLogin = findViewById(R.id.tvGoLogin);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void initBottomLoginText() {
        String fullText = "已有账号？立即登录";
        String actionText = "立即登录";
        SpannableString spannableString = new SpannableString(fullText);
        int start = fullText.indexOf(actionText);
        int end = start + actionText.length();
        spannableString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_primary_dark)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvGoLogin.setText(spannableString);
    }

    private void bindEvents() {
        btnTogglePassword.setOnClickListener(view ->
                togglePasswordVisibility(etPassword, btnTogglePassword, true));
        btnToggleConfirmPassword.setOnClickListener(view ->
                togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, false));

        ivCaptcha.setOnClickListener(view -> refreshCaptcha());
        tvRefreshCaptcha.setOnClickListener(view -> refreshCaptcha());

        tvGoLogin.setOnClickListener(view -> finish());
        btnRegister.setOnClickListener(view -> handleRegister());
    }

    private void togglePasswordVisibility(EditText editText, ImageButton toggleButton, boolean isMainPassword) {
        if (isMainPassword) {
            isPasswordVisible = !isPasswordVisible;
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        }
        boolean visible = isMainPassword ? isPasswordVisible : isConfirmPasswordVisible;
        int selectionStart = editText.getSelectionStart();
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off);
        }
        editText.setSelection(Math.max(selectionStart, 0));
    }

    private void refreshCaptcha() {
        AuthApi.Impl.getRegisterCaptcha(new ApiCallback<AuthApi.CaptchaResponse>() {
            @Override
            public void onSuccess(AuthApi.CaptchaResponse data) {
                if (data != null) {
                    currentCaptchaId = data.captchaId;
                    showBase64Image(data.imageBase64);
                    etCaptcha.setText("");
                }
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(RegisterActivity.this, "获取验证码失败: " + error.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRegister() {
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String inputCaptcha = etCaptcha.getText().toString().trim();

        if (account.isEmpty()) { etAccount.setError("请输入账号"); etAccount.requestFocus(); return; }
        if (password.isEmpty()) { etPassword.setError("请输入密码"); etPassword.requestFocus(); return; }
        if (confirmPassword.isEmpty()) { etConfirmPassword.setError("请再次输入密码"); etConfirmPassword.requestFocus(); return; }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }
        if (inputCaptcha.isEmpty()) { etCaptcha.setError("请输入验证码"); etCaptcha.requestFocus(); return; }

        btnRegister.setEnabled(false);
        btnRegister.setText("注册中...");

        AuthApi.RegisterRequest req = new AuthApi.RegisterRequest();
        req.account = account;
        req.password = password;
        req.confirmPassword = confirmPassword;
        req.captchaId = currentCaptchaId;
        req.captchaCode = inputCaptcha;

        AuthApi.Impl.register(req, new ApiCallback<AuthApi.SimpleResponse>() {
            @Override
            public void onSuccess(AuthApi.SimpleResponse data) {
                Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(RegisterActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnRegister.setEnabled(true);
                btnRegister.setText("注册");
                refreshCaptcha();
            }
        });
    }

    private void showBase64Image(String base64) {
        try {
            String pure = base64;
            if (pure.contains(",")) pure = pure.substring(pure.indexOf(",") + 1);
            byte[] bytes = Base64.decode(pure, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            ivCaptcha.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode captcha image", e);
        }
    }
}
