package com.example.ipollusen;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.example.ipollusen.R;
public class MapsFragment extends Fragment {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = "MapsFragment";
    private static final String CSV_FILE_PATH = "/storage/emulated/0/Android/data/com.example.ipollusen/files/sensor_data.csv";
    private static final int TRAIL_COLOR = 0xFF00FF00; // Green color for the trail
    private static final float TRAIL_STROKE_WIDTH = 5.0f;

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_maps, container, false);

        // Initialize the osmdroid configuration
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        // Setup the MapView
        mapView = rootView.findViewById(R.id.mapView);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Setup MyLocation overlay
        setupMyLocationOverlay();

        // Request necessary permissions
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        });

        // Load markers and trails from the CSV file
        loadMarkersAndTrailFromCSV();

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Get the current location and update the map center
        getCurrentLocation();

        return rootView;
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Create a GeoPoint based on the current location
                            GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            // Set the map's center to the current location
                            mapView.getController().setCenter(currentLocation);
                        } else {
                            Log.e(TAG, "Failed to get current location");
                        }
                    }
                });
    }

    private void loadMarkersAndTrailFromCSV() {
        File csvFile = new File(CSV_FILE_PATH);

        if (!csvFile.exists()) {
            Log.e(TAG, "CSV file not found at: " + CSV_FILE_PATH);
            return;
        }

        List<GeoPoint> geoPoints = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstRow = true; // Flag to skip the header row
            while ((line = br.readLine()) != null) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue; // Skip the header row
                }

                String[] data = line.split(","); // Use comma as the delimiter (change based on your CSV format)

                // Ensure there are at least two columns for latitude and longitude
                if (data.length < 2) {
                    Log.w(TAG, "Skipping row due to insufficient columns: " + line);
                    continue;
                }

                // Latitude and Longitude are the last two columns
                String latitudeStr = data[data.length - 2];
                String longitudeStr = data[data.length - 1];

                // Skip rows with missing latitude or longitude
                if (latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
                    Log.w(TAG, "Skipping row due to missing latitude/longitude: " + line);
                    continue;
                }

                try {
                    double latitude = Double.parseDouble(latitudeStr);
                    double longitude = Double.parseDouble(longitudeStr);

                    // Skip rows where latitude or longitude is 0.0 (invalid location)
                    if (latitude == 0.0 || longitude == 0.0) {
                        Log.w(TAG, "Skipping row with invalid location (0.0, 0.0): " + line);
                        continue;
                    }

                    GeoPoint point = new GeoPoint(latitude, longitude);
                    geoPoints.add(point);

                    // Add a marker for each valid point
                    Marker marker = new Marker(mapView);
                    marker.setPosition(point);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle("Lat: " + latitude + ", Lon: " + longitude);
                    mapView.getOverlays().add(marker);

                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid latitude/longitude in row: " + line, e);
                }
            }

            // Draw a trail if there are multiple valid points
            if (geoPoints.size() > 1) {
                Polyline trail = new Polyline();
                trail.setPoints(geoPoints);
                trail.getOutlinePaint().setColor(TRAIL_COLOR);
                trail.getOutlinePaint().setStrokeWidth(TRAIL_STROKE_WIDTH);
                mapView.getOverlays().add(trail);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file", e);
        }
    }


    private void setupMyLocationOverlay() {
        // You may use a location provider to get the current location and display it on the map
        myLocationOverlay = new MyLocationNewOverlay(mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted && myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
                myLocationOverlay.enableFollowLocation();
                getCurrentLocation(); // Set current location once permissions are granted
            } else {
                Log.e(TAG, "Permissions not granted");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }
}
