package com.example.nestchat.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nestchat.AccountSecurityActivity;
import com.example.nestchat.EditProfileActivity;
import com.example.nestchat.R;
import com.example.nestchat.RelationManageActivity;
import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.RelationApi;
import com.example.nestchat.api.UserApi;
import com.example.nestchat.util.AvatarImageLoader;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MineFragment extends Fragment {

    private static final String STATUS_BOUND = "bound";
    private static final String STATUS_PENDING = "pending";
    private static final String TYPE_BIND = "bind";
    private static final String TYPE_UNBIND = "unbind";
    private static final String ROLE_INITIATOR = "initiator";
    private static final String ROLE_TARGET = "target";

    private MaterialButton btnMoodHappy;
    private MaterialButton btnMoodCalm;
    private MaterialButton btnMoodLove;
    private MaterialButton btnMoodSad;
    private MaterialButton btnMoodWronged;
    private MaterialButton btnMoodAngry;
    private MaterialButton btnMoodTired;
    private MaterialButton btnMoodAnxious;
    private MaterialButton btnManageRelation;
    private ImageView ivAvatar;
    private TextView tvMood;
    private TextView tvMoodEmoji;
    private TextView tvNickname;
    private TextView tvUserId;
    private TextView tvRelationTitle;
    private TextView tvRelationSubtitle;
    private ImageView ivRelationAvatar;

    private String currentMoodCode = "happy";

    public MineFragment() {
        // Required empty public constructor.
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvMood = view.findViewById(R.id.tvMood);
        tvMoodEmoji = view.findViewById(R.id.tvMoodEmoji);
        btnMoodHappy = view.findViewById(R.id.btnMoodHappy);
        btnMoodCalm = view.findViewById(R.id.btnMoodCalm);
        btnMoodLove = view.findViewById(R.id.btnMoodLove);
        btnMoodSad = view.findViewById(R.id.btnMoodSad);
        btnMoodWronged = view.findViewById(R.id.btnMoodWronged);
        btnMoodAngry = view.findViewById(R.id.btnMoodAngry);
        btnMoodTired = view.findViewById(R.id.btnMoodTired);
        btnMoodAnxious = view.findViewById(R.id.btnMoodAnxious);
        tvNickname = view.findViewById(R.id.tvNickname);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvRelationTitle = view.findViewById(R.id.tvRelationTitle);
        tvRelationSubtitle = view.findViewById(R.id.tvRelationSubtitle);
        ivRelationAvatar = view.findViewById(R.id.ivRelationAvatar);
        btnManageRelation = view.findViewById(R.id.btnManageRelation);

        bindClicks(view);
        loadProfile();
        loadRelationCard();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadRelationCard();
    }

    private void loadProfile() {
        UserApi.Impl.getMineProfile(new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                if (data == null || !isAdded()) {
                    return;
                }
                if (tvNickname != null) {
                    tvNickname.setText(data.nickname != null ? data.nickname : "");
                }
                if (tvUserId != null) {
                    tvUserId.setText(buildPhoneText(data.account));
                }
                if (ivAvatar != null) {
                    AvatarImageLoader.load(ivAvatar, data.avatarUrl, 14);
                }
                currentMoodCode = normalizeMoodCode(data.moodCode);
                applyMoodUi(currentMoodCode, data.moodText);
            }

            @Override
            public void onError(ApiError error) {
                // Silently fail on profile load.
            }
        });
    }

    private void loadRelationCard() {
        RelationApi.Impl.getCurrentRelation(new ApiCallback<RelationApi.RelationStatusResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationStatusResponse data) {
                if (!isAdded()) {
                    return;
                }
                if (data != null && STATUS_BOUND.equals(data.status)) {
                    if (isIncomingUnbindRequest(data)) {
                        renderIncomingUnbindRequest(data);
                        return;
                    }
                    if (isOutgoingUnbindPending(data)) {
                        renderOutgoingUnbindPending(data);
                        return;
                    }
                    renderBoundRelation(data);
                    return;
                }
                if (data != null && STATUS_PENDING.equals(data.status)) {
                    loadPendingRelationCard();
                    return;
                }
                renderNoRelation();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) {
                    return;
                }
                renderNoRelation();
            }
        });
    }

    private void loadPendingRelationCard() {
        RelationApi.Impl.getRelationApplications(new ApiCallback<RelationApi.RelationApplicationsResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationApplicationsResponse data) {
                if (!isAdded()) {
                    return;
                }
                List<RelationApi.RelationApplication> items = data != null ? data.items : null;
                RelationApi.RelationApplication incomingBindRequest = findFirstApplicationByType(items, TYPE_BIND);
                if (incomingBindRequest != null) {
                    renderIncomingRequest(incomingBindRequest);
                    return;
                }
                renderOutgoingPending();
            }

            @Override
            public void onError(ApiError error) {
                if (!isAdded()) {
                    return;
                }
                renderOutgoingPending();
            }
        });
    }

    private void renderBoundRelation(RelationApi.RelationStatusResponse relation) {
        String partnerName = resolvePartnerName(relation);
        long companionDays = Math.max(0, relation.companionDays);
        tvRelationTitle.setText("已与" + partnerName + "建立关系");
        tvRelationSubtitle.setText("已相伴 " + companionDays + " 天");
        btnManageRelation.setText("管理关系");
        AvatarImageLoader.load(ivRelationAvatar, relation.partnerAvatarUrl, 10);
    }

    private void renderIncomingRequest(RelationApi.RelationApplication application) {
        String initiatorPhone = application != null ? safeTrim(application.initiatorPhone) : "";
        String displayName = initiatorPhone.isEmpty() ? "对方" : initiatorPhone;
        tvRelationTitle.setText("收到绑定请求");
        tvRelationSubtitle.setText(displayName + " 想与你建立绑定关系");
        btnManageRelation.setText("去处理");
        AvatarImageLoader.load(ivRelationAvatar, "", 10);
    }

    private void renderOutgoingPending() {
        tvRelationTitle.setText("绑定申请处理中");
        tvRelationSubtitle.setText("已发出绑定申请，等待对方确认。");
        btnManageRelation.setText("查看状态");
        AvatarImageLoader.load(ivRelationAvatar, "", 10);
    }

    private void renderNoRelation() {
        tvRelationTitle.setText("当前未绑定关系");
        tvRelationSubtitle.setText("输入对方手机号后发送绑定申请。");
        btnManageRelation.setText("去绑定");
        AvatarImageLoader.load(ivRelationAvatar, "", 10);
    }

    private void renderIncomingUnbindRequest(RelationApi.RelationStatusResponse relation) {
        String partnerName = resolvePartnerName(relation);
        tvRelationTitle.setText("收到解绑申请");
        tvRelationSubtitle.setText(partnerName + " 希望解除当前绑定关系");
        btnManageRelation.setText("去处理");
        AvatarImageLoader.load(ivRelationAvatar, relation.partnerAvatarUrl, 10);
    }

    private void renderOutgoingUnbindPending(RelationApi.RelationStatusResponse relation) {
        String partnerName = resolvePartnerName(relation);
        tvRelationTitle.setText("解绑申请处理中");
        tvRelationSubtitle.setText("已向 " + partnerName + " 发起解绑申请，等待对方确认");
        btnManageRelation.setText("查看状态");
        AvatarImageLoader.load(ivRelationAvatar, relation.partnerAvatarUrl, 10);
    }

    private RelationApi.RelationApplication findFirstApplicationByType(
            List<RelationApi.RelationApplication> items, String type) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        for (RelationApi.RelationApplication item : items) {
            if (item != null && type.equals(safeTrim(item.type))) {
                return item;
            }
        }
        return null;
    }

    private boolean isIncomingUnbindRequest(RelationApi.RelationStatusResponse relation) {
        return relation != null
                && TYPE_UNBIND.equals(safeTrim(relation.pendingApplicationType))
                && ROLE_TARGET.equals(safeTrim(relation.pendingApplicationRole));
    }

    private boolean isOutgoingUnbindPending(RelationApi.RelationStatusResponse relation) {
        return relation != null
                && TYPE_UNBIND.equals(safeTrim(relation.pendingApplicationType))
                && ROLE_INITIATOR.equals(safeTrim(relation.pendingApplicationRole));
    }

    private String resolvePartnerName(RelationApi.RelationStatusResponse relation) {
        if (relation == null) {
            return "对方";
        }
        String partnerRemark = safeTrim(relation.partnerRemark);
        if (!partnerRemark.isEmpty()) {
            return partnerRemark;
        }
        String partnerNickname = safeTrim(relation.partnerNickname);
        if (!partnerNickname.isEmpty()) {
            return partnerNickname;
        }
        String partnerPhone = safeTrim(relation.partnerPhone);
        if (!partnerPhone.isEmpty()) {
            return partnerPhone;
        }
        return "对方";
    }

    private void applyMoodUi(String moodCode, String moodText) {
        String normalizedMoodCode = normalizeMoodCode(moodCode);
        String displayMoodText = resolveMoodText(normalizedMoodCode, moodText);
        switch (normalizedMoodCode) {
            case "calm":
                updateMoodSelectionUi(btnMoodCalm, normalizedMoodCode, displayMoodText);
                break;
            case "love":
                updateMoodSelectionUi(btnMoodLove, normalizedMoodCode, displayMoodText);
                break;
            case "sad":
                updateMoodSelectionUi(btnMoodSad, normalizedMoodCode, displayMoodText);
                break;
            case "wronged":
                updateMoodSelectionUi(btnMoodWronged, normalizedMoodCode, displayMoodText);
                break;
            case "angry":
                updateMoodSelectionUi(btnMoodAngry, normalizedMoodCode, displayMoodText);
                break;
            case "tired":
                updateMoodSelectionUi(btnMoodTired, normalizedMoodCode, displayMoodText);
                break;
            case "anxious":
                updateMoodSelectionUi(btnMoodAnxious, normalizedMoodCode, displayMoodText);
                break;
            default:
                updateMoodSelectionUi(btnMoodHappy, normalizedMoodCode, displayMoodText);
                break;
        }
    }

    private void bindClicks(View view) {
        View cardProfile = view.findViewById(R.id.cardProfile);
        View cardRelation = view.findViewById(R.id.cardRelation);
        View itemAccountSecurity = view.findViewById(R.id.itemAccountSecurity);

        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class))
        );
        cardRelation.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RelationManageActivity.class))
        );
        btnManageRelation.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RelationManageActivity.class))
        );
        itemAccountSecurity.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AccountSecurityActivity.class))
        );

        btnMoodHappy.setOnClickListener(v -> updateMoodOnServer("happy"));
        btnMoodCalm.setOnClickListener(v -> updateMoodOnServer("calm"));
        btnMoodLove.setOnClickListener(v -> updateMoodOnServer("love"));
        btnMoodSad.setOnClickListener(v -> updateMoodOnServer("sad"));
        btnMoodWronged.setOnClickListener(v -> updateMoodOnServer("wronged"));
        btnMoodAngry.setOnClickListener(v -> updateMoodOnServer("angry"));
        btnMoodTired.setOnClickListener(v -> updateMoodOnServer("tired"));
        btnMoodAnxious.setOnClickListener(v -> updateMoodOnServer("anxious"));
    }

    private void updateMoodOnServer(String moodCode) {
        UserApi.UpdateMoodRequest req = new UserApi.UpdateMoodRequest();
        req.moodCode = moodCode;

        UserApi.Impl.updateMood(req, new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                if (data == null || !isAdded()) {
                    return;
                }
                currentMoodCode = normalizeMoodCode(data.moodCode);
                applyMoodUi(currentMoodCode, data.moodText);
            }

            @Override
            public void onError(ApiError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateMoodSelectionUi(MaterialButton selectedButton, String moodCode, String moodText) {
        resetMoodButton(btnMoodHappy);
        resetMoodButton(btnMoodCalm);
        resetMoodButton(btnMoodLove);
        resetMoodButton(btnMoodSad);
        resetMoodButton(btnMoodWronged);
        resetMoodButton(btnMoodAngry);
        resetMoodButton(btnMoodTired);
        resetMoodButton(btnMoodAnxious);

        selectedButton.setSelected(true);
        selectedButton.setTextColor(requireContext().getColor(R.color.brand_primary_dark));
        selectedButton.setIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.brand_primary_dark)));
        tvMood.setText(moodText);
        tvMoodEmoji.setText(resolveMoodEmoji(moodCode));
    }

    private void resetMoodButton(MaterialButton button) {
        button.setSelected(false);
        button.setTextColor(requireContext().getColor(R.color.text_secondary));
        button.setIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.text_secondary)));
    }

    private String resolveMoodEmoji(String moodCode) {
        switch (moodCode) {
            case "calm":
                return "😌";
            case "love":
                return "🥰";
            case "sad":
                return "😢";
            case "wronged":
                return "🥺";
            case "angry":
                return "😤";
            case "tired":
                return "😣";
            case "anxious":
                return "😰";
            default:
                return "🙂";
        }
    }

    private String buildPhoneText(String account) {
        String value = safeTrim(account);
        if (value.isEmpty()) {
            return "手机号: 未设置";
        }
        return "手机号: " + value;
    }

    private String normalizeMoodCode(String moodCode) {
        String value = safeTrim(moodCode);
        if (value.isEmpty()) {
            return "happy";
        }
        return value;
    }

    private String resolveMoodText(String moodCode, String moodText) {
        String value = safeTrim(moodText);
        if (!value.isEmpty()) {
            return value;
        }
        switch (moodCode) {
            case "calm":
                return "平静";
            case "love":
                return "心动";
            case "sad":
                return "难过";
            case "wronged":
                return "委屈";
            case "angry":
                return "生气";
            case "tired":
                return "疲惫";
            case "anxious":
                return "焦虑";
            default:
                return "开心";
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
