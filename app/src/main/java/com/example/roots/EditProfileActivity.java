package com.example.roots;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        EditText etNewName = findViewById(R.id.etNewName);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etNewName.getText().toString().trim();
            if (!newName.isEmpty()) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("fullName", newName)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
            }
        });
    }
}