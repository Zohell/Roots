package com.example.roots;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvProfileName;
    private TextView tvPostCount;
    private RecyclerView rvProfilePosts;
    private ProfileAdapter adapter;
    private List<Post> myPostList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUid;
    private String profileUidToLoad;
    private Uri imageUri;

    private LinearLayout layoutCategoryButtonsProfile;
    private RecyclerView rvCategoryPeople;
    private ProfilePeopleAdapter peopleAdapter;
    private List<User> currentPeopleList = new ArrayList<>();
    private String selectedCategoryOnProfile = "Everyone";

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivProfileImage.setImageURI(imageUri);
                    uploadProfilePicture();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("isDarkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String targetUid = getIntent().getStringExtra("targetUid");
        profileUidToLoad = (targetUid != null) ? targetUid : currentUid;

        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvPostCount = findViewById(R.id.tvPostCount);
        rvProfilePosts = findViewById(R.id.rvProfilePosts);

        layoutCategoryButtonsProfile = findViewById(R.id.layoutCategoryButtonsProfile);
        rvCategoryPeople = findViewById(R.id.rvCategoryPeople);
        rvCategoryPeople.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        peopleAdapter = new ProfilePeopleAdapter(this, currentPeopleList);
        rvCategoryPeople.setAdapter(peopleAdapter);

        rvProfilePosts.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ProfileAdapter(this, myPostList);
        rvProfilePosts.setAdapter(adapter);

        BottomNavManager.setup(this, "profile");

        loadUserProfile(profileUidToLoad);
        loadMyPosts(profileUidToLoad);
        loadDynamicCategoriesForProfile(profileUidToLoad);

        // ADD TO FAMILY BUTTON LOGIC
        Button btnAddFamily = findViewById(R.id.btnAddFamily);
        if (!profileUidToLoad.equals(currentUid)) {
            btnAddFamily.setVisibility(View.VISIBLE);
            btnAddFamily.setOnClickListener(v -> showAddToFeedDialog(profileUidToLoad, tvProfileName.getText().toString()));
        }

        if (profileUidToLoad.equals(currentUid)) {
            ivProfileImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            });
            tvProfileName.setOnClickListener(v -> showEditNameDialog());
        }
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        if (name == null) name = documentSnapshot.getString("name");
                        String imageUrl = documentSnapshot.getString("profileImage");

                        tvProfileName.setText(name != null ? name : "Family Member");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).circleCrop().into(ivProfileImage);
                        } else {
                            ivProfileImage.setImageResource(android.R.drawable.ic_menu_camera);
                        }
                    } else {
                        tvProfileName.setText("Family Member");
                    }
                }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void loadMyPosts(String uid) {
        db.collection("posts")
                .whereEqualTo("authorUid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    myPostList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        myPostList.add(doc.toObject(Post.class));
                    }
                    tvPostCount.setText(myPostList.size() + " Posts");
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadDynamicCategoriesForProfile(String profileOwnerUid) {
        Set<String> categoriesToDisplay = new HashSet<>();

        if (profileOwnerUid.equals(currentUid)) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            Set<String> customFeeds = prefs.getStringSet("categories", new HashSet<>());
            if (customFeeds != null) {
                categoriesToDisplay.addAll(customFeeds);
            }
        }

        db.collection("family_connections")
                .whereEqualTo("ownerUid", profileOwnerUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("category")) {
                            categoriesToDisplay.add(doc.getString("category"));
                        }
                    }

                    categoriesToDisplay.remove("Everyone");
                    List<String> sortedCategories = new ArrayList<>(categoriesToDisplay);
                    Collections.sort(sortedCategories, String.CASE_INSENSITIVE_ORDER);

                    sortedCategories.add(0, "Everyone");

                    generateDynamicCategoryButtonsProfile(profileOwnerUid, sortedCategories);
                    loadPeopleInSelectedCategory(profileOwnerUid, selectedCategoryOnProfile);
                });
    }

    private void generateDynamicCategoryButtonsProfile(String profileOwnerUid, List<String> allFeeds) {
        layoutCategoryButtonsProfile.removeAllViews();

        for (String feed : allFeeds) {
            Button btn = new Button(this);
            btn.setText(feed);
            btn.setTextColor(Color.parseColor("#424242"));
            btn.setPadding(40, 12, 40, 12);
            btn.setAllCaps(false);
            btn.setTextSize(14);
            btn.setTypeface(null, Typeface.BOLD);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(100f);
            shape.setColor(Color.WHITE);
            shape.setStroke(2, Color.parseColor("#E0E0E0"));
            btn.setBackground(shape);

            if (feed.equals(selectedCategoryOnProfile)) {
                btn.setTextColor(Color.WHITE);
                shape.setColor(Color.parseColor("#2196F3"));
                shape.setStroke(2, Color.parseColor("#1976D2"));
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                selectedCategoryOnProfile = feed;
                generateDynamicCategoryButtonsProfile(profileOwnerUid, allFeeds);
                loadPeopleInSelectedCategory(profileOwnerUid, feed);
            });

            layoutCategoryButtonsProfile.addView(btn);
        }
    }

    private void loadPeopleInSelectedCategory(String profileOwnerUid, String categoryName) {
        currentPeopleList.clear();
        peopleAdapter.notifyDataSetChanged();

        Query dynamicQuery;
        if (categoryName.equalsIgnoreCase("Everyone")) {
            dynamicQuery = db.collection("family_connections").whereEqualTo("ownerUid", profileOwnerUid);
        } else {
            dynamicQuery = db.collection("family_connections")
                    .whereEqualTo("ownerUid", profileOwnerUid)
                    .whereEqualTo("category", categoryName);
        }

        dynamicQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> targetUids = new ArrayList<>();
            Map<String, String> customNamesMap = new HashMap<>();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.contains("targetUid")) {
                    String tUid = doc.getString("targetUid");
                    targetUids.add(tUid);
                    if(doc.contains("customName")) {
                        customNamesMap.put(tUid, doc.getString("customName"));
                    }
                }
            }

            if (targetUids.isEmpty()) {
                rvCategoryPeople.setVisibility(View.GONE);
                return;
            }

            rvCategoryPeople.setVisibility(View.VISIBLE);

            if (!targetUids.isEmpty()) {
                List<String> chunk = targetUids.size() > 10 ? targetUids.subList(0, 10) : targetUids;
                db.collection("users").whereIn("uid", chunk).get()
                        .addOnSuccessListener(userSnapshots -> {
                            for (DocumentSnapshot userDoc : userSnapshots) {
                                User u = userDoc.toObject(User.class);
                                if (u != null) {
                                    if(customNamesMap.containsKey(u.getUid())){
                                        u.setFullName(customNamesMap.get(u.getUid()));
                                    }
                                    currentPeopleList.add(u);
                                }
                            }
                            peopleAdapter.notifyDataSetChanged();
                        });
            }
        });
    }

    private void showAddToFeedDialog(String targetUid, String targetName) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etNickname = new EditText(this);
        etNickname.setHint("Nickname (Optional, e.g. Ammi)");
        etNickname.setText(targetName);
        layout.addView(etNickname);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> customFeeds = prefs.getStringSet("categories", new HashSet<>());
        List<String> feedList = new ArrayList<>(customFeeds);
        feedList.remove("Everyone");

        android.widget.Spinner spinner = new android.widget.Spinner(this);
        spinner.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, feedList));
        layout.addView(spinner);

        new AlertDialog.Builder(this)
                .setTitle("Add to Family Feed")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String nickname = etNickname.getText().toString().trim();
                    String category = spinner.getSelectedItem().toString();

                    Map<String, Object> conn = new HashMap<>();
                    conn.put("ownerUid", currentUid);
                    conn.put("targetUid", targetUid);
                    conn.put("customName", nickname.isEmpty() ? targetName : nickname);
                    conn.put("category", category);

                    db.collection("family_connections").add(conn)
                            .addOnSuccessListener(doc -> Toast.makeText(this, "Added to " + category, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditNameDialog() {
        EditText et = new EditText(this);
        et.setText(tvProfileName.getText().toString());
        et.setPadding(50, 50, 50, 50);
        new AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = et.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("fullName", newName);
                        update.put("name", newName);
                        db.collection("users").document(currentUid).set(update, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> tvProfileName.setText(newName));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadProfilePicture() {
        Toast.makeText(this, "Profile Picture Updating...", Toast.LENGTH_LONG).show();

        MediaManager.get().upload(imageUri).option("resource_type", "image").callback(new UploadCallback() {
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override public void onSuccess(String requestId, Map resultData) {
                String url = (String) resultData.get("secure_url");
                Map<String, Object> update = new HashMap<>();
                update.put("profileImage", url);
                db.collection("users").document(currentUid).set(update, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profile Picture Updated!", Toast.LENGTH_SHORT).show());
            }

            @Override public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(ProfileActivity.this, "Upload fail: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }
}