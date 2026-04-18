package com.example.nestchat.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.nestchat.ChatImagePreviewActivity;
import com.example.nestchat.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private static final String STATE_MESSAGES = "state_messages";
    private static final String STATE_IS_VOICE_MODE = "state_is_voice_mode";
    private static final int TYPE_LEFT = 0;
    private static final int TYPE_RIGHT = 1;
    private static final int TYPE_SYSTEM = 2;
    private static final int CONTENT_TEXT = 0;
    private static final int CONTENT_VOICE = 1;
    private static final int CONTENT_IMAGE = 2;
    private static final long MIN_RECORD_DURATION_MS = 800L;
    private static final int CANCEL_DISTANCE_DP = 72;

    private NestedScrollView scrollMessages;
    private LinearLayout layoutMessageList;
    private View layoutInputBar;
    private EditText etMessageInput;
    private TextView btnInputMode;
    private TextView btnHoldToSpeak;
    private TextView btnAi;
    private View btnPhoto;
    private View btnSend;
    private final ArrayList<ChatMessage> messages = new ArrayList<>();

    private boolean isVoiceMode = false;
    private boolean isRecording = false;
    private boolean isRecordCancelPending = false;
    private int inputBarBaseBottomMargin;
    private long recordStartTime;
    private float recordStartRawY;
    private String currentRecordingPath;
    private String currentPlayingAudioPath;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    private final ActivityResultLauncher<String> recordAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(requireContext(), "麦克风权限已开启，请重新按住说话", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "未开启麦克风权限，无法发送语音", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                messages.add(ChatMessage.image(TYPE_RIGHT, getCurrentTime(), uri.toString()));
                appendLastMessage();
            });

    public ChatFragment() {
        // Required empty public constructor.
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        applyWindowInsets(view);
        restoreState(savedInstanceState);
        bindEvents(view);
        renderMessages();
        updateInputModeUi();
        scrollToBottom();
    }

    private void initViews(View view) {
        scrollMessages = view.findViewById(R.id.scrollMessages);
        layoutMessageList = view.findViewById(R.id.layoutMessageList);
        layoutInputBar = view.findViewById(R.id.layoutInputBar);
        etMessageInput = view.findViewById(R.id.etMessageInput);
        btnInputMode = view.findViewById(R.id.btnInputMode);
        btnHoldToSpeak = view.findViewById(R.id.btnHoldToSpeak);
        btnAi = view.findViewById(R.id.btnAi);
        btnPhoto = view.findViewById(R.id.btnPhoto);
        btnSend = view.findViewById(R.id.btnSend);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) layoutInputBar.getLayoutParams();
        inputBarBaseBottomMargin = params.bottomMargin;
    }

    private void applyWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.rootChat), (root, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) layoutInputBar.getLayoutParams();
            params.bottomMargin = inputBarBaseBottomMargin + (imeVisible ? imeInsets.bottom : 0);
            layoutInputBar.setLayoutParams(params);

            if (imeVisible) {
                scrollToBottom();
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(view.findViewById(R.id.rootChat));
    }

    private void restoreState(Bundle savedInstanceState) {
        messages.clear();
        if (savedInstanceState == null) {
            seedMessages();
            return;
        }

        isVoiceMode = savedInstanceState.getBoolean(STATE_IS_VOICE_MODE, false);
        ArrayList<Bundle> savedItems = savedInstanceState.getParcelableArrayList(STATE_MESSAGES);
        if (savedItems == null || savedItems.isEmpty()) {
            seedMessages();
            return;
        }

        for (Bundle bundle : savedItems) {
            messages.add(ChatMessage.fromBundle(bundle));
        }
    }

    private void bindEvents(View view) {
        btnInputMode.setOnClickListener(v -> toggleInputMode());
        btnHoldToSpeak.setOnTouchListener((v, event) -> handleVoiceTouch(event));

        etMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateInputModeUi();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });

        etMessageInput.setOnFocusChangeListener((v, hasFocus) -> updateInputModeUi());
        btnPhoto.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        btnAi.setOnClickListener(v -> showAiBottomSheet());
        btnSend.setOnClickListener(v -> sendTextMessage());
    }

    private boolean handleVoiceTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startVoiceRecording(event.getRawY());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateRecordCancelState(event.getRawY());
                return true;
            case MotionEvent.ACTION_UP:
                stopVoiceRecording(!isRecordCancelPending);
                return true;
            case MotionEvent.ACTION_CANCEL:
                stopVoiceRecording(false);
                return true;
            default:
                return false;
        }
    }

    private void seedMessages() {
        messages.add(new ChatMessage(TYPE_LEFT, CONTENT_TEXT, "嗨，你在忙什么呀？", "14:32"));
        messages.add(new ChatMessage(TYPE_RIGHT, CONTENT_TEXT, "刚结束学习，准备放松一下～", "14:35"));
        messages.add(new ChatMessage(TYPE_SYSTEM, CONTENT_TEXT, "今天是你们相伴的第 12 天", "14:40"));
        messages.add(new ChatMessage(TYPE_LEFT, CONTENT_TEXT, "今天过得怎么样？", "14:41"));
        messages.add(new ChatMessage(TYPE_RIGHT, CONTENT_TEXT, "还好，就是有点累哈哈", "14:43"));
        messages.add(new ChatMessage(TYPE_LEFT, CONTENT_TEXT, "辛苦啦～\n记得休息一下，我在呢 🌙", "14:45"));
    }

    private void toggleInputMode() {
        isVoiceMode = !isVoiceMode;
        etMessageInput.setText("");
        etMessageInput.clearFocus();
        updateInputModeUi();
    }

    private void updateInputModeUi() {
        if (isVoiceMode) {
            btnInputMode.setText("键盘");
            etMessageInput.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
            btnHoldToSpeak.setVisibility(View.VISIBLE);
            btnAi.setVisibility(View.GONE);
            btnPhoto.setVisibility(View.GONE);
            updateVoiceRecordButtonState();
            return;
        }

        btnInputMode.setText("语音");
        etMessageInput.setVisibility(View.VISIBLE);
        btnHoldToSpeak.setVisibility(View.GONE);

        boolean hasText = !TextUtils.isEmpty(etMessageInput.getText().toString().trim());
        if (hasText) {
            btnAi.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.VISIBLE);
            btnPhoto.setVisibility(View.GONE);
        } else {
            btnAi.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
            btnPhoto.setVisibility(View.VISIBLE);
        }
    }

    private void sendTextMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            etMessageInput.setError("请输入消息");
            etMessageInput.requestFocus();
            return;
        }

        messages.add(new ChatMessage(TYPE_RIGHT, CONTENT_TEXT, content, getCurrentTime()));
        etMessageInput.setText("");
        etMessageInput.clearFocus();
        appendLastMessage();
    }

    private void startVoiceRecording(float rawY) {
        if (!hasRecordAudioPermission()) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }

        if (isRecording) {
            return;
        }

        releaseRecorder();
        File outputFile = new File(getVoiceMessageDir(), "voice_" + System.currentTimeMillis() + ".m4a");
        currentRecordingPath = outputFile.getAbsolutePath();

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setOutputFile(currentRecordingPath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            recordStartTime = System.currentTimeMillis();
            recordStartRawY = rawY;
            isRecording = true;
            isRecordCancelPending = false;
            updateVoiceRecordButtonState();
        } catch (IOException | RuntimeException e) {
            releaseRecorder();
            currentRecordingPath = null;
            Toast.makeText(requireContext(), "录音启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRecordCancelState(float rawY) {
        if (!isRecording) {
            return;
        }
        boolean shouldCancel = recordStartRawY - rawY > dpToPx(CANCEL_DISTANCE_DP);
        if (isRecordCancelPending == shouldCancel) {
            return;
        }
        isRecordCancelPending = shouldCancel;
        updateVoiceRecordButtonState();
    }

    private void stopVoiceRecording(boolean shouldSend) {
        if (!isRecording) {
            return;
        }

        boolean shouldCancel = !shouldSend || isRecordCancelPending;
        long durationMs = System.currentTimeMillis() - recordStartTime;
        boolean recordSucceeded = true;

        try {
            mediaRecorder.stop();
        } catch (RuntimeException e) {
            recordSucceeded = false;
        } finally {
            releaseRecorder();
        }

        isRecording = false;
        isRecordCancelPending = false;
        updateVoiceRecordButtonState();

        if (!recordSucceeded || currentRecordingPath == null) {
            deleteVoiceFile(currentRecordingPath);
            currentRecordingPath = null;
            Toast.makeText(requireContext(), "录音失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        if (shouldCancel) {
            deleteVoiceFile(currentRecordingPath);
            currentRecordingPath = null;
            Toast.makeText(requireContext(), "已取消发送语音", Toast.LENGTH_SHORT).show();
            return;
        }

        if (durationMs < MIN_RECORD_DURATION_MS) {
            deleteVoiceFile(currentRecordingPath);
            currentRecordingPath = null;
            Toast.makeText(requireContext(), "录音时间太短", Toast.LENGTH_SHORT).show();
            return;
        }

        int durationSeconds = Math.max(1, (int) Math.round(durationMs / 1000f));
        messages.add(ChatMessage.voice(TYPE_RIGHT, getCurrentTime(), currentRecordingPath, durationSeconds));
        currentRecordingPath = null;
        appendLastMessage();
    }

    private boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private File getVoiceMessageDir() {
        File directory = new File(requireContext().getFilesDir(), "voice_messages");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private void appendLastMessage() {
        if (messages.isEmpty()) {
            return;
        }
        View itemView = createMessageView(messages.get(messages.size() - 1));
        layoutMessageList.addView(itemView);
        scrollToBottom();
    }

    private void renderMessages() {
        layoutMessageList.removeAllViews();
        for (ChatMessage message : messages) {
            layoutMessageList.addView(createMessageView(message));
        }
    }

    private View createMessageView(ChatMessage message) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View itemView;

        if (message.type == TYPE_LEFT) {
            itemView = inflater.inflate(R.layout.item_chat_message_left, layoutMessageList, false);
            bindBubbleMessage(itemView, message, false);
            return itemView;
        }

        if (message.type == TYPE_RIGHT) {
            itemView = inflater.inflate(R.layout.item_chat_message_right, layoutMessageList, false);
            bindBubbleMessage(itemView, message, true);
            return itemView;
        }

        itemView = inflater.inflate(R.layout.item_chat_message_system, layoutMessageList, false);
        ((TextView) itemView.findViewById(R.id.tvTime)).setText(message.time);
        ((TextView) itemView.findViewById(R.id.tvMessage)).setText(message.content);
        return itemView;
    }

    private void bindBubbleMessage(View itemView, ChatMessage message, boolean showMeta) {
        TextView tvTime = itemView.findViewById(R.id.tvTime);
        TextView tvMessage = itemView.findViewById(R.id.tvMessage);
        ImageView ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
        tvTime.setText(message.time);

        if (message.contentType == CONTENT_VOICE) {
            tvMessage.setVisibility(View.VISIBLE);
            ivMessageImage.setVisibility(View.GONE);
            boolean isPlaying = !TextUtils.isEmpty(currentPlayingAudioPath)
                    && currentPlayingAudioPath.equals(message.audioPath);
            tvMessage.setText(isPlaying
                    ? "正在播放 " + message.durationSeconds + "''"
                    : "点击播放语音 " + message.durationSeconds + "''");
            tvMessage.setMinWidth(dpToPx(132));
            tvMessage.setBackgroundResource(getVoiceBubbleBackground(message.type, isPlaying));
            tvMessage.setTextColor(getVoiceTextColor(message.type));
            tvMessage.setCompoundDrawablePadding(dpToPx(8));
            tvMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    tintedVoiceDrawable(isPlaying, message.type == TYPE_RIGHT),
                    null,
                    null,
                    null
            );
            tvMessage.setOnClickListener(v -> playVoiceMessage(message.audioPath));
            ivMessageImage.setOnClickListener(null);
        } else if (message.contentType == CONTENT_IMAGE) {
            tvMessage.setVisibility(View.GONE);
            ivMessageImage.setVisibility(View.VISIBLE);
            ivMessageImage.setImageURI(Uri.parse(message.imageUri));
            ivMessageImage.setBackgroundResource(message.type == TYPE_RIGHT
                    ? R.drawable.bg_chat_image_frame_right
                    : R.drawable.bg_chat_image_frame_left);
            ivMessageImage.setOnClickListener(v -> openImagePreview(message.imageUri));
            tvMessage.setOnClickListener(null);
        } else {
            tvMessage.setVisibility(View.VISIBLE);
            ivMessageImage.setVisibility(View.GONE);
            tvMessage.setText(message.content);
            tvMessage.setMinWidth(0);
            tvMessage.setBackgroundResource(message.type == TYPE_RIGHT
                    ? R.drawable.bg_chat_bubble_right
                    : R.drawable.bg_chat_bubble_left);
            tvMessage.setTextColor(message.type == TYPE_RIGHT
                    ? ContextCompat.getColor(requireContext(), R.color.white)
                    : ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            tvMessage.setCompoundDrawablePadding(0);
            tvMessage.setOnClickListener(null);
            ivMessageImage.setOnClickListener(null);
        }

        if (showMeta) {
            TextView tvMeta = itemView.findViewById(R.id.tvMeta);
            tvMeta.setText("已送达 " + message.time);
        }
    }

    private void playVoiceMessage(String audioPath) {
        if (TextUtils.isEmpty(audioPath) || !new File(audioPath).exists()) {
            Toast.makeText(requireContext(), "语音文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioPath.equals(currentPlayingAudioPath) && mediaPlayer != null && mediaPlayer.isPlaying()) {
            releasePlayer(true);
            return;
        }

        releasePlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            currentPlayingAudioPath = audioPath;
            renderMessages();
            mediaPlayer.setOnCompletionListener(mp -> releasePlayer(true));
            mediaPlayer.start();
        } catch (IOException e) {
            releasePlayer(true);
            Toast.makeText(requireContext(), "语音播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAiBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_ai_unavailable_sheet, null, false);
        contentView.findViewById(R.id.btnCloseAiSheet).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(contentView);
        dialog.show();
    }

    private void openImagePreview(String imageUri) {
        if (TextUtils.isEmpty(imageUri)) {
            return;
        }
        Intent intent = new Intent(requireContext(), ChatImagePreviewActivity.class);
        intent.putExtra(ChatImagePreviewActivity.EXTRA_IMAGE_URI, imageUri);
        startActivity(intent);
    }

    private void scrollToBottom() {
        scrollMessages.post(() -> scrollMessages.fullScroll(View.FOCUS_DOWN));
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    private void releaseRecorder() {
        if (mediaRecorder == null) {
            return;
        }
        mediaRecorder.reset();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    private void releasePlayer() {
        releasePlayer(false);
    }

    private void releasePlayer(boolean refreshUi) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (!TextUtils.isEmpty(currentPlayingAudioPath)) {
            currentPlayingAudioPath = null;
            if (refreshUi) {
                renderMessages();
            }
        }
    }

    private void deleteVoiceFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isRecording) {
            stopVoiceRecording(false);
        }
        releasePlayer(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseRecorder();
        releasePlayer(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_VOICE_MODE, isVoiceMode);
        ArrayList<Bundle> bundles = new ArrayList<>();
        for (ChatMessage message : messages) {
            bundles.add(message.toBundle());
        }
        outState.putParcelableArrayList(STATE_MESSAGES, bundles);
    }

    private void updateVoiceRecordButtonState() {
        int backgroundRes;
        int iconRes;
        int textColorRes;
        String text;

        if (isRecordCancelPending) {
            backgroundRes = R.drawable.bg_chat_voice_button_cancel;
            iconRes = R.drawable.ic_chat_mic_cancel;
            textColorRes = R.color.chat_voice_cancel_text;
            text = "松开 取消发送";
        } else if (isRecording) {
            backgroundRes = R.drawable.bg_chat_voice_button_recording;
            iconRes = R.drawable.ic_chat_mic_recording;
            textColorRes = R.color.chat_voice_record_text;
            text = "松开 发送";
        } else {
            backgroundRes = R.drawable.bg_chat_voice_button_idle;
            iconRes = R.drawable.ic_chat_mic_idle;
            textColorRes = R.color.text_primary;
            text = "按住 说话";
        }

        btnHoldToSpeak.setText(text);
        btnHoldToSpeak.setBackgroundResource(backgroundRes);
        btnHoldToSpeak.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
        btnHoldToSpeak.setCompoundDrawablePadding(dpToPx(8));
        btnHoldToSpeak.setCompoundDrawablesRelativeWithIntrinsicBounds(
                AppCompatResources.getDrawable(requireContext(), iconRes),
                null,
                null,
                null
        );
    }

    private int getVoiceBubbleBackground(int type, boolean isPlaying) {
        if (type == TYPE_RIGHT) {
            return isPlaying
                    ? R.drawable.bg_chat_voice_bubble_right_playing
                    : R.drawable.bg_chat_bubble_right;
        }
        return isPlaying
                ? R.drawable.bg_chat_voice_bubble_left_playing
                : R.drawable.bg_chat_bubble_left;
    }

    private int getVoiceTextColor(int type) {
        return ContextCompat.getColor(
                requireContext(),
                type == TYPE_RIGHT ? R.color.white : R.color.text_primary
        );
    }

    private Drawable tintedVoiceDrawable(boolean isPlaying, boolean isRightMessage) {
        int drawableRes = isPlaying ? R.drawable.ic_chat_voice_playing : R.drawable.ic_chat_voice_idle;
        Drawable drawable = AppCompatResources.getDrawable(requireContext(), drawableRes);
        if (drawable == null) {
            return null;
        }
        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        int tintColor = ContextCompat.getColor(
                requireContext(),
                isRightMessage ? R.color.white : (isPlaying ? R.color.brand_primary_dark : R.color.text_secondary)
        );
        DrawableCompat.setTint(wrapped, tintColor);
        return wrapped;
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class ChatMessage {
        final int type;
        final int contentType;
        final String content;
        final String time;
        final String audioPath;
        final int durationSeconds;
        final String imageUri;

        ChatMessage(int type, int contentType, String content, String time) {
            this(type, contentType, content, time, "", 0, "");
        }

        ChatMessage(int type, int contentType, String content, String time,
                    String audioPath, int durationSeconds, String imageUri) {
            this.type = type;
            this.contentType = contentType;
            this.content = content;
            this.time = time;
            this.audioPath = audioPath;
            this.durationSeconds = durationSeconds;
            this.imageUri = imageUri;
        }

        static ChatMessage voice(int type, String time, String audioPath, int durationSeconds) {
            return new ChatMessage(type, CONTENT_VOICE, "", time, audioPath, durationSeconds, "");
        }

        static ChatMessage image(int type, String time, String imageUri) {
            return new ChatMessage(type, CONTENT_IMAGE, "", time, "", 0, imageUri);
        }

        Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt("type", type);
            bundle.putInt("contentType", contentType);
            bundle.putString("content", content);
            bundle.putString("time", time);
            bundle.putString("audioPath", audioPath);
            bundle.putInt("durationSeconds", durationSeconds);
            bundle.putString("imageUri", imageUri);
            return bundle;
        }

        static ChatMessage fromBundle(Bundle bundle) {
            return new ChatMessage(
                    bundle.getInt("type", TYPE_LEFT),
                    bundle.getInt("contentType", CONTENT_TEXT),
                    bundle.getString("content", ""),
                    bundle.getString("time", ""),
                    bundle.getString("audioPath", ""),
                    bundle.getInt("durationSeconds", 0),
                    bundle.getString("imageUri", "")
            );
        }
    }
}
