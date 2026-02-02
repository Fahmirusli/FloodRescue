package com.example.floodrescue;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        EditText etPass1 = findViewById(R.id.etNewPass);
        EditText etPass2 = findViewById(R.id.etConfirmPass);

        findViewById(R.id.btnBackReset).setOnClickListener(v -> finish());

        findViewById(R.id.btnResetFinal).setOnClickListener(v -> {
            String p1 = etPass1.getText().toString();
            String p2 = etPass2.getText().toString();

            if (p1.isEmpty() || !p1.equals(p2)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Password Reset Successful!", Toast.LENGTH_SHORT).show();
                // Go back to Login
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}