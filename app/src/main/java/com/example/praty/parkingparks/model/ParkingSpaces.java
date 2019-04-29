package com.example.praty.parkingparks.model;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/*
model used in the project
 */
public class ParkingSpaces {

    private String slots;
    private String imageUri;
    private String address;
    private Double latitude;
    private Double longitude;
    private String description;

    public ParkingSpaces(String slots, String imageUri, String address, Double latitude,Double longitude, String description) {
        this.slots = slots;
        this.imageUri = imageUri;
        this.address = address;
        this.latitude = latitude;
        this.longitude=longitude;
        this.description = description;
    }

    public ParkingSpaces() {

    }

    public String getSlots() {
        return slots;
    }

    public void setSlots(String slots) {
        this.slots = slots;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
