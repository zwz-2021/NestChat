package com.example.nestchat;

import android.os.Bundle;
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
import com.google.android.material.button.MaterialButton;

public class RelationManageActivity extends AppCompatActivity {

    private EditText etTargetPhone;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private TextView tvCurrentPhone;
    private MaterialButton btnBind;

    private String currentStatus = "none";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_relation_manage);

        applyWindowInsets();
        initViews();
        bindEvents();
        loadRelation();
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
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        tvCurrentPhone = findViewById(R.id.tvCurrentPhone);
        btnBind = findViewById(R.id.btnBind);
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());
        btnBind.setOnClickListener(v -> sendBindRequest());
        findViewById(R.id.itemCheckStatus).setOnClickListener(v -> loadRelation());
        findViewById(R.id.itemUnbind).setOnClickListener(v -> unbind());
    }

    private void loadRelation() {
        RelationApi.Impl.getCurrentRelation(new ApiCallback<RelationApi.RelationStatusResponse>() {
            @Override
            public void onSuccess(RelationApi.RelationStatusResponse data) {
                if (data == null || data.status == null) {
                    currentStatus = "none";
                    renderStatus(null);
                    return;
                }
                currentStatus = data.status;
                renderStatus(data);
            }

            @Override
            public void onError(ApiError error) {
                currentStatus = "none";
                renderStatus(null);
            }
        });
    }

    private void renderStatus(RelationApi.RelationStatusResponse data) {
        if ("bound".equals(currentStatus) && data != null) {
            tvStatusTitle.setText("当前已建立绑定关系");
            tvStatusDesc.setText("你们已相伴 " + data.companionDays + " 天");
            String partner = data.partnerNickname != null ? data.partnerNickname : data.partnerPhone;
            tvCurrentPhone.setText("绑定对象：" + (partner != null ? partner : ""));
            return;
        }

        if ("pending".equals(currentStatus)) {
            tvStatusTitle.setText("绑定申请已发送");
            tvStatusDesc.setText("等待对方确认后建立关系。");
            tvCurrentPhone.setText("绑定对象：等待中...");
            return;
        }

        tvStatusTitle.setText("当前没有绑定关系");
        tvStatusDesc.setText("输入对方手机号后即可发起绑定申请。");
        tvCurrentPhone.setText("绑定对象：未设置");
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
                btnBind.setEnabled(true);
                loadRelation();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(RelationManageActivity.this, error.message, Toast.LENGTH_SHORT).show();
                btnBind.setEnabled(true);
            }
        });
    }

    private void unbind() {
        if ("none".equals(currentStatus)) {
            Toast.makeText(this, "当前没有可解除的关系", Toast.LENGTH_SHORT).show();
            return;
        }

        RelationApi.Impl.unbind(new ApiCallback<RelationApi.SimpleResponse>() {
            @Override
            public void onSuccess(RelationApi.SimpleResponse data) {
                Toast.makeText(RelationManageActivity.this, "已解除绑定", Toast.LENGTH_SHORT).show();
                etTargetPhone.setText("");
                loadRelation();
            }

            @Override
            public void onError(ApiError error) {
                Toast.makeText(RelationManageActivity.this, error.message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
