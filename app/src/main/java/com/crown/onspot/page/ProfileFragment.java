package com.crown.onspot.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.crown.onspot.R;
import com.crown.onspot.controller.AppController;
import com.crown.onspot.model.User;
import com.crown.onspot.utils.preference.PreferenceKey;
import com.crown.onspot.utils.preference.Preferences;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = ProfileFragment.class.getName();
    private ImageView mProfileImageIV;
    private TextView mDisplayNameTV;
    private TextView mEmailTV;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        initiateUi(root);
        setUpUi();
        return root;
    }

    private void initiateUi(View root) {
        mProfileImageIV = root.findViewById(R.id.iv_fp_profile_image);
        mDisplayNameTV = root.findViewById(R.id.tv_fp_display_name);
        mEmailTV = root.findViewById(R.id.tv_fp_email);
        root.findViewById(R.id.ll_fp_logout).setOnClickListener(this);
    }

    private void setUpUi() {
        User user = Preferences.getInstance(getContext().getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        Glide.with(this)
                .load(user.getProfileImageUrl())
                .apply(new RequestOptions().centerCrop().circleCrop())
                .into(mProfileImageIV);
        mDisplayNameTV.setText(user.getDisplayName());
        mEmailTV.setText(user.getEmail());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_fp_logout: {
                AppController.getInstance().signOut(getActivity());
            }
        }

    }
}
