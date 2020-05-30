package com.crown.onspot.page;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;
import com.crown.onspot.R;
import com.crown.onspot.controller.AppController;
import com.crown.onspot.controller.GetUser;
import com.crown.onspot.model.User;
import com.crown.onspot.utils.MessageUtils;
import com.crown.onspot.utils.abstracts.OnHttpResponse;
import com.crown.onspot.utils.preference.PreferenceKey;
import com.crown.onspot.utils.preference.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shobhitpuri.custombuttons.GoogleSignInButton;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener, MessageUtils.OnSnackBarActionListener, OnHttpResponse {
    private final int RC_SIGN_IN = 100;
    private final int RC_CREATE_BUSINESS = 101;
    private final int RC_HTTP_BUSINESS_AVAILABILITY = 102;
    private final int RC_HTTP_HAS_USER_ACCOUNT = 103;
    private final int RC_HTTP_GET_USER = 104;
    private final int RC_INTENT_VERIFY_MOBILE_NUMBER = 2;
    private final String TAG = SignInActivity.class.getSimpleName();

    private View mParentView;
    private ProgressBar mLoadingPb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_with_google);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        mParentView = findViewById(android.R.id.content);
        RelativeLayout mRootLayout = findViewById(R.id.iasiwg_root_layout);
        GoogleSignInButton mSignInButton = findViewById(R.id.iasiwg_sign_in_button);
        mLoadingPb = findViewById(R.id.pb_iasiwg_loading);

        mSignInButton.setOnClickListener(this);

        AnimationDrawable animationDrawable = (AnimationDrawable) mRootLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(2000);

        animationDrawable.start();

        if (AppController.getInstance().isAuthenticated()) {
            User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
            verifyUser(user);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iasiwg_sign_in_button) {
            signInWithGoogle();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_SIGN_IN: {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    MessageUtils.showActionIndefiniteSnackBar(mParentView, "Sign in failed", "RETRY", RC_SIGN_IN, SignInActivity.this);
                    Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
                } catch (Exception e) {
                    MessageUtils.showActionIndefiniteSnackBar(mParentView, "Sign in failed", "RETRY", RC_SIGN_IN, SignInActivity.this);
                    Log.w(TAG, "signInResult:failed=" + e.getStackTrace());
                }
                break;
            }
            case RC_INTENT_VERIFY_MOBILE_NUMBER: {
                if (resultCode == RESULT_OK && data != null) {
                    String verifiedMobileNumber = data.getStringExtra(PhoneVerificationActivity.KEY_PHONE_NO);
                    updateUserPhoneNumber(verifiedMobileNumber);
                }
            }
            break;
        }
    }

    private void updateUserPhoneNumber(String verifiedMobileNumber) {
        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        String userId = user.getUserId();
        Map<String, Object> map = new HashMap<>();
        map.put("phoneNumber", verifiedMobileNumber);
        map.put("hasPhoneNumberVerified", true);
        Log.v(TAG, map.toString());
        FirebaseFirestore.getInstance().collection(getString(R.string.ref_user))
                .document(userId).update(map)
                .addOnCompleteListener(task -> {
                    Log.v(TAG, "On complete");
                    if (task.isSuccessful()) {
                        navigateMainActivity();
                    }
                }).addOnFailureListener(error -> Log.v(TAG, "On fail"));
    }

    private void firebaseAuthWithGoogle(@NonNull GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        AppController.getInstance().getFirebaseAuth().signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithCredential:success");
                FirebaseUser user = AppController.getInstance().getFirebaseAuth().getCurrentUser();
                if (user != null) {
                    Log.v(TAG, "\n\nUser token: " + user.getIdToken(false) + "\n\n");
                    GetUser.get(this);
                    mLoadingPb.setVisibility(View.VISIBLE);
                } else {
                    MessageUtils.showActionIndefiniteSnackBar(mParentView, "Sign in failed", "RETRY", RC_SIGN_IN, SignInActivity.this);
                }
            } else {
                MessageUtils.showActionIndefiniteSnackBar(mParentView, "Sign in failed", "RETRY", RC_SIGN_IN, SignInActivity.this);
                Log.w(TAG, "signInWithCredential:failure", task.getException());
            }
        });
    }

    private void navigateMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void signInWithGoogle() {
        Intent signInIntent = AppController.getInstance().getGoogleSignInClient().getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onSnackBarActionClicked(View view, int requestCode) {
        if (requestCode == RC_SIGN_IN) {
            signInWithGoogle();
        }
    }

    @Override
    public void onHttpResponse(String response, int request) {
        Log.d(TAG, response);
        if (request == 0) {
            mLoadingPb.setVisibility(View.VISIBLE);
            User user = User.fromJson(response);
            Preferences preferences = Preferences.getInstance(getApplicationContext());
            preferences.setObject(user, PreferenceKey.USER);
            verifyUser(user);
        }
    }

    @Override
    public void onHttpErrorResponse(VolleyError error, int request) {
        mLoadingPb.setVisibility(View.VISIBLE);
    }

    private void verifyUser(User user) {
        Log.d(TAG, user.toString());
        if (user.isHasPhoneNumberVerified() && user.getPhoneNumber() != null && !TextUtils.isEmpty(user.getPhoneNumber())) {
            navigateMainActivity();
        } else {
            verifyMobileNumber(Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class).getPhoneNumber());
        }
    }

    private void verifyMobileNumber(String mobileNumber) {
        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.putExtra(PhoneVerificationActivity.KEY_PHONE_NO, mobileNumber);
        startActivityForResult(intent, RC_INTENT_VERIFY_MOBILE_NUMBER);
    }
}
