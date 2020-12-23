package com.crown.onspot.page;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
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

public class HomeFragment extends Fragment {
    private final List<ListItem> mDataset = new ArrayList<>();
    private boolean forceRefresh;
    private SwitchMaterial hodFilterSwitch;
    private ListItemAdapter mAdapter;
    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.toolBar.setTitle("Home");
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolBar);

        setUpUI();
        return binding.getRoot();
    }

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

        int INDEX_HOD_FILTER_MENU_SWITCH = 0;
        hodFilterSwitch = (SwitchMaterial) menu.getItem(INDEX_HOD_FILTER_MENU_SWITCH).getActionView();
        hodFilterSwitch.setOnCheckedChangeListener(this::onHODFilterChanged);
        refreshHodFilterSwitch();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.infoIllv.hide();
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
            getCurrentLocation();
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
            error.printStackTrace();
            binding.loadingBounce.setVisibility(View.INVISIBLE);
            binding.infoIllv.show(R.drawable.ill_undraw_empty_xct9, "Failed to get data!!");
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

            if (getActivity() != null && data.length() == 0) {
                binding.infoIllv.show(R.drawable.ill_undraw_city_girl_ccpd, "No active seller in your area");
            } else binding.infoIllv.hide();
        } catch (Exception ignore) {
        }
    }
}
