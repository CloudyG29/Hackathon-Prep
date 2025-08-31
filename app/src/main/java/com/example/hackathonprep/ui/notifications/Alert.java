package com.example.hackathonprep.ui.notifications;

public class Alert {
    String type, location, time;

    public Alert(String stype, String slocation, String stime) {
        type=stype;
        location=slocation;
        time=stime;
    }

    public String getTitle() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public String getTime() {
        return time;
    }


    public Object getType() {
        return type;
    }
}
