package com.crown.onspot.page;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.OSLocation;
import com.crown.library.onspotlibrary.model.OrderStatusRecord;
import com.crown.library.onspotlibrary.model.business.BusinessOrder;
import com.crown.library.onspotlibrary.model.business.BusinessV4;
import com.crown.library.onspotlibrary.model.cart.OSCart;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.order.OSOrderUpload;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.model.user.UserOrder;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.OSBroadcastReceiver;
import com.crown.library.onspotlibrary.utils.OSLocationUtils;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.callback.OnReceiveOSBroadcasts;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.utils.emun.OrderStatus;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspot.R;
import com.crown.onspot.databinding.ActivityOrderSummaryBinding;
import com.crown.onspot.view.CreateContactDialog;
import com.crown.onspot.view.CreateLocationDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class OrderSummaryActivity extends AppCompatActivity implements OnReceiveOSBroadcasts {
    public static final String CART = "CART";
    public static final String BUSINESS = "BUSINESS";
    private final String TAG = OrderSummaryActivity.class.getName();
    private final int RC_INTENT_VERIFY_MOBILE_NUMBER = 2;
    private IntentFilter mIntentFilter;
    private OSBroadcastReceiver mBroadcastReceiver;
    private ArrayList<OSCart> orders;
    private BusinessV4 business;
    private UserOS user;
    private final CreateContactDialog.OnContactSubmit onContactSubmit = (name, phoneNo) -> {
        if (!phoneNo.trim().replace("+91", "").equals(user.getPhoneNumber().trim().replace("+91", ""))) {
            Intent intent = new Intent(this, PhoneVerificationActivity.class);
            intent.putExtra(PhoneVerificationActivity.KEY_PHONE_NO, phoneNo);
            startActivityForResult(intent, RC_INTENT_VERIFY_MOBILE_NUMBER);
        }
    };
    private long itemCost;
    private long totalTax;
    private long finalAmount;
    private LoadingBounceDialog loadingBounce;
    private ActivityOrderSummaryBinding binding;
    private final CreateLocationDialog.OnLocationSubmit onLocationChange = location -> {
        if (business != null && business.getLocation() != null) {
            double distance = OSLocationUtils.getDistance(business.getLocation(), location);
            if (distance > business.getDeliveryRange()) {
                new AlertDialog.Builder(this)
                        .setTitle(business.getDisplayName())
                        .setMessage("Selected location is out of delivery range.")
                        .setPositiveButton("Change", (dialog, which) -> binding.changeAddressBtn.performClick())
                        .show();
                return;
            }
        }

        FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId())
                .update(getString(R.string.field_location), location)
                .addOnSuccessListener(result -> Toast.makeText(this, "Update completed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error -> OSMessage.showAIBar(this, "Location update failed!!", "Retry", v -> binding.changeAddressBtn.performClick()));
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mBroadcastReceiver = new OSBroadcastReceiver(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.action_os_changes));

        loadingBounce = new LoadingBounceDialog(this);
        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);

        setSupportActionBar(binding.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initiateUi();

        String jsonOrderList = getIntent().getStringExtra(CART);
        orders = new Gson().fromJson(jsonOrderList, new TypeToken<List<OSCart>>() {
        }.getType());
        if (orders != null) {
            showOrder();
        }

        String businessRefId = getIntent().getStringExtra(BUSINESS);
        assert businessRefId != null;
        FirebaseFirestore.getInstance().collection(OSString.refBusiness).document(businessRefId).get()
                .addOnSuccessListener(documentSnapshot -> business = documentSnapshot.toObject(BusinessV4.class))
                .addOnFailureListener(e -> {
                    OSMessage.showSToast(this, "Failed to get business info!!");
                    onBackPressed();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpUi();
    }

    private void initiateUi() {
        binding.changeContactBtn.setOnClickListener(this::onClickedChangeContact);
        binding.changeAddressBtn.setOnClickListener(this::onClickedChangeAddress);
        binding.submitBtn.setOnClickListener(this::onClickedSubmit);
    }

    private void setUpUi() {
        OSLocation location = user.getLocation();
        binding.contactInclude.nameTv.setText(user.getDisplayName());
        binding.contactInclude.phoneNoTv.setText(user.getPhoneNumber());
        if (location != null) {
            binding.addressInclude.addressTv.setText(user.getLocation().getAddressLine());
            binding.addressInclude.howToReachTv.setText(user.getLocation().getHowToReach());
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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void onClickedSubmit(View view) {
        if (business == null) {
            OSMessage.showSBar(this, "Business info loading... please wait");
            return;
        }

        OSOrderUpload orderUpload = createOrder();
        if (orderUpload == null) return;
        loadingBounce.show();
        FirebaseFirestore.getInstance().collection(getString(R.string.ref_order)).add(orderUpload).addOnSuccessListener(result -> {
            loadingBounce.dismiss();
            Toast.makeText(this, "Order placed", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(error -> {
            loadingBounce.dismiss();
            OSMessage.showAIBar(this, "Failed to place order", "Retry", v -> onClickedSubmit(view));
        });
    }

    private OSOrderUpload createOrder() {
        //todo: is empty contact

        if (OSLocationUtils.isEmpty(user.getLocation())) {
            OSMessage.showSToast(this, "Location details required");
            binding.changeAddressBtn.performClick();
            return null;
        }

        OSOrderUpload order = new OSOrderUpload();
        order.setTotalPrice(itemCost + totalTax);
        order.setFinalPrice(finalAmount);

        List<OSCartLite> items = new ArrayList<>();
        for (OSCart item : orders) {
            OSCartLite cart = new OSCartLite();
            cart.setItemId(item.getItemId());
            cart.setItemName(item.getItemName());
            cart.setBusinessRefId(business.getBusinessRefId());
            cart.setPrice(item.getPrice());
            cart.setQuantity(item.getQuantity());
            items.add(cart);
        }
        order.setItems(items);
        order.setBusiness(BusinessOrder.fromBusiness(business));
        order.setCustomer(UserOrder.fromUser(user));
        order.setStatus(OrderStatus.ORDERED);
        order.setOrderedAt(new Timestamp(new Date()));
        order.setStatusRecord(Collections.singletonList(new OrderStatusRecord(OrderStatus.ORDERED, new Timestamp(new Date()))));
        return order;
    }

    private void onClickedChangeAddress(View view) {
        CreateLocationDialog dialog = new CreateLocationDialog();
        if (user.getLocation() != null) {
            Bundle bundle = new Bundle();
            bundle.putString(CreateLocationDialog.KEY_LOCATION, new Gson().toJson(user.getLocation()));
            dialog.setArguments(bundle);
        }
        dialog.show(getSupportFragmentManager(), "");
        dialog.setOnSubmitListener(onLocationChange);
    }

    private void onClickedChangeContact(View view) {
        CreateContactDialog dialog = new CreateContactDialog();
        Bundle bundle = new Bundle();
        bundle.putString(CreateContactDialog.NAME, user.getDisplayName());
        bundle.putString(CreateContactDialog.PHONE_NO, user.getPhoneNumber());
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "");
        dialog.setOnSubmitListener(onContactSubmit);
    }

    private void updatePhoneNo(String number) {
        FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId())
                .update(getString(R.string.field_phone_number), number)
                .addOnSuccessListener(result -> Toast.makeText(this, "Update completed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error -> OSMessage.showAIBar(this, "Contact update failed!!", "Retry", v -> binding.changeContactBtn.performClick()));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private void showOrder() {
        // todo: consider delivery charge
        binding.ordersOiv.clear();
        for (OSCart item : orders) {
            int quantity = (int) item.getQuantity();
            itemCost += item.getPrice().getPrice() * quantity;
            totalTax += BusinessItemUtils.getTaxAmount(item.getPrice()) * quantity;
            double itemFinalAmount = BusinessItemUtils.getFinalPrice(item.getPrice()) * quantity;
            finalAmount += itemFinalAmount;
            binding.ordersOiv.addChild(quantity, item.getItemName(), (int) itemFinalAmount);
        }
        String inr = getString(R.string.inr);
        binding.itemCostTv.setText(String.format(Locale.ENGLISH, "%s%d", inr, itemCost));
        binding.taxTv.setText(String.format(Locale.ENGLISH, "+ %s%d", inr, totalTax));
        binding.discountTv.setText(String.format(Locale.ENGLISH, "- %s%d", inr, (itemCost + totalTax) - finalAmount));
        binding.finalPriceTv.setText(String.format(Locale.ENGLISH, "%s%d", inr, finalAmount));
    }

    @Override
    public void onReceiveBroadcast(Context context, Intent intent) {
        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        setUpUi();
    }
}
