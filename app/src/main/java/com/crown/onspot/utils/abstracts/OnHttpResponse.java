package com.crown.onspot.utils.abstracts;

import com.android.volley.VolleyError;

public interface OnHttpResponse {
    void onHttpResponse(String response, int request);

    void onHttpErrorResponse(VolleyError error, int request);
}
