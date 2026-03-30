package com.example.roots;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchDarkMode;
    // YE LINE THEEK KI HAI: Button ko TextView bana diya hai
    private TextView btnEditProfile, btnManageNicknames, btnManageFeeds, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        BottomNavManager.setup(this, "settings");

        switchDarkMode = findViewById(R.id.switchDarkMode);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnManageNicknames = findViewById(R.id.btnManageNicknames);
        btnManageFeeds = findViewById(R.id.btnManageFeeds);
        btnLogout = findViewById(R.id.btnLogout);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("isDarkMode", false);

        switchDarkMode.setChecked(isDark);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("isDarkMode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
        });

        btnManageNicknames.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ManageNicknamesActivity.class));
        });

        btnManageFeeds.setOnClickListener(v -> {
            Set<String> customFeeds = prefs.getStringSet("categories", new HashSet<>());

            if (customFeeds == null || customFeeds.isEmpty()) {
                Toast.makeText(this, "No feeds found!", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] feedArray = customFeeds.toArray(new String[0]);

            new AlertDialog.Builder(this)
                    .setTitle("Select Feed")
                    .setItems(feedArray, (dialog, which) -> {
                        String selectedFeed = feedArray[which];

                        CharSequence[] options = new CharSequence[]{"Rename Feed", "View/Remove Members", "Delete Feed"};
                        new AlertDialog.Builder(this)
                                .setTitle(selectedFeed + " Options")
                                .setItems(options, (dialogInner, whichInner) -> {
                                    if (whichInner == 0) {
                                        EditText etRename = new EditText(this);
                                        etRename.setText(selectedFeed);
                                        etRename.setPadding(50, 50, 50, 50);

                                        new AlertDialog.Builder(this)
                                                .setTitle("Rename Feed")
                                                .setView(etRename)
                                                .setPositiveButton("Save", (renameDialog, renameWhich) -> {
                                                    String newName = etRename.getText().toString().trim();
                                                    if (!newName.isEmpty() && !newName.equals(selectedFeed)) {
                                                        Set<String> updatedFeeds = new HashSet<>(customFeeds);
                                                        updatedFeeds.remove(selectedFeed);
                                                        updatedFeeds.add(newName);
                                                        prefs.edit().putStringSet("categories", updatedFeeds).apply();

                                                        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                                        FirebaseFirestore.getInstance().collection("family_connections")
                                                                .whereEqualTo("ownerUid", currentUid)
                                                                .whereEqualTo("category", selectedFeed)
                                                                .get()
                                                                .addOnSuccessListener(queryDocs -> {
                                                                    for (DocumentSnapshot doc : queryDocs) {
                                                                        doc.getReference().update("category", newName);
                                                                    }
                                                                });

                                                        FirebaseFirestore.getInstance().collection("posts")
                                                                .whereEqualTo("authorUid", currentUid)
                                                                .whereArrayContains("authorFeeds", selectedFeed)
                                                                .get()
                                                                .addOnSuccessListener(postDocs -> {
                                                                    for (DocumentSnapshot postDoc : postDocs) {
                                                                        List<String> feeds = (List<String>) postDoc.get("authorFeeds");
                                                                        if (feeds != null && feeds.contains(selectedFeed)) {
                                                                            feeds.remove(selectedFeed);
                                                                            feeds.add(newName);
                                                                            postDoc.getReference().update("authorFeeds", feeds);
                                                                        }
                                                                    }
                                                                    Toast.makeText(this, "Feed Renamed to " + newName, Toast.LENGTH_SHORT).show();
                                                                });
                                                    }
                                                })
                                                .setNegativeButton("Cancel", null)
                                                .show();

                                    } else if (whichInner == 1) {
                                        Intent intent = new Intent(SettingsActivity.this, ManageGroupMembersActivity.class);
                                        intent.putExtra("groupName", selectedFeed);
                                        startActivity(intent);
                                    } else {
                                        new AlertDialog.Builder(this)
                                                .setTitle("Delete " + selectedFeed + "?")
                                                .setMessage("Do you want to delete this feed?")
                                                .setPositiveButton("Yes, Delete it", (confirmDialog, confirmWhich) -> {
                                                    Set<String> updatedFeeds = new HashSet<>(customFeeds);
                                                    updatedFeeds.remove(selectedFeed);
                                                    prefs.edit().putStringSet("categories", updatedFeeds).apply();
                                                    Toast.makeText(this, selectedFeed + " feed deleted!", Toast.LENGTH_SHORT).show();
                                                })
                                                .setNegativeButton("Cancel", null)
                                                .show();
                                    }
                                }).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Do you want to log out?")
                    .setPositiveButton("Yes, Log Out", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}