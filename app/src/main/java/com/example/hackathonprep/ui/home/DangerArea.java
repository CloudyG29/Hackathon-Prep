package com.example.hackathonprep.ui.home;

import com.google.android.gms.maps.model.LatLng;

public class DangerArea {
    LatLng location;
    String type;
    String title;
    double radius; // Added radius property

    public DangerArea(LatLng location, String type, String title, double radius) {
        this.location = location;
        this.type = type;
        this.title = title;
        this.radius = radius;
    }
}