package com.example.roots;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
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

public class ManageGroupMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MemberAdapter adapter;
    private List<Map<String, String>> memberList = new ArrayList<>();
    private FirebaseFirestore db;
    private String groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_group_members);

        db = FirebaseFirestore.getInstance();
        groupName = getIntent().getStringExtra("groupName");

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(groupName + " Members");

        // 1. RecyclerView Setup
        recyclerView = findViewById(R.id.recyclerViewMembers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. Adapter with Delete Logic
        adapter = new MemberAdapter(memberList, (uid, position) -> {
            showRemoveDialog(uid, position);
        });
        recyclerView.setAdapter(adapter);

        loadMembers();
    }

    private void loadMembers() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("family_connections")
                .whereEqualTo("ownerUid", currentUid)
                .whereEqualTo("category", groupName)
                .get()
                .addOnSuccessListener(queryDocs -> {
                    memberList.clear();
                    for (DocumentSnapshot doc : queryDocs) {
                        Map<String, String> member = new HashMap<>();
                        member.put("uid", doc.getId()); // Document ID to delete
                        member.put("name", doc.getString("customName"));
                        memberList.add(member);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showRemoveDialog(String docId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Do you want to remove this member?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("family_connections").document(docId).delete()
                            .addOnSuccessListener(aVoid -> {
                                memberList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(this, "Member removed", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}