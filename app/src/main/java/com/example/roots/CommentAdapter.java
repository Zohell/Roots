package com.example.roots;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private Context context;
    private List<Comment> commentList;
    private Map<String, String> nicknameMap;
    private FirebaseFirestore db;
    private String postId;
    private String currentUid;

    public CommentAdapter(Context context, List<Comment> commentList, Map<String, String> nicknameMap, String postId) {
        this.context = context;
        this.commentList = commentList;
        this.nicknameMap = nicknameMap;
        this.postId = postId;
        this.db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvCommentText.setText(comment.getText());

        String authorUid = comment.getAuthorUid();
        String displayName = nicknameMap.containsKey(authorUid) ? nicknameMap.get(authorUid) : "Family Member";
        holder.tvCommentAuthor.setText(displayName);

        db.collection("users").document(authorUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String profileImageUrl = documentSnapshot.getString("profileImage");
                String realName = documentSnapshot.getString("fullName");
                if (!nicknameMap.containsKey(authorUid) && realName != null) {
                    holder.tvCommentAuthor.setText(realName);
                }
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(context).load(profileImageUrl).circleCrop().into(holder.ivCommentProfile);
                } else {
                    holder.ivCommentProfile.setImageResource(android.R.drawable.ic_menu_camera);
                }
            }
        });

        if (currentUid != null && currentUid.equals(authorUid)) {
            holder.ivDeleteComment.setVisibility(View.VISIBLE);
            holder.ivDeleteComment.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Comment")
                        .setMessage("Delete karna hai?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.collection("posts").document(postId).collection("comments").document(comment.getCommentId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        db.collection("posts").document(postId).update("commentCount", com.google.firebase.firestore.FieldValue.increment(-1));
                                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        } else {
            holder.ivDeleteComment.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCommentProfile, ivDeleteComment;
        TextView tvCommentAuthor, tvCommentText;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCommentProfile = itemView.findViewById(R.id.ivCommentProfile);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            ivDeleteComment = itemView.findViewById(R.id.ivDeleteComment);
        }
    }
}