package com.example.floodrescue;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmergencyAssistanceBottomSheet extends BottomSheetDialogFragment {

    // Member variables declared at class level to avoid scope errors
    private EditText etDescription;
    private Spinner spinnerCategory;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // FIXED: Pointing to your actual layout name
        View view = inflater.inflate(R.layout.layout_report_incident_sheet, container, false);

        // Initialize Services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // FIXED: Using the specific IDs from your XML
        etDescription = view.findViewById(android.R.id.edit); // You need to add android:id="@+id/etDescription" to your EditText in XML
        spinnerCategory = view.findViewById(R.id.spinnerReportCategory);

        setupSpinner();
        getCurrentLocation();

        // FIXED: Click listeners for your specific button IDs
        view.findViewById(R.id.btnCancelReport).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSendReport).setOnClickListener(v -> submitReport());

        return view;
    }

    private void setupSpinner() {
        String[] categories = {"Flood", "Shelter", "Roadblock"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                }
            });
        }
    }

    private void submitReport() {
        // Ensure you give your EditText an ID in the XML first!
        String description = "";
        EditText actualET = getView().findViewById(R.id.etDescription); // Add this ID to XML
        if(actualET != null) description = actualET.getText().toString().trim();

        String selectedType = spinnerCategory.getSelectedItem().toString();

        Map<String, Object> report = new HashMap<>();
        report.put("type", selectedType); // Vital for dashboard filtering
        report.put("description", description);
        report.put("latitude", currentLat);
        report.put("longitude", currentLng);
        report.put("timestamp", System.currentTimeMillis());

        db.collection("reports").add(report)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(requireContext(), "Incident reported as " + selectedType, Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}