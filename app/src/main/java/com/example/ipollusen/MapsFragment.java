package com.example.ipollusen;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.collection.BuildConfig;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapsFragment extends Fragment {

    private MapView mapView;
    private TextView locationTextView;
    private FusedLocationProviderClient fusedLocationClient; // Fused Location Provider
    private LocationCallback locationCallback; // Location updates callback
    private Marker userLocationMarker; // Marker for the user's current location

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Set up osmdroid configuration with a relevant user agent
        Context ctx = getActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);


        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize MapView
        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Initialize TextView for displaying location details
        locationTextView = view.findViewById(R.id.locationTextView);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize marker for user's current location
        userLocationMarker = new Marker(mapView);
        userLocationMarker.setIcon(getResources().getDrawable(R.drawable.marker_icon)); // Replace with your marker icon
        userLocationMarker.setTitle("You are here");
        mapView.getOverlays().add(userLocationMarker); // Add marker to map

        // Set up location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationOnMap(location);
                }
            }
        };

        // Request location permission if needed
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Start receiving location updates
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Set up the "center on location" button
        AppCompatImageButton centerButton = view.findViewById(R.id.center_button);
        centerButton.setOnClickListener(v -> centerOnCurrentLocation());
    }

    // Method to start receiving location updates
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000); // Update every 5 seconds
            locationRequest.setFastestInterval(2000); // Fastest update every 2 seconds
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // High accuracy

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    // Method to update the map and TextView with the new location
    private void updateLocationOnMap(Location location) {
        GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        userLocationMarker.setPosition(userLocation); // Update marker position
        userLocationMarker.setVisible(true); // Ensure marker is visible
        mapView.invalidate(); // Refresh the map

        // Center the map on the user's location
        IMapController mapController = mapView.getController();
        mapController.setCenter(userLocation);

        // Update the TextView with the coordinates
        String coordinates = "Latitude: " + userLocation.getLatitude() + ", Longitude: " + userLocation.getLongitude();
        locationTextView.setText(coordinates);
    }

    // Method to center the map on the current location
    private void centerOnCurrentLocation() {
        if (userLocationMarker.getPosition() != null) {
            IMapController mapController = mapView.getController();
            mapController.setCenter(userLocationMarker.getPosition());
        } else {
            Toast.makeText(requireContext(), "Current location not available yet", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume(); // Important for osmdroid map lifecycle
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause(); // Important for osmdroid map lifecycle
        fusedLocationClient.removeLocationUpdates(locationCallback); // Stop receiving location updates when fragment is paused
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDetach(); // Clean up osmdroid resources when the fragment is destroyed
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, start receiving location updates
            startLocationUpdates();
        }
    }
}
