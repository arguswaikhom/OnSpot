package com.crown.onspot.page;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Cache;
import com.android.volley.Cache.Entry;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.crown.onspot.R;
import com.crown.onspot.controller.AppController;
import com.crown.onspot.model.Shop;
import com.crown.onspot.utils.HttpVolleyRequest;
import com.crown.onspot.utils.JSONParsing;
import com.crown.onspot.utils.abstracts.ListItem;
import com.crown.onspot.utils.abstracts.OnHttpResponse;
import com.crown.onspot.view.ListItemAdapter;
import com.crown.onspot.view.ViewLoadingDotsBounce;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NearBusinessFragment extends Fragment implements OnHttpResponse {
    private static final String TAG = NearBusinessFragment.class.getName();
    private static final int RC_GET_BUSINESS = 1;
    @BindView(R.id.view_fp_loading)
    ViewLoadingDotsBounce mLoadingView;
    @BindView(R.id.tv_wtl_warning)
    TextView mNoCurrentOrderMessage;
    private String URL_GET_BUSINESS;
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataset = new ArrayList<>();
        URL_GET_BUSINESS = getString(R.string.domain) + "/getUserAllBusiness/";
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, root);

        Toolbar toolbar = root.findViewById(R.id.tbar_fmo_tool_bar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("OnSpot");

        setUpRecycler(root);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLoadingView.getVisibility() == View.VISIBLE)
            mLoadingView.setVisibility(View.INVISIBLE);
        getCurrentLocation();
    }

    private void setUpRecycler(View root) {
        RecyclerView mRecyclerView = root.findViewById(R.id.rv_bnrvl_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ListItemAdapter(getActivity(), mDataset);
        mRecyclerView.setAdapter(mAdapter);
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        Dexter.withActivity(getActivity())
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    if (getActivity() == null) return;
                    LocationServices.getFusedLocationProviderClient(getActivity()).getLastLocation().addOnSuccessListener(getActivity(), location -> {
                        if (location != null) {
                            Log.v(TAG, location.getLatitude() + "");
                            Log.v(TAG, location.getLongitude() + "");

                            getBusiness(new GeoPoint(location.getLatitude(), location.getLongitude()));
                        }
                    });
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();
    }

    private void getBusiness(GeoPoint geoPoint) {
        Map<String, String> map = new HashMap<>();
        map.put("data", new Gson().toJson(geoPoint));

        Entry entry = AppController.getInstance().getRequestQueue().getCache().get(URL_GET_BUSINESS);
        if (entry != null) {
            if (entry.isExpired()) {
                HttpVolleyRequest request = new HttpVolleyRequest(Request.Method.POST, URL_GET_BUSINESS, null, RC_GET_BUSINESS, null, map, this);
                request.execute();
            }

            String data = new String(entry.data, StandardCharsets.UTF_8);
            updateBusiness(data);
            return;
        }

        mLoadingView.setVisibility(View.VISIBLE);
        HttpVolleyRequest request = new HttpVolleyRequest(Request.Method.POST, URL_GET_BUSINESS, null, RC_GET_BUSINESS, null, map, this);
        request.execute();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onHttpResponse(String response, int request) {
        Log.v(TAG, response);
        if (mLoadingView.getVisibility() == View.VISIBLE) mLoadingView.setVisibility(View.GONE);

        if (request == RC_GET_BUSINESS) {
            JSONObject object = JSONParsing.ObjectFromString(response);
            String status = JSONParsing.StringFromObject(object, "status");
            if (status.equals("200") || status.equals("204")) {
                addNearBusinessToCache(response);
                updateBusiness(response);
            } else {
                addNearBusinessToCache(null);
                mNoCurrentOrderMessage.setText("Something went wrong");
                mNoCurrentOrderMessage.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addNearBusinessToCache(String response) {
        Cache cache = AppController.getInstance().getRequestQueue().getCache();
        Entry cacheEntry = new Entry();

        if (response == null) {
            cache.put(URL_GET_BUSINESS, null);
            return;
        }

        cacheEntry.ttl = System.currentTimeMillis() + 120000;
        cacheEntry.data = response.getBytes();
        cache.put(URL_GET_BUSINESS, cacheEntry);
    }

    private void updateBusiness(String response) {
        JSONObject object = JSONParsing.ObjectFromString(response);
        String status = JSONParsing.StringFromObject(object, "status");

        mDataset.clear();
        if (status.equals("204")) {
            mAdapter.notifyDataSetChanged();
            mNoCurrentOrderMessage.setText("No business available in your area");
            mNoCurrentOrderMessage.setVisibility(View.VISIBLE);
            return;
        }
        mNoCurrentOrderMessage.setVisibility(View.INVISIBLE);

        JSONArray array = JSONParsing.ArrayFromObject(object, "data");
        for (int i = 0; i < array.length(); i++) {
            try {
                Shop business = new Gson().fromJson(array.get(i).toString(), Shop.class);
                Log.v(TAG, "Business: " + business);
                mDataset.add(business);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onHttpErrorResponse(VolleyError error, int request) {
        Log.v(TAG, error.toString());
        mLoadingView.setVisibility(View.GONE);

        Entry entry = AppController.getInstance().getRequestQueue().getCache().get(URL_GET_BUSINESS);
        if (entry == null) {
            mNoCurrentOrderMessage.setText("Something went wrong");
            mNoCurrentOrderMessage.setVisibility(View.VISIBLE);
        } else {
            mNoCurrentOrderMessage.setVisibility(View.INVISIBLE);
        }
    }
}
