package com.crown.onspot.page;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.crown.library.onspotlibrary.controller.OSVolley;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.explore.OSExplore;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.onspot.R;
import com.crown.onspot.databinding.FragmentExploreBinding;
import com.crown.onspot.view.ListItemAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExploreFragment extends Fragment {

    private final List<ListItem> dataset = new ArrayList<>();
    private ListItemAdapter adapter;
    private FragmentExploreBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        init();
        getDataset();
        return binding.getRoot();
    }

    private void init() {
        adapter = new ListItemAdapter(dataset);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        binding.exploreListRv.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.HORIZONTAL));
        binding.exploreListRv.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        binding.exploreListRv.setLayoutManager(layoutManager);
        binding.exploreListRv.setAdapter(adapter);
        binding.searchSv.setOnSearchClickedListener(this::onExploreSearch);
    }

    private void getDataset() {
        OSVolley volleyInstance = OSVolley.getInstance(getContext());

        String exploreCache = volleyInstance.getFreshCache(OSString.apiExplore);
        if (exploreCache != null) {
            displayDataset(exploreCache);
        } else {
            String oldExploreCache = volleyInstance.getCache(OSString.apiExplore);
            if (oldExploreCache != null)
                displayDataset(oldExploreCache);
            binding.searchSv.showProcessing();
            OSVolley.getInstance(getContext()).addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiExplore, response -> {
                volleyInstance.setCache(OSString.apiExplore, response, 300000L);
                displayDataset(response);
            }, error -> {
                binding.searchSv.dismissProcessing();
                if (getActivity() != null)
                    OSMessage.showSBar(getActivity(), getString(R.string.msg_something_went_wrong));
            }));
        }
    }

    private void displayDataset(String json) {
        dataset.clear();
        try {
            JSONArray array = new JSONArray(json);
            // todo: handle empty response
            try {
                for (int i = 0; i < array.length(); i++) {
                    dataset.add(OSExplore.fromJson(array.get(i).toString()));
                }
            } catch (JSONException ignore) {
            }
        } catch (JSONException ignore) {
            if (getActivity() != null)
                OSMessage.showSBar(getActivity(), getString(R.string.msg_something_went_wrong));
        }
        Collections.shuffle(dataset);
        adapter.notifyDataSetChanged();
        binding.searchSv.dismissProcessing();
    }

    private void onExploreSearch(String keywords) {
        if (TextUtils.isEmpty(keywords)) return;

        // todo: control double submit clicks without keyword changes

        binding.searchSv.showProcessing();
        OSVolley.getInstance(getContext()).addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiExplore, response -> {
            binding.searchSv.dismissProcessing();
            Log.d("debug", response);
            displayDataset(response);
        }, error -> {
            binding.searchSv.dismissProcessing();
            if (getActivity() != null)
                OSMessage.showSBar(getActivity(), getString(R.string.msg_something_went_wrong));
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                param.put(OSString.keyKeywords, keywords);
                return param;
            }
        });
        OSMessage.showSToast(getContext(), keywords);
    }
}