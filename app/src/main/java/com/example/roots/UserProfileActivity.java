package com.example.roots;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivUserProfile;
    private TextView tvUserFullName, tvProfileHeader;
    private RecyclerView rvUserPosts;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private String targetUid;
    private String customName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Retrieve passed extras from intent
        targetUid = getIntent().getStringExtra("targetUid");
        customName = getIntent().getStringExtra("customName"); // This is the nickname/relation

        // Find views
        tvProfileHeader = findViewById(R.id.tvProfileHeader);
        ivUserProfile = findViewById(R.id.ivUserProfile);
        tvUserFullName = findViewById(R.id.tvUserFullName);
        rvUserPosts = findViewById(R.id.rvUserPosts);

        // Set header text
        tvProfileHeader.setText(customName + "'s Profile");

        // Set up RecyclerView
        rvUserPosts.setLayoutManager(new LinearLayoutManager(this));

        // Create a custom nickname map just for this user in the adapter
        HashMap<String, String> singleUserMap = new HashMap<>();
        singleUserMap.put(targetUid, customName);

        // Create adapter, passing the map with just this one user's info
        postAdapter = new PostAdapter(this, postList, singleUserMap);
        rvUserPosts.setAdapter(postAdapter);

        db = FirebaseFirestore.getInstance();

        // Load user details
        loadUserProfile();
        // Load posts created by this user
        loadUserPosts();
    }

    private void loadUserProfile() {
        if (targetUid == null) return;

        db.collection("users").document(targetUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String fullName = documentSnapshot.getString("fullName");
                String profileImageUrl = documentSnapshot.getString("profileImage");

                tvUserFullName.setText(fullName);
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).circleCrop().into(ivUserProfile);
                } else {
                    ivUserProfile.setImageResource(android.R.drawable.ic_menu_camera);
                }
            }
        });
    }

    private void loadUserPosts() {
        if (targetUid == null) return;

        db.collection("posts")
                .whereEqualTo("authorUid", targetUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Post p = doc.toObject(Post.class);
                            p.setPostId(doc.getId());
                            postList.add(p);
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }
}