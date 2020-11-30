package com.crown.onspot.page;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.controller.OSVolley;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.business.BusinessV4;
import com.crown.library.onspotlibrary.model.preference.OSOnSpotAppPreferences;
import com.crown.library.onspotlibrary.utils.CurrentLocation;
import com.crown.library.onspotlibrary.utils.OSAppPreferenceUtils;
import com.crown.library.onspotlibrary.utils.OSCommonDialog;
import com.crown.library.onspotlibrary.utils.OSJsonParse;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSAPOnSpotHomeHODFilter;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspot.R;
import com.crown.onspot.databinding.FragmentHomeBinding;
import com.crown.onspot.view.ListItemAdapter;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// todo: use OSVolley to maintain cache
public class HomeFragment extends Fragment /*implements OnHttpResponse*/ {
    private static final String TAG = HomeFragment.class.getName();
    private static final int RC_GET_BUSINESS = 1;
    private final int INDEX_HOD_FILTER_MENU_SWITCH = 0;
    private boolean forceRefresh;
    private SwitchMaterial hodFilterSwitch;
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataset = new ArrayList<>();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.toolBar.setTitle("Home");
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolBar);

        setUpUI();
        return binding.getRoot();
    }

    /*@Override
    public void onStart() {
        super.onStart();
        if (binding.loading.getVisibility() == View.VISIBLE)
            binding.loading.setVisibility(View.INVISIBLE);
    }*/

    private void setUpUI() {
        binding.listRv.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        binding.listRv.setLayoutManager(mLayoutManager);
        mAdapter = new ListItemAdapter(mDataset);
        binding.listRv.setAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

        hodFilterSwitch = (SwitchMaterial) menu.getItem(INDEX_HOD_FILTER_MENU_SWITCH).getActionView();
        hodFilterSwitch.setOnCheckedChangeListener(this::onHODFilterChanged);
        refreshHodFilterSwitch();
    }

    @Override
    public void onResume() {
        super.onResume();
        getContent();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            forceRefresh = true;
            getContent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshHodFilterSwitch() {
        OSOnSpotAppPreferences preferences = OSAppPreferenceUtils.getOSPreferences(getContext());
        hodFilterSwitch.setChecked(preferences.getHomeHODFilter() == OSAPOnSpotHomeHODFilter.ONLY_HOD_BUSINESS);
    }

    private void onHODFilterChanged(CompoundButton compoundButton, boolean b) {
        OSOnSpotAppPreferences preferences = OSAppPreferenceUtils.getOSPreferences(getContext());
        preferences.setHomeHODFilter(b ? OSAPOnSpotHomeHODFilter.ONLY_HOD_BUSINESS : OSAPOnSpotHomeHODFilter.OPEN_ALL_BUSINESS);
        OSPreferences.getInstance(getContext()).setObject(preferences, OSPreferenceKey.APP_PREFERENCES);
        if (b) OSMessage.showSToast(getContext(), "Home delivery available only");
        getContent();
    }

    /*private void getBusiness(GeoPoint geoPoint) {
        Map<String, String> map = new HashMap<>();
        map.put("data", new Gson().toJson(geoPoint));

        Entry entry = AppController.getInstance().getRequestQueue().getCache().get(OSString.apiGetUserAllBusiness);
        if (entry != null) {
            if (entry.isExpired()) {
                HttpVolleyRequest request = new HttpVolleyRequest(Request.Method.POST, OSString.apiGetUserAllBusiness, null, RC_GET_BUSINESS, null, map, this);
                request.execute();
            }

            String data = new String(entry.data, StandardCharsets.UTF_8);
            updateBusiness(data);
            return;
        }

        binding.loading.setVisibility(View.VISIBLE);
        HttpVolleyRequest request = new HttpVolleyRequest(Request.Method.POST, OSString.apiGetUserAllBusiness, null, RC_GET_BUSINESS, null, map, this);
        request.execute();
    }*/

    /*@SuppressLint("SetTextI18n")
    @Override
    public void onHttpResponse(String response, int request) {
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
    }*/

    /*private void addNearBusinessToCache(String response) {
        Cache cache = AppController.getInstance().getRequestQueue().getCache();
        Entry cacheEntry = new Entry();

        if (response == null) {
            cache.put(OSString.apiGetUserAllBusiness, null);
            return;
        }

        cacheEntry.ttl = System.currentTimeMillis() + 120000;
        cacheEntry.data = response.getBytes();
        cache.put(OSString.apiGetUserAllBusiness, cacheEntry);
    }*/

    /*private void updateBusiness(String response) {
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
                mDataset.add(business);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        mAdapter.notifyDataSetChanged();
    }*/

    /*@Override
    public void onHttpErrorResponse(VolleyError error, int request) {
        Log.v(TAG, error.toString());
        binding.loading.setVisibility(View.GONE);

        Entry entry = AppController.getInstance().getRequestQueue().getCache().get(OSString.apiGetUserAllBusiness);
        if (entry == null) {
            binding.infoInclude.warningTv.setText("Something went wrong");
            binding.infoInclude.warningTv.setVisibility(View.VISIBLE);
        } else {
            binding.infoInclude.warningTv.setVisibility(View.INVISIBLE);
        }
    }*/


    ////////////////////////////////////////////////
    private void getContent() {
        OSOnSpotAppPreferences preferences = OSAppPreferenceUtils.getOSPreferences(getContext());
        OSAPOnSpotHomeHODFilter filter = preferences.getHomeHODFilter();
        String cacheKey = filter.name() + OSString.apiGetUserBusiness;
        OSVolley volley = OSVolley.getInstance(getContext());

        // Get content from cache and display
        String cache = volley.getCache(cacheKey);
        if (!TextUtils.isEmpty(cache)) displayContent(cache);

        // If the cache has fresh content, no need to refresh
        // If refresh == true || previous cache has 0 content, send force request
        int contentLength = 0;
        try {
            contentLength = OSJsonParse.arrayFromObject(OSJsonParse.stringToObject(cache), OSString.data).length();
        } catch (Exception ignore) {
        }
        if (volley.isCacheExpired(cacheKey) || forceRefresh || contentLength == 0) {
            if (forceRefresh) forceRefresh = false;
            Log.d("debug", "Http called");
            getCurrentLocation();
        } else {
            Log.d("debug", "Cache");
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        Dexter.withActivity(getActivity()).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    if (getActivity() == null) return;
                    CurrentLocation.getInstance(getContext()).get(location -> requestContent(new GeoPoint(location.getLatitude(),
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

    private void requestContent(GeoPoint currentLocation) {
        OSVolley volley = OSVolley.getInstance(getContext());

        // The cache doesn't have the content or has outdated content
        // Send a new request and update the content
        binding.loadingBounce.setVisibility(View.VISIBLE);
        volley.addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiGetUserBusiness, response -> {
            binding.loadingBounce.setVisibility(View.INVISIBLE);
            String filter = OSJsonParse.stringFromObject(OSJsonParse.stringToObject(response), OSString.filter);
            volley.setCache(filter + OSString.apiGetUserBusiness, response, 300000L);
            displayContent(response);
        }, error -> {
            binding.loadingBounce.setVisibility(View.INVISIBLE);
            OSMessage.showSToast(getActivity(), "Failed to get data!!");
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                OSOnSpotAppPreferences preferences = OSAppPreferenceUtils.getOSPreferences(getContext());
                param.put(OSString.filter, preferences.getHomeHODFilter().name());
                param.put(OSString.userLocation, new Gson().toJson(currentLocation));
                return param;
            }
        });
    }

    private void displayContent(String content) {
        Log.d("debug", content);

        try {
            JSONObject object = new JSONObject(content);
            JSONArray data = object.getJSONArray(OSString.data);
            OSAPOnSpotHomeHODFilter filter = OSAPOnSpotHomeHODFilter.valueOf(object.getString(OSString.filter));

            // If this request filter and preference filter aren't same, ignore
            OSOnSpotAppPreferences preferences = OSAppPreferenceUtils.getOSPreferences(getContext());
            if (!preferences.getHomeHODFilter().equals(filter)) return;

            mDataset.clear();
            for (int i = 0; i < data.length(); i++) {
                BusinessV4 business = BusinessV4.fromJson(data.get(i).toString(), BusinessV4.class);
                mDataset.add(business);
            }
            mAdapter.notifyDataSetChanged();

            if (getActivity() != null && data.length() == 0)
                OSMessage.showSBar(getActivity(), "No business found in your area.");
        } catch (Exception ignore) {
        }
    }
}
