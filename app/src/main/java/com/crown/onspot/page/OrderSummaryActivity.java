package com.crown.onspot.page;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.OSLocation;
import com.crown.library.onspotlibrary.model.OSShippingCharge;
import com.crown.library.onspotlibrary.model.OrderStatusRecord;
import com.crown.library.onspotlibrary.model.business.BusinessOrder;
import com.crown.library.onspotlibrary.model.business.BusinessV3;
import com.crown.library.onspotlibrary.model.cart.OSCart;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.order.OSOrderUpload;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.model.user.UserOrder;
import com.crown.library.onspotlibrary.page.EditProfileActivity;
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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
    private IntentFilter mIntentFilter;
    private OSBroadcastReceiver mBroadcastReceiver;
    private ArrayList<OSCart> orders;
    private BusinessV3 business;
    private UserOS user;
    private long itemCost;
    private long totalTax;
    private long shippingCharge;
    private long finalAmount;
    private String bussRefId;
    private ListenerRegistration bussListener;
    private LoadingBounceDialog loadingBounce;
    private ActivityOrderSummaryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup broadcast receiver to listen to any user data changes
        mBroadcastReceiver = new OSBroadcastReceiver(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.action_os_changes));

        loadingBounce = new LoadingBounceDialog(this);
        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);

        initiateUi();
        handleIntent();
        showOrder();    // showOrder() should execute before handleBusiness() to calculate proper shipping charge
        handleBusiness();
    }

    @Override
    protected void onResume() {
        super.onResume();
        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        registerReceiver(mBroadcastReceiver, mIntentFilter);
        setUpUserInfo();
        manageShippingCharge();
        showFinalPrice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bussListener != null) bussListener.remove();
    }

    @Override
    public void onReceiveBroadcast(Context context, Intent intent) {
        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        setUpUserInfo();
    }

    private void initiateUi() {
        setSupportActionBar(binding.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.changeContactBtn.setOnClickListener(this::intentToEditProfile);
        binding.changeAddressBtn.setOnClickListener(this::intentToEditProfile);
        binding.submitBtn.setOnClickListener(this::onClickedSubmit);
    }

    /**
     * Receive business ref ID and selected orders from the {@link SelectProductActivity}
     */
    private void handleIntent() {
        String jsonOrderList = getIntent().getStringExtra(CART);
        bussRefId = getIntent().getStringExtra(BUSINESS);
        orders = new Gson().fromJson(jsonOrderList, new TypeToken<List<OSCart>>() {
        }.getType());
    }

    private void handleBusiness() {
        if (TextUtils.isEmpty(bussRefId)) return;
        bussListener = FirebaseFirestore.getInstance().collection(OSString.refBusiness).document(bussRefId).addSnapshotListener((bussDoc, e) -> {
            if (bussDoc != null) {
                business = bussDoc.toObject(BusinessV3.class);

                Boolean isOpen = (Boolean) bussDoc.get(OSString.fieldIsOpen);
                Boolean isActive = (Boolean) bussDoc.get(OSString.fieldIsActive);
                Boolean adminBlocked = (Boolean) bussDoc.get(OSString.fieldAdminBlocked);

                // If the business is inActive or close or blocked; back to the select product activity.
                // Select product activity will handle based on the business status
                if (business == null || isActive == null || !isActive || (adminBlocked != null && adminBlocked) || (isOpen != null && !isOpen)) {
                    finish();
                    return;
                }

                manageShippingCharge();
                showFinalPrice();
            } else {
                OSMessage.showSToast(this, "Failed to get business info!!");
                onBackPressed();
            }
        });
    }

    /**
     * Hide delivery charge views
     */
    private void hideShippingChargeViews() {
        binding.summaryInclude.deliveryChargeLabel.setVisibility(View.GONE);
        binding.summaryInclude.deliveryChargeTv.setVisibility(View.GONE);
    }

    private void showShippingChargeViews() {
        binding.summaryInclude.deliveryChargeLabel.setVisibility(View.VISIBLE);
        binding.summaryInclude.deliveryChargeTv.setVisibility(View.VISIBLE);
    }

    private void showInfoBanner(String text) {
        binding.infoBannerTv.setVisibility(View.VISIBLE);
        binding.infoBannerTv.setText(text);
    }

    /**
     * 1 -> If no HOD, display HOD not available
     * 2 -> If there is no customer location, display info to select location
     * 3 -> If the customer selected location is not in the business dRange; let the user know
     * 4 -> If free shipping available or the shipping charge is 0; display free shipping available
     * 5 -> If business to customer distance is lower than the business free shipping range; display enabled free shipping
     * 6 -> If total price is >= business free shipping price; display free shipping with min price info
     */
    private void manageShippingCharge() {
        if (business == null) return;
        binding.infoBannerTv.setVisibility(View.GONE);

        //Let the user know that home delivery is not available for this business
        if (business.getHodAvailable() == null || !business.getHodAvailable()) {
            showInfoBanner(getString(R.string.msg_info_no_hod_business));
            hideShippingChargeViews();
            return;
        }

        // The below code of this method will executes only when home delivery is available
        // If the shipping charge was hidden before; this will display back
        showShippingChargeViews();

        if (OSLocationUtils.isEmpty(user.getLocation())) {
            showInfoBanner("Add delivery address to see offers based on location");
            return;
        }

        double distance = OSLocationUtils.getDistance(business.getLocation(), user.getLocation());
        if (distance > business.getDeliveryRange()) {
            // Distance between business location and customer's delivery location is more than the business delivery range
            showInfoBanner(getString(R.string.msg_info_no_hod_in_selected_location));
            return;
        }

        // The below code will execute only when the customer's selected location is in business's delivery range
        OSShippingCharge sc = business.getShippingCharges();
        if (business.getFsAvailable() == null || business.getFsAvailable() ||
                sc.getPerOrder() == null || sc.getPerOrder() == 0) {
            // The business has free shipping charge
            showInfoBanner("Free shipping charge enabled for your order");
            return;
        }

        if (sc.getFreeShippingDistance() != null && distance <= sc.getFreeShippingDistance()) {
            // The customer is in free shipping distance
            showInfoBanner("Free shipping charge enabled for your selected delivery address");
            return;
        }

        if (sc.getFreeShippingPrice() != null) {
            // Business has free shipping charge offer based on the order final price
            String freeShippingInfo = String.format(Locale.ENGLISH, "Free delivery with order more than ₹ %d", sc.getFreeShippingPrice());
            showInfoBanner(freeShippingInfo);
            if (itemCost >= sc.getFreeShippingPrice()) return;
        }

        shippingCharge = sc.getPerOrder() == null ? 0 : sc.getPerOrder();
    }

    private void setUpUserInfo() {
        OSLocation location = user.getLocation();
        binding.contactInclude.nameTv.setText(user.getDisplayName());
        binding.contactInclude.phoneNoTv.setText(user.getPhoneNumber());
        if (location != null) {
            binding.addressInclude.addressTv.setText(user.getLocation().getAddressLine());
            binding.addressInclude.howToReachTv.setText(user.getLocation().getHowToReach());
        }
    }

    private void onClickedSubmit(View view) {
        if (business == null) return;

        OSOrderUpload orderUpload = createOrder();
        if (orderUpload == null) return;
        loadingBounce.show();
        FirebaseFirestore.getInstance().collection(OSString.refOrder).add(orderUpload).addOnSuccessListener(result -> {
            loadingBounce.dismiss();
            Toast.makeText(this, "Order placed", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(error -> {
            loadingBounce.dismiss();
            OSMessage.showAIBar(this, "Failed to place order", "Retry", v -> onClickedSubmit(view));
        });
    }

    private OSOrderUpload createOrder() {
        if (TextUtils.isEmpty(user.getDisplayName()) || TextUtils.isEmpty(user.getPhoneNumber())) {
            OSMessage.showSToast(this, "Contact details required");
            return null;
        }

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

    private void intentToEditProfile(View view) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra(EditProfileActivity.USER_ID, user.getUserId());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private void showOrder() {
        binding.ordersOiv.clear();
        for (OSCart item : orders) {
            int quantity = (int) item.getQuantity();
            itemCost += item.getPrice().getPrice() * quantity;
            totalTax += BusinessItemUtils.getTaxAmount(item.getPrice()) * quantity;
            double itemFinalAmount = BusinessItemUtils.getFinalPrice(item.getPrice()) * quantity;
            finalAmount += itemFinalAmount;
            binding.ordersOiv.addChild(quantity, item.getItemName(), (int) itemFinalAmount);
        }
        binding.summaryInclude.itemCostTv.setText(String.format(Locale.ENGLISH, "%s%d", OSString.inrSymbol, itemCost));
        binding.summaryInclude.taxTv.setText(String.format(Locale.ENGLISH, "+ %s%d", OSString.inrSymbol, totalTax));
        binding.summaryInclude.discountTv.setText(String.format(Locale.ENGLISH, "- %s%d", OSString.inrSymbol, (itemCost + totalTax) - finalAmount));
    }

    private void showFinalPrice() {
        if (business == null) return;
        binding.summaryInclude.deliveryChargeTv.setText(String.format(Locale.ENGLISH, "+ %s%d", OSString.inrSymbol, shippingCharge));
        finalAmount += shippingCharge;
        binding.summaryInclude.finalPriceTv.setText(String.format(Locale.ENGLISH, "%s%d", OSString.inrSymbol, finalAmount));
    }
}
