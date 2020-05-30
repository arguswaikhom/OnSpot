package com.crown.onspot.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

public class OSLocation {
    private String addressLine;
    private GeoPoint geoPoint;
    private String postalCode;
    private String howToReach;

    public OSLocation() {
    }

    public OSLocation(String addressLine, GeoPoint geoPoint, String postalCode, String howToReach) {
        this.addressLine = addressLine;
        this.geoPoint = geoPoint;
        this.postalCode = postalCode;
        this.howToReach = howToReach;
    }

    public String getHowToReach() {
        return howToReach;
    }

    public void setHowToReach(String howToReach) {
        this.howToReach = howToReach;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}