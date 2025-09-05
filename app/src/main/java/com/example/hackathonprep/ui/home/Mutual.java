package com.example.hackathonprep.ui.home;

import com.google.android.gms.maps.model.LatLng;

public class Mutual {
    LatLng location;
    String name;
    String initials;

    Mutual(LatLng location, String name) {
        this.location = location;
        this.name = name;

        // Get initials automatically from name
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            this.initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        } else {
            this.initials = ("" + parts[0].charAt(0)).toUpperCase();
        }
    }

    public String getInitials() {
        return initials;
    }
}

