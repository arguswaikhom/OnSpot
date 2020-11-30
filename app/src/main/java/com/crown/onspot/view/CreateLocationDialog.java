package com.crown.onspot.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.crown.library.onspotlibrary.model.OSLocation;
import com.crown.onspot.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.schibstedspain.leku.LocationPickerActivity;

import static android.app.Activity.RESULT_OK;
import static com.schibstedspain.leku.LocationPickerActivityKt.LATITUDE;
import static com.schibstedspain.leku.LocationPickerActivityKt.LOCATION_ADDRESS;
import static com.schibstedspain.leku.LocationPickerActivityKt.LONGITUDE;
import static com.schibstedspain.leku.LocationPickerActivityKt.ZIPCODE;

@Deprecated
public class CreateLocationDialog extends DialogFragment implements View.OnClickListener {
    public static final String KEY_LOCATION = "LOCATION";
    private final int RC_INTENT_SELECT_LOCATION_FROM_MAP = 1;

    private OSLocation mSelectedOSLocation;
    private TextInputLayout addressTIL;
    private TextInputLayout howToReachTIL;
    private TextInputEditText addressTIET;
    private TextInputEditText howToReachTIET;
    private Button selectLocationBtn;
    private OnLocationSubmit clickHandler;

    public void setOnSubmitListener(OnLocationSubmit clickHandler) {
        this.clickHandler = clickHandler;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_create_location, null);

        initiateUi(root);
        selectLocationBtn.setOnClickListener(this);

        Bundle argument = getArguments();
        if (argument != null) {
            String json = argument.getString(KEY_LOCATION, null);
            if (json != null) {
                mSelectedOSLocation = new Gson().fromJson(json, OSLocation.class);
                setUpUi();
            }
        }
        return builder.setView(root).create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_INTENT_SELECT_LOCATION_FROM_MAP) {
            if (resultCode == RESULT_OK && data != null) {
                double latitude = data.getDoubleExtra(LATITUDE, 0.0);
                double longitude = data.getDoubleExtra(LONGITUDE, 0.0);
                String address = data.getStringExtra(LOCATION_ADDRESS);
                String postalCode = data.getStringExtra(ZIPCODE);

                mSelectedOSLocation = new OSLocation(address, new GeoPoint(latitude, longitude), postalCode, "");
                setUpUi();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cl_ok:
                onCLickedOk();
                break;
            case R.id.btn_cl_cancel:
                this.dismiss();
                break;
            case R.id.btn_cl_select_location:
                showPlacePicker();
        }
    }

    private void onCLickedOk() {
        String address = addressTIL.getEditText().getText().toString().trim();
        String howToReach = howToReachTIL.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(address)) {
            addressTIL.setError("Invalid input");
            return;
        }

        if (TextUtils.isEmpty(howToReach)) {
            howToReachTIL.setError("Invalid input");
            return;
        }

        mSelectedOSLocation.setAddressLine(address);
        mSelectedOSLocation.setHowToReach(howToReach);

        if (clickHandler != null)
            clickHandler.onLocationSubmit(mSelectedOSLocation);
        this.dismiss();
    }

    private void setUpUi() {
        if (mSelectedOSLocation != null) {
            addressTIET.setText(mSelectedOSLocation.getAddressLine());
            howToReachTIET.setText(mSelectedOSLocation.getHowToReach());
        }
    }

    private void initiateUi(View root) {
        addressTIL = root.findViewById(R.id.til_cl_address_line);
        howToReachTIL = root.findViewById(R.id.til_cl_how_to_reach);
        addressTIET = root.findViewById(R.id.tiet_cl_address_line);
        howToReachTIET = root.findViewById(R.id.tiet_cl_how_to_reach);
        selectLocationBtn = root.findViewById(R.id.btn_cl_select_location);
        root.findViewById(R.id.btn_cl_cancel).setOnClickListener(this);
        root.findViewById(R.id.btn_cl_ok).setOnClickListener(this);
    }

    private void showPlacePicker() {
        Intent pickerActivity = new LocationPickerActivity.Builder()
                .withGeolocApiKey(getResources().getString(R.string.key_api))
                //.withMapStyle(R.raw.place_picker_style)
                .build(getContext());

        startActivityForResult(pickerActivity, RC_INTENT_SELECT_LOCATION_FROM_MAP);
    }

    public interface OnLocationSubmit {
        void onLocationSubmit(OSLocation location);
    }
}
