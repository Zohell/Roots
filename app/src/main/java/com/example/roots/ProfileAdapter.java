package com.example.roots;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView; // YAHAN HAI FIX!
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.PostViewHolder> {
    private Context context;
    private List<Post> postList;

    public ProfileAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        Glide.with(context)
                .load(post.getImageUrl())
                .centerCrop()
                .into(holder.ivGridPostImage);

        holder.itemView.setOnClickListener(v -> showFullScreenImage(post));
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGridPostImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGridPostImage = itemView.findViewById(R.id.ivGridPostImage);
        }
    }

    private void showFullScreenImage(Post post) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);

        ImageView fullImage = new ImageView(context);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        fullImage.setLayoutParams(imageParams);
        fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(context).load(post.getImageUrl()).into(fullImage);

        LinearLayout bottomBar = new LinearLayout(context);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(40, 30, 40, 10);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvLikeIcon = new TextView(context);
        tvLikeIcon.setText("❤️ ");
        tvLikeIcon.setTextSize(24);

        TextView tvLikeCount = new TextView(context);
        int likesCount = (post.getLikes() != null) ? post.getLikes().size() : 0;
        tvLikeCount.setText(String.valueOf(likesCount));
        tvLikeCount.setTextColor(Color.WHITE);
        tvLikeCount.setTextSize(16);
        tvLikeCount.setPadding(0, 0, 60, 0);

        TextView tvCommentIcon = new TextView(context);
        tvCommentIcon.setText("💬 ");
        tvCommentIcon.setTextSize(24);

        TextView tvCommentCount = new TextView(context);
        tvCommentCount.setText(String.valueOf(post.getCommentCount()));
        tvCommentCount.setTextColor(Color.WHITE);
        tvCommentCount.setTextSize(16);

        bottomBar.addView(tvLikeIcon);
        bottomBar.addView(tvLikeCount);
        bottomBar.addView(tvCommentIcon);
        bottomBar.addView(tvCommentCount);

        TextView tvCaption = new TextView(context);
        tvCaption.setTextColor(Color.WHITE);
        tvCaption.setPadding(40, 10, 40, 40);
        tvCaption.setTextSize(14);

        if (post.getCaption() != null && !post.getCaption().isEmpty()) {
            tvCaption.setText(post.getCaption());
        } else {
            tvCaption.setVisibility(View.GONE);
        }

        mainLayout.addView(fullImage);
        mainLayout.addView(bottomBar);
        mainLayout.addView(tvCaption);

        dialog.setContentView(mainLayout);
        dialog.show();

        fullImage.setOnClickListener(v -> dialog.dismiss());
    }
}