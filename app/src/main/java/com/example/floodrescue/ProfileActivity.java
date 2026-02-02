package com.example.floodrescue;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    // Variable to track which contact's photo we are changing
    private ImageView pendingAvatarView = null;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // 1. IMAGE PICKER REGISTRY
    private final ActivityResultLauncher<String> pickContactImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && pendingAvatarView != null) {
                    pendingAvatarView.setImageURI(uri); // Set the photo on the specific row
                    // Optional: Make it round if you have a library, or just scale it
                    pendingAvatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 2. Setup standard buttons (Back, Edit, Logout)
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            // When going to Edit, we can reload when we come back
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnLogoutProfile).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 3. LOAD REAL USER DATA
        loadUserProfile();

        // ... (Keep your existing Emergency Contact logic below here) ...
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // A. Update Simple Info from Login (Name & Email)
            TextView tvName = findViewById(R.id.tvProfileName); // Make sure you have this ID in XML
            TextView tvEmail = findViewById(R.id.tvProfileEmail); // Make sure you have this ID in XML

            // Set defaults from Google Login
            tvName.setText(user.getDisplayName());
            tvEmail.setText(user.getEmail());

            // B. Fetch Extra Info (Phone, Blood Type, etc.) from Database
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            // If we have saved specific data, override the display
                            String dbName = document.getString("fullName");
                            String phone = document.getString("phone");

                            if (dbName != null) tvName.setText(dbName);

                            // Example: If you had a TextView for phone, you would set it here
                            // TextView tvPhone = findViewById(R.id.tvProfilePhone);
                            // tvPhone.setText(phone);
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data every time we come back to this screen (e.g., after Editing)
        loadUserProfile();
    }

    // --- HELPER 1: SHOW DIALOG (For both Adding AND Editing) ---
    private void showContactDialog(LinearLayout container, View existingView, String oldName, String oldRelation, String oldPhone) {

        // Setup the Input Layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Contact Name");
        if (oldName != null) inputName.setText(oldName); // Pre-fill if editing
        dialogLayout.addView(inputName);

        final EditText inputRelation = new EditText(this);
        inputRelation.setHint("Relationship (e.g. Wife)");
        if (oldRelation != null) inputRelation.setText(oldRelation);
        dialogLayout.addView(inputRelation);

        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("Phone Number");
        inputPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if (oldPhone != null) inputPhone.setText(oldPhone);
        dialogLayout.addView(inputPhone);

        String title = (existingView == null) ? "Add Contact" : "Edit Contact";
        String btnText = (existingView == null) ? "Add" : "Update";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogLayout)
                .setPositiveButton(btnText, (dialog, which) -> {
                    String name = inputName.getText().toString();
                    String relation = inputRelation.getText().toString();
                    String phone = inputPhone.getText().toString();

                    if (!name.isEmpty()) {
                        if (existingView == null) {
                            // CASE 1: CREATE NEW ROW
                            addContactRow(container, name, relation, phone);
                        } else {
                            // CASE 2: UPDATE EXISTING ROW
                            updateContactRow(existingView, name, relation, phone);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- HELPER 2: ADD NEW ROW ---
    private void addContactRow(LinearLayout container, String name, String relation, String phone) {
        View rowView = getLayoutInflater().inflate(R.layout.item_emergency_contact, container, false);

        // 1. Setup Clickable Image (To Change Photo)
        ImageView avatar = rowView.findViewById(R.id.imgContactAvatar); // We need to add this ID in XML!
        if(avatar == null) avatar = (ImageView) ((LinearLayout)rowView).getChildAt(0); // Fallback if ID missing

        ImageView finalAvatar = avatar;
        avatar.setOnClickListener(v -> {
            pendingAvatarView = finalAvatar; // Remember this is the row we are changing
            pickContactImage.launch("image/*");
        });

        // 2. Setup Clickable Text (To Edit Details)
        LinearLayout textLayout = (LinearLayout) ((LinearLayout)rowView).getChildAt(1);
        textLayout.setOnClickListener(v -> {
            // Get current values to pre-fill
            TextView tvName = rowView.findViewById(R.id.tvContactName);
            TextView tvRel = rowView.findViewById(R.id.tvContactRelation);

            // We have to parse the string "● Wife • 555-1234" back into parts
            String currentName = tvName.getText().toString();
            String fullRelString = tvRel.getText().toString();
            String currentRel = "";
            String currentPhone = "";

            // Simple logic to split the string back up
            if(fullRelString.contains("•")) {
                String[] parts = fullRelString.replace("● ", "").split(" • ");
                if(parts.length > 0) currentRel = parts[0];
                if(parts.length > 1) currentPhone = parts[1];
            }

            showContactDialog(null, rowView, currentName, currentRel, currentPhone);
        });

        // 3. Setup Delete Button
        rowView.findViewById(R.id.btnDeleteContact).setOnClickListener(v -> {
            container.removeView(rowView);
            Toast.makeText(this, "Contact Deleted", Toast.LENGTH_SHORT).show();
        });

        updateContactRow(rowView, name, relation, phone); // Fill text
        container.addView(rowView);
    }

    // --- HELPER 3: UPDATE EXISTING ROW ---
    private void updateContactRow(View rowView, String name, String relation, String phone) {
        TextView tvName = rowView.findViewById(R.id.tvContactName);
        TextView tvRelation = rowView.findViewById(R.id.tvContactRelation);

        tvName.setText(name);
        tvRelation.setText("● " + relation + " • " + phone);
    }
}