package com.example.floodrescue; // Make sure this matches your folder name!

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Load the Welcome Screen Design
        setContentView(R.layout.activity_main);

        // 2. Find the "Start Using App" button
        Button btnStart = findViewById(R.id.btnStart);
        TextView tvLogin = findViewById(R.id.tvLoginLink);

        // 3. When clicked, go to LoginActivity
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // 4. The "Log In" text link does the same thing
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}