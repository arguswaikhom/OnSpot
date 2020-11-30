package com.crown.onspot.page;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.crown.onspot.R;
import com.crown.onspot.controller.Validate;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken;
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks;

import java.util.concurrent.TimeUnit;

@Deprecated
public class PhoneVerificationActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String KEY_PHONE_NO = "PHONE_NO";
    public static final String TAG = PhoneVerificationActivity.class.getName();

    private TextInputLayout mVerificationCodeTIL;
    private TextInputEditText mVerificationCodeTIET;
    private TextInputEditText mPhoneNumberTIET;
    private AppCompatButton mVerifyACBtn;
    private AppCompatButton mSendVerificationCodeACBtn;

    private FirebaseAuth mFirebaseAuth;
    private ForceResendingToken mResendToken;
    private String mVerificationID;
    private String mPhoneNumber;
    private final OnVerificationStateChangedCallbacks mPhoneVerificationCallBack = new OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            mVerificationCodeTIET.setText(phoneAuthCredential.getSmsCode());
            signInWithPhoneAuthCredential(phoneAuthCredential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                Toast.makeText(getApplicationContext(), "Invalid request", Toast.LENGTH_SHORT).show();
            } else if (e instanceof FirebaseTooManyRequestsException) {
                Toast.makeText(getApplicationContext(), "Server error", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            mVerificationID = s;
            mResendToken = forceResendingToken;
            Toast.makeText(getApplicationContext(), "OTP sent", Toast.LENGTH_SHORT).show();
        }
    };
    private boolean mHasSentCodeBefore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        setUpUI();
        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getString(KEY_PHONE_NO) != null) {
            mPhoneNumberTIET.setText(bundle.getString(KEY_PHONE_NO));
        }
    }

    private void setUpUI() {
        mPhoneNumberTIET = findViewById(R.id.tiet_apv_phone_number);
        mVerificationCodeTIET = findViewById(R.id.tiet_apv_otp);
        mVerificationCodeTIL = findViewById(R.id.til_apv_otp);
        mVerifyACBtn = findViewById(R.id.acbtn_apv_verify);
        mSendVerificationCodeACBtn = findViewById(R.id.acbtn_apv_send_otp);

        mVerifyACBtn.setOnClickListener(this);
        mSendVerificationCodeACBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.acbtn_apv_verify: {
                onClickedVerify();
                break;
            }
            case R.id.acbtn_apv_send_otp: {
                onClickedSendOTP();
                break;
            }
        }
    }

    private void onClickedVerify() {
        String phoneNumber = "+91" + mPhoneNumberTIET.getText().toString().trim();
        String verificationCode = mVerificationCodeTIL.getEditText().getText().toString().trim();

        if (!Validate.isPhoneNumber(phoneNumber)) {
            mPhoneNumberTIET.setError("Invalid input");
            return;
        }

        if (TextUtils.isEmpty(verificationCode)) {
            mVerificationCodeTIL.setError("Invalid input");
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationID, verificationCode);
        signInWithPhoneAuthCredential(credential);
    }

    private void onClickedSendOTP() {
        mPhoneNumber = "+91" + mPhoneNumberTIET.getText().toString().trim();

        if (!Validate.isPhoneNumber(mPhoneNumber)) {
            mPhoneNumberTIET.setError("Invalid input");
            return;
        }

        if (mHasSentCodeBefore) {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(mPhoneNumber, 120, TimeUnit.SECONDS, this, mPhoneVerificationCallBack, mResendToken);
        } else {
            mHasSentCodeBefore = true;
            mSendVerificationCodeACBtn.setText("RESEND OTP");
            PhoneAuthProvider.getInstance().verifyPhoneNumber(mPhoneNumber, 120, TimeUnit.SECONDS, this, mPhoneVerificationCallBack);
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mFirebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getApplicationContext(), "Verified", Toast.LENGTH_SHORT).show();
                onVerifiedMobileNumber();
            } else {
                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(getApplicationContext(), "Invalid code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void onVerifiedMobileNumber() {
        Log.v("aaa", "onVerifiedMobileNumber");
        Intent intent = new Intent();
        intent.putExtra(KEY_PHONE_NO, mPhoneNumber);
        setResult(RESULT_OK, intent);
        finish();
    }
}
