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
import android.util.Log;
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
import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.ChatApi;
import com.example.nestchat.api.FileApi;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
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

    private String conversationId;
    private String nextCursor;
    private boolean hasMore = false;

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
                uploadAndSendImage(uri);
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
        bindEvents(view);
        updateInputModeUi();
        loadChatSession();
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

    private void loadChatSession() {
        ChatApi.Impl.getChatSession(new ApiCallback<ChatApi.ChatSessionResponse>() {
            @Override
            public void onSuccess(ChatApi.ChatSessionResponse data) {
                if (!isAdded()) return;
                if (data != null && data.conversationId != null) {
                    conversationId = data.conversationId;
                    loadMessages(null);
                } else {
                    messages.clear();
                    messages.add(new ChatMessage(TYPE_SYSTEM, CONTENT_TEXT,
                            "暂无聊天会话，请先绑定关系", getCurrentTime()));
                    renderMessages();
                }
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) return;
                messages.clear();
                messages.add(new ChatMessage(TYPE_SYSTEM, CONTENT_TEXT,
                        "加载会话失败: " + error.message, getCurrentTime()));
                renderMessages();
            }
        });
    }

    private void loadMessages(String cursor) {
        if (conversationId == null) return;

        ChatApi.Impl.getMessages(conversationId, cursor, 30, new ApiCallback<ChatApi.MessageListResponse>() {
            @Override
            public void onSuccess(ChatApi.MessageListResponse data) {
                if (!isAdded()) return;
                if (data == null) return;

                if (cursor == null) {
                    messages.clear();
                }

                if (data.items != null) {
                    // Server returns newest first, we need oldest first for display
                    List<ChatApi.MessageResponse> items = data.items;
                    for (int i = items.size() - 1; i >= 0; i--) {
                        messages.add(convertMessage(items.get(i)));
                    }
                }

                nextCursor = data.nextCursor;
                hasMore = data.hasMore;
                renderMessages();
                scrollToBottom();
            }

            @Override
            public void onError(ApiError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "加载消息失败: " + error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private ChatMessage convertMessage(ChatApi.MessageResponse msg) {
        int type;
        if ("system".equals(msg.senderType)) {
            type = TYPE_SYSTEM;
        } else if ("self".equals(msg.senderType)) {
            type = TYPE_RIGHT;
        } else {
            type = TYPE_LEFT;
        }

        int contentType = CONTENT_TEXT;
        String content = msg.content != null ? msg.content : "";
        String imageUri = "";
        String audioPath = "";
        int durationSeconds = 0;

        if ("image".equals(msg.messageType)) {
            contentType = CONTENT_IMAGE;
            imageUri = msg.imageUrl != null ? msg.imageUrl : "";
        } else if ("voice".equals(msg.messageType)) {
            contentType = CONTENT_VOICE;
            audioPath = msg.voiceUrl != null ? msg.voiceUrl : "";
            durationSeconds = msg.durationSeconds;
        }

        String time = "";
        if (msg.createdAt != null && msg.createdAt.length() >= 16) {
            time = msg.createdAt.substring(11, 16);
        }

        return new ChatMessage(type, contentType, content, time, audioPath, durationSeconds, imageUri);
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

        if (conversationId == null) {
            Toast.makeText(requireContext(), "暂无聊天会话", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optimistic: show message immediately
        messages.add(new ChatMessage(TYPE_RIGHT, CONTENT_TEXT, content, getCurrentTime()));
        etMessageInput.setText("");
        etMessageInput.clearFocus();
        appendLastMessage();

        ChatApi.SendTextMessageRequest req = new ChatApi.SendTextMessageRequest();
        req.conversationId = conversationId;
        req.content = content;
        req.clientMessageId = UUID.randomUUID().toString();

        ChatApi.Impl.sendTextMessage(req, new ApiCallback<ChatApi.MessageResponse>() {
            @Override
            public void onSuccess(ChatApi.MessageResponse data) {
                // Message sent successfully — already shown optimistically
            }

            @Override
            public void onError(ApiError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "发送失败: " + error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadAndSendImage(Uri uri) {
        File tempFile = copyUriToTempFile(uri);
        if (tempFile == null) {
            Toast.makeText(requireContext(), "图片读取失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show optimistically
        messages.add(ChatMessage.image(TYPE_RIGHT, getCurrentTime(), uri.toString()));
        appendLastMessage();

        FileApi.UploadFileRequest uploadReq = new FileApi.UploadFileRequest();
        uploadReq.localPath = tempFile.getAbsolutePath();
        uploadReq.mimeType = "image/jpeg";
        uploadReq.bizType = "chat";

        FileApi.Impl.uploadImage(uploadReq, new ApiCallback<FileApi.UploadFileResponse>() {
            @Override
            public void onSuccess(FileApi.UploadFileResponse data) {
                tempFile.delete();
                if (data == null || conversationId == null) return;

                ChatApi.SendImageMessageRequest req = new ChatApi.SendImageMessageRequest();
                req.conversationId = conversationId;
                req.imageFileId = data.fileId;
                req.clientMessageId = UUID.randomUUID().toString();

                ChatApi.Impl.sendImageMessage(req, new ApiCallback<ChatApi.MessageResponse>() {
                    @Override
                    public void onSuccess(ChatApi.MessageResponse data) {
                        // sent
                    }

                    @Override
                    public void onError(ApiError error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "图片发送失败: " + error.message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                tempFile.delete();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "图片上传失败: " + error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private File copyUriToTempFile(Uri uri) {
        try {
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            if (in == null) return null;
            File temp = new File(requireContext().getCacheDir(), "chat_upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(temp);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return temp;
        } catch (Exception e) {
            return null;
        }
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
        String voicePath = currentRecordingPath;
        currentRecordingPath = null;

        // Show optimistically
        messages.add(ChatMessage.voice(TYPE_RIGHT, getCurrentTime(), voicePath, durationSeconds));
        appendLastMessage();

        // Upload and send
        uploadAndSendVoice(voicePath, durationSeconds);
    }

    private void uploadAndSendVoice(String voicePath, int durationSeconds) {
        if (conversationId == null) return;

        FileApi.UploadFileRequest uploadReq = new FileApi.UploadFileRequest();
        uploadReq.localPath = voicePath;
        uploadReq.mimeType = "audio/mp4";
        uploadReq.bizType = "chat";

        FileApi.Impl.uploadVoice(uploadReq, new ApiCallback<FileApi.UploadFileResponse>() {
            @Override
            public void onSuccess(FileApi.UploadFileResponse data) {
                if (data == null) return;

                ChatApi.SendVoiceMessageRequest req = new ChatApi.SendVoiceMessageRequest();
                req.conversationId = conversationId;
                req.voiceFileId = data.fileId;
                req.durationSeconds = durationSeconds;
                req.clientMessageId = UUID.randomUUID().toString();

                ChatApi.Impl.sendVoiceMessage(req, new ApiCallback<ChatApi.MessageResponse>() {
                    @Override
                    public void onSuccess(ChatApi.MessageResponse data) {
                        // sent
                    }

                    @Override
                    public void onError(ApiError error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "语音发送失败: " + error.message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "语音上传失败: " + error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
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
            String imgUri = message.imageUri;
            if (imgUri != null && !imgUri.isEmpty()) {
                if (imgUri.startsWith("content://") || imgUri.startsWith("file://")) {
                    ivMessageImage.setImageURI(Uri.parse(imgUri));
                } else {
                    // Network image — just show placeholder for now
                    ivMessageImage.setImageDrawable(null);
                }
            }
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
        if (TextUtils.isEmpty(audioPath)) {
            Toast.makeText(requireContext(), "语音文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        // For local files
        if (!audioPath.startsWith("http") && !new File(audioPath).exists()) {
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
