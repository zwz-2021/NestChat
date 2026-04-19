package com.example.nestchat;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class ForgetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgetPasswordActivity";
    private static final long CODE_COUNTDOWN_MILLIS = 60_000L;

    private EditText etAccount;
    private EditText etVerifyCode;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private ImageButton ivToggleNewPassword;
    private ImageButton ivToggleConfirmPassword;
    private MaterialButton btnGetCode;
    private MaterialButton btnResetPassword;
    private TextView tvGoLogin;

    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);

        applyWindowInsets();
        initViews();
        initGoLoginText();
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
        etAccount = findViewById(R.id.etAccount);
        etVerifyCode = findViewById(R.id.etVerifyCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnGetCode = findViewById(R.id.btnGetCode);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvGoLogin = findViewById(R.id.tvGoLogin);
    }

    private void initGoLoginText() {
        String fullText = "想起密码了？立即登录";
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
        btnGetCode.setOnClickListener(view -> handleGetCode());
        btnResetPassword.setOnClickListener(view -> handleResetPassword());

        ivToggleNewPassword.setOnClickListener(view ->
                togglePasswordVisibility(etNewPassword, ivToggleNewPassword, true));
        ivToggleConfirmPassword.setOnClickListener(view ->
                togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, false));

        tvGoLogin.setOnClickListener(view -> finish());
    }

    private void handleGetCode() {
        String account = etAccount.getText().toString().trim();
        if (account.isEmpty()) {
            etAccount.setError("请输入账号");
            etAccount.requestFocus();
            return;
        }

        btnGetCode.setEnabled(false);

        AuthApi.SendResetCodeRequest req = new AuthApi.SendResetCodeRequest();
        req.account = account;

        AuthApi.Impl.sendResetCode(req, new ApiCallback<AuthApi.SimpleResponse>() {
            @Override
            public void onSuccess(AuthApi.SimpleResponse data) {
                Toast.makeText(ForgetPasswordActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                startCodeCountDown();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(ForgetPasswordActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnGetCode.setEnabled(true);
            }
        });
    }

    private void startCodeCountDown() {
        countDownTimer = new CountDownTimer(CODE_COUNTDOWN_MILLIS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnGetCode.setText(millisUntilFinished / 1000L + "s 后重试");
            }

            @Override
            public void onFinish() {
                btnGetCode.setEnabled(true);
                btnGetCode.setText("获取验证码");
            }
        };
        countDownTimer.start();
    }

    private void togglePasswordVisibility(EditText editText, ImageButton toggleButton, boolean isNewPasswordField) {
        if (isNewPasswordField) {
            isNewPasswordVisible = !isNewPasswordVisible;
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        }
        boolean visible = isNewPasswordField ? isNewPasswordVisible : isConfirmPasswordVisible;
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off);
        }
        editText.setSelection(editText.getText().length());
    }

    private void handleResetPassword() {
        String account = etAccount.getText().toString().trim();
        String verifyCode = etVerifyCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (account.isEmpty()) { etAccount.setError("请输入账号"); etAccount.requestFocus(); return; }
        if (verifyCode.isEmpty()) { etVerifyCode.setError("请输入验证码"); etVerifyCode.requestFocus(); return; }
        if (newPassword.isEmpty()) { etNewPassword.setError("请输入新密码"); etNewPassword.requestFocus(); return; }
        if (confirmPassword.isEmpty()) { etConfirmPassword.setError("请再次输入新密码"); etConfirmPassword.requestFocus(); return; }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            etNewPassword.setError("密码长度需为 6-20 位"); etNewPassword.requestFocus(); return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的新密码不一致");
            Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("重置中...");

        AuthApi.ResetPasswordRequest req = new AuthApi.ResetPasswordRequest();
        req.account = account;
        req.verifyCode = verifyCode;
        req.newPassword = newPassword;
        req.confirmPassword = confirmPassword;

        AuthApi.Impl.resetPassword(req, new ApiCallback<AuthApi.SimpleResponse>() {
            @Override
            public void onSuccess(AuthApi.SimpleResponse data) {
                Toast.makeText(ForgetPasswordActivity.this, "密码重置成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(ForgetPasswordActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("重置密码");
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        super.onDestroy();
    }
}
