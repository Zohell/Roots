package com.example.roots;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<DocumentSnapshot> categoryList;
    private OnCategoryDeleteListener deleteListener;

    public interface OnCategoryDeleteListener {
        void onDeleteClick(DocumentSnapshot document, int position);
    }

    public CategoryAdapter(List<DocumentSnapshot> categoryList, OnCategoryDeleteListener deleteListener) {
        this.categoryList = categoryList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        DocumentSnapshot doc = categoryList.get(position);
        holder.tvCategoryName.setText(doc.getString("name"));

        holder.btnDeleteCategory.setOnClickListener(v -> {
            // BUG FIX: Sahi current position nikalna zaroori hai
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                deleteListener.onDeleteClick(categoryList.get(currentPosition), currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        Button btnDeleteCategory;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            btnDeleteCategory = itemView.findViewById(R.id.btnDeleteCategory);
        }
    }
}