package com.example.roots;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class SearchUsersActivity extends AppCompatActivity {

    private EditText etSearchUser;
    private RecyclerView rvSearchUsers;
    private SearchUserAdapter adapter;
    private List<DocumentSnapshot> allUsers = new ArrayList<>();
    private List<DocumentSnapshot> filteredUsers = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        etSearchUser = findViewById(R.id.etSearchUser);
        rvSearchUsers = findViewById(R.id.rvSearchUsers);
        rvSearchUsers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SearchUserAdapter(this, filteredUsers);
        rvSearchUsers.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        loadAllUsers();

        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllUsers() {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allUsers.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                if (!doc.getId().equals(currentUid)) {
                    allUsers.add(doc);
                }
            }
            filteredUsers.addAll(allUsers);
            adapter.notifyDataSetChanged();
        });
    }

    private void filter(String text) {
        filteredUsers.clear();
        for (DocumentSnapshot doc : allUsers) {
            String name = doc.getString("fullName");
            if (name != null && name.toLowerCase().contains(text.toLowerCase())) {
                filteredUsers.add(doc);
            }
        }
        adapter.notifyDataSetChanged();
    }
}