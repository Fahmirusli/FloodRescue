package com.example.floodrescue;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnSignUp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up); // Check this matches your XML name

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Connect UI
        etName = findViewById(R.id.etName);     // Update XML ID if needed
        etEmail = findViewById(R.id.etEmail);       // Update XML ID if needed
        etPassword = findViewById(R.id.etPassword); // Update XML ID if needed
        btnSignUp = findViewById(R.id.btnSignUp);   // Update XML ID if needed

        // 3. Setup Button Logic
        btnSignUp.setOnClickListener(v -> createAccount());

        // Link to go back to Login
        TextView tvLoginLink = findViewById(R.id.tvLoginLink); // If you have a "Already have account? Login" text
        if(tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> finish());
        }
    }

    private void createAccount() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // A. Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        // B. Create User in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // C. Success! Now save their info to Database
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToDatabase(user, name);
                    } else {
                        // Failure
                        Toast.makeText(SignUpActivity.this, "Sign Up Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(FirebaseUser user, String name) {
        String uid = user.getUid();

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("fullName", name);
        newUser.put("email", user.getEmail());
        newUser.put("role", "user");
        newUser.put("phone", "");
        newUser.put("created_at", System.currentTimeMillis());

        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                    // D. Go to Dashboard (or Verification Page if you prefer)
                    Intent intent = new Intent(SignUpActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                });
    }
}