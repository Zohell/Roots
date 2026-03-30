package com.example.roots;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ProfilePeopleAdapter extends RecyclerView.Adapter<ProfilePeopleAdapter.PersonViewHolder> {

    private Context context;
    private List<User> peopleList;

    public ProfilePeopleAdapter(Context context, List<User> peopleList) {
        this.context = context;
        this.peopleList = peopleList;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        User person = peopleList.get(position);

        String displayName = (person.getFullName() != null) ? person.getFullName() : "Roots Member";
        holder.tvPersonName.setText(displayName);

        if (person.getProfileImage() != null && !person.getProfileImage().isEmpty()) {
            Glide.with(context).load(person.getProfileImage()).circleCrop().into(holder.ivPersonAvatar);
        } else {
            holder.ivPersonAvatar.setImageResource(android.R.drawable.ic_menu_camera);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("targetUid", person.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return peopleList.size();
    }

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPersonAvatar;
        TextView tvPersonName;

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPersonAvatar = itemView.findViewById(R.id.ivPersonAvatar);
            tvPersonName = itemView.findViewById(R.id.tvPersonName);
        }
    }
}