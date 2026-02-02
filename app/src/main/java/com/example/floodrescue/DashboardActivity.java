package com.example.floodrescue;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private JSONArray hourlyTimeArray, hourlyTempArray, hourlyCodeArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupButtons();
        updateDashboardName();
    }

    private void setupButtons() {
        // Report Incident Button
        findViewById(R.id.btnReport).setOnClickListener(v -> {
            ReportIncidentBottomSheet reportSheet = new ReportIncidentBottomSheet();
            reportSheet.show(getSupportFragmentManager(), "ReportIncidentSheet");
        });

        // SOS Button
        findViewById(R.id.btnSOS).setOnClickListener(v -> {
            EmergencyAssistanceBottomSheet sosSheet = new EmergencyAssistanceBottomSheet();
            sosSheet.show(getSupportFragmentManager(), "EmergencySheet");
        });

        findViewById(R.id.btnZoomIn).setOnClickListener(v -> { if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomIn()); });
        findViewById(R.id.btnZoomOut).setOnClickListener(v -> { if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomOut()); });
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> getUserLocation());

        findViewById(R.id.btnCalendar).setOnClickListener(v -> showHourlyForecast());

        findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.imgAvatarDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // --- FILTER CHIPS WITH COLOR FEEDBACK ---
        findViewById(R.id.chipShelters).setOnClickListener(v -> loadMapMarkers("Shelter", v));
        findViewById(R.id.chipFloods).setOnClickListener(v -> loadMapMarkers("Flood", v));
        findViewById(R.id.chipRoads).setOnClickListener(v -> loadMapMarkers("Roadblock", v));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setCompassEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    private void loadMapMarkers(String type, View selectedChip) {
        if (mMap == null) return;

        // 1. Clear existing markers and update chip colors
        mMap.clear();
        resetChipColors(); // Helper to set all chips back to default gray
        selectedChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4ED8")));

        // 2. Query Firebase for the specific type
        db.collection("reports")
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Use Double (Capital D) to allow for null values without crashing
                        Double lat = document.getDouble("latitude");
                        Double lng = document.getDouble("longitude");
                        String desc = document.getString("description");

                        // CRITICAL NULL CHECK: Only add marker if both coordinates exist
                        if (lat != null && lng != null) {
                            LatLng pos = new LatLng(lat, lng);

                            // Choose marker color based on type
                            float hue = BitmapDescriptorFactory.HUE_RED;
                            if (type.equals("Shelter")) hue = BitmapDescriptorFactory.HUE_AZURE;
                            else if (type.equals("Roadblock")) hue = BitmapDescriptorFactory.HUE_ORANGE;

                            mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(type)
                                    .snippet(desc)
                                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading markers", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetChipColors() {
        int gray = Color.parseColor("#374151");
        findViewById(R.id.chipShelters).setBackgroundTintList(ColorStateList.valueOf(gray));
        findViewById(R.id.chipFloods).setBackgroundTintList(ColorStateList.valueOf(gray));
        findViewById(R.id.chipRoads).setBackgroundTintList(ColorStateList.valueOf(gray));
    }

    private void fetchWeatherData(double lat, double lon) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            String response = "";
            try {
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                        "&current=temperature_2m,relative_humidity_2m,weather_code&hourly=temperature_2m,weather_code&forecast_days=7";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                response = sb.toString();
            } catch (Exception e) { e.printStackTrace(); }

            final String res = response;
            handler.post(() -> {
                try {
                    if (res == null || res.isEmpty()) return;
                    JSONObject json = new JSONObject(res);
                    
                    // Current weather
                    JSONObject current = json.getJSONObject("current");
                    double temp = current.getDouble("temperature_2m");
                    int humidity = current.getInt("relative_humidity_2m");
                    int code = current.getInt("weather_code");

                    // Hourly forecast
                    JSONObject hourly = json.getJSONObject("hourly");
                    hourlyTimeArray = hourly.getJSONArray("time");
                    hourlyTempArray = hourly.getJSONArray("temperature_2m");
                    hourlyCodeArray = hourly.getJSONArray("weather_code");

                    // Update UI
                    TextView tvTemp = findViewById(R.id.tvWeatherTemp);
                    TextView tvCondition = findViewById(R.id.tvWeatherCondition);
                    TextView tvHumidity = findViewById(R.id.tvWeatherHumidity);

                    if (tvTemp != null) tvTemp.setText(String.format("%.0f°C", temp));
                    if (tvCondition != null) tvCondition.setText(decodeWeatherCode(code));
                    if (tvHumidity != null) tvHumidity.setText("Humidity: " + humidity + "%");

                } catch (Exception e) { e.printStackTrace(); }
            });
        });
    }

    private void showHourlyForecast() {
        if (hourlyTimeArray == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.bottom_sheet_forecast);

        LinearLayout dateBox = dialog.findViewById(R.id.layoutDateSelector);
        LinearLayout contentBox = dialog.findViewById(R.id.layoutForecastContainer);
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        java.util.Calendar cal = java.util.Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Button b = new Button(this);
            int dIdx = (cal.get(java.util.Calendar.DAY_OF_WEEK) + i - 1) % 7;
            b.setText(days[dIdx]);
            b.setTextColor(Color.WHITE);
            b.setBackgroundColor(Color.TRANSPARENT);
            final int dayOffset = i;
            b.setOnClickListener(v -> displayDayForecast(contentBox, dayOffset));
            dateBox.addView(b);
        }
        displayDayForecast(contentBox, 0);
        dialog.show();
    }

    private void displayDayForecast(LinearLayout container, int dayIndex) {
        container.removeAllViews();
        try {
            int startHour = dayIndex * 24;
            for (int i = startHour; i < startHour + 24 && i < hourlyTimeArray.length(); i++) {
                View row = getLayoutInflater().inflate(R.layout.item_forecast_row, container, false);

                String rawTime = hourlyTimeArray.getString(i);
                TextView tvTime = row.findViewById(R.id.tvRowTime);
                TextView tvTemp = row.findViewById(R.id.tvRowTemp);
                TextView tvCond = row.findViewById(R.id.tvRowCondition);
                TextView tvIcon = row.findViewById(R.id.tvRowIcon);

                if (tvTime != null) tvTime.setText(rawTime.substring(rawTime.indexOf("T") + 1));
                if (tvTemp != null) tvTemp.setText(String.format("%.0f°C", hourlyTempArray.getDouble(i)));
                if (tvCond != null) tvCond.setText(decodeWeatherCode(hourlyCodeArray.getInt(i)));
                if (tvIcon != null) tvIcon.setText(getWeatherEmoji(hourlyCodeArray.getInt(i)));

                container.addView(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String decodeWeatherCode(int c) {
        if (c <= 3) return "Clear/Cloudy";
        if (c <= 67) return "Rain";
        if (c >= 80) return "Storm";
        return "Foggy";
    }

    private String getWeatherEmoji(int c) {
        if (c <= 3) return "☀️";
        if (c <= 67) return "🌧️";
        if (c >= 80) return "⛈️";
        return "🌫️";
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        mMap.setMyLocationEnabled(true);
        getUserLocation();
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                fetchWeatherData(location.getLatitude(), location.getLongitude());
                checkSafeZoneStatus(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void checkSafeZoneStatus(double lat, double lon) {
        TextView tv = findViewById(R.id.tvWeatherStatus);
        if (tv == null) return;
        
        db.collection("reports")
                .whereEqualTo("type", "Flood")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean dangerFound = false;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double reportLat = document.getDouble("latitude");
                        Double reportLng = document.getDouble("longitude");
                        if (reportLat != null && reportLng != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(lat, lon, reportLat, reportLng, results);
                            if (results[0] < 5000) {
                                dangerFound = true;
                                break;
                            }
                        }
                    }
                    
                    if (dangerFound) {
                        tv.setText("DANGER: FLOOD NEARBY");
                        tv.setTextColor(Color.RED);
                    } else {
                        tv.setText("SAFE ZONE");
                        tv.setTextColor(Color.parseColor("#4ADE80"));
                    }
                });
    }

    private void updateDashboardName() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(d -> {
                if (d.exists()) {
                    TextView tv = findViewById(R.id.tvUserName);
                    if (tv != null) tv.setText(d.getString("fullName"));
                }
            });
        }
    }
}
