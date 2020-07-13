package com.crown.onspot.page;

import android.content.Intent;
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

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ProfileFragment extends Fragment {

    private static final String TAG = ProfileFragment.class.getName();
    private ImageView mProfileImageIV;
    private TextView mDisplayNameTV;
    private TextView mEmailTV;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, root);

        initiateUi(root);
        setUpUi();
        return root;
    }

    private void initiateUi(View root) {
        mProfileImageIV = root.findViewById(R.id.iv_fp_profile_image);
        mDisplayNameTV = root.findViewById(R.id.tv_fp_display_name);
        mEmailTV = root.findViewById(R.id.tv_fp_email);
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

    @OnClick(R.id.ll_fp_logout)
    void onClickedLogout() {
        AppController.getInstance().signOut(getActivity());
    }

    @OnClick(R.id.ll_fp_contact_us)
    void onClickedContactUs() {
        startActivity(new Intent(getContext(), ContactUsActivity.class));
    }
}
