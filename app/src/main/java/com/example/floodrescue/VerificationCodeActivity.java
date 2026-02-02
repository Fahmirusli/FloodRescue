package com.example.floodrescue;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VerificationCodeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        String email = getIntent().getStringExtra("email");
        TextView tvEmail = findViewById(R.id.tvSentEmail);
        tvEmail.setText("Sent code to " + email);

        // Inputs
        EditText otp1 = findViewById(R.id.otp1);
        EditText otp2 = findViewById(R.id.otp2);
        EditText otp3 = findViewById(R.id.otp3);
        EditText otp4 = findViewById(R.id.otp4);

        findViewById(R.id.btnBackVerify).setOnClickListener(v -> finish());

        findViewById(R.id.btnVerify).setOnClickListener(v -> {
            String code = otp1.getText().toString() + otp2.getText().toString() +
                    otp3.getText().toString() + otp4.getText().toString();

            // Inside the btnVerify listener...
            if (code.equals("1234")) {
                Toast.makeText(this, "Verified!", Toast.LENGTH_SHORT).show();

                // CHECK WHERE WE CAME FROM
                String source = getIntent().getStringExtra("source");

                if ("signup".equals(source)) {
                    // Came from Sign Up -> Go to Dashboard
                    Intent intent = new Intent(this, LoginActivity.class);
                    // This clears the history so you can't back-button to login
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    // Came from Forgot Password -> Go to Reset Password
                    startActivity(new Intent(this, ResetPasswordActivity.class));
                }
                finish();
            }
        });
    }
}