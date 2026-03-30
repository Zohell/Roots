package com.example.roots;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ProfilePostAdapter extends RecyclerView.Adapter<ProfilePostAdapter.ProfilePostViewHolder> {

    private Context context;
    private List<Post> postList;

    public ProfilePostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public ProfilePostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile_post, parent, false);
        return new ProfilePostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfilePostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Glide library se photo square layout mein load karna
        Glide.with(context)
                .load(post.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivGridPostImage);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ProfilePostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGridPostImage;

        public ProfilePostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGridPostImage = itemView.findViewById(R.id.ivGridPostImage);
        }
    }
}