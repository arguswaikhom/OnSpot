package com.crown.onspot.page;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.page.EditProfileActivity;
import com.crown.library.onspotlibrary.utils.OSCommonIntents;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspot.controller.AppController;
import com.crown.onspot.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private static final String TAG = ProfileFragment.class.getName();
    private UserOS user;
    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        user = OSPreferences.getInstance(getContext().getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);

        setUpMenuClickListener();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpUi();
    }

    private void setUpMenuClickListener() {
        binding.editProfileOpi.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            intent.putExtra(EditProfileActivity.USER_ID, user.getUserId());
            startActivity(intent);
        });
        binding.contactUsOpi.setOnClickListener(v -> startActivity(new Intent(getContext(), ContactUsActivity.class)));
        binding.shareOpi.setOnClickListener(v -> OSCommonIntents.onIntentShareAppLink(getContext()));
        binding.rateThisAppOpi.setOnClickListener(v -> OSCommonIntents.onIntentAppOnPlayStore(getContext()));
        binding.logoutOpi.setOnClickListener(v -> AppController.getInstance().signOut(getActivity()));
    }

    private void setUpUi() {
        Glide.with(this)
                .load(user.getProfileImageUrl())
                .apply(new RequestOptions().centerCrop().circleCrop())
                .into(binding.ivFpProfileImage);
        binding.tvFpDisplayName.setText(user.getDisplayName());
        binding.tvFpEmail.setText(user.getEmail());
    }
}
