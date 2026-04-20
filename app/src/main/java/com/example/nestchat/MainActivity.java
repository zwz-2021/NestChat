package com.example.nestchat;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.nestchat.fragment.ChatFragment;
import com.example.nestchat.fragment.DiaryFragment;
import com.example.nestchat.fragment.MineFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private TextView titleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            View bottomNavigation = findViewById(R.id.bottom_navigation);

            view.setPadding(systemBars.left, 0, systemBars.right, 0);

            ViewCompat.setPaddingRelative(findViewById(R.id.top_bar),
                    systemBars.left + dpToPx(20),
                    systemBars.top + dpToPx(16),
                    systemBars.right + dpToPx(20),
                    dpToPx(14));

            bottomNavigation.setPadding(
                    0,
                    dpToPx(2),
                    0,
                    systemBars.bottom + dpToPx(2)
            );
            bottomNavigation.setVisibility(isImeVisible ? View.GONE : View.VISIBLE);
            return insets;
        });

        titleView = findViewById(R.id.tv_title);
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_chat) {
                switchFragment(new ChatFragment(), getString(R.string.title_chat));
                return true;
            }
            if (item.getItemId() == R.id.nav_diary) {
                switchFragment(new DiaryFragment(), getString(R.string.title_diary));
                return true;
            }
            if (item.getItemId() == R.id.nav_mine) {
                switchFragment(new MineFragment(), getString(R.string.title_mine));
                return true;
            }
            return false;
        });

        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_chat);
        }
    }

    private void switchFragment(Fragment fragment, String title) {
        titleView.setText(title);
        // Hide title bar for chat fragment
        if (fragment instanceof ChatFragment) {
            findViewById(R.id.top_bar).setVisibility(View.GONE);
        } else {
            findViewById(R.id.top_bar).setVisibility(View.VISIBLE);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
