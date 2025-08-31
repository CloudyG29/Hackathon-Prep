package com.example.hackathonprep.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hackathonprep.R;
import com.example.hackathonprep.databinding.FragmentHomeBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Spinner spinnerDangerTypes;
    private FusedLocationProviderClient fusedLocationClient;
    private FragmentHomeBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // Sample data source for danger areas
    private List<DangerArea> dangerAreas = new ArrayList<>();
    private String[] dangerTypes = {"All", "High Crime", "GBV Hotspot", "Unsafe at Night"};
    private String selectedFilterType = "All";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        spinnerDangerTypes = root.findViewById(R.id.spinnerDangerTypes);

        // Populate sample data
        populateSampleData();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                dangerTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDangerTypes.setAdapter(adapter);

        spinnerDangerTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilterType = dangerTypes[position];
                Toast.makeText(requireContext(), "Selected: " + selectedFilterType, Toast.LENGTH_SHORT).show();

                // Call the new method to update markers
                updateMapMarkersAndCircles();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Set up the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return root;
    }

    private void populateSampleData() {
        // Dummy data for demonstration around Johannesburg/Pretoria area.
        // Radius values are in meters.
        dangerAreas.add(new DangerArea(new LatLng(-26.1952, 28.0340), "High Crime", "Central Johannesburg Market", 150)); // Egoli
        dangerAreas.add(new DangerArea(new LatLng(-26.2041, 28.0473), "GBV Hotspot", "Hillbrow Area", 100)); // Hillbrow
        dangerAreas.add(new DangerArea(new LatLng(-25.7479, 28.2293), "Unsafe at Night", "Hatfield Square", 80)); // Hatfield, Pretoria
        dangerAreas.add(new DangerArea(new LatLng(-26.1211, 28.0396), "High Crime", "Sandton Taxi Rank", 120)); // Sandton
        dangerAreas.add(new DangerArea(new LatLng(-26.0094, 27.9150), "Unsafe at Night", "Fourways Shopping Area", 90)); // Fourways
        dangerAreas.add(new DangerArea(new LatLng(-26.1714, 27.9009), "GBV Hotspot", "Soweto Community Park", 110)); // Soweto
    }

    private void updateMapMarkersAndCircles() {
        if (mMap == null) {
            return;
        }

        // Clear all existing markers and shapes
        mMap.clear();

        // Add the campus circle back (or remove if no longer needed)
        LatLng campus = new LatLng(-25.7545, 28.2314); // University of Pretoria main campus
        mMap.addCircle(new CircleOptions()
                .center(campus)
                .radius(200)
                .strokeColor(Color.parseColor("#448AFF")) // Light blue
                .fillColor(0x33448AFF)); // Semi-transparent light blue

        // Iterate through the danger areas and add markers and circles based on the filter
        for (DangerArea area : dangerAreas) {
            if (selectedFilterType.equals("All") || area.type.equals(selectedFilterType)) {
                // Add a marker for the center of the danger area
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(area.location)
                        .title(area.title)
                        .snippet(area.type);

                // Define colors for circles
                int circleStrokeColor = Color.BLACK;
                int circleFillColor = 0x00000000; // Transparent default

                // Customize marker colors and circle colors based on type
                switch (area.type) {
                    case "High Crime":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        circleStrokeColor = Color.parseColor("#FF0000"); // Red
                        circleFillColor = 0x33FF0000; // Semi-transparent red
                        break;
                    case "GBV Hotspot":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                        circleStrokeColor = Color.parseColor("#FF00FF"); // Magenta
                        circleFillColor = 0x33FF00FF; // Semi-transparent magenta
                        break;
                    case "Unsafe at Night":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        circleStrokeColor = Color.parseColor("#FFA500"); // Orange
                        circleFillColor = 0x33FFA500; // Semi-transparent orange
                        break;
                }
                mMap.addMarker(markerOptions);

                // Add a circle to visually represent the danger area
                mMap.addCircle(new CircleOptions()
                        .center(area.location)
                        .radius(area.radius) // Use the radius from DangerArea
                        .strokeColor(circleStrokeColor)
                        .strokeWidth(3) // Make the stroke a bit wider for visibility
                        .fillColor(circleFillColor));
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Initially populate the map with all markers and circles
        updateMapMarkersAndCircles();

        // Check location permission and move camera safely
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            moveToUserLocation();
        }
        // Set a default camera position if user location isn't immediately available or permitted
        // This will move the camera to a general area of Johannesburg/Pretoria
        LatLng defaultLocation = new LatLng(-26.2041, 28.0473); // Central Johannesburg
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now it's safe to get the location
                moveToUserLocation();
            } else {
                // Permission denied, handle gracefully (e.g., show a message)
                Toast.makeText(requireContext(), "Location permission denied. Cannot show user's location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void moveToUserLocation() {
        // Double-check permission before using location
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (mMap == null) return; // extra safety

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15)); // Use animateCamera for smoother transition
                } else {
                    // Fallback if location is null, remains on the default set in onMapReady
                    Toast.makeText(requireContext(), "Could not retrieve current location.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}