package com.example.nestchat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String CAPTCHA_SOURCE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

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

    private final Random random = new Random();
    private String currentCaptcha = "";
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 初始化页面结构与交互。
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
        SpannableString spannableString = new SpannableString(fullText);
        int start = fullText.indexOf("立即注册");
        int end = fullText.length();
        spannableString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_primary_dark)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        tvRegister.setText(spannableString);
    }

    private void bindEvents() {
        btnTogglePassword.setOnClickListener(view -> togglePasswordVisibility());
        ivCaptcha.setOnClickListener(view -> refreshCaptcha());
        tvRefreshCaptcha.setOnClickListener(view -> refreshCaptcha());

        tvForgotPassword.setOnClickListener(view ->
                Toast.makeText(this, "忘记密码功能待接入", Toast.LENGTH_SHORT).show()
        );

        tvRegister.setOnClickListener(view ->
                Toast.makeText(this, "注册功能待接入", Toast.LENGTH_SHORT).show()
        );

        btnLogin.setOnClickListener(view -> handleLogin());
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        int selectionStart = etPassword.getSelectionStart();

        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            Log.d(TAG, "Password visibility enabled");
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            Log.d(TAG, "Password visibility disabled");
        }

        etPassword.setSelection(Math.max(selectionStart, 0));
    }

    private void handleLogin() {
        // 这里只做本地校验，后续你接接口时可以在这里继续扩展。
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String inputCaptcha = etCaptcha.getText().toString().trim().toUpperCase(Locale.ROOT);

        Log.d(TAG, "Attempt login, account length = " + account.length() + ", rememberMe = " + cbRememberMe.isChecked());

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

        if (inputCaptcha.isEmpty()) {
            etCaptcha.setError("请输入验证码");
            etCaptcha.requestFocus();
            return;
        }

        if (!inputCaptcha.equals(currentCaptcha)) {
            Log.w(TAG, "Captcha mismatch, expected = " + currentCaptcha + ", actual = " + inputCaptcha);
            etCaptcha.setError("验证码错误");
            Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show();
            etCaptcha.requestFocus();
            etCaptcha.selectAll();
            refreshCaptcha();
            return;
        }

        Log.i(TAG, "Login validation passed");
        Toast.makeText(this, "登录校验通过，等待后续接口接入", Toast.LENGTH_SHORT).show();
    }

    private void refreshCaptcha() {
        // 每次刷新都重新生成随机字符，并绘制成位图。
        currentCaptcha = createCaptchaText();
        ivCaptcha.setImageBitmap(createCaptchaBitmap(currentCaptcha));
        etCaptcha.setText("");
        Log.d(TAG, "Captcha refreshed: " + currentCaptcha);
    }

    private String createCaptchaText() {
        int length = random.nextBoolean() ? 4 : 5;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CAPTCHA_SOURCE.length());
            builder.append(CAPTCHA_SOURCE.charAt(index));
        }
        return builder.toString();
    }

    private Bitmap createCaptchaBitmap(String captchaText) {
        int width = dpToPx(112);
        int height = dpToPx(58);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 先画底色，再叠加噪点、干扰线和验证码字符。
        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setShader(new LinearGradient(
                0, 0, width, height,
                new int[]{
                        ContextCompat.getColor(this, R.color.captcha_bg_start),
                        ContextCompat.getColor(this, R.color.captcha_bg_end)
                },
                null,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(0f, 0f, width, height, dpToPx(14), dpToPx(14), backgroundPaint);

        drawNoiseDots(canvas, width, height);
        drawInterferenceLines(canvas, width, height);
        drawCaptchaText(canvas, captchaText, width, height);

        return bitmap;
    }

    private void drawNoiseDots(Canvas canvas, int width, int height) {
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 72; i++) {
            dotPaint.setColor(randomColor(
                    ContextCompat.getColor(this, R.color.captcha_noise_light),
                    ContextCompat.getColor(this, R.color.captcha_noise_dark)
            ));
            canvas.drawCircle(
                    random.nextInt(width),
                    random.nextInt(height),
                    1f + random.nextFloat() * 2.2f,
                    dotPaint
            );
        }
    }

    private void drawInterferenceLines(Canvas canvas, int width, int height) {
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(dpToPx(1.2f));
        linePaint.setAlpha(150);

        for (int i = 0; i < 5; i++) {
            linePaint.setColor(randomColor(
                    ContextCompat.getColor(this, R.color.captcha_line_start),
                    ContextCompat.getColor(this, R.color.captcha_line_end)
            ));
            float startX = random.nextInt(width / 3);
            float startY = random.nextInt(height);
            float endX = width - random.nextInt(width / 3);
            float endY = random.nextInt(height);
            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }
    }

    private void drawCaptchaText(Canvas canvas, String captchaText, int width, int height) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(dpToPx(22));
        textPaint.setFakeBoldText(true);

        Rect textBounds = new Rect();
        float charWidth = width / (float) (captchaText.length() + 1);

        for (int i = 0; i < captchaText.length(); i++) {
            String letter = String.valueOf(captchaText.charAt(i));
            textPaint.setColor(randomTextColor());
            textPaint.getTextBounds(letter, 0, letter.length(), textBounds);

            float baseX = charWidth * (i + 0.7f);
            float baseY = height / 2f + textBounds.height() / 2f;
            float offsetY = random.nextInt(dpToPx(8)) - dpToPx(4);
            float rotation = random.nextInt(31) - 15;

            canvas.save();
            canvas.rotate(rotation, baseX, baseY);
            canvas.drawText(letter, baseX, baseY + offsetY, textPaint);
            canvas.restore();
        }
    }

    @ColorInt
    private int randomTextColor() {
        int[] colors = {
                ContextCompat.getColor(this, R.color.captcha_text_blue),
                ContextCompat.getColor(this, R.color.captcha_text_purple),
                ContextCompat.getColor(this, R.color.captcha_text_teal),
                ContextCompat.getColor(this, R.color.captcha_text_ink)
        };
        return colors[random.nextInt(colors.length)];
    }

    @ColorInt
    private int randomColor(@ColorInt int startColor, @ColorInt int endColor) {
        int red = averageChannel(Color.red(startColor), Color.red(endColor));
        int green = averageChannel(Color.green(startColor), Color.green(endColor));
        int blue = averageChannel(Color.blue(startColor), Color.blue(endColor));
        return Color.rgb(red, green, blue);
    }

    private int averageChannel(int start, int end) {
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        return min + random.nextInt(max - min + 1);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
