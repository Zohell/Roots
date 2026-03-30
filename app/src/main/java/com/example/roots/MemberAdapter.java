package com.example.roots;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Map<String, String>> memberList;
    private OnMemberRemoveListener removeListener;

    // Interface Activity ke saath communicate karne ke liye
    public interface OnMemberRemoveListener {
        void onRemoveClick(String uid, int position);
    }

    public MemberAdapter(List<Map<String, String>> memberList, OnMemberRemoveListener removeListener) {
        this.memberList = memberList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // item_member.xml ko inflate kar raha hai
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Map<String, String> member = memberList.get(position);

        // Name set kar raha hai (Map se "name" key utha kar)
        holder.tvMemberName.setText(member.get("name"));

        // Remove button click listener
        holder.btnRemoveMember.setOnClickListener(v -> {
            if (removeListener != null) {
                // Member ki UID aur position Activity ko bhej raha hai
                removeListener.onRemoveClick(member.get("uid"), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName;
        Button btnRemoveMember;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            btnRemoveMember = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}