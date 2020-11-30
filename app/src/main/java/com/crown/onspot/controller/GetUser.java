package com.crown.onspot.controller;

import android.content.Context;

import com.android.volley.Request;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.onspot.utils.HttpVolleyRequest;
import com.crown.onspot.utils.OnHttpResponse;

import java.util.HashMap;
import java.util.Map;

public class GetUser {
    public static void get(Context context) {
        getUserDetails(context, 0);
    }

    public static void get(Context context, int request) {
        getUserDetails(context, request);
    }

    private static void getUserDetails(Context context, int request) {
        String userId = AppController.getInstance().getFirebaseAuth().getUid();

        Map<String, String> param = new HashMap<>();
        param.put("userId", userId);

        HttpVolleyRequest httpVolleyRequest = new HttpVolleyRequest(Request.Method.POST, OSString.apiGetUser, null, request, null, param, (OnHttpResponse) context);
        httpVolleyRequest.execute();
    }
}
