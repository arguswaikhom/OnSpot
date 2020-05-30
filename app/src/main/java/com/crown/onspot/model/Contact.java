package com.crown.onspot.model;

public class Contact {
    private String name;
    private String phoneNo;

    public Contact() {
    }

    public Contact(String name, String phoneNo) {
        this.name = name;
        this.phoneNo = phoneNo;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }
}
