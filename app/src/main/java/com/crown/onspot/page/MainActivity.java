package com.crown.onspot.page;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.page.PhoneVerificationActivity;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspot.R;
import com.crown.onspot.controller.AppController;
import com.crown.onspot.databinding.ActivityMainBinding;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private UserOS user;
    private ListenerRegistration mUserChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AppController.getInstance().isAuthenticated()) {
            AppController.getInstance().signOut(this);
            return;
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(binding.navView, navController);

        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        syncAccountInfo();
        verifyDeviceToken();
        verifyUserContact();
    }

    private void verifyUserContact() {
        if (TextUtils.isEmpty(user.getPhoneNumber())) {
            startActivity(new Intent(this, PhoneVerificationActivity.class));
        }
    }

    private void syncAccountInfo() {
        mUserChangeListener = FirebaseFirestore.getInstance().collection(OSString.refUser).document(user.getUserId()).addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                UserOS updatedUser = doc.toObject(UserOS.class);
                if (updatedUser != null) {
                    this.user = updatedUser;
                    OSPreferences.getInstance(getApplicationContext()).setObject(updatedUser, OSPreferenceKey.USER);
                    sendBroadcast(new Intent(getString(R.string.action_os_changes)));
                }
            }
        });
    }

    private void verifyDeviceToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (OSListUtils.isEmpty(user.getDeviceToken()) || !user.getDeviceToken().contains(token)) {
                FirebaseFirestore.getInstance().collection(OSString.refUser).document(user.getUserId())
                        .update(OSString.fieldDeviceTokenOSD, FieldValue.arrayUnion(token));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUserChangeListener != null) mUserChangeListener.remove();
    }
}
