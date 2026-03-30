package com.example.roots;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private Map<String, String> nicknameMap;
    private FirebaseFirestore db;
    private String currentUid;

    public PostAdapter(Context context, List<Post> postList, Map<String, String> nicknameMap) {
        this.context = context;
        this.postList = postList;
        this.nicknameMap = nicknameMap;
        this.db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.tvCaption.setText(post.getCaption());

        // MEDIA LOAD LOGIC
        String mediaUrl = post.getImageUrl();
        holder.videoViewPost.stopPlayback();
        holder.videoViewPost.setVisibility(View.GONE);
        holder.ivPostImage.setVisibility(View.VISIBLE);
        holder.ivPlayButton.setVisibility(View.GONE);

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            if ("video".equals(post.getMediaType())) {
                holder.ivPlayButton.setVisibility(View.VISIBLE);
                String thumbnailUrl = mediaUrl.replace(".mp4", ".jpg");
                Glide.with(context).load(thumbnailUrl).into(holder.ivPostImage);

                holder.ivPostImage.setOnClickListener(v -> {
                    holder.ivPostImage.setVisibility(View.GONE);
                    holder.ivPlayButton.setVisibility(View.GONE);
                    holder.videoViewPost.setVisibility(View.VISIBLE);
                    holder.videoViewPost.setVideoPath(mediaUrl);
                    holder.videoViewPost.setOnPreparedListener(mp -> {
                        mp.setLooping(true);
                        holder.videoViewPost.start();
                    });
                });

                holder.videoViewPost.setOnClickListener(v -> {
                    if (holder.videoViewPost.isPlaying()) {
                        holder.videoViewPost.pause();
                        holder.ivPlayButton.setVisibility(View.VISIBLE);
                    } else {
                        holder.videoViewPost.start();
                        holder.ivPlayButton.setVisibility(View.GONE);
                    }
                });

            } else {
                Glide.with(context).load(mediaUrl).into(holder.ivPostImage);
                holder.ivPostImage.setOnClickListener(null);
            }
        }

        // AUTHOR NAME LOAD
        String authorUid = post.getAuthorUid();
        String displayName = nicknameMap.containsKey(authorUid) ? nicknameMap.get(authorUid) : "Family Member";
        holder.tvAuthorName.setText(displayName);
        View.OnClickListener profileClickListener = v -> {
            android.content.Intent intent = new android.content.Intent(context, UserProfileActivity.class);
            intent.putExtra("targetUid", authorUid);
            intent.putExtra("customName", displayName); // Aapka diya hua nickname
            context.startActivity(intent);
        };
        holder.ivAuthorProfile.setOnClickListener(profileClickListener);
        holder.tvAuthorName.setOnClickListener(profileClickListener);

        db.collection("users").document(authorUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String profileImageUrl = documentSnapshot.getString("profileImage");
                String realName = documentSnapshot.getString("fullName");
                if (!nicknameMap.containsKey(authorUid) && realName != null) {
                    holder.tvAuthorName.setText(realName);
                }
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(context).load(profileImageUrl).circleCrop().into(holder.ivAuthorProfile);
                } else {
                    holder.ivAuthorProfile.setImageResource(android.R.drawable.ic_menu_camera);
                }
            }
        });

        // ==========================================
        // POST DELETE LOGIC
        // ==========================================
        if (currentUid != null && currentUid.equals(authorUid)) {
            holder.ivDeletePost.setVisibility(View.VISIBLE);
            holder.ivDeletePost.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Post")
                        .setMessage("Do you really want to delete post?")
                        .setPositiveButton("Yes Delete it!", (dialog, which) -> {
                            if (post.getPostId() != null) {
                                db.collection("posts").document(post.getPostId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(context, "Post Deleted!", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            holder.ivDeletePost.setVisibility(View.GONE);
        }

        // ==========================================
        // INSTANT 1-TAP LIKE / UNLIKE LOGIC
        // ==========================================
        List<String> originalLikes = post.getLikes() != null ? post.getLikes() : new ArrayList<>();
        List<String> localLikes = new ArrayList<>(originalLikes);
        final boolean[] isLikedLocally = {localLikes.contains(currentUid)};

        updateLikeUI(holder.tvLike, isLikedLocally[0], localLikes.size());

        holder.tvLike.setOnClickListener(v -> {
            if (post.getPostId() == null || currentUid == null) return;
            isLikedLocally[0] = !isLikedLocally[0];

            if (isLikedLocally[0]) {
                localLikes.add(currentUid);
                updateLikeUI(holder.tvLike, true, localLikes.size());
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayUnion(currentUid));
            } else {
                localLikes.remove(currentUid);
                updateLikeUI(holder.tvLike, false, localLikes.size());
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayRemove(currentUid));
            }
        });

        // ==========================================
        // COMMENT COUNT LOGIC
        // ==========================================
        int cCount = post.getCommentCount();
        if (cCount > 0) {
            holder.tvComment.setText("💬 " + cCount);
            holder.tvComment.setTextColor(Color.parseColor("#4A90E2"));
        } else {
            holder.tvComment.setText("💬 Comment");
            holder.tvComment.setTextColor(Color.GRAY);
        }

        // COMMENT SCREEN OPEN LOGIC
        holder.tvComment.setOnClickListener(v -> {
            if (post.getPostId() == null) return;
            android.content.Intent intent = new android.content.Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });
    }

    private void updateLikeUI(TextView tvLike, boolean isLiked, int count) {
        if (isLiked) {
            tvLike.setText("❤️ " + count);
            tvLike.setTextColor(Color.parseColor("#E91E63"));
        } else {
            tvLike.setText("🤍 " + (count > 0 ? count : "Like"));
            tvLike.setTextColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() { return postList.size(); }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAuthorProfile, ivPostImage, ivPlayButton, ivDeletePost;
        TextView tvAuthorName, tvCaption, tvLike, tvComment;
        VideoView videoViewPost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAuthorProfile = itemView.findViewById(R.id.ivAuthorProfile);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            ivDeletePost = itemView.findViewById(R.id.ivDeletePost);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
            videoViewPost = itemView.findViewById(R.id.videoViewPost);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            tvLike = itemView.findViewById(R.id.tvLike);
            tvComment = itemView.findViewById(R.id.tvComment);
        }
    }

    public void filterList(List<Post> filteredList) {
        this.postList = filteredList;
        notifyDataSetChanged();
    }
}