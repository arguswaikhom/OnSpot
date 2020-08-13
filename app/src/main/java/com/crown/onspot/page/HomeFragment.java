package com.crown.onspot.page;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Cache;
import com.android.volley.Cache.Entry;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.business.BusinessV4;
import com.crown.library.onspotlibrary.utils.CurrentLocation;
import com.crown.library.onspotlibrary.utils.OSCommonDialog;
import com.crown.library.onspotlibrary.utils.OSJsonParse;
import com.crown.onspot.R;
import com.crown.onspot.controller.AppController;
import com.crown.onspot.databinding.FragmentHomeBinding;
import com.crown.onspot.utils.HttpVolleyRequest;
import com.crown.onspot.utils.OnHttpResponse;
import com.crown.onspot.view.ListItemAdapter;
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

public class HomeFragment extends Fragment implements OnHttpResponse {
    private static final String TAG = HomeFragment.class.getName();
    private static final int RC_GET_BUSINESS = 1;
    private String URL_GET_BUSINESS;
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataset = new ArrayList<>();
        URL_GET_BUSINESS = getString(R.string.domain) + "/getUserAllBusiness/";
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.toolBar.setTitle("Home");
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolBar);
        setUpRecycler();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (binding.loading.getVisibility() == View.VISIBLE)
            binding.loading.setVisibility(View.INVISIBLE);
        getCurrentLocation();
    }

    private void setUpRecycler() {
        binding.listRv.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        binding.listRv.setLayoutManager(mLayoutManager);
        mAdapter = new ListItemAdapter(mDataset);
        binding.listRv.setAdapter(mAdapter);
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        Dexter.withActivity(getActivity()).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    if (getActivity() == null) return;
                    CurrentLocation.getInstance(getContext()).get(location -> getBusiness(new GeoPoint(location.getLatitude(),
                            location.getLongitude())), () -> OSCommonDialog.locationError(getActivity()), null);
                }

                if (report.isAnyPermissionPermanentlyDenied()) {
                    OSCommonDialog.appSettings(getActivity());
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

        binding.loading.setVisibility(View.VISIBLE);
        HttpVolleyRequest request = new HttpVolleyRequest(Request.Method.POST, URL_GET_BUSINESS, null, RC_GET_BUSINESS, null, map, this);
        request.execute();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onHttpResponse(String response, int request) {
        Log.v(TAG, response);
        if (binding.loading.getVisibility() == View.VISIBLE)
            binding.loading.setVisibility(View.GONE);

        if (request == RC_GET_BUSINESS) {
            JSONObject object = OSJsonParse.stringToObject(response);
            String status = OSJsonParse.stringFromObject(object, "status");
            if (status.equals("200") || status.equals("204")) {
                addNearBusinessToCache(response);
                updateBusiness(response);
            } else {
                addNearBusinessToCache(null);
                binding.infoInclude.warningTv.setText("Something went wrong");
                binding.infoInclude.warningTv.setVisibility(View.VISIBLE);
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
        JSONObject object = OSJsonParse.stringToObject(response);
        String status = OSJsonParse.stringFromObject(object, "status");

        mDataset.clear();
        if (status.equals("204")) {
            mAdapter.notifyDataSetChanged();
            binding.infoInclude.warningTv.setText("No business available in your area");
            binding.infoInclude.warningTv.setVisibility(View.VISIBLE);
            return;
        }
        binding.infoInclude.warningTv.setVisibility(View.INVISIBLE);

        JSONArray array = OSJsonParse.arrayFromObject(object, "data");
        for (int i = 0; i < array.length(); i++) {
            try {
                BusinessV4 business = new Gson().fromJson(array.get(i).toString(), BusinessV4.class);
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
        binding.loading.setVisibility(View.GONE);

        Entry entry = AppController.getInstance().getRequestQueue().getCache().get(URL_GET_BUSINESS);
        if (entry == null) {
            binding.infoInclude.warningTv.setText("Something went wrong");
            binding.infoInclude.warningTv.setVisibility(View.VISIBLE);
        } else {
            binding.infoInclude.warningTv.setVisibility(View.INVISIBLE);
        }
    }
}
