package com.example.nestchat.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.nestchat.api.UserApi;
import com.google.android.material.button.MaterialButton;

public class MineFragment extends Fragment {

    private MaterialButton btnMoodHappy;
    private MaterialButton btnMoodSad;
    private MaterialButton btnMoodTired;
    private TextView tvMood;
    private TextView tvMoodEmoji;
    private TextView tvNickname;

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

        tvMood = view.findViewById(R.id.tvMood);
        tvMoodEmoji = view.findViewById(R.id.tvMoodEmoji);
        btnMoodHappy = view.findViewById(R.id.btnMoodHappy);
        btnMoodSad = view.findViewById(R.id.btnMoodSad);
        btnMoodTired = view.findViewById(R.id.btnMoodTired);
        tvNickname = view.findViewById(R.id.tvNickname);

        bindClicks(view);
        loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        UserApi.Impl.getMineProfile(new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                if (data == null || !isAdded()) return;
                if (tvNickname != null) tvNickname.setText(data.nickname != null ? data.nickname : "");
                if (data.moodCode != null) {
                    currentMoodCode = data.moodCode;
                    applyMoodUi(data.moodCode, data.moodText);
                }
            }

            @Override
            public void onError(ApiError error) {
                // silently fail on profile load
            }
        });
    }

    private void applyMoodUi(String moodCode, String moodText) {
        switch (moodCode) {
            case "sad":
                updateMoodSelectionUi(btnMoodSad, moodText != null ? moodText : "难过");
                break;
            case "tired":
                updateMoodSelectionUi(btnMoodTired, moodText != null ? moodText : "疲惫");
                break;
            default:
                updateMoodSelectionUi(btnMoodHappy, moodText != null ? moodText : "开心");
                break;
        }
    }

    private void bindClicks(View view) {
        View cardProfile = view.findViewById(R.id.cardProfile);
        View cardRelation = view.findViewById(R.id.cardRelation);
        View btnManageRelation = view.findViewById(R.id.btnManageRelation);
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
        btnMoodSad.setOnClickListener(v -> updateMoodOnServer("sad"));
        btnMoodTired.setOnClickListener(v -> updateMoodOnServer("tired"));
    }

    private void updateMoodOnServer(String moodCode) {
        UserApi.UpdateMoodRequest req = new UserApi.UpdateMoodRequest();
        req.moodCode = moodCode;

        UserApi.Impl.updateMood(req, new ApiCallback<UserApi.ProfileResponse>() {
            @Override
            public void onSuccess(UserApi.ProfileResponse data) {
                if (data == null || !isAdded()) return;
                currentMoodCode = moodCode;
                applyMoodUi(data.moodCode, data.moodText);
            }

            @Override
            public void onError(ApiError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateMoodSelectionUi(MaterialButton selectedButton, String moodText) {
        btnMoodHappy.setSelected(false);
        btnMoodSad.setSelected(false);
        btnMoodTired.setSelected(false);

        btnMoodHappy.setTextColor(requireContext().getColor(R.color.text_secondary));
        btnMoodSad.setTextColor(requireContext().getColor(R.color.text_secondary));
        btnMoodTired.setTextColor(requireContext().getColor(R.color.text_secondary));

        btnMoodHappy.setIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.text_secondary)));
        btnMoodSad.setIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.text_secondary)));
        btnMoodTired.setIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.text_secondary)));

        selectedButton.setSelected(true);
        selectedButton.setTextColor(requireContext().getColor(R.color.brand_primary_dark));
        selectedButton.setIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.brand_primary_dark)));
        tvMood.setText(moodText);
        tvMoodEmoji.setText(getMoodEmoji(moodText));
    }

    private String getMoodEmoji(String moodText) {
        if ("难过".equals(moodText)) {
            return "😢";
        }
        if ("疲惫".equals(moodText)) {
            return "😣";
        }
        return "🙂";
    }
}
