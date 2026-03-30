package com.example.roots;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Map<String, String> categoryMap = new HashMap<>();
    private LinearLayout layoutCategories;
    private RecyclerView recyclerViewFeed;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUid;

    private String currentCategory = "";
    private com.google.firebase.firestore.ListenerRegistration feedListener;
    private Map<String, String> nicknameMap = new HashMap<>();
    private String inviteCategoryTemp = "";

    private final androidx.activity.result.ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri contactUri = result.getData().getData();
                    String[] projection = {android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER};
                    try (android.database.Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            String number = cursor.getString(0);
                            sendSmsInvite(number, inviteCategoryTemp);
                        }
                    }
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    checkSuggestedUsers();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("isDarkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "duk0yltw6");
            config.put("api_key", "661438681126276");
            config.put("api_secret", "dii41v42hzXXQwKhMJQLFf7R8fI");
            MediaManager.init(getApplicationContext(), config);
        } catch (Exception e) {
        }

        layoutCategories = findViewById(R.id.layoutCategories);
        recyclerViewFeed = findViewById(R.id.recyclerViewFeed);
        recyclerViewFeed.setLayoutManager(new LinearLayoutManager(this));

        postAdapter = new PostAdapter(this, postList, nicknameMap);
        recyclerViewFeed.setAdapter(postAdapter);

        // Naya Search Bar Icon Logic (Lamba wala searchView hataya gaya hai)
        View btnFindRootsUsers = findViewById(R.id.btnFindRootsUsers);
        if (btnFindRootsUsers != null) {
            btnFindRootsUsers.setOnClickListener(v -> showSearchAndSuggestDialog());
        }

        BottomNavManager.setup(this, "feed");

        loadDynamicCategories();
        loadNicknamesAndPosts();

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            checkSuggestedUsers();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS);
        }
    }

    private void filterFeed(String text) {
        if (postList == null || postAdapter == null) return;

        if (text.isEmpty()) {
            postAdapter.filterList(postList);
            return;
        }

        List<Post> filtered = new ArrayList<>();
        for (Post post : postList) {
            String authorName = nicknameMap.containsKey(post.getAuthorUid()) ? nicknameMap.get(post.getAuthorUid()) : "Family Member";

            if (authorName.toLowerCase().contains(text.toLowerCase()) ||
                    (post.getCaption() != null && post.getCaption().toLowerCase().contains(text.toLowerCase()))) {
                filtered.add(post);
            }
        }
        postAdapter.filterList(filtered);
    }

    // Custom Popup for Search and Suggestions
    private void showSearchAndSuggestDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        android.widget.EditText searchInput = new android.widget.EditText(this);
        searchInput.setHint("Search all app users...");
        layout.addView(searchInput);

        android.widget.TextView txtTitle = new android.widget.TextView(this);
        txtTitle.setTextColor(Color.GRAY);
        layout.addView(txtTitle);

        android.widget.ListView listView = new android.widget.ListView(this);
        layout.addView(listView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Find & Add Family")
                .setView(layout)
                .setNegativeButton("Close", null)
                .show();

        // 1. Get Local Contacts safely
        List<String> myContacts = new ArrayList<>();
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try (android.database.Cursor c = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)) {
                while (c != null && c.moveToNext()) {
                    int phoneIdx = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (phoneIdx >= 0) {
                        String phone = c.getString(phoneIdx);
                        if (phone != null) {
                            String cleanPhone = phone.replaceAll("[^0-9]", "");
                            if (cleanPhone.length() >= 10) {
                                myContacts.add(cleanPhone.substring(cleanPhone.length() - 10));
                            }
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 2. Fetch Users & Setup Search/Suggestions
        db.collection("users").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                txtTitle.setText("\nFailed to load users.");
                return;
            }

            List<DocumentSnapshot> allUsers = task.getResult().getDocuments();

            Runnable updateList = () -> {
                String query = searchInput.getText().toString().trim().toLowerCase();
                boolean isSearching = !query.isEmpty();
                txtTitle.setText(isSearching ? "\nSearch Results:" : "\nSuggested Contacts on Roots:");

                List<String> names = new ArrayList<>();
                List<String> uids = new ArrayList<>();

                for (DocumentSnapshot doc : allUsers) {
                    String uid = doc.getId();
                    String phone = doc.getString("phone");
                    String name = doc.getString("fullName");

                    if (name == null) name = doc.getString("name"); // Fallback
                    if (name == null || uid.equals(currentUid)) continue;

                    boolean isContact = false;
                    if (phone != null) {
                        String cleanDbPhone = phone.replaceAll("[^0-9]", "");
                        if (cleanDbPhone.length() >= 10) {
                            isContact = myContacts.contains(cleanDbPhone.substring(cleanDbPhone.length() - 10));
                        }
                    }

                    boolean alreadyAdded = nicknameMap.containsKey(uid);

                    if (isSearching) {
                        // Jab type karein, toh naam match hone par sabko dikhao
                        if (name.toLowerCase().contains(query)) {
                            String displayName = name;
                            if (isContact) displayName += " (Contact)";
                            if (alreadyAdded) displayName += " (Already Added)";

                            names.add(displayName);
                            uids.add(uid);
                        }
                    } else {
                        // Jab box khali ho, sirf Contacts dikhao jo add nahi hue hain
                        if (isContact && !alreadyAdded) {
                            names.add(name + " (Contact)");
                            uids.add(uid);
                        }
                    }
                }

                if (names.isEmpty()) {
                    txtTitle.setText(isSearching ? "\nNo users found for '" + query + "'." : "\nNo new suggestions from your contacts.");
                }

                android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
                listView.setAdapter(adapter);

                listView.setOnItemClickListener((p, v, pos, id) -> {
                    dialog.dismiss();
                    String selectedUid = uids.get(pos);
                    String selectedName = names.get(pos).replace(" (Contact)", "").replace(" (Already Added)", "");

                    // Instagram jaisa navigation: User ki UID aur Naam ProfileActivity me bhejo
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    intent.putExtra("targetUid", selectedUid);
                    intent.putExtra("targetName", selectedName);
                    startActivity(intent);
                });
            };

            updateList.run(); // Initial run

            // 3. Live Search Listener
            searchInput.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                public void onTextChanged(CharSequence s, int st, int b, int c) { updateList.run(); }
                public void afterTextChanged(android.text.Editable s) {}
            });
        });
    }


    private void loadDynamicCategories() {
        layoutCategories.removeAllViews();
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> customFeeds = prefs.getStringSet("categories", null);

        if (customFeeds == null || customFeeds.isEmpty()) {
            customFeeds = new HashSet<>();
            customFeeds.add("Mom Side");
            customFeeds.add("Dad Side");
            prefs.edit().putStringSet("categories", customFeeds).apply();
        }

        List<String> allFeeds = new ArrayList<>(customFeeds);
        java.util.Collections.sort(allFeeds, String.CASE_INSENSITIVE_ORDER);

        if (!allFeeds.contains(currentCategory) && !allFeeds.isEmpty()) {
            currentCategory = allFeeds.get(0);
        }

        for (String feed : allFeeds) {
            if (feed.equalsIgnoreCase("Everyone")) continue;
            addCategoryButton(feed);
        }

        Button btnPlus = new Button(this);
        btnPlus.setText("+");
        btnPlus.setTextColor(Color.parseColor("#424242"));
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(100f);
        shape.setColor(Color.WHITE);
        shape.setStroke(2, Color.parseColor("#E0E0E0"));
        btnPlus.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 0);
        btnPlus.setLayoutParams(params);
        btnPlus.setOnClickListener(v -> showAddCategoryDialog());
        layoutCategories.addView(btnPlus);
    }

    private void addCategoryButton(String categoryName) {
        Button btn = new Button(this);
        btn.setText(categoryName);
        btn.setTextColor(Color.parseColor("#424242"));
        btn.setPadding(40, 12, 40, 12);
        btn.setAllCaps(false);
        btn.setTextSize(16);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setElevation(2f);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(100f);
        shape.setColor(Color.WHITE);
        shape.setStroke(2, Color.parseColor("#E0E0E0"));
        btn.setBackground(shape);

        if (categoryName.equals(currentCategory)) {
            btn.setTextColor(Color.WHITE);
            shape.setColor(Color.parseColor("#2196F3"));
            shape.setStroke(2, Color.parseColor("#1976D2"));
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 0);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            currentCategory = categoryName;
            loadDynamicCategories();
            fetchFamilyPosts(categoryName);
        });

        layoutCategories.addView(btn);

        btn.setOnLongClickListener(v -> {
            inviteCategoryTemp = categoryName;
            Intent pickContact = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            contactPickerLauncher.launch(pickContact);
            return true;
        });
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter Feed Name (e.g. Cousin)");
        input.setPadding(50, 50, 50, 50);

        new AlertDialog.Builder(this)
                .setTitle("Add Custom Feed")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String newFeed = input.getText().toString().trim();
                    if (!newFeed.isEmpty()) {
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        Set<String> customFeeds = prefs.getStringSet("categories", new HashSet<>());
                        Set<String> updatedFeeds = new HashSet<>(customFeeds);
                        updatedFeeds.add(newFeed);
                        prefs.edit().putStringSet("categories", updatedFeeds).apply();
                        loadDynamicCategories();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadNicknamesAndPosts() {
        if (currentUid == null) return;

        db.collection("family_connections")
                .whereEqualTo("ownerUid", currentUid)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        nicknameMap.clear();
                        categoryMap.clear();
                        for (DocumentSnapshot doc : value) {
                            String targetUid = doc.getString("targetUid");
                            nicknameMap.put(targetUid, doc.getString("customName"));
                            categoryMap.put(targetUid, doc.getString("category"));
                        }
                        fetchFamilyPosts(currentCategory);
                    }
                });
    }

    private void fetchFamilyPosts(String side) {
        if (feedListener != null) {
            feedListener.remove();
        }

        feedListener = db.collection("posts")
                .whereArrayContains("visibleTo", currentUid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        postList.clear();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            Post p = doc.toObject(Post.class);
                            p.setPostId(doc.getId());

                            if (side.equals("Everyone") || (p.getVisibleTo() != null && p.getVisibleTo().contains("Everyone"))) {
                                postList.add(p);
                            } else if (p.getAuthorUid().equals(currentUid)) {
                                if (p.getAuthorFeeds() != null && p.getAuthorFeeds().contains(side)) {
                                    postList.add(p);
                                }
                            } else {
                                String authorsCategoryInMyPhone = categoryMap.get(p.getAuthorUid());
                                if (authorsCategoryInMyPhone != null && authorsCategoryInMyPhone.equals(side)) {
                                    postList.add(p);
                                }
                            }
                        }
                        if (postAdapter != null) {
                            postAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void sendSmsInvite(String phoneNumber, String category) {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:" + phoneNumber));
        smsIntent.putExtra("sms_body", "Hey! Join my '" + category + "' feed on the Roots Family App. Download here: [App Link]");
        startActivity(smsIntent);
    }

    private void checkSuggestedUsers() {
        List<String> myContacts = new ArrayList<>();
        try (android.database.Cursor cursor = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int index = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (index >= 0) {
                        String phone = cursor.getString(index).replaceAll("[^0-9]", "");
                        if (phone.length() >= 10) {
                            myContacts.add(phone.substring(phone.length() - 10));
                        }
                    }
                }
            }
        }

        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String uid = doc.getId();
                String phone = doc.getString("phone");
                String name = doc.getString("fullName");

                if (uid.equals(currentUid) || nicknameMap.containsKey(uid) || phone == null)
                    continue;

                String cleanPhone = phone.replaceAll("[^0-9]", "");
                if (cleanPhone.length() >= 10 && myContacts.contains(cleanPhone.substring(cleanPhone.length() - 10))) {
                    showSuggestionNotification(uid, name);
                    break;
                }
            }
        });
    }

    private void showSuggestionNotification(String targetUid, String realName) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        android.widget.EditText etCustomName = new android.widget.EditText(this);
        etCustomName.setHint("Nickname (Optional)");
        etCustomName.setText(realName);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        java.util.Set<String> customFeeds = prefs.getStringSet("categories", new java.util.HashSet<>());

        List<String> allFeeds = new java.util.ArrayList<>();
        if (customFeeds != null) {
            allFeeds.addAll(customFeeds);
        }
        allFeeds.remove("Everyone");

        android.widget.TextView tvLabel = new android.widget.TextView(this);
        tvLabel.setText("Select Feed:");
        tvLabel.setPadding(10, 20, 10, 10);

        android.widget.Spinner spinnerCategory = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, allFeeds);
        spinnerCategory.setAdapter(spinnerAdapter);

        layout.addView(etCustomName);
        layout.addView(tvLabel);
        layout.addView(spinnerCategory);

        new AlertDialog.Builder(this)
                .setTitle("New Family Member on Roots! 🎉")
                .setMessage("Your contact " + realName + " is on Roots. Add them to your feed?")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String customName = etCustomName.getText().toString().trim();
                    String category = spinnerCategory.getSelectedItem().toString();

                    if (customName.isEmpty()) {
                        customName = realName;
                    }

                    Map<String, Object> connection = new HashMap<>();
                    connection.put("ownerUid", currentUid);
                    connection.put("targetUid", targetUid);
                    connection.put("customName", customName);
                    connection.put("category", category);

                    db.collection("family_connections").add(connection)
                            .addOnSuccessListener(doc -> {
                                Toast.makeText(this, "Added to " + category, Toast.LENGTH_SHORT).show();
                                loadNicknamesAndPosts();
                            });
                })
                .setNegativeButton("Skip for now", null)
                .show();
    }
}