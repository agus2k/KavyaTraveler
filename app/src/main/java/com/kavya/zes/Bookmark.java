package com.kavya.zes;

public class Bookmark {
    public String name;
    public double lat;
    public double lng;

    public Bookmark(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public String toString() {
        return name;
    }
}
