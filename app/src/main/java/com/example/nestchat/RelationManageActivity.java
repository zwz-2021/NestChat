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

import com.google.android.material.button.MaterialButton;

public class RelationManageActivity extends AppCompatActivity {

    private static final String KEY_RELATION_STATUS = "relation_status";
    private static final String KEY_TARGET_PHONE = "target_phone";

    private static final int STATUS_NONE = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_BOUND = 2;

    private EditText etTargetPhone;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private TextView tvCurrentPhone;

    private int relationStatus = STATUS_NONE;
    private String targetPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_relation_manage);

        if (savedInstanceState != null) {
            relationStatus = savedInstanceState.getInt(KEY_RELATION_STATUS, STATUS_NONE);
            targetPhone = savedInstanceState.getString(KEY_TARGET_PHONE, "");
        }

        applyWindowInsets();
        initViews();
        bindEvents();
        renderStatus();
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
    }

    private void bindEvents() {
        ImageView ivBack = findViewById(R.id.ivBack);
        MaterialButton btnBind = findViewById(R.id.btnBind);

        ivBack.setOnClickListener(v -> finish());
        btnBind.setOnClickListener(v -> sendBindRequest());
        findViewById(R.id.itemCheckStatus).setOnClickListener(v -> checkStatus());
        findViewById(R.id.itemUnbind).setOnClickListener(v -> unbind());
    }

    private void sendBindRequest() {
        String phone = etTargetPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            etTargetPhone.setError("请输入对方手机号");
            etTargetPhone.requestFocus();
            return;
        }

        targetPhone = phone;
        relationStatus = STATUS_PENDING;
        renderStatus();
        Toast.makeText(this, "绑定申请已发送（演示）", Toast.LENGTH_SHORT).show();
    }

    private void checkStatus() {
        if (relationStatus == STATUS_NONE) {
            Toast.makeText(this, "当前没有绑定关系", Toast.LENGTH_SHORT).show();
            return;
        }

        if (relationStatus == STATUS_PENDING) {
            Toast.makeText(this, "当前状态：待对方确认", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "当前状态：已绑定", Toast.LENGTH_SHORT).show();
    }

    private void unbind() {
        if (relationStatus == STATUS_NONE) {
            Toast.makeText(this, "当前没有可解除的关系", Toast.LENGTH_SHORT).show();
            return;
        }

        relationStatus = STATUS_NONE;
        targetPhone = "";
        etTargetPhone.setText("");
        renderStatus();
        Toast.makeText(this, "已解除绑定（演示）", Toast.LENGTH_SHORT).show();
    }

    private void renderStatus() {
        if (relationStatus == STATUS_NONE) {
            tvStatusTitle.setText("当前没有绑定关系");
            tvStatusDesc.setText("输入对方手机号后即可发起绑定申请。");
            tvCurrentPhone.setText("绑定对象：未设置");
            return;
        }

        if (relationStatus == STATUS_PENDING) {
            tvStatusTitle.setText("绑定申请已发送");
            tvStatusDesc.setText("等待对方确认后建立关系。");
            tvCurrentPhone.setText("绑定对象：" + targetPhone);
            return;
        }

        tvStatusTitle.setText("当前已建立绑定关系");
        tvStatusDesc.setText("你们当前处于已绑定状态。");
        tvCurrentPhone.setText("绑定对象：" + targetPhone);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_RELATION_STATUS, relationStatus);
        outState.putString(KEY_TARGET_PHONE, targetPhone);
    }
}
