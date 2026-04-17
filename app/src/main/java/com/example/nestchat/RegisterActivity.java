package com.example.nestchat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * 注册页，只保留当前界面实际使用到的交互与校验逻辑。
 */
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

    private String currentCaptcha = "";
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
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        tvGoLogin.setText(spannableString);
    }

    private void bindEvents() {
        btnTogglePassword.setOnClickListener(view ->
                togglePasswordVisibility(etPassword, btnTogglePassword, true)
        );

        btnToggleConfirmPassword.setOnClickListener(view ->
                togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, false)
        );

        ivCaptcha.setOnClickListener(view -> refreshCaptcha());
        tvRefreshCaptcha.setOnClickListener(view -> refreshCaptcha());

        tvGoLogin.setOnClickListener(view -> {
            Log.d(TAG, "Go to login clicked");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

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
            Log.d(TAG, "Password visibility enabled: " + (isMainPassword ? "password" : "confirm"));
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off);
            Log.d(TAG, "Password visibility disabled: " + (isMainPassword ? "password" : "confirm"));
        }

        editText.setSelection(Math.max(selectionStart, 0));
    }

    private void refreshCaptcha() {
        CaptchaUtil.CaptchaResult captchaResult = CaptchaUtil.createCaptcha(this);
        currentCaptcha = captchaResult.getCode();
        ivCaptcha.setImageBitmap(captchaResult.getBitmap());
        etCaptcha.setText("");
        Log.d(TAG, "Captcha refreshed: " + currentCaptcha);
    }

    private void handleRegister() {
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String inputCaptcha = etCaptcha.getText().toString().trim().toUpperCase(Locale.ROOT);

        Log.d(TAG, "Attempt register, account length = " + account.length());

        if (account.isEmpty()) {
            etAccount.setError("请输入手机号");
            etAccount.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("请输入密码");
            etPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("请再次输入密码");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return;
        }

        if (inputCaptcha.isEmpty()) {
            etCaptcha.setError("请输入验证码");
            etCaptcha.requestFocus();
            return;
        }

        if (!inputCaptcha.equals(currentCaptcha)) {
            etCaptcha.setError("验证码错误");
            Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show();
            etCaptcha.requestFocus();
            etCaptcha.selectAll();
            refreshCaptcha();
            return;
        }

        Log.i(TAG, "Register validation passed");
        Toast.makeText(this, "注册成功，进入下一步", Toast.LENGTH_SHORT).show();
    }
}
