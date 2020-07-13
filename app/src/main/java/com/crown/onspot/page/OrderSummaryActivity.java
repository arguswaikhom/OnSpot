package com.crown.onspot.page;

import android.app.AlertDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.crown.onspot.R;
import com.crown.onspot.model.Contact;
import com.crown.onspot.model.OSLocation;
import com.crown.onspot.model.OrderItem;
import com.crown.onspot.model.Shop;
import com.crown.onspot.model.StatusRecord;
import com.crown.onspot.model.User;
import com.crown.onspot.utils.preference.PreferenceKey;
import com.crown.onspot.utils.preference.Preferences;
import com.crown.onspot.view.CreateContactDialog;
import com.crown.onspot.view.CreateLocationDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//import com.crown.onspot.model.Location;

public class OrderSummaryActivity extends AppCompatActivity implements View.OnClickListener,
        CreateContactDialog.OnContactDialogActionClicked, CreateLocationDialog.OnLocationDialogActionClicked {
    public static final String KEY_ORDER = "ORDER";
    public static final String KEY_SHOP = "SHOP";
    private final String TAG = OrderSummaryActivity.class.getName();
    private final int RC_INTENT_VERIFY_MOBILE_NUMBER = 2;

    private TableLayout mOrderTL;
    private TextView mNameTv;
    private TextView mPhoneNo;
    private TextView mAddress;
    private TextView mHowToReach;
    private Button mSubmitBtn;
    private ProgressBar mLoadingPbar;
    private FirebaseFirestore mFireStore;
    private ArrayList<OrderItem> mOrder;
    private Shop mShop;
    private long totalAmount = 0;
    private long finalAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        Toolbar toolbar = findViewById(R.id.tbar_fm_tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFireStore = FirebaseFirestore.getInstance();
        initiateUi();

        String jsonOrderList = getIntent().getStringExtra(KEY_ORDER);
        mOrder = new Gson().fromJson(jsonOrderList, new TypeToken<List<OrderItem>>() {
        }.getType());
        if (mOrder != null) {
            showOrder();
        }

        String json = getIntent().getStringExtra(KEY_SHOP);
        if (json != null && !json.isEmpty()) {
            mShop = new Gson().fromJson(json, Shop.class);
        }
    }

    private void initiateUi() {
        mOrderTL = findViewById(R.id.tl_ao_order_list);
        findViewById(R.id.btn_ao_change_contact).setOnClickListener(this);
        findViewById(R.id.btn_ao_change_address).setOnClickListener(this);
        mSubmitBtn = findViewById(R.id.btn_ao_submit);
        mSubmitBtn.setOnClickListener(this);
        mLoadingPbar = findViewById(R.id.pbar_ao_loading);

        View contact = findViewById(R.id.include_ao_contact);
        mNameTv = contact.findViewById(R.id.tv_cd_name);
        mPhoneNo = contact.findViewById(R.id.tv_cd_phone_no);

        View destination = findViewById(R.id.include_ao_address);
        mAddress = destination.findViewById(R.id.tv_dd_address);
        mHowToReach = destination.findViewById(R.id.tv_dd_reach);
    }

    private void setUpUi() {
        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        OSLocation location = user.getLocation();

        mNameTv.setText(user.getDisplayName());
        mPhoneNo.setText(user.getPhoneNumber());

        if (location != null) {
            mAddress.setText(user.getLocation().getAddressLine());
            mHowToReach.setText(user.getLocation().getHowToReach());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpUi();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ao_change_contact: {
                showCreateContactDialog();
                break;
            }
            case R.id.btn_ao_change_address: {
                showCreateLocationDialog();
                break;
            }
            case R.id.btn_ao_submit: {
                onClickedSubmit();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_INTENT_VERIFY_MOBILE_NUMBER: {
                if (resultCode == RESULT_OK && data != null) {
                    String verifiedMobileNumber = data.getStringExtra(PhoneVerificationActivity.KEY_PHONE_NO);
                    updatePhoneNo(verifiedMobileNumber);
                }
            }
            break;
        }
    }

    private void onClickedSubmit() {
        Map<String, Object> param = getDataToUpload();
        if (param == null) return;
        mLoadingPbar.setVisibility(View.VISIBLE);
        mSubmitBtn.setEnabled(false);
        mFireStore.collection("order").add(param).addOnCompleteListener(task -> {
            mLoadingPbar.setVisibility(View.GONE);
            Toast.makeText(this, "Order placed", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(error -> {
            mSubmitBtn.setEnabled(true);
            mLoadingPbar.setVisibility(View.GONE);
        });
    }

    private Map<String, Object> getDataToUpload() {
        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> orderItem = new ArrayList<>();
        for (OrderItem item : mOrder) {
            orderItem.add(item.getUploadable());
        }

        if (user.getLocation() == null || user.getLocation().isEmpty()) {
            Toast.makeText(this, "Invalid delivery location", Toast.LENGTH_SHORT).show();
            return null;
        }

        map.put("customerId", user.getUserId());
        map.put("customerDisplayName", user.getDisplayName());
        map.put("businessRefId", mShop.getBusinessRefId());
        map.put("businessDisplayName", mShop.getDisplayName());
        map.put("status", StatusRecord.Status.ORDERED);
        map.put("statusRecord", Collections.singletonList(new StatusRecord(StatusRecord.Status.ORDERED, new Timestamp(new Date()))));
        map.put("totalPrice", totalAmount);
        map.put("finalPrice", finalAmount);
        map.put("items", orderItem);
        map.put("destination", user.getLocation());
        map.put("contact", new Contact(user.getDisplayName(), user.getPhoneNumber()));

        return map;
    }

    private void showCreateLocationDialog() {
        CreateLocationDialog dialog = new CreateLocationDialog();
        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        OSLocation OSLocation = user.getLocation();
        if (OSLocation != null) {
            Bundle bundle = new Bundle();
            bundle.putString(CreateLocationDialog.KEY_LOCATION, new Gson().toJson(OSLocation));
            dialog.setArguments(bundle);
        }
        dialog.show(getSupportFragmentManager(), "");
    }

    private void showCreateContactDialog() {
        CreateContactDialog dialog = new CreateContactDialog();
        Bundle bundle = new Bundle();
        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        bundle.putString(CreateContactDialog.KEY_NAME, user.getDisplayName());
        bundle.putString(CreateContactDialog.KEY_PHONE_NO, user.getPhoneNumber());
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContactDialogPositiveActionClicked(String name, String phoneNo) {
        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        String userId = user.getUserId();

        if (!phoneNo.replace("+91", "").equals(user.getPhoneNumber().replace("+91", ""))) {
            Intent intent = new Intent(this, PhoneVerificationActivity.class);
            intent.putExtra(PhoneVerificationActivity.KEY_PHONE_NO, phoneNo);
            startActivityForResult(intent, RC_INTENT_VERIFY_MOBILE_NUMBER);
        }
    }

    private void updatePhoneNo(String number) {
        Map<String, Object> map = new HashMap<>();
        map.put("phoneNumber", number);
        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        String userId = user.getUserId();

        FirebaseFirestore.getInstance().collection(getString(R.string.ref_user))
                .document(userId)
                .update(map)
                .addOnCompleteListener(task -> {
                    setUpUi();
                    Toast.makeText(this, "Update completed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show());
    }

    private void showOrder() {
        long totalDiscount = 0;
        for (OrderItem item : mOrder) {
            totalAmount += (item.getPriceWithTax() * item.getQuantity());
            long itemAmount = item.getFinalPrice() * (item.getQuantity());
            finalAmount += itemAmount;


            LinearLayout root = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.order__order_item, null);
            ((TextView) root.findViewById(R.id.tv_ooi_quantity)).setText(String.format(Locale.ENGLISH, "x%d", item.getQuantity()));
            ((TextView) root.findViewById(R.id.tv_ooi_item_name)).setText(item.getItemName());
            ((TextView) root.findViewById(R.id.tv_ooi_price)).setText(String.format("₹ %d", item.getPriceWithTax() * item.getQuantity()));
            mOrderTL.addView(root);
        }
        totalDiscount = totalAmount - finalAmount;

        ((TextView) findViewById(R.id.tv_ao_total_price)).setText(String.format("₹ %d", totalAmount));
        ((TextView) findViewById(R.id.tv_ao_discount)).setText("- ₹ " + totalDiscount);
        ((TextView) findViewById(R.id.tv_ao_final_price)).setText("₹ " + finalAmount);
    }

    @Override
    public void onLocationDialogPositiveActionClicked(OSLocation OSLocation) {
        if (mShop != null && mShop.getLocation() != null) {
            User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
            GeoPoint businessGeoPoint = mShop.getLocation().getGeoPoint();
            Log.v("TAG", "Business location: " + mShop.getLocation());

            Location startLocation = new Location("start");
            startLocation.setLatitude(businessGeoPoint.getLatitude());
            startLocation.setLongitude(businessGeoPoint.getLongitude());

            Location endLocation = new Location("end");
            endLocation.setLatitude(OSLocation.getGeoPoint().getLatitude());
            endLocation.setLongitude(OSLocation.getGeoPoint().getLongitude());

            double distance = (startLocation.distanceTo(endLocation) / 1000);
            double deliveryRange = mShop.getDeliveryRange();

            Log.v(TAG, "Business: " + businessGeoPoint + " DLocation: " + endLocation);
            Log.v(TAG, "Distance: " + distance + " Range: " + deliveryRange);
            if (distance > deliveryRange) {
                new AlertDialog.Builder(this)
                        .setTitle(mShop.getDisplayName())
                        .setMessage("Selected location is out of business's delivery range.")
                        .setPositiveButton("Change", (dialog, which) -> showCreateLocationDialog())
                        .show();
                return;
            }
        }

        User user = Preferences.getInstance(getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        FirebaseFirestore.getInstance().collection(getString(R.string.ref_user))
                .document(user.getUserId())
                .update("location", OSLocation)
                .addOnCompleteListener(task -> {
                    setUpUi();
                    Toast.makeText(this, "Update completed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show());
    }
}
