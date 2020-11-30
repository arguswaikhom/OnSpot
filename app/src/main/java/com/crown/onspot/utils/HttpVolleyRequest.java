package com.crown.onspot.utils;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.crown.onspot.controller.AppController;

import java.util.Map;

@Deprecated
public class HttpVolleyRequest {
    private final String TAG = HttpVolleyRequest.class.getSimpleName();
    private final int mMethod;
    private final int mRequest;
    private final String mUrl;
    private final String mTag;
    private final OnHttpResponse mOnHttpResponse;
    private Map<String, String> mHttpHeader = null;
    private Map<String, String> mHttpParam = null;

    public HttpVolleyRequest(int method, String url, String tag, int request, OnHttpResponse onHttpResponse) {
        this.mMethod = method;
        this.mUrl = url;
        this.mOnHttpResponse = onHttpResponse;
        this.mTag = tag;
        this.mRequest = request;
    }

    public HttpVolleyRequest(int method, String url, String tag, int request, Map<String, String> header, Map<String, String> param, OnHttpResponse onHttpResponse) {
        this(method, url, tag, request, onHttpResponse);
        this.mHttpParam = param;
        this.mHttpHeader = header;
    }

    public void execute() {
        StringRequest stringRequest = new StringRequest(this.mMethod, this.mUrl,
                response -> mOnHttpResponse.onHttpResponse(response, mRequest),
                error -> mOnHttpResponse.onHttpErrorResponse(error, mRequest)) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Log.v(TAG, "Header: " + mHttpHeader);
                if (mHttpHeader != null && !mHttpHeader.isEmpty()) {
                    return mHttpHeader;
                }
                return super.getHeaders();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Log.v(TAG, "Param: " + mHttpParam);
                if (mHttpParam != null && !mHttpParam.isEmpty()) {
                    return mHttpParam;
                }
                return super.getParams();
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        if (mTag != null && !mTag.isEmpty()) {
            AppController.getInstance().addToRequestQueue(stringRequest, mTag);
        } else {
            AppController.getInstance().addToRequestQueue(stringRequest);
        }
    }
}
