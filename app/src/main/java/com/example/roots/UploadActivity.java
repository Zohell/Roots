package com.example.roots;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UploadActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private EditText etCaption;
    private LinearLayout layoutCheckboxes;
    private CheckBox cbEveryone;
    private Button btnUpload;
    private Uri imageUri;
    private FirebaseFirestore db;
    private String currentUid;
    private String currentMediaType = "image";

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    String mimeType = getContentResolver().getType(imageUri);

                    if (mimeType != null && mimeType.startsWith("video")) {
                        try {
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(this, imageUri);
                            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            long timeInMillisec = Long.parseLong(time);
                            retriever.release();

                            if (timeInMillisec > 60000) {
                                Toast.makeText(this, "video must be less than 1 minute", Toast.LENGTH_LONG).show();
                                imageUri = null;
                                ivPreview.setImageResource(android.R.drawable.ic_menu_camera);
                                return;
                            }
                            currentMediaType = "video";
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        currentMediaType = "image";
                    }
                    ivPreview.setImageURI(imageUri);
                }
            }
    );
// UploadActivity.java ke andar ye dono functions replace karo:

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        ivPreview = findViewById(R.id.ivPreview);
        etCaption = findViewById(R.id.etCaption);
        layoutCheckboxes = findViewById(R.id.layoutCheckboxes);
        cbEveryone = findViewById(R.id.cbEveryone);
        btnUpload = findViewById(R.id.btnUpload);

        BottomNavManager.setup(this, "upload");

        ivPreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            galleryLauncher.launch(intent);
        });

        loadDynamicCheckboxes();

        btnUpload.setOnClickListener(v -> {
            if (imageUri == null) {
                Toast.makeText(this, "Please select an image or video", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> visibleToFeeds = new ArrayList<>();

            if (cbEveryone.isChecked()) {
                visibleToFeeds.add("Everyone");
            } else {
                // Loop through all dynamic checkboxes
                for (int i = 0; i < layoutCheckboxes.getChildCount(); i++) {
                    View child = layoutCheckboxes.getChildAt(i);
                    if (child instanceof CheckBox) {
                        CheckBox cb = (CheckBox) child;
                        if (cb.isChecked() && !cb.getText().toString().equals("Everyone")) {
                            visibleToFeeds.add(cb.getText().toString());
                        }
                    }
                }
            }

            if (visibleToFeeds.isEmpty()) {
                Toast.makeText(this, "Please select at least one feed", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadToCloudinary(visibleToFeeds);
        });
    }

    private void loadDynamicCheckboxes() {
        // Clear old dynamic checkboxes, but keep cbEveryone (index 0)
        int childCount = layoutCheckboxes.getChildCount();
        if (childCount > 1) {
            layoutCheckboxes.removeViews(1, childCount - 1);
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> customFeeds = prefs.getStringSet("categories", new HashSet<>());

        // Remove hardcoded Mom/Dad Side and just use what's in preferences
        List<String> allFeeds = new ArrayList<>();
        if (customFeeds != null) {
            for (String feed : customFeeds) {
                if (!feed.equalsIgnoreCase("Everyone")) {
                    allFeeds.add(feed);
                }
            }
        }

        // Sort alphabetically
        java.util.Collections.sort(allFeeds, String.CASE_INSENSITIVE_ORDER);

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int textColor = (currentNightMode == Configuration.UI_MODE_NIGHT_YES) ? Color.WHITE : Color.BLACK;

        // Set cbEveryone text color correctly based on theme
        cbEveryone.setTextColor(textColor);

        for (String feed : allFeeds) {
            CheckBox cb = new CheckBox(this);
            cb.setText(feed);
            cb.setTextColor(textColor);
            cb.setTextSize(16);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90E2"))); // Blue tint

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(40, 0, 10, 0);
            cb.setLayoutParams(params);
            layoutCheckboxes.addView(cb);
        }
    }

    private void uploadToCloudinary(List<String> visibleToFeeds) {
        Toast.makeText(this, "Uploading your post please wait..", Toast.LENGTH_LONG).show();

        MediaManager.get().upload(imageUri).option("resource_type", "auto").callback(new UploadCallback() {
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onSuccess(String requestId, Map resultData) {
                String url = (String) resultData.get("secure_url");
                savePostToFirestore(url, visibleToFeeds, currentMediaType);
            }
            @Override public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(UploadActivity.this, "Upload fail: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void savePostToFirestore(String imageUrl, List<String> visibleToFeeds, String mediaType) {
        String caption = etCaption.getText().toString().trim();

        if (visibleToFeeds.contains("Everyone")) {
            List<String> allowedViewers = new ArrayList<>();
            allowedViewers.add("Everyone");
            allowedViewers.add(currentUid);
            finalizePostSave(imageUrl, caption, allowedViewers, visibleToFeeds, mediaType);
        } else {
            db.collection("family_connections")
                    .whereEqualTo("ownerUid", currentUid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<String> allowedViewers = new ArrayList<>();
                        allowedViewers.add(currentUid);
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String cat = doc.getString("category");
                            if (visibleToFeeds.contains(cat)) {
                                String targetUid = doc.getString("targetUid");
                                if (targetUid != null && !allowedViewers.contains(targetUid)) {
                                    allowedViewers.add(targetUid);
                                }
                            }
                        }
                        finalizePostSave(imageUrl, caption, allowedViewers, visibleToFeeds, mediaType);
                    });
        }
    }

    private void finalizePostSave(String imageUrl, String caption, List<String> allowedViewers, List<String> authorFeeds, String mediaType) {
        Map<String, Object> post = new HashMap<>();
        post.put("authorUid", currentUid);
        post.put("imageUrl", imageUrl);
        post.put("caption", caption);
        post.put("visibleTo", allowedViewers);
        post.put("authorFeeds", authorFeeds);
        post.put("mediaType", mediaType);
        post.put("timestamp", System.currentTimeMillis());

        db.collection("posts").add(post).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Post Uploaded!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(UploadActivity.this, MainActivity.class));
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Firestore error", Toast.LENGTH_SHORT).show());
    }
}