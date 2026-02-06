package com.example.floodrescue;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Configure Google Sign In
        String webClientId = "796896053089-6bae0iijq0nt4pq7lla8d8to5k4bqj3c.apps.googleusercontent.com";

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 3. Setup Buttons
        setupButtons();
    }

    private void setupButtons() {
        // GOOGLE LOGIN BUTTON
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> signInWithGoogle());

        // REGULAR EMAIL LOGIN
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            EditText etEmail = findViewById(R.id.etEmail);
            EditText etPass = findViewById(R.id.etPassword);
            String email = etEmail.getText().toString();
            String password = etPass.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            signInWithEmail(email, password);
        });

        // FORGOT PASSWORD
        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // SIGN UP LINK
        findViewById(R.id.tvSignUpLink).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        // EMERGENCY REPORT BUTTON
        View btnEmergency = findViewById(R.id.btnEmergency);
        if (btnEmergency != null) {
            btnEmergency.setOnClickListener(v -> {
                EmergencyAssistanceBottomSheet sosSheet = new EmergencyAssistanceBottomSheet();
                sosSheet.show(getSupportFragmentManager(), "EmergencySheetFromLogin");
            });
        }
    }

    // --- EMAIL SIGN IN LOGIC ---
    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // --- GOOGLE SIGN IN LOGIC ---

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed
                Toast.makeText(this, "Google Sign In Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login Success
                        FirebaseUser user = mAuth.getCurrentUser();
                        syncUserToDatabase(user); // <--- NEW STEP: Save to DB
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- NEW HELPER METHOD: SYNC USER TO FIRESTORE ---
    private void syncUserToDatabase(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (user == null) return;
        String uid = user.getUid();

        // Check if this user already exists in our database
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // User exists -> Just go to Dashboard
                            updateUI(user);
                        } else {
                            // NEW USER -> Create a Profile in Database
                            Map<String, Object> newUser = new HashMap<>();
                            newUser.put("fullName", user.getDisplayName());
                            newUser.put("email", user.getEmail());
                            newUser.put("role", "user"); // Default role
                            newUser.put("phone", "");    // Empty for now
                            newUser.put("bloodType", "");
                            newUser.put("created_at", System.currentTimeMillis());

                            db.collection("users").document(uid).set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show();
                                        updateUI(user);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }


    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish(); // Prevent going back to login
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Uncomment this line if you want Auto-Login (Skip login screen if already logged in)
        // if(currentUser != null){
        //    updateUI(currentUser);
        // }
    }
}
