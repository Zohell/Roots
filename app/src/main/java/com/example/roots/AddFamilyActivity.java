package com.example.roots;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AddFamilyActivity extends AppCompatActivity {
    private RecyclerView rvContacts;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList = new ArrayList<>();
    private FirebaseFirestore db; // Error fix: db define kiya gaya hai
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_family);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvContacts = findViewById(R.id.rvContacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        // ContactAdapter error fix
        contactAdapter = new ContactAdapter(contactList, contact -> {
            // Nickname aur side selection logic yahan aayega
        });
        rvContacts.setAdapter(contactAdapter);
    }
}