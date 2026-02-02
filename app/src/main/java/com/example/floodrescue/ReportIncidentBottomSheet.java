package com.example.floodrescue;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportIncidentBottomSheet extends BottomSheetDialogFragment {

    private ImageView imgPreview;
    private ImageView btnRemoveImage;
    private LinearLayout layoutPlaceholder;
    private Spinner spinnerCategory;
    private EditText etDescription;
    private TextView tvAddress, tvLatLng;
    private Uri selectedImageUri = null;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private final ActivityResultLauncher<String> pickMedia = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPreview.setImageURI(uri);
                    imgPreview.setVisibility(View.VISIBLE);
                    btnRemoveImage.setVisibility(View.VISIBLE);
                    layoutPlaceholder.setVisibility(View.GONE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_report_incident_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Bind UI Elements
        View btnUpload = view.findViewById(R.id.btnUploadEvidence);
        imgPreview = view.findViewById(R.id.imgPreview);
        btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        layoutPlaceholder = view.findViewById(R.id.layoutPlaceholder);
        spinnerCategory = view.findViewById(R.id.spinnerReportCategory);
        etDescription = view.findViewById(R.id.etDescription);
        tvAddress = view.findViewById(R.id.tvReportAddress);
        tvLatLng = view.findViewById(R.id.tvReportLatLng);

        setupSpinner();
        getCurrentLocation();

        btnUpload.setOnClickListener(v -> pickMedia.launch("image/*"));

        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            imgPreview.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
            layoutPlaceholder.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.btnCancelReport).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSendReport).setOnClickListener(v -> submitReport());
    }

    private void setupSpinner() {
        String[] categories = {"Flood", "Shelter", "Roadblock"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, categories) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    tvLatLng.setText(String.format(Locale.getDefault(), "Lat: %.4f, Long: %.4f", currentLat, currentLng));
                    updateAddressText(currentLat, currentLng);
                }
            });
        }
    }

    private void updateAddressText(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressStr = address.getAddressLine(0);
                tvAddress.setText(addressStr);
            }
        } catch (Exception e) {
            tvAddress.setText("Address unavailable");
        }
    }

    private void submitReport() {
        if (etDescription == null || spinnerCategory == null) return;

        String desc = etDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (desc.isEmpty()) {
            etDescription.setError("Description required");
            return;
        }

        if (selectedImageUri != null) {
            uploadImageToStorage(desc, category);
        } else {
            saveReportToFirestore(desc, category, null);
        }
    }

    private void uploadImageToStorage(String desc, String category) {
        com.google.firebase.storage.StorageReference storageRef =
                com.google.firebase.storage.FirebaseStorage.getInstance().getReference()
                        .child("incident_images/" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveReportToFirestore(desc, category, uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveReportToFirestore(String desc, String category, String imageUrl) {
        Map<String, Object> report = new HashMap<>();
        if (mAuth.getCurrentUser() != null) {
            report.put("userId", mAuth.getCurrentUser().getUid());
            report.put("userName", mAuth.getCurrentUser().getDisplayName());
        } else {
            report.put("userName", "Anonymous");
        }

        // CAPTURE DEVICE INFORMATION (USER-AGENT)
        String userAgent = "Android " + android.os.Build.VERSION.RELEASE + " (" + android.os.Build.MODEL + ")";
        
        report.put("type", category);
        report.put("description", desc);
        report.put("latitude", currentLat);
        report.put("longitude", currentLng);
        report.put("address", tvAddress.getText().toString());
        report.put("timestamp", System.currentTimeMillis());
        report.put("userAgent", userAgent);

        if (imageUrl != null) {
            report.put("imageUrl", imageUrl);
        }

        db.collection("reports").add(report)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(getContext(), "Report Sent!", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog != null) {
            // FIX: Using the internal ID design_bottom_sheet safely
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(layoutParams);

                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }
}