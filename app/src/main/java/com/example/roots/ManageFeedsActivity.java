package com.example.roots;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ManageFeedsActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private CategoryAdapter adapter;
    private List<DocumentSnapshot> categoryList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_feeds);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CategoryAdapter(categoryList, (document, position) -> {
            showDeleteConfirmation(document.getId(), position, document.getString("name"));
        });
        rvCategories.setAdapter(adapter);

        loadCategories();
    }

    private void loadCategories() {
        db.collection("family_categories")
                .whereEqualTo("ownerUid", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    categoryList.addAll(queryDocumentSnapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load feeds", Toast.LENGTH_SHORT).show());
    }

    // NAYA LOGIC: Delete karne se pehle poochega
    private void showDeleteConfirmation(String docId, int position, String feedName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Feed")
                .setMessage("Are you sure you want to delete '" + feedName + "'? You can't see the feeds post once deleted.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCategory(docId, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Asli delete logic UI refresh ke sath
    private void deleteCategory(String docId, int position) {
        db.collection("family_categories").document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Feed removed successfully!", Toast.LENGTH_SHORT).show();
                    categoryList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, categoryList.size()); // BUG FIX: Baki list ko theek karta hai
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting feed", Toast.LENGTH_SHORT).show());
    }
}