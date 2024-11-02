package com.example.ipollusen;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

// Import your app's BuildConfig


public class MapsFragment extends Fragment {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_maps, container, false);

        // Set up osmdroid configuration
        Configuration.getInstance().setUserAgentValue("com.example.ipollusen");


        // Initialize MapView
        mapView = rootView.findViewById(R.id.mapView);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Set a default location (Eiffel Tower)
        GeoPoint startPoint = new GeoPoint(48.8583, 2.2944);
        mapView.getController().setCenter(startPoint);

        // Add a marker at the location
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Eiffel Tower");
        mapView.getOverlays().add(startMarker);

        // Initialize MyLocationNewOverlay to show current location
        GpsMyLocationProvider locationProvider = new GpsMyLocationProvider(requireContext());
        myLocationOverlay = new MyLocationNewOverlay(locationProvider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();  // Optional: centers the map on the user's location
        mapView.getOverlays().add(myLocationOverlay);

        // Request permissions if necessary
        requestPermissionsIfNecessary(new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        });

        return rootView;
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_PERMISSIONS_REQUEST_CODE);
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // Permission denied
                    return;
                }
            }
            // Permissions granted, proceed with map initialization if required
            if (myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
                myLocationOverlay.enableFollowLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume(); // Needed for compass, my location overlays, etc.
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation(); // Re-enable location overlay
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause(); // Needed for compass, my location overlays, etc.
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation(); // Disable location overlay when paused
        }
    }
}
