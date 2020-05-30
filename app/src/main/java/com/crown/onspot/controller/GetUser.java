package com.crown.onspot.controller;

import android.content.Context;

import com.android.volley.Request;
import com.crown.onspot.R;
import com.crown.onspot.utils.HttpVolleyRequest;
import com.crown.onspot.utils.abstracts.OnHttpResponse;

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
        String url = context.getResources().getString(R.string.domain) + "/getUser/";
        String userId = AppController.getInstance().getFirebaseAuth().getUid();

        Map<String, String> param = new HashMap<>();
        param.put("userId", userId);

        HttpVolleyRequest httpVolleyRequest = new HttpVolleyRequest(Request.Method.POST, url, null, request, null, param, (OnHttpResponse) context);
        httpVolleyRequest.execute();
    }
}
