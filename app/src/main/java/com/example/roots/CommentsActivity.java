package com.example.roots;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {
    private RecyclerView rvComments;
    private EditText etCommentText;
    private Button btnSendComment;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private Map<String, String> nicknameMap = new HashMap<>();
    private FirebaseFirestore db;
    private String currentUid;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        postId = getIntent().getStringExtra("postId");
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        rvComments = findViewById(R.id.rvComments);
        etCommentText = findViewById(R.id.etCommentText);
        btnSendComment = findViewById(R.id.btnSendComment);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, commentList, nicknameMap, postId);
        rvComments.setAdapter(commentAdapter);

        loadNicknames();

        btnSendComment.setOnClickListener(v -> postComment());
    }

    private void loadNicknames() {
        if (currentUid == null) return;
        db.collection("family_connections").whereEqualTo("ownerUid", currentUid)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        nicknameMap.clear();
                        for (DocumentSnapshot doc : value) {
                            nicknameMap.put(doc.getString("targetUid"), doc.getString("customName"));
                        }
                        loadComments();
                    }
                });
    }

    private void loadComments() {
        if (postId == null) return;
        db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        commentList.clear();
                        for (DocumentSnapshot doc : value) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(doc.getId());
                                commentList.add(comment);
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                        if (!commentList.isEmpty()) {
                            rvComments.scrollToPosition(commentList.size() - 1);
                        }
                    }
                });
    }

    private void postComment() {
        String text = etCommentText.getText().toString().trim();
        if (text.isEmpty() || currentUid == null || postId == null) return;

        btnSendComment.setEnabled(false);
        Comment newComment = new Comment();
        newComment.setAuthorUid(currentUid);
        newComment.setText(text);
        newComment.setTimestamp(System.currentTimeMillis());

        db.collection("posts").document(postId).collection("comments").add(newComment)
                .addOnSuccessListener(documentReference -> {
                    etCommentText.setText("");
                    btnSendComment.setEnabled(true);
                    db.collection("posts").document(postId).update("commentCount", FieldValue.increment(1));
                })
                .addOnFailureListener(e -> {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show();
                });
    }
}