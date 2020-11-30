package com.crown.onspot.controller;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class Validate {

    public static boolean isEmail(String email) {
        if (email == null || TextUtils.isEmpty(email)) {
            return false;
        }

        String pattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        return email.trim().matches(pattern);
    }


    public static boolean isPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || TextUtils.isEmpty(phoneNumber)) {
            return false;
        }

        String pattern = "^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$";
        Matcher matcher = Pattern.compile(pattern).matcher(phoneNumber.trim());

        return matcher.find();
    }
}
