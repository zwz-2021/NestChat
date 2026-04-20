package com.example.nestchat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nestchat.api.ApiCallback;
import com.example.nestchat.api.ApiError;
import com.example.nestchat.api.RelationApi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class RelationManageActivity extends AppCompatActivity {

    private static final String TYPE_BIND = "bind";
    private static final String TYPE_UNBIND = "unbind";
    private static final String ROLE_INITIATOR = "initiator";

    private EditText etTargetPhone;
    private TextView tvStatusChip;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private TextView tvCurrentPhone;
    private TextView tvIncomingRequestTitle;
    private TextView tvIncomingRequestDesc;
    private TextView tvPendingProgressDesc;
    private TextView tvArchivePartner;
    private TextView tvArchiveBoundAt;
    private TextView tvArchiveDays;
    private TextView tvArchiveRemark;
    private MaterialButton btnBind;
    private MaterialButton btnAcceptRequest;
    private MaterialButton btnRejectRequest;
    private View cardIncomingRequest;
    private View cardPendingProgress;
    private View cardRelationArchive;
    private View cardBindRequest;
    private View cardBindGuide;
    private View itemUnbind;
    private View dividerUnbind;

    private String currentStatus = "none";
    private RelationApi.RelationStatusResponse currentRelation;
    private RelationApi.RelationApplication currentIncomingApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_relation_manage);

        applyWindowInsets();
        initViews();
        bindEvents();
        loadRelationData();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        etTargetPhone = findViewById(R.id.etTargetPhone);
        tvStatusChip = findViewById(R.id.tvStatusChip);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        tvCurrentPhone = findViewById(R.id.tvCurrentPhone);
        tvIncomingRequestTitle = findViewById(R.id.tvIncomingRequestTitle);
        tvIncomingRequestDesc = findViewById(R.id.tvIncomingRequestDesc);
        tvPendingProgressDesc = findViewById(R.id.tvPendingProgressDesc);
        tvArchivePartner = findViewById(R.id.tvArchivePartner);
        tvArchiveBoundAt = findViewById(R.id.tvArchiveBoundAt);
        tvArchiveDays = findViewById(R.id.tvArchiveDays);
        tvArchiveRemark = findViewById(R.id.tvArchiveRemark);
        btnBind = findViewById(R.id.btnBind);
        btnAcceptRequest = findViewById(R.id.btnAcceptRequest);
        btnRejectRequest = findViewById(R.id.btnRejectRequest);
        cardIncomingRequest = findViewById(R.id.cardIncomingRequest);
        cardPendingProgress = findViewById(R.id.cardPendingProgress);
        cardRelationArchive = findViewById(R.id.cardRelationArchive);
        cardBindRequest = findViewById(R.id.cardBindRequest);
        cardBindGuide = findViewById(R.id.cardBindGuide);
        itemUnbind = findViewById(R.id.itemUnbind);
        dividerUnbind = findViewById(R.id.dividerUnbind);
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());
        btnBind.setOnClickListener(v -> sendBindRequest());
        btnAcceptRequest.setOnClickListener(v -> acceptIncomingRequest());
        btnRejectRequest.setOnClickListener(v -> rejectIncomingRequest());
        findViewById(R.id.itemCheckStatus).setOnClickListener(v -> showRelationStatusSheet());
        itemUnbind.setOnClickListener(v -> requestUnbind());
    }

    private void loadRelationData() {
        currentIncomingApplication = null;
        loadRelation();
        loadIncomingApplications();
    }

    private void loadRelation() {
        RelationApi.Impl.getCurrentRelation(new ApiCallback<RelationApi.RelationStatusResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationStatusResponse data) {
                currentRelation = data;
                currentStatus = data != null && data.status != null ? data.status : "none";
                renderStatus();
            }

            @Override
            public void onError(ApiError error) {
                currentRelation = null;
                currentStatus = "none";
                renderStatus();
            }
        });
    }

    private void loadIncomingApplications() {
        RelationApi.Impl.getRelationApplications(new ApiCallback<RelationApi.RelationApplicationsResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationApplicationsResponse data) {
                List<RelationApi.RelationApplication> items = data != null ? data.items : null;
                currentIncomingApplication = items != null && !items.isEmpty() ? items.get(0) : null;
                renderStatus();
            }

            @Override
            public void onError(ApiError error) {
                currentIncomingApplication = null;
                renderStatus();
            }
        });
    }

    private void renderStatus() {
        hideDynamicCards();
        setUnbindEntryVisible(false);

        if (isIncomingUnbindRequest()) {
            renderIncomingUnbindState();
            return;
        }

        if (isOutgoingUnbindPending()) {
            renderOutgoingUnbindPendingState();
            return;
        }

        if ("bound".equals(currentStatus) && currentRelation != null) {
            renderBoundState();
            return;
        }

        if (isIncomingBindRequest()) {
            renderIncomingBindState();
            return;
        }

        if ("pending".equals(currentStatus)) {
            renderOutgoingBindPendingState();
            return;
        }

        renderNoRelationState();
    }

    private void renderBoundState() {
        String partner = resolvePartnerName(currentRelation);
        long companionDays = Math.max(0, currentRelation.companionDays);

        tvStatusChip.setText("已绑定");
        tvStatusTitle.setText("你们已经建立稳定关系");
        tvStatusDesc.setText("当前可以查看关系档案、状态详情以及发起解绑申请。");
        tvCurrentPhone.setText("绑定对象：" + partner);

        showRelationArchive(partner, companionDays);
        setUnbindEntryVisible(true);
    }

    private void renderIncomingBindState() {
        String initiatorPhone = safeTrim(currentIncomingApplication.initiatorPhone);
        String displayName = initiatorPhone.isEmpty() ? "对方" : initiatorPhone;

        tvStatusChip.setText("收到绑定请求");
        tvStatusTitle.setText("有人想与你建立绑定关系");
        tvStatusDesc.setText("你可以先查看请求详情，再决定是否接受。");
        tvCurrentPhone.setText("申请人：" + displayName);

        cardIncomingRequest.setVisibility(View.VISIBLE);
        tvIncomingRequestTitle.setText("待处理绑定请求");
        tvIncomingRequestDesc.setText(displayName + " 发来绑定申请\n申请时间：" + resolveApplicationTime(currentIncomingApplication.createdAt));
        btnAcceptRequest.setText("接受绑定");
        btnRejectRequest.setText("拒绝");
    }

    private void renderOutgoingBindPendingState() {
        tvStatusChip.setText("绑定申请处理中");
        tvStatusTitle.setText("绑定申请已经发出");
        tvStatusDesc.setText("现在只需要等待对方确认，确认后系统会自动建立关系。");
        tvCurrentPhone.setText("当前流程：等待对方接受");

        cardPendingProgress.setVisibility(View.VISIBLE);
        tvPendingProgressDesc.setText("你的绑定申请已进入等待确认阶段，对方处理后会同步更新当前页面。");
    }

    private void renderIncomingUnbindState() {
        String partner = resolvePartnerName(currentRelation);
        long companionDays = Math.max(0, currentRelation.companionDays);

        tvStatusChip.setText("收到解绑请求");
        tvStatusTitle.setText(partner + " 希望解除当前绑定关系");
        tvStatusDesc.setText("在你同意之前，当前关系会继续保持。");
        tvCurrentPhone.setText("当前关系：仍然有效");

        showRelationArchive(partner, companionDays);

        cardIncomingRequest.setVisibility(View.VISIBLE);
        tvIncomingRequestTitle.setText("待处理解绑请求");
        tvIncomingRequestDesc.setText(partner + " 发起了解绑申请\n申请时间：" + resolveApplicationTime(currentIncomingApplication.createdAt));
        btnAcceptRequest.setText("同意解绑");
        btnRejectRequest.setText("保留关系");
    }

    private void renderOutgoingUnbindPendingState() {
        String partner = resolvePartnerName(currentRelation);
        long companionDays = Math.max(0, currentRelation.companionDays);

        tvStatusChip.setText("解绑申请处理中");
        tvStatusTitle.setText("你已发起解绑申请");
        tvStatusDesc.setText("当前关系仍然有效，需要等待对方同意后才会真正解除。");
        tvCurrentPhone.setText("待确认对象：" + partner);

        showRelationArchive(partner, companionDays);

        cardPendingProgress.setVisibility(View.VISIBLE);
        tvPendingProgressDesc.setText("你的解绑申请已发送，对方同意前不会解除当前绑定关系。");
    }

    private void renderNoRelationState() {
        tvStatusChip.setText("未绑定");
        tvStatusTitle.setText("当前还没有建立绑定关系");
        tvStatusDesc.setText("先发起申请，再等待对方确认，最后建立关系。");
        tvCurrentPhone.setText("绑定对象：未设置");

        cardBindRequest.setVisibility(View.VISIBLE);
        cardBindGuide.setVisibility(View.VISIBLE);
        updateBindFormState(true, "发送绑定申请");
    }

    private void showRelationArchive(String partner, long companionDays) {
        cardRelationArchive.setVisibility(View.VISIBLE);
        tvArchivePartner.setText("绑定对象：" + partner);
        tvArchiveBoundAt.setText("绑定时间：" + resolveBoundAt(currentRelation != null ? currentRelation.boundAt : null));
        tvArchiveDays.setText("相伴天数：" + companionDays + " 天");
        tvArchiveRemark.setText("备注：" + resolveRemark(currentRelation != null ? currentRelation.partnerRemark : null));
    }

    private void hideDynamicCards() {
        cardIncomingRequest.setVisibility(View.GONE);
        cardPendingProgress.setVisibility(View.GONE);
        cardRelationArchive.setVisibility(View.GONE);
        cardBindRequest.setVisibility(View.GONE);
        cardBindGuide.setVisibility(View.GONE);
    }

    private void setUnbindEntryVisible(boolean visible) {
        itemUnbind.setVisibility(visible ? View.VISIBLE : View.GONE);
        dividerUnbind.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isIncomingBindRequest() {
        return currentIncomingApplication != null && TYPE_BIND.equals(currentIncomingApplication.type);
    }

    private boolean isIncomingUnbindRequest() {
        return currentIncomingApplication != null
                && TYPE_UNBIND.equals(currentIncomingApplication.type)
                && "bound".equals(currentStatus);
    }

    private boolean isOutgoingUnbindPending() {
        return currentRelation != null
                && "bound".equals(currentStatus)
                && TYPE_UNBIND.equals(safeTrim(currentRelation.pendingApplicationType))
                && ROLE_INITIATOR.equals(safeTrim(currentRelation.pendingApplicationRole));
    }

    private void updateBindFormState(boolean enabled, String buttonText) {
        etTargetPhone.setEnabled(enabled);
        btnBind.setEnabled(enabled);
        btnBind.setText(buttonText);
    }

    private void showRelationStatusSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View contentView = LayoutInflater.from(this)
                .inflate(R.layout.layout_relation_status_dialog, null, false);

        TextView tvDialogTitle = contentView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogStatusChip = contentView.findViewById(R.id.tvDialogStatusChip);
        TextView tvDialogPrimary = contentView.findViewById(R.id.tvDialogPrimary);
        TextView tvDialogSecondary = contentView.findViewById(R.id.tvDialogSecondary);
        TextView tvDialogHint = contentView.findViewById(R.id.tvDialogHint);
        MaterialButton btnCloseDialog = contentView.findViewById(R.id.btnCloseRelationStatusDialog);

        tvDialogTitle.setText("绑定状态详情");

        if (isIncomingUnbindRequest()) {
            String partner = resolvePartnerName(currentRelation);
            tvDialogStatusChip.setText("收到解绑请求");
            tvDialogPrimary.setText("当前关系还有效，但对方发起了解绑申请。");
            tvDialogSecondary.setText("申请对象：" + partner + "\n申请时间：" + resolveApplicationTime(currentIncomingApplication.createdAt));
            tvDialogHint.setText("下一步：你可以同意解绑，也可以拒绝并继续保持当前绑定关系。");
        } else if (isOutgoingUnbindPending()) {
            String partner = resolvePartnerName(currentRelation);
            tvDialogStatusChip.setText("解绑申请处理中");
            tvDialogPrimary.setText("你发起的解绑申请正在等待对方确认。");
            tvDialogSecondary.setText("待确认对象：" + partner + "\n申请时间：" + resolveApplicationTime(currentRelation.pendingApplicationCreatedAt));
            tvDialogHint.setText("在对方同意前，系统不会真正解除当前绑定关系。");
        } else if ("bound".equals(currentStatus) && currentRelation != null) {
            String partner = resolvePartnerName(currentRelation);
            long companionDays = Math.max(0, currentRelation.companionDays);
            tvDialogStatusChip.setText("已绑定");
            tvDialogPrimary.setText("当前关系已经建立，可以继续维护这段关系。");
            tvDialogSecondary.setText("绑定对象：" + partner + "\n绑定时间：" + resolveBoundAt(currentRelation.boundAt));
            tvDialogHint.setText("相伴天数：" + companionDays + " 天\n备注：" + resolveRemark(currentRelation.partnerRemark));
        } else if (isIncomingBindRequest()) {
            String initiatorPhone = safeTrim(currentIncomingApplication.initiatorPhone);
            tvDialogStatusChip.setText("收到绑定请求");
            tvDialogPrimary.setText("这是一条等待你确认的绑定请求。");
            tvDialogSecondary.setText("申请人：" + (initiatorPhone.isEmpty() ? "对方" : initiatorPhone)
                    + "\n申请时间：" + resolveApplicationTime(currentIncomingApplication.createdAt));
            tvDialogHint.setText("下一步：你可以在当前页面直接接受或拒绝这条请求。");
        } else if ("pending".equals(currentStatus)) {
            tvDialogStatusChip.setText("绑定申请处理中");
            tvDialogPrimary.setText("你发出的绑定申请还在等待对方处理。");
            tvDialogSecondary.setText("当前阶段：等待对方确认");
            tvDialogHint.setText("下一步：对方接受后，系统会自动建立绑定关系并更新本页。");
        } else {
            tvDialogStatusChip.setText("未绑定");
            tvDialogPrimary.setText("当前还没有任何绑定关系。");
            tvDialogSecondary.setText("当前阶段：可发起新的绑定申请");
            tvDialogHint.setText("下一步：输入对方手机号，发送申请并等待对方确认。");
        }

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(contentView);
        dialog.show();
    }

    private void sendBindRequest() {
        String phone = etTargetPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            etTargetPhone.setError("请输入对方手机号");
            etTargetPhone.requestFocus();
            return;
        }

        btnBind.setEnabled(false);

        RelationApi.CreateBindRequest req = new RelationApi.CreateBindRequest();
        req.targetPhone = phone;

        RelationApi.Impl.createBindRequest(req, new ApiCallback<RelationApi.RelationStatusResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationStatusResponse data) {
                Toast.makeText(RelationManageActivity.this, "绑定申请已发送", Toast.LENGTH_SHORT).show();
                etTargetPhone.setText("");
                loadRelationData();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(RelationManageActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnBind.setEnabled(true);
                btnBind.setText("发送绑定申请");
            }
        });
    }

    private void requestUnbind() {
        if (!"bound".equals(currentStatus) || currentRelation == null) {
            Toast.makeText(this, "当前没有可申请解绑的关系", Toast.LENGTH_SHORT).show();
            return;
        }

        itemUnbind.setEnabled(false);
        RelationApi.Impl.requestUnbind(new ApiCallback<RelationApi.RelationStatusResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationStatusResponse data) {
                Toast.makeText(RelationManageActivity.this, "解绑申请已发送", Toast.LENGTH_SHORT).show();
                itemUnbind.setEnabled(true);
                loadRelationData();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(RelationManageActivity.this, error.message, Toast.LENGTH_SHORT).show();
                itemUnbind.setEnabled(true);
            }
        });
    }

    private void acceptIncomingRequest() {
        if (currentIncomingApplication == null || safeTrim(currentIncomingApplication.applicationId).isEmpty()) {
            Toast.makeText(this, "当前没有可处理的请求", Toast.LENGTH_SHORT).show();
            return;
        }

        String successText = TYPE_UNBIND.equals(currentIncomingApplication.type) ? "已同意解绑申请" : "已接受绑定请求";
        setRequestButtonsEnabled(false);
        RelationApi.Impl.acceptRelation(currentIncomingApplication.applicationId,
                new ApiCallback<RelationApi.RelationStatusResponse>() {
                    @Override
                    public void onSuccess(RelationApi.RelationStatusResponse data) {
                        Toast.makeText(RelationManageActivity.this, successText, Toast.LENGTH_SHORT).show();
                        setRequestButtonsEnabled(true);
                        loadRelationData();
                    }

                    @Override
                    public void onError(ApiError error) {
                        Toast.makeText(RelationManageActivity.this, error.message, Toast.LENGTH_SHORT).show();
                        setRequestButtonsEnabled(true);
                    }
                });
    }

    private void rejectIncomingRequest() {
        if (currentIncomingApplication == null || safeTrim(currentIncomingApplication.applicationId).isEmpty()) {
            Toast.makeText(this, "当前没有可处理的请求", Toast.LENGTH_SHORT).show();
            return;
        }

        String successText = TYPE_UNBIND.equals(currentIncomingApplication.type) ? "已拒绝解绑申请" : "已拒绝绑定请求";
        setRequestButtonsEnabled(false);
        RelationApi.Impl.rejectRelation(currentIncomingApplication.applicationId,
                new ApiCallback<RelationApi.RelationStatusResponse>() {
                    @Override
                    public void onSuccess(RelationApi.RelationStatusResponse data) {
                        Toast.makeText(RelationManageActivity.this, successText, Toast.LENGTH_SHORT).show();
                        setRequestButtonsEnabled(true);
                        loadRelationData();
                    }

                    @Override
                    public void onError(ApiError error) {
                        Toast.makeText(RelationManageActivity.this, error.message, Toast.LENGTH_SHORT).show();
                        setRequestButtonsEnabled(true);
                    }
                });
    }

    private void setRequestButtonsEnabled(boolean enabled) {
        btnAcceptRequest.setEnabled(enabled);
        btnRejectRequest.setEnabled(enabled);
    }

    private String resolvePartnerName(RelationApi.RelationStatusResponse relation) {
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

    private String resolveRemark(String remark) {
        String value = safeTrim(remark);
        return value.isEmpty() ? "未设置" : value;
    }

    private String resolveBoundAt(String boundAt) {
        String value = safeTrim(boundAt);
        return value.isEmpty() ? "未知" : value;
    }

    private String resolveApplicationTime(String createdAt) {
        String value = safeTrim(createdAt);
        return value.isEmpty() ? "未知" : value;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
