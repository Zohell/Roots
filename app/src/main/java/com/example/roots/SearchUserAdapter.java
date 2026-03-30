package com.example.roots;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.ViewHolder> {

    private Context context;
    private List<DocumentSnapshot> userList;

    public SearchUserAdapter(Context context, List<DocumentSnapshot> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot userDoc = userList.get(position);
        String targetUid = userDoc.getId();
        String name = userDoc.getString("fullName");
        String profileImage = userDoc.getString("profileImage");

        holder.tvSearchName.setText(name);
        if (profileImage != null && !profileImage.isEmpty()) {
            Glide.with(context).load(profileImage).circleCrop().into(holder.ivSearchProfile);
        } else {
            holder.ivSearchProfile.setImageResource(android.R.drawable.ic_menu_camera);
        }
        android.view.View.OnClickListener profileClickListener = v -> {
            android.content.Intent intent = new android.content.Intent(context, UserProfileActivity.class);
            intent.putExtra("targetUid", targetUid);
            intent.putExtra("customName", name);
            context.startActivity(intent);
        };

        holder.ivSearchProfile.setOnClickListener(profileClickListener);
        holder.tvSearchName.setOnClickListener(profileClickListener);
        holder.btnAddUser.setOnClickListener(v -> showAddDialog(targetUid, name));
    }

    private void showAddDialog(String targetUid, String realName) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etCustomName = new EditText(context);
        etCustomName.setHint("Nickname (Optional)"); // Optional likh diya hai
        etCustomName.setText(realName);

        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        java.util.Set<String> customFeeds = prefs.getStringSet("categories", new java.util.HashSet<>());

        List<String> allFeeds = new java.util.ArrayList<>();
        allFeeds.add("Mom Side");
        allFeeds.add("Dad Side");
        if (customFeeds != null) {
            for (String feed : customFeeds) {
                if (!allFeeds.contains(feed) && !feed.equalsIgnoreCase("Everyone")) {
                    allFeeds.add(feed);
                }
            }
        }

        TextView tvLabel = new TextView(context);
        tvLabel.setText("Select Feed:");
        tvLabel.setPadding(10, 20, 10, 10);

        Spinner spinnerCategory = new Spinner(context);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, allFeeds);
        spinnerCategory.setAdapter(spinnerAdapter);

        layout.addView(etCustomName);
        layout.addView(tvLabel);
        layout.addView(spinnerCategory);

        new AlertDialog.Builder(context)
                .setTitle("Add to Family")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String customName = etCustomName.getText().toString().trim();
                    String category = spinnerCategory.getSelectedItem().toString();

                    // Agar khali chhod diya, toh asli naam use kar lenge
                    if (customName.isEmpty()) {
                        customName = realName;
                    }

                    Map<String, Object> connection = new HashMap<>();
                    connection.put("ownerUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    connection.put("targetUid", targetUid);
                    connection.put("customName", customName);
                    connection.put("category", category);

                    FirebaseFirestore.getInstance().collection("family_connections").add(connection)
                            .addOnSuccessListener(doc -> Toast.makeText(context, "Added to " + category, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() { return userList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSearchProfile;
        TextView tvSearchName;
        Button btnAddUser;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSearchProfile = itemView.findViewById(R.id.ivSearchProfile);
            tvSearchName = itemView.findViewById(R.id.tvSearchName);
            btnAddUser = itemView.findViewById(R.id.btnAddUser);
        }
    }
}