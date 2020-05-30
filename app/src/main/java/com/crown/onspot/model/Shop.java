package com.crown.onspot.model;

import androidx.annotation.NonNull;

import com.crown.onspot.utils.ListItemKey;
import com.crown.onspot.utils.abstracts.ListItem;
import com.google.gson.Gson;

import java.util.List;

public class Shop extends ListItem {
    public static final int TYPE = ListItemKey.BUSINESS;

    private String businessId;
    private String businessRefId;
    private String businessType;
    private String displayName;
    private String mobileNumber;
    private OSLocation location;
    private Double deliveryRange;
    private List<String> imageUrls;

    public Shop() {
    }

    public String getImageUrl() {
        return imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public OSLocation getLocation() {
        return location;
    }

    public void setLocation(OSLocation location) {
        this.location = location;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getBusinessRefId() {
        return businessRefId.trim();
    }

    public void setBusinessRefId(String businessRefId) {
        this.businessRefId = businessRefId;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public Double getDeliveryRange() {
        return deliveryRange;
    }

    @Override
    public int getItemType() {
        return TYPE;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
