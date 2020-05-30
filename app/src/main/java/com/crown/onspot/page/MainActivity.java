package com.crown.onspot.page;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.crown.onspot.R;
import com.crown.onspot.model.User;
import com.crown.onspot.utils.preference.PreferenceKey;
import com.crown.onspot.utils.preference.Preferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity implements EventListener<DocumentSnapshot> {
    private final String TAG = MainActivity.class.getName();
    private ListenerRegistration mUserChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        mUserChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId()).addSnapshotListener(this);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_profile).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        uploadDeviceToken();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUserChangeListener.remove();
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
        if (snapshot != null && snapshot.exists()) {
            User user = snapshot.toObject(User.class);
            if (user != null) {
                Preferences preferences = Preferences.getInstance(getApplicationContext());
                preferences.setObject(user, PreferenceKey.USER);
            }
        }
    }

    private void uploadDeviceToken() {
        String token = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.DEVICE_TOKEN, String.class);
        Log.v(TAG, "Token state: " + token);
        if (token == null) {
            Log.v(TAG, "Getting token");
            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
                Log.v(TAG, "" + task.isSuccessful());

                if (task.isSuccessful()) {
                    Log.v(TAG, "Token: " + task.getResult().getToken());
                    sendDeviceToken(task.getResult().getToken());
                }
            }).addOnFailureListener(error -> Log.v(TAG, "#####\n" + error + "\n#####"));
        }
    }

    private void sendDeviceToken(String token) {
        Preferences preferences = Preferences.getInstance(getApplicationContext());
        User user = preferences.getObject(PreferenceKey.USER, User.class);

        String field = getString(R.string.field_device_token);
        FirebaseFirestore.getInstance().collection(getString(R.string.ref_user))
                .document(user.getUserId())
                .update(field, FieldValue.arrayUnion(token))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        preferences.setObject(token, PreferenceKey.DEVICE_TOKEN);
                    }
                });
    }
}
