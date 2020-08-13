package com.crown.onspot.page;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspot.R;
import com.crown.onspot.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity implements EventListener<DocumentSnapshot> {
    private final String TAG = MainActivity.class.getName();
    private UserOS user;
    private ListenerRegistration mUserChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.v("debug", "\n\nDevice info:\n" + new Gson().toJson(new android.os.Build()) + "\n\n");

        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        mUserChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId()).addSnapshotListener(this);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(binding.navView, navController);

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(result -> {
            if (user.getDeviceToken() == null || user.getDeviceToken().isEmpty() || !user.getDeviceToken().contains(result.getToken())) {
                FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId())
                        .update(getString(R.string.field_device_token), FieldValue.arrayUnion(result.getToken()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUserChangeListener.remove();
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
        if (snapshot != null && snapshot.exists()) {
            UserOS user = snapshot.toObject(UserOS.class);
            if (user != null) {
                this.user = user;
                OSPreferences.getInstance(getApplicationContext()).setObject(user, OSPreferenceKey.USER);
                Intent intent = new Intent(getString(R.string.action_os_changes));
                sendBroadcast(intent);
            }
        }
    }
}
