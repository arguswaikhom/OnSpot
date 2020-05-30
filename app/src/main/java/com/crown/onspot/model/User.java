package com.crown.onspot.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class User {
    private String displayName;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private String userId;
    private String businessId;
    private String businessRefId;
    private OSLocation location;
    private boolean hasOnSpotAccount;
    private boolean hasOnSpotBusinessAccount;
    private boolean hasOnSpotDeliveryAccount;
    private boolean hasPhoneNumberVerified;

    public User() {
    }

    public static User fromJson(String json) {
        return new Gson().fromJson(json, User.class);
    }

    public OSLocation getLocation() {
        return location;
    }

    public void setLocation(OSLocation location) {
        this.location = location;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isHasPhoneNumberVerified() {
        return hasPhoneNumberVerified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getUserId() {
        return userId.trim();
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getBusinessRefId() {
        return businessRefId;
    }

    public boolean isHasOnSpotAccount() {
        return hasOnSpotAccount;
    }

    public boolean isHasOnSpotBusinessAccount() {
        return hasOnSpotBusinessAccount;
    }

    public boolean isHasOnSpotDeliveryAccount() {
        return hasOnSpotDeliveryAccount;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
