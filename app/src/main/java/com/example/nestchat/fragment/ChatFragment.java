package com.example.nestchat.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.AiApi;
import com.example.nestchat.api.ChatApi;
import com.example.nestchat.api.FileApi;
import com.example.nestchat.api.MediaUrlResolver;
import com.example.nestchat.api.RelationApi;
import com.example.nestchat.util.AvatarImageLoader;
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
    private ImageView ivPartnerAvatar;
    private TextView tvPartnerName;
    private TextView tvCompanionDays;
    private TextView tvPartnerMood;
    private TextView tvLastActive;
    private TextView tvTodayDiary;
    private View layoutRiskTip;
    private TextView tvRiskTipTitle;
    private TextView tvRiskTipMessage;
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
    private String myAvatarUrl;
    private String partnerAvatarUrl;
    private boolean isBound = false;
    private final ArrayList<String> currentPartnerTurnTexts = new ArrayList<>();
    private final ArrayList<String> currentPartnerTurnMessageKeys = new ArrayList<>();
    private String currentTurnOwner = "";
    private String currentPartnerTurnKey = "";
    private String lastAnalyzedPartnerTurnKey = "";
    private String activeRiskRequestKey = "";

    private static final long POLLING_INTERVAL_MS = 1000;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            refreshNewMessages();
            pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
        }
    };

    private String lastMessageTime;

    private final ActivityResultLauncher<String> recordAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(requireContext(), "麦克风权限已开启，请重新按住说话", Toast.LENGTH_SHORT).show();
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
        sendHeartbeat();
    }

    @Override
    public void onResume() {
        super.onResume();
        sendHeartbeat();
    }

    @Override
    public void onStart() {
        super.onStart();
        startPolling();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPolling();
        if (isRecording) {
            stopVoiceRecording(false);
        }
        releasePlayer(true);
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
        ivPartnerAvatar = view.findViewById(R.id.ivPartnerAvatar);
        tvPartnerName = view.findViewById(R.id.tvPartnerName);
        tvCompanionDays = view.findViewById(R.id.tvCompanionDays);
        tvPartnerMood = view.findViewById(R.id.tvPartnerMood);
        tvLastActive = view.findViewById(R.id.tvLastActive);
        tvTodayDiary = view.findViewById(R.id.tvTodayDiary);
        layoutRiskTip = view.findViewById(R.id.layoutRiskTip);
        tvRiskTipTitle = view.findViewById(R.id.tvRiskTipTitle);
        tvRiskTipMessage = view.findViewById(R.id.tvRiskTipMessage);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) layoutInputBar.getLayoutParams();
        inputBarBaseBottomMargin = params.bottomMargin;
    }

    private void applyWindowInsets(View view) {
        View chatHeader = view.findViewById(R.id.chatHeader);
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.rootChat), (root, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

            // Apply system bars top padding to header
            chatHeader.setPadding(
                    chatHeader.getPaddingLeft(),
                    systemBars.top + dpToPx(12),
                    chatHeader.getPaddingRight(),
                    chatHeader.getPaddingBottom()
            );

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
        // 首先检查关系状态
        RelationApi.Impl.getCurrentRelation(new ApiCallback<RelationApi.RelationStatusResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationStatusResponse relationData) {
                if (!isAdded()) return;

                isBound = relationData != null && "bound".equals(relationData.status);

                if (!isBound) {
                    renderUnboundState();
                    return;
                }

                // 已绑定，加载聊天会话
                loadChatSessionData();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) return;
                isBound = false;
                renderUnboundState();
            }
        });
    }

    private void loadChatSessionData() {
        ChatApi.Impl.getChatSession(new ApiCallback<ChatApi.ChatSessionResponse>() {
            @Override
            public void onSuccess(ChatApi.ChatSessionResponse data) {
                if (!isAdded()) return;
                if (data != null && data.conversationId != null) {
                    conversationId = data.conversationId;
                    partnerAvatarUrl = data.partnerAvatarUrl != null ? data.partnerAvatarUrl : "";
                    updateChatHeader(data);
                    loadMyAvatar();
                    loadMessages(null);
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

    private void renderUnboundState() {
        resetRiskDetectionState();
        // 隐藏聊天相关内容
        scrollMessages.setVisibility(View.GONE);
        layoutInputBar.setVisibility(View.GONE);

        // 显示未绑定状态
        tvPartnerName.setText("暂未绑定关系");
        tvCompanionDays.setText("当前没有可聊天的对象");
        tvPartnerMood.setText("绑定后可开始聊天");
        tvLastActive.setText("进入「我的」页面发起绑定申请");
        tvTodayDiary.setVisibility(View.GONE);

        // 设置默认头像
        ivPartnerAvatar.setImageResource(R.drawable.ic_user);
        ivPartnerAvatar.setImageTintList(ColorStateList.valueOf(
                requireContext().getColor(R.color.brand_primary_dark)));

        // 清空消息列表
        messages.clear();
        messages.add(new ChatMessage(TYPE_SYSTEM, CONTENT_TEXT,
                "请先在「我的」页面与对方建立绑定关系", getCurrentTime()));
        renderMessages();
    }

    private void renderBoundState() {
        scrollMessages.setVisibility(View.VISIBLE);
        layoutInputBar.setVisibility(View.VISIBLE);
    }

    private void updateChatHeader(ChatApi.ChatSessionResponse data) {
        if (data == null || !isBound) return;

        // 确保显示聊天界面
        renderBoundState();

        // Update partner name
        String name = !TextUtils.isEmpty(data.partnerNickname)
                ? data.partnerNickname
                : "TA";
        tvPartnerName.setText(name);

        // Update partner avatar
        AvatarImageLoader.load(ivPartnerAvatar, partnerAvatarUrl, 10);

        // Update companion days
        int days = data.companionDays > 0 ? data.companionDays : 0;
        tvCompanionDays.setText("💕 已相伴 " + days + " 天");

        // Update partner mood
        String moodEmoji = getMoodEmoji(data.partnerMoodCode);
        String moodText = !TextUtils.isEmpty(data.partnerMoodText) ? data.partnerMoodText : "平静";
        tvPartnerMood.setText(moodEmoji + " " + moodText);

        // Update last active time
        if (!TextUtils.isEmpty(data.partnerLastActiveAt)) {
            String timeAgo = formatTimeAgo(data.partnerLastActiveAt);
            tvLastActive.setText("最后活跃：" + timeAgo);
        } else {
            tvLastActive.setText("最后活跃：未知");
        }

        // Update today diary status
        if (data.partnerTodayDiary) {
            tvTodayDiary.setVisibility(View.VISIBLE);
            tvTodayDiary.setText("今日已写日记");
        } else {
            tvTodayDiary.setVisibility(View.GONE);
        }
    }

    private String getMoodEmoji(String moodCode) {
        if (TextUtils.isEmpty(moodCode)) return "😊";
        switch (moodCode) {
            case "calm": return "😌";
            case "love": return "🥰";
            case "sad": return "😢";
            case "wronged": return "🥺";
            case "angry": return "😤";
            case "tired": return "😣";
            case "anxious": return "😰";
            default: return "🙂";
        }
    }

    private String formatTimeAgo(String dateTimeStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateTimeStr);
            if (date == null) return "未知";

            long diffMs = System.currentTimeMillis() - date.getTime();
            long diffMinutes = diffMs / (1000 * 60);
            long diffHours = diffMinutes / 60;
            long diffDays = diffHours / 24;

            if (diffMinutes < 1) return "刚刚";
            if (diffMinutes < 60) return diffMinutes + "分钟前";
            if (diffHours < 24) return diffHours + "小时前";
            if (diffDays < 7) return diffDays + "天前";
            return "一周前";
        } catch (Exception e) {
            return "未知";
        }
    }

    private void loadMyAvatar() {
        com.example.nestchat.api.UserApi.Impl.getMineProfile(new ApiCallback<com.example.nestchat.api.UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(com.example.nestchat.api.UserApi.ProfileResponse data) {
                if (!isAdded()) return;
                if (data != null && data.avatarUrl != null) {
                    myAvatarUrl = data.avatarUrl;
                    renderMessages();
                }
            }

            @Override
            public void onError(ApiError error) {
                // Ignore avatar loading error
            }
        });
    }

    private void sendHeartbeat() {
        com.example.nestchat.api.UserApi.Impl.heartbeat(new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // Heartbeat sent successfully
            }

            @Override
            public void onError(ApiError error) {
                // Ignore heartbeat error
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
                    lastMessageTime = null;
                    resetRiskDetectionState();
                }

                if (data.items != null) {
                    // Server returns newest first, we need oldest first for display
                    List<ChatApi.MessageResponse> items = data.items;
                    for (int i = items.size() - 1; i >= 0; i--) {
                        messages.add(convertMessage(items.get(i)));
                        updateLastMessageTime(items.get(i));
                    }
                }

                nextCursor = data.nextCursor;
                hasMore = data.hasMore;
                renderMessages();
                scrollToBottom();

                // Start polling after initial load
                startPolling();
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
        } else if ("me".equals(msg.senderType)) {
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
            imageUri = MediaUrlResolver.resolve(msg.imageUrl);
        } else if ("voice".equals(msg.messageType)) {
            contentType = CONTENT_VOICE;
            audioPath = MediaUrlResolver.resolve(msg.voiceUrl);
            durationSeconds = msg.durationSeconds;
        }

        String time = "";
        if (msg.createdAt != null && msg.createdAt.length() >= 16) {
            time = msg.createdAt.substring(11, 16);
        }

        return new ChatMessage(
                type,
                contentType,
                content,
                time,
                audioPath,
                durationSeconds,
                imageUri,
                safeTrim(msg.messageId),
                safeTrim(msg.clientMessageId)
        );
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
        // 检查是否已绑定
        if (!isBound || conversationId == null) {
            Toast.makeText(requireContext(), "请先绑定关系后再开始聊天", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            etMessageInput.setError("请输入消息");
            etMessageInput.requestFocus();
            return;
        }

        onLocalUserTurnStarted();

        String clientMessageId = UUID.randomUUID().toString();

        // Optimistic: show message immediately
        messages.add(new ChatMessage(TYPE_RIGHT, CONTENT_TEXT, content, getCurrentTime(),
                "", 0, "", "", clientMessageId));
        etMessageInput.setText("");
        etMessageInput.clearFocus();
        appendLastMessage();

        ChatApi.SendTextMessageRequest req = new ChatApi.SendTextMessageRequest();
        req.conversationId = conversationId;
        req.content = content;
        req.clientMessageId = clientMessageId;

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

        onLocalUserTurnStarted();

        String clientMessageId = UUID.randomUUID().toString();

        // Show optimistically
        messages.add(ChatMessage.image(TYPE_RIGHT, getCurrentTime(), uri.toString(), clientMessageId));
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
                req.clientMessageId = clientMessageId;

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

        onLocalUserTurnStarted();

        String clientMessageId = UUID.randomUUID().toString();

        // Show optimistically
        messages.add(ChatMessage.voice(TYPE_RIGHT, getCurrentTime(), voicePath, durationSeconds, clientMessageId));
        appendLastMessage();

        // Upload and send
        uploadAndSendVoice(voicePath, durationSeconds, clientMessageId);
    }

    private void uploadAndSendVoice(String voicePath, int durationSeconds, String clientMessageId) {
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
                req.clientMessageId = clientMessageId;

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

        // Load avatar
        if (message.type == TYPE_LEFT) {
            ImageView ivPartnerAvatar = itemView.findViewById(R.id.ivPartnerAvatar);
            AvatarImageLoader.load(ivPartnerAvatar, partnerAvatarUrl, 8);
        } else if (message.type == TYPE_RIGHT) {
            ImageView ivSelfAvatar = itemView.findViewById(R.id.ivSelfAvatar);
            AvatarImageLoader.load(ivSelfAvatar, myAvatarUrl, 8);
        }

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
            AvatarImageLoader.loadContent(ivMessageImage, message.imageUri);
            ivMessageImage.setBackgroundResource
                    (message.type == TYPE_RIGHT
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
        String resolvedAudioPath = resolvePlayableAudioPath(audioPath);
        if (TextUtils.isEmpty(resolvedAudioPath)) {
            Toast.makeText(requireContext(), "语音文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        // For local files
        if (!resolvedAudioPath.startsWith("http") && !new File(resolvedAudioPath).exists()) {
            Toast.makeText(requireContext(), "语音文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resolvedAudioPath.equals(currentPlayingAudioPath) && mediaPlayer != null && mediaPlayer.isPlaying()) {
            releasePlayer(true);
            return;
        }

        releasePlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(resolvedAudioPath);
            mediaPlayer.prepare();
            currentPlayingAudioPath = resolvedAudioPath;
            renderMessages();
            mediaPlayer.setOnCompletionListener(mp -> releasePlayer(true));
            mediaPlayer.start();
        } catch (IOException e) {
            releasePlayer(true);
            Toast.makeText(requireContext(), "语音播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAiBottomSheet() {
        String originalText = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(originalText)) {
            Toast.makeText(requireContext(), "请先输入内容", Toast.LENGTH_SHORT).show();
            etMessageInput.requestFocus();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_ai_mode_sheet, null, false);

        contentView.findViewById(R.id.btnModeGentle).setOnClickListener(v -> {
            dialog.dismiss();
            optimizeMessage(originalText, "gentle");
        });

        contentView.findViewById(R.id.btnModeSincere).setOnClickListener(v -> {
            dialog.dismiss();
            optimizeMessage(originalText, "sincere");
        });

        contentView.findViewById(R.id.btnModeComfort).setOnClickListener(v -> {
            dialog.dismiss();
            optimizeMessage(originalText, "comfort");
        });

        dialog.setContentView(contentView);
        dialog.show();
    }

    private void optimizeMessage(String originalText, String mode) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_ai_result_sheet, null, false);

        TextView tvOriginalText = contentView.findViewById(R.id.tvOriginalText);
        TextView tvOptimizedText = contentView.findViewById(R.id.tvOptimizedText);
        TextView tvModeLabel = contentView.findViewById(R.id.tvModeLabel);
        TextView btnClose = contentView.findViewById(R.id.btnCloseAiResult);
        TextView btnCopy = contentView.findViewById(R.id.btnCopy);
        TextView btnReplace = contentView.findViewById(R.id.btnReplace);

        tvOriginalText.setText(originalText);
        tvOptimizedText.setText("正在优化...");
        tvModeLabel.setText(getModeLabel(mode));

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnCopy.setOnClickListener(v -> {
            String optimized = tvOptimizedText.getText().toString();
            copyToClipboard(optimized);
            Toast.makeText(requireContext(), "已复制", Toast.LENGTH_SHORT).show();
        });

        btnReplace.setOnClickListener(v -> {
            String optimized = tvOptimizedText.getText().toString();
            etMessageInput.setText(optimized);
            etMessageInput.setSelection(optimized.length());
            etMessageInput.requestFocus();
            dialog.dismiss();
        });

        dialog.setContentView(contentView);
        dialog.show();

        // Call AI API
        AiApi.Impl.optimizeMessage(originalText, mode, new ApiCallback<AiApi.OptimizeMessageResponse>() {
            @Override
            public void onSuccess(AiApi.OptimizeMessageResponse data) {
                if (!isAdded() || data == null) return;
                tvOptimizedText.setText(data.optimizedText != null ? data.optimizedText : originalText);
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) return;
                tvOptimizedText.setText(originalText);
                Toast.makeText(requireContext(), "优化失败: " + error.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getModeLabel(String mode) {
        switch (mode) {
            case "gentle": return "更温柔";
            case "sincere": return "更真诚";
            case "comfort": return "安慰一下";
            default: return "AI 优化";
        }
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("AI 优化文本", text);
        clipboard.setPrimaryClip(clip);
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

    private String resolvePlayableAudioPath(String audioPath) {
        if (TextUtils.isEmpty(audioPath)) {
            return "";
        }
        File localFile = new File(audioPath);
        if (localFile.exists()) {
            return audioPath;
        }
        return MediaUrlResolver.resolve(audioPath);
    }

    private void resetRiskDetectionState() {
        currentTurnOwner = "";
        currentPartnerTurnTexts.clear();
        currentPartnerTurnMessageKeys.clear();
        currentPartnerTurnKey = "";
        lastAnalyzedPartnerTurnKey = "";
        activeRiskRequestKey = "";
        hideRiskPrompt();
    }

    private void onLocalUserTurnStarted() {
        currentTurnOwner = "me";
        clearCurrentPartnerTurn();
        hideRiskPrompt();
    }

    private void startPartnerTurnIfNeeded() {
        if ("ta".equals(currentTurnOwner)) {
            return;
        }
        currentTurnOwner = "ta";
        clearCurrentPartnerTurn();
        hideRiskPrompt();
    }

    private void clearCurrentPartnerTurn() {
        currentPartnerTurnTexts.clear();
        currentPartnerTurnMessageKeys.clear();
        currentPartnerTurnKey = "";
        activeRiskRequestKey = "";
    }

    private boolean recordIncomingMessageForRisk(ChatApi.MessageResponse msg) {
        String senderType = safeTrim(msg.senderType);
        if ("me".equals(senderType)) {
            onLocalUserTurnStarted();
            return false;
        }
        if (!"ta".equals(senderType)) {
            return false;
        }

        startPartnerTurnIfNeeded();

        if (!"text".equals(safeTrim(msg.messageType))) {
            return false;
        }

        String content = safeTrim(msg.content);
        if (content.isEmpty()) {
            return false;
        }

        currentPartnerTurnTexts.add(content);
        currentPartnerTurnMessageKeys.add(resolveRiskMessageKey(msg));
        currentPartnerTurnKey = TextUtils.join("|", currentPartnerTurnMessageKeys);
        return true;
    }

    private String resolveRiskMessageKey(ChatApi.MessageResponse msg) {
        String messageId = safeTrim(msg.messageId);
        if (!messageId.isEmpty()) {
            return messageId;
        }
        String createdAt = safeTrim(msg.createdAt);
        if (!createdAt.isEmpty()) {
            return createdAt;
        }
        return safeTrim(msg.messageType) + ":" + safeTrim(msg.content);
    }

    private void triggerEmotionRiskAnalysisIfNeeded() {
        if (!"ta".equals(currentTurnOwner)) {
            return;
        }
        if (currentPartnerTurnTexts.isEmpty() || TextUtils.isEmpty(currentPartnerTurnKey)) {
            hideRiskPrompt();
            return;
        }
        if (currentPartnerTurnKey.equals(lastAnalyzedPartnerTurnKey)) {
            return;
        }

        final String requestKey = currentPartnerTurnKey;
        final ArrayList<String> messagesToAnalyze = new ArrayList<>(currentPartnerTurnTexts);
        activeRiskRequestKey = requestKey;

        ChatApi.Impl.analyzeEmotionRisk(messagesToAnalyze, new ApiCallback<ChatApi.EmotionRiskResponse>() {
            @Override
            public void onSuccess(ChatApi.EmotionRiskResponse data) {
                if (!isAdded()) {
                    return;
                }
                if (!requestKey.equals(activeRiskRequestKey)
                        || !requestKey.equals(currentPartnerTurnKey)
                        || !"ta".equals(currentTurnOwner)) {
                    return;
                }

                lastAnalyzedPartnerTurnKey = requestKey;
                activeRiskRequestKey = "";

                if (data != null && data.shouldPrompt && !TextUtils.isEmpty(safeTrim(data.message))) {
                    showRiskPrompt(
                            TextUtils.isEmpty(safeTrim(data.title)) ? "情绪风险提示" : safeTrim(data.title),
                            safeTrim(data.message)
                    );
                    return;
                }
                hideRiskPrompt();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded() || !requestKey.equals(activeRiskRequestKey)) {
                    return;
                }
                lastAnalyzedPartnerTurnKey = requestKey;
                activeRiskRequestKey = "";
                hideRiskPrompt();
            }
        });
    }

    private void showRiskPrompt(String title, String message) {
        if (layoutRiskTip == null) {
            return;
        }
        tvRiskTipTitle.setText(title);
        tvRiskTipMessage.setText(message);
        layoutRiskTip.setVisibility(View.VISIBLE);
    }

    private void hideRiskPrompt() {
        if (layoutRiskTip == null) {
            return;
        }
        layoutRiskTip.setVisibility(View.GONE);
        tvRiskTipTitle.setText("");
        tvRiskTipMessage.setText("");
    }

    private void startPolling() {
        if (!isBound || conversationId == null) {
            return;
        }
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void refreshNewMessages() {
        if (!isAdded() || !isBound || conversationId == null) {
            return;
        }

        ChatApi.Impl.getMessages(conversationId, null, 50, new ApiCallback<ChatApi.MessageListResponse>() {
            @Override
            public void onSuccess(ChatApi.MessageListResponse data) {
                if (!isAdded() || data == null || data.items == null) {
                    return;
                }

                boolean hasNewMessages = false;
                boolean shouldAnalyzeRisk = false;
                for (int i = data.items.size() - 1; i >= 0; i--) {
                    ChatApi.MessageResponse msg = data.items.get(i);
                    if (isNewMessage(msg)) {
                        ChatMessage converted = convertMessage(msg);
                        int existingIndex = findMessageIndexForServerMessage(converted);
                        if (existingIndex >= 0) {
                            messages.set(existingIndex, converted);
                        } else {
                            messages.add(converted);
                        }
                        hasNewMessages = true;
                        updateLastMessageTime(msg);
                        if (recordIncomingMessageForRisk(msg)) {
                            shouldAnalyzeRisk = true;
                        }
                    }
                }

                if (shouldAnalyzeRisk) {
                    triggerEmotionRiskAnalysisIfNeeded();
                }

                if (hasNewMessages) {
                    renderMessages();
                    scrollToBottom();
                }
            }

            @Override
            public void onError(ApiError error) {
                // Silently ignore polling errors
            }
        });
    }

    private boolean isNewMessage(ChatApi.MessageResponse msg) {
        if (msg.createdAt == null) {
            return false;
        }
        if (lastMessageTime == null) {
            return true;
        }
        return msg.createdAt.compareTo(lastMessageTime) > 0;
    }

    private void upsertIncomingMessage(ChatApi.MessageResponse msg) {
        if (msg == null) {
            return;
        }

        ChatMessage converted = convertMessage(msg);
        int existingIndex = findMessageIndexForServerMessage(converted);
        if (existingIndex >= 0) {
            messages.set(existingIndex, converted);
        } else {
            messages.add(converted);
        }

        updateLastMessageTime(msg);
        renderMessages();
        scrollToBottom();
    }

    private int findMessageIndexForServerMessage(ChatMessage incoming) {
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage existing = messages.get(i);
            if (!safeTrim(incoming.messageId).isEmpty()
                    && incoming.messageId.equals(existing.messageId)) {
                return i;
            }
            if (!safeTrim(incoming.clientMessageId).isEmpty()
                    && incoming.clientMessageId.equals(existing.clientMessageId)) {
                return i;
            }
        }
        return -1;
    }

    private void updateLastMessageTime(ChatApi.MessageResponse msg) {
        if (msg.createdAt != null && (lastMessageTime == null || msg.createdAt.compareTo(lastMessageTime) > 0)) {
            lastMessageTime = msg.createdAt;
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
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
        final String messageId;
        final String clientMessageId;

        ChatMessage(int type, int contentType, String content, String time) {
            this(type, contentType, content, time, "", 0, "", "", "");
        }

        ChatMessage(int type, int contentType, String content, String time,
                    String audioPath, int durationSeconds, String imageUri) {
            this(type, contentType, content, time, audioPath, durationSeconds, imageUri, "", "");
        }

        ChatMessage(int type, int contentType, String content, String time,
                    String audioPath, int durationSeconds, String imageUri,
                    String messageId, String clientMessageId) {
            this.type = type;
            this.contentType = contentType;
            this.content = content;
            this.time = time;
            this.audioPath = audioPath;
            this.durationSeconds = durationSeconds;
            this.imageUri = imageUri;
            this.messageId = messageId;
            this.clientMessageId = clientMessageId;
        }

        static ChatMessage voice(int type, String time, String audioPath, int durationSeconds,
                                 String clientMessageId) {
            return new ChatMessage(type, CONTENT_VOICE, "", time, audioPath, durationSeconds, "",
                    "", clientMessageId);
        }

        static ChatMessage image(int type, String time, String imageUri, String clientMessageId) {
            return new ChatMessage(type, CONTENT_IMAGE, "", time, "", 0, imageUri,
                    "", clientMessageId);
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
            bundle.putString("messageId", messageId);
            bundle.putString("clientMessageId", clientMessageId);
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
                    bundle.getString("imageUri", ""),
                    bundle.getString("messageId", ""),
                    bundle.getString("clientMessageId", "")
            );
        }
    }
}
