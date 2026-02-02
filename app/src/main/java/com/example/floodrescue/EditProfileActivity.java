package com.example.floodrescue;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    // Firebase Variables
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI Input Fields
    private EditText etName, etPhone, etEmail, etAddress, etBlood, etMobility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Connect UI variables to XML IDs
        etName = findViewById(R.id.etEditName);
        etPhone = findViewById(R.id.etEditPhone);
        etEmail = findViewById(R.id.etEditEmail);
        etAddress = findViewById(R.id.etEditAddress);
        etBlood = findViewById(R.id.etEditBlood);
        etMobility = findViewById(R.id.etEditMobility);

        // 3. Load existing data immediately
        loadCurrentData();

        // 4. Setup "Save Changes" Button
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileChanges());

        // 5. Setup "Cancel" Button
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    // --- HELPER 1: FETCH DATA FROM DATABASE ---
    private void loadCurrentData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // First, set the email from Auth (cannot be changed easily, so we usually disable this field or leave it read-only)
            etEmail.setText(user.getEmail());
            etEmail.setEnabled(false); // Make email un-editable for security if you wish

            // Now fetch the rest from Firestore
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            // Safely get strings (handles nulls if data is missing)
                            String name = document.getString("fullName");
                            String phone = document.getString("phone");
                            String address = document.getString("address");
                            String blood = document.getString("bloodType");
                            String mobility = document.getString("mobility");

                            // Populate the fields
                            if (name != null) etName.setText(name);
                            if (phone != null) etPhone.setText(phone);
                            if (address != null) etAddress.setText(address);
                            if (blood != null) etBlood.setText(blood);
                            if (mobility != null) etMobility.setText(mobility);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // --- HELPER 2: SAVE DATA TO DATABASE ---
    private void saveProfileChanges() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Get text from inputs
        String newName = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newAddress = etAddress.getText().toString().trim();
        String newBlood = etBlood.getText().toString().trim();
        String newMobility = etMobility.getText().toString().trim();

        // Basic Validation
        if (newName.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        // Prepare Data Map
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("fullName", newName);
        updateMap.put("phone", newPhone);
        updateMap.put("address", newAddress);
        updateMap.put("bloodType", newBlood);
        updateMap.put("mobility", newMobility);

        // Send to Firestore
        db.collection("users").document(user.getUid())
                .update(updateMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close screen and return to Profile
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}