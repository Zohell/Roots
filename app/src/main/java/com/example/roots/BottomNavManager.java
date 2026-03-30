package com.example.roots;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BottomNavManager {
    public static void setup(Activity activity, String currentTab) {
        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navUpload = activity.findViewById(R.id.navUpload);
        LinearLayout navProfile = activity.findViewById(R.id.navProfile);
        LinearLayout navSettings = activity.findViewById(R.id.navSettings);

        if (navHome == null || navUpload == null || navProfile == null || navSettings == null) return;

        navHome.setOnClickListener(v -> { if (!currentTab.equals("feed")) { activity.startActivity(new Intent(activity, MainActivity.class)); activity.overridePendingTransition(0, 0); } });
        navUpload.setOnClickListener(v -> { if (!currentTab.equals("upload")) { activity.startActivity(new Intent(activity, UploadActivity.class)); activity.overridePendingTransition(0, 0); } });
        navProfile.setOnClickListener(v -> { if (!currentTab.equals("profile")) { activity.startActivity(new Intent(activity, ProfileActivity.class)); activity.overridePendingTransition(0, 0); } });
        navSettings.setOnClickListener(v -> { if (!currentTab.equals("settings")) { activity.startActivity(new Intent(activity, SettingsActivity.class)); activity.overridePendingTransition(0, 0); } });

        setHighlight(navHome, currentTab.equals("feed"));
        setHighlight(navUpload, currentTab.equals("upload"));
        setHighlight(navProfile, currentTab.equals("profile"));
        setHighlight(navSettings, currentTab.equals("settings"));
    }

    public static void setHighlight(LinearLayout navItem, boolean isActive) {
        if (navItem == null) return;
        try {
            ImageView icon = null;
            TextView text = null;
            // Safe tarika: Layout ke andar khud dhoondo ki Icon aur Text kahan hai
            for (int i = 0; i < navItem.getChildCount(); i++) {
                View child = navItem.getChildAt(i);
                if (child instanceof ImageView) icon = (ImageView) child;
                if (child instanceof TextView) text = (TextView) child;
            }

            if (icon != null) {
                icon.setColorFilter(isActive ? Color.parseColor("#4A90E2") : Color.parseColor("#888888"));
            }
            if (text != null) {
                text.setTextColor(isActive ? Color.parseColor("#4A90E2") : Color.parseColor("#888888"));
                text.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}