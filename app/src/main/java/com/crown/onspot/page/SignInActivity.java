package com.crown.onspot.page;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.crown.library.onspotlibrary.controller.OSGoogleSignIn;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspot.controller.AppController;
import com.crown.onspot.databinding.SignInLayoutBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class SignInActivity extends AppCompatActivity implements OSGoogleSignIn.OnGoogleSignInResponse {
    public static final int RC_SIGN_IN = 0;
    private OSGoogleSignIn mGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SignInLayoutBinding binding = SignInLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (AppController.getInstance().isAuthenticated()) {
            navHomeActivity();
            return;
        }

        binding.iasiwgSignInButton.setOnClickListener(this::onClickedSignIn);
    }

    void onClickedSignIn(View view) {
        AppController controller = AppController.getInstance();
        mGoogleSignIn = new OSGoogleSignIn(this, controller.getGoogleSignInClient(), controller.getFirebaseAuth(), RC_SIGN_IN, this);
        mGoogleSignIn.pickAccount();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) mGoogleSignIn.signIn(data);
    }

    @Override
    public void onSuccessGoogleSignIn(DocumentSnapshot doc) {
        UserOS user = doc.toObject(UserOS.class);
        if (user == null) {
            Toast.makeText(this, "Can't get user details", Toast.LENGTH_SHORT).show();
            return;
        }

        // If the user is signing in for the first time, update user doc
        if (TextUtils.isEmpty(user.getUserId()) || user.getHasOnSpotAccount() == null || !user.getHasOnSpotAccount()) {
            user.setUserId(doc.getId());
            user.setHasOnSpotAccount(true);
            FirebaseFirestore.getInstance().collection(OSString.refUser).document(doc.getId()).set(user, SetOptions.merge());
        }

        OSPreferences preferences = OSPreferences.getInstance(getApplicationContext());
        preferences.setObject(user, OSPreferenceKey.USER);
        navHomeActivity();
    }

    @Override
    public void onFailureGoogleSignIn(String response, Exception e) {
        OSMessage.showSToast(this, response);
        if (e != null) e.printStackTrace();
    }

    private void navHomeActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
