package com.example.floodrescue;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        EditText etEmail = findViewById(R.id.etForgotEmail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());

        findViewById(R.id.btnSendReset).setOnClickListener(v -> {
            if(etEmail.getText().toString().isEmpty()) {
                etEmail.setError("Required");
                return;
            }
            // Go to Verification Screen
            // In ForgotPasswordActivity, when clicking "Send Link"
            Intent intent = new Intent(ForgotPasswordActivity.this, VerificationCodeActivity.class);
            intent.putExtra("email", etEmail.getText().toString());
            intent.putExtra("source", "forgot"); // <--- ADD THIS LINE
            startActivity(intent);
        });
    }
}