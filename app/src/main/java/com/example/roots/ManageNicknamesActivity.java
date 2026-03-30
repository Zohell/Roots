package com.example.roots;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageNicknamesActivity extends AppCompatActivity {

    private RecyclerView rvManageNicknames;
    private FirebaseFirestore db;
    private String currentUid;
    private List<DocumentSnapshot> connectionList = new ArrayList<>();
    private ConnectionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_nicknames);

        rvManageNicknames = findViewById(R.id.rvManageNicknames);
        rvManageNicknames.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        adapter = new ConnectionAdapter();
        rvManageNicknames.setAdapter(adapter);

        loadConnections();
    }

    private void loadConnections() {
        if (currentUid == null) return;
        db.collection("family_connections")
                .whereEqualTo("ownerUid", currentUid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        connectionList.clear();
                        connectionList.addAll(value.getDocuments());
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // Inner Adapter Class
    private class ConnectionAdapter extends RecyclerView.Adapter<ConnectionAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(ManageNicknamesActivity.this).inflate(R.layout.item_manage_nickname, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = connectionList.get(position);
            String docId = doc.getId();
            String targetUid = doc.getString("targetUid");
            String customName = doc.getString("customName");
            String category = doc.getString("category");

            holder.tvCurrentNickname.setText("Nickname: " + customName);
            holder.tvCurrentCategory.setText("Feed: " + category);

            // Target user ka asli naam fetch karna
            db.collection("users").document(targetUid).get().addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    holder.tvTargetName.setText(userDoc.getString("fullName"));
                }
            });

            // EDIT LOGIC
            holder.ivEditNickname.setOnClickListener(v -> {
                LinearLayout layout = new LinearLayout(ManageNicknamesActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 40, 50, 10);

                EditText etCustomName = new EditText(ManageNicknamesActivity.this);
                etCustomName.setText(customName);
                etCustomName.setHint("New Nickname");

                EditText etCategory = new EditText(ManageNicknamesActivity.this);
                etCategory.setText(category);
                etCategory.setHint("New Category");

                layout.addView(etCustomName);
                layout.addView(etCategory);

                new AlertDialog.Builder(ManageNicknamesActivity.this)
                        .setTitle("Edit Connection")
                        .setView(layout)
                        .setPositiveButton("Update", (dialog, which) -> {
                            String newName = etCustomName.getText().toString().trim();
                            String newCat = etCategory.getText().toString().trim();
                            if (!newName.isEmpty() && !newCat.isEmpty()) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("customName", newName);
                                updates.put("category", newCat);
                                db.collection("family_connections").document(docId).update(updates)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(ManageNicknamesActivity.this, "Updated!", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // DELETE LOGIC
            holder.ivDeleteConnection.setOnClickListener(v -> {
                new AlertDialog.Builder(ManageNicknamesActivity.this)
                        .setTitle("Remove Member?")
                        .setMessage("Are you sure you want to remove them from your feeds?")
                        .setPositiveButton("Yes, Remove", (dialog, which) -> {
                            db.collection("family_connections").document(docId).delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(ManageNicknamesActivity.this, "Removed successfully", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return connectionList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTargetName, tvCurrentNickname, tvCurrentCategory;
            ImageView ivEditNickname, ivDeleteConnection;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTargetName = itemView.findViewById(R.id.tvTargetName);
                tvCurrentNickname = itemView.findViewById(R.id.tvCurrentNickname);
                tvCurrentCategory = itemView.findViewById(R.id.tvCurrentCategory);
                ivEditNickname = itemView.findViewById(R.id.ivEditNickname);
                ivDeleteConnection = itemView.findViewById(R.id.ivDeleteConnection);
            }
        }
    }
}