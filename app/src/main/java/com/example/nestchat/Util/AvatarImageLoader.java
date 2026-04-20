package com.example.nestchat.util;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.nestchat.R;
import com.example.nestchat.api.MediaUrlResolver;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class
AvatarImageLoader {

    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AvatarImageLoader() {
    }
    public static void load(ImageView imageView, String imageUrl, int placeholderPaddingDp) {
        String normalizedUrl = MediaUrlResolver.resolve(imageUrl);
        imageView.setTag(normalizedUrl);
        if (normalizedUrl.isEmpty()) {
            showPlaceholder(imageView, placeholderPaddingDp);
            return;
        }

        loadRemoteBitmap(
                imageView,
                normalizedUrl,
                () -> showPlaceholder(imageView, placeholderPaddingDp)
        );
    }

    public static void loadContent(ImageView imageView, String imageRef) {
        String normalizedRef = MediaUrlResolver.resolve(imageRef);
        imageView.setTag(normalizedRef);
        if (normalizedRef.isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }

        imageView.setPadding(0, 0, 0, 0);
        imageView.setClipToOutline(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageTintList(null);

        if (normalizedRef.startsWith("content://")
                || normalizedRef.startsWith("file://")
                || normalizedRef.startsWith("android.resource://")) {
            imageView.setImageURI(Uri.parse(normalizedRef));
            return;
        }

        loadRemoteBitmap(imageView, normalizedRef, () -> imageView.setImageDrawable(null));
    }

    public static void showLocal(ImageView imageView, Uri uri) {
        imageView.setTag(uri.toString());
        imageView.setPadding(0, 0, 0, 0);
        imageView.setClipToOutline(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageTintList(null);
        imageView.setImageURI(uri);
    }

    private static void showBitmap(ImageView imageView, Bitmap bitmap) {
        imageView.setPadding(0, 0, 0, 0);
        imageView.setClipToOutline(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageTintList(null);
        imageView.setImageBitmap(bitmap);
    }

    private static void loadRemoteBitmap(ImageView imageView, String imageUrl, Runnable onFailure) {
        Request request = new Request.Builder().url(imageUrl).build();
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN_HANDLER.post(() -> {
                    if (imageUrl.equals(imageView.getTag())) {
                        onFailure.run();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    response.close();
                    MAIN_HANDLER.post(() -> {
                        if (imageUrl.equals(imageView.getTag())) {
                            onFailure.run();
                        }
                    });
                    return;
                }

                byte[] bytes = response.body().bytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                response.close();

                MAIN_HANDLER.post(() -> {
                    if (!imageUrl.equals(imageView.getTag())) {
                        return;
                    }
                    if (bitmap == null) {
                        onFailure.run();
                        return;
                    }
                    showBitmap(imageView, bitmap);
                });
            }
        });
    }

    private static void showPlaceholder(ImageView imageView, int placeholderPaddingDp) {
        int paddingPx = Math.round(placeholderPaddingDp * imageView.getResources().getDisplayMetrics().density);
        imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        imageView.setClipToOutline(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.ic_user);
        imageView.setImageTintList(ColorStateList.valueOf(imageView.getContext().getColor(R.color.brand_primary_dark)));
    }

}
