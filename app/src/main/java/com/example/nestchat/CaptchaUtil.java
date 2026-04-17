package com.example.nestchat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import java.util.Random;

/**
 * 生成简单图片验证码，便于在登录、注册等页面复用。
 */
public final class CaptchaUtil {

    private static final String CAPTCHA_SOURCE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private CaptchaUtil() {
    }

    public static CaptchaResult createCaptcha(Context context) {
        Random random = new Random();
        String code = createCaptchaText(random);
        Bitmap bitmap = createCaptchaBitmap(context, random, code);
        return new CaptchaResult(code, bitmap);
    }

    private static String createCaptchaText(Random random) {
        int length = random.nextBoolean() ? 4 : 5;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CAPTCHA_SOURCE.length());
            builder.append(CAPTCHA_SOURCE.charAt(index));
        }
        return builder.toString();
    }

    private static Bitmap createCaptchaBitmap(Context context, Random random, String captchaText) {
        int width = dpToPx(context, 112);
        int height = dpToPx(context, 58);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setShader(new LinearGradient(
                0, 0, width, height,
                new int[]{
                        ContextCompat.getColor(context, R.color.captcha_bg_start),
                        ContextCompat.getColor(context, R.color.captcha_bg_end)
                },
                null,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(0f, 0f, width, height, dpToPx(context, 14), dpToPx(context, 14), backgroundPaint);

        drawNoiseDots(context, canvas, random, width, height);
        drawInterferenceLines(context, canvas, random, width, height);
        drawCaptchaText(context, canvas, random, captchaText, width, height);
        return bitmap;
    }

    private static void drawNoiseDots(Context context, Canvas canvas, Random random, int width, int height) {
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 72; i++) {
            dotPaint.setColor(randomColor(
                    random,
                    ContextCompat.getColor(context, R.color.captcha_noise_light),
                    ContextCompat.getColor(context, R.color.captcha_noise_dark)
            ));
            canvas.drawCircle(
                    random.nextInt(width),
                    random.nextInt(height),
                    1f + random.nextFloat() * 2.2f,
                    dotPaint
            );
        }
    }

    private static void drawInterferenceLines(Context context, Canvas canvas, Random random, int width, int height) {
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(dpToPx(context, 1.2f));
        linePaint.setAlpha(150);

        for (int i = 0; i < 5; i++) {
            linePaint.setColor(randomColor(
                    random,
                    ContextCompat.getColor(context, R.color.captcha_line_start),
                    ContextCompat.getColor(context, R.color.captcha_line_end)
            ));
            float startX = random.nextInt(width / 3);
            float startY = random.nextInt(height);
            float endX = width - random.nextInt(width / 3);
            float endY = random.nextInt(height);
            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }
    }

    private static void drawCaptchaText(Context context, Canvas canvas, Random random, String captchaText, int width, int height) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(dpToPx(context, 22));
        textPaint.setFakeBoldText(true);

        Rect textBounds = new Rect();
        float charWidth = width / (float) (captchaText.length() + 1);

        for (int i = 0; i < captchaText.length(); i++) {
            String letter = String.valueOf(captchaText.charAt(i));
            textPaint.setColor(randomTextColor(context, random));
            textPaint.getTextBounds(letter, 0, letter.length(), textBounds);

            float baseX = charWidth * (i + 0.7f);
            float baseY = height / 2f + textBounds.height() / 2f;
            float offsetY = random.nextInt(dpToPx(context, 8)) - dpToPx(context, 4);
            float rotation = random.nextInt(31) - 15;

            canvas.save();
            canvas.rotate(rotation, baseX, baseY);
            canvas.drawText(letter, baseX, baseY + offsetY, textPaint);
            canvas.restore();
        }
    }

    @ColorInt
    private static int randomTextColor(Context context, Random random) {
        int[] colors = {
                ContextCompat.getColor(context, R.color.captcha_text_blue),
                ContextCompat.getColor(context, R.color.captcha_text_purple),
                ContextCompat.getColor(context, R.color.captcha_text_teal),
                ContextCompat.getColor(context, R.color.captcha_text_ink)
        };
        return colors[random.nextInt(colors.length)];
    }

    @ColorInt
    private static int randomColor(Random random, @ColorInt int startColor, @ColorInt int endColor) {
        int red = averageChannel(random, Color.red(startColor), Color.red(endColor));
        int green = averageChannel(random, Color.green(startColor), Color.green(endColor));
        int blue = averageChannel(random, Color.blue(startColor), Color.blue(endColor));
        return Color.rgb(red, green, blue);
    }

    private static int averageChannel(Random random, int start, int end) {
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        return min + random.nextInt(max - min + 1);
    }

    private static int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static final class CaptchaResult {
        private final String code;
        private final Bitmap bitmap;

        public CaptchaResult(String code, Bitmap bitmap) {
            this.code = code;
            this.bitmap = bitmap;
        }

        public String getCode() {
            return code;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }
    }
}
