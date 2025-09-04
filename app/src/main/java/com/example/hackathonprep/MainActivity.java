package com.example.hackathonprep;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import androidx.activity.EdgeToEdge;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;



    private TextView ans;
    public static final int REQUEST_RECORD_AUDIO = 1;

    public static final String ACCESS_KEY = "oUD1Hs9X9838Yhni2sjw+n8aaWXSZBzB11dC/xXLZkXB7VK+GaA0LQ==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        LatLng campus = new LatLng(-25.7545, 28.2314); // Example: Pretoria campus
        mMap.addMarker(new MarkerOptions()
                .position(campus)
                .title("My Campus")
                .snippet("Tap me for more info"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus, 15));


        // Options: NORMAL, SATELLITE, TERRAIN, HYBRID
        mMap.addCircle(new CircleOptions()
                .center(campus)
                .radius(200) // in meters
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF));


        // Example: put a marker on Johannesburg
        LatLng joburg = new LatLng(-26.2041, 28.0473);
        mMap.addMarker(new MarkerOptions().position(joburg).title("Marker in Joburg"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(joburg, 12));
    }



}



