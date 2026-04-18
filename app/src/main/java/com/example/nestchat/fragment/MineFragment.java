package com.example.nestchat.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nestchat.AccountSecurityActivity;
import com.example.nestchat.EditProfileActivity;
import com.example.nestchat.R;
import com.example.nestchat.RelationManageActivity;
import com.google.android.material.button.MaterialButton;

public class MineFragment extends Fragment {

    private MaterialButton btnMoodHappy;
    private MaterialButton btnMoodSad;
    private MaterialButton btnMoodTired;
    private TextView tvMood;
    private TextView tvMoodEmoji;

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

        bindClicks(view);
        updateMoodSelection(btnMoodHappy, "开心");
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

        btnMoodHappy.setOnClickListener(v -> updateMoodSelection(btnMoodHappy, "开心"));
        btnMoodSad.setOnClickListener(v -> updateMoodSelection(btnMoodSad, "难过"));
        btnMoodTired.setOnClickListener(v -> updateMoodSelection(btnMoodTired, "疲惫"));
    }

    private void updateMoodSelection(MaterialButton selectedButton, String moodText) {
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
