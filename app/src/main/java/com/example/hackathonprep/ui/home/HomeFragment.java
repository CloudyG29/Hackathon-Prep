package com.example.hackathonprep.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.hackathonprep.R;
import com.example.hackathonprep.databinding.FragmentHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private FragmentHomeBinding binding;
    private GoogleMap mMap;
    private Marker myLocationMarker;
    private Spinner spinnerDangerTypes;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private List<DangerArea> dangerAreas = new ArrayList<>();
    private String[] dangerTypes = {"All", "High Crime", "GBV Hotspot", "Unsafe at Night"};
    private String selectedFilterType = "All";

    private List<Mutual> mutuals = new ArrayList<>();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        spinnerDangerTypes = root.findViewById(R.id.spinnerDangerTypes);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupSpinner();
        populateDangerAreas();
        populateMutuals();
        setupLocationUpdates();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        binding.fabFocusMe.setOnClickListener(v -> {
            if (myLocationMarker != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocationMarker.getPosition(), 15));
            }
        });

        return root;
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, dangerTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDangerTypes.setAdapter(adapter);

        spinnerDangerTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilterType = dangerTypes[position];
                updateMapMarkersAndCircles();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void populateDangerAreas() {
        dangerAreas.add(new DangerArea(new LatLng(-26.1952, 28.0340), "High Crime", "Central Johannesburg Market", 150));
        dangerAreas.add(new DangerArea(new LatLng(-26.2041, 28.0473), "GBV Hotspot", "Hillbrow Area", 100));
        dangerAreas.add(new DangerArea(new LatLng(-25.7479, 28.2293), "Unsafe at Night", "Hatfield Square", 80));
        dangerAreas.add(new DangerArea(new LatLng(-26.1211, 28.0396), "High Crime", "Sandton Taxi Rank", 120));
        dangerAreas.add(new DangerArea(new LatLng(-26.0094, 27.9150), "Unsafe at Night", "Fourways Shopping Area", 90));
        dangerAreas.add(new DangerArea(new LatLng(-26.1714, 27.9009), "GBV Hotspot", "Soweto Community Park", 110));
    }

    private void populateMutuals() {
        mutuals.add(new Mutual(new LatLng(-26.2034, 28.0456), "Thabo Mokoena"));
        mutuals.add(new Mutual(new LatLng(-25.7557, 28.2332), "Lerato Dlamini"));
        mutuals.add(new Mutual(new LatLng(-26.1375, 27.9734), "Sipho Nkosi"));
    }

    private void setupLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // 5 seconds
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                moveToUserLocation(locationResult.getLastLocation());
            }
        };
    }

    private void moveToUserLocation(Location location) {
        if (mMap == null || location == null) return;
        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        Bitmap bmp = createInitialsMarker("ME", Color.parseColor("#4CAF50")); // green circle for me

        if (myLocationMarker == null) {
            myLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(myLatLng)
                    .title("Me")
                    .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
        } else {
            myLocationMarker.setPosition(myLatLng);
            myLocationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
        }
    }

    private void updateMapMarkersAndCircles() {
        if (mMap == null) return;

        // Save current user location
        LatLng myLatLng = myLocationMarker != null ? myLocationMarker.getPosition() : null;

        // Clear all
        mMap.clear();

        // Add danger areas
        for (DangerArea area : dangerAreas) {
            if (selectedFilterType.equals("All") || area.type.equals(selectedFilterType)) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(area.location)
                        .title(area.title)
                        .snippet(area.type);

                int strokeColor = Color.BLACK, fillColor = 0x00000000;

                switch (area.type) {
                    case "High Crime":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        strokeColor = Color.RED;
                        fillColor = 0x33FF0000;
                        break;
                    case "GBV Hotspot":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                        strokeColor = Color.MAGENTA;
                        fillColor = 0x33FF00FF;
                        break;
                    case "Unsafe at Night":
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        strokeColor = Color.parseColor("#FFA500");
                        fillColor = 0x33FFA500;
                        break;
                }
                mMap.addMarker(markerOptions);
                mMap.addCircle(new CircleOptions()
                        .center(area.location)
                        .radius(area.radius)
                        .strokeColor(strokeColor)
                        .strokeWidth(3)
                        .fillColor(fillColor));
            }
        }

        // Add mutuals
        for (Mutual friend : mutuals) {
            Bitmap bmp = createInitialsMarker(friend.getInitials(), Color.parseColor("#2196F3"));
            mMap.addMarker(new MarkerOptions()
                    .position(friend.location)
                    .title(friend.name)
                    .icon(BitmapDescriptorFactory.fromBitmap(bmp))
            );
        }

        // Restore user marker
        if (myLatLng != null) {
            Bitmap bmp = createInitialsMarker("ME", Color.parseColor("#4CAF50"));
            myLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(myLatLng)
                    .title("Me")
                    .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
        }
    }

    private Bitmap createInitialsMarker(String initials, int color) {
        int size = 120;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paintCircle = new Paint();
        paintCircle.setColor(color);
        paintCircle.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paintCircle);

        Paint paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(40f);
        paintText.setFakeBoldText(true);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        paintText.getTextBounds(initials, 0, initials.length(), bounds);
        canvas.drawText(initials, size / 2f, size / 2f - bounds.exactCenterY(), paintText);

        return bitmap;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }

        LatLng defaultLoc = new LatLng(-26.2041, 28.0473);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 10));

        updateMapMarkersAndCircles();
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (fusedLocationClient != null && locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
