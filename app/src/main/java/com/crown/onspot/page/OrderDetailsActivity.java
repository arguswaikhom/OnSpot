package com.crown.onspot.page;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.controller.OSVolley;
import com.crown.library.onspotlibrary.model.OrderStatusRecord;
import com.crown.library.onspotlibrary.model.business.BusinessV2a;
import com.crown.library.onspotlibrary.model.businessItem.BusinessItemV4;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.review.OSRUOrderByCustomer;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.model.user.UserV3;
import com.crown.library.onspotlibrary.page.BusinessActivity;
import com.crown.library.onspotlibrary.page.BusinessReviewActivity;
import com.crown.library.onspotlibrary.utils.OSCommonIntents;
import com.crown.library.onspotlibrary.utils.OSJsonParse;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSRatingUtils;
import com.crown.library.onspotlibrary.utils.OSReviewClassUtils;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.utils.emun.OrderStatus;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.library.onspotlibrary.views.OrderPassView;
import com.crown.onspot.databinding.ActivityOrderDetailsBinding;
import com.crown.onspot.view.ImageSliderAdapter;
import com.google.gson.Gson;
import com.smarteist.autoimageslider.SliderAnimations;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity {
    public static final String ORDER = "ORDER";
    private static final String TAG = OrderDetailsActivity.class.getName();
    private final Gson gson = new Gson();
    private final List<BusinessItemV4> orderItems = new ArrayList<>();
    private OSOrder order;
    private UserV3 customer;
    private UserV3 delivery;
    private BusinessV2a business;
    private List<String> sliderImages;
    private ImageSliderAdapter imageAdapter;
    private LoadingBounceDialog loadingDialog;
    private ActivityOrderDetailsBinding binding;
    private OSRUOrderByCustomer customerReview = new OSRUOrderByCustomer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(binding.getRoot());
        loadingDialog = new LoadingBounceDialog(this);
        setSupportActionBar(binding.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        order = gson.fromJson(getIntent().getStringExtra(ORDER), OSOrder.class);
        customer = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        initUi();
        getOrderDetails();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initUi() {
        sliderImages = new ArrayList<>();
        imageAdapter = new ImageSliderAdapter(sliderImages);
        binding.sliderInclude.imageSlider.setSliderAdapter(imageAdapter);
        binding.sliderInclude.imageSlider.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
        binding.sliderInclude.imageSlider.startAutoCycle();

        binding.ratingsInclude.reviewMoreBtn.setOnClickListener(this::onClickedReviewMore);
        binding.deliveryInclude.deliveryRootCl.setOnClickListener(v -> {
            if (order.getStatus() == OrderStatus.DELIVERED)
                onClickedReviewMore(binding.deliveryInclude.deliveryRootCl);
        });
        binding.ratingsInclude.submitOrderReviewBtn.setOnClickListener(this::onClickedSubmitOrderReview);

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELED) {
            binding.ratingsInclude.ratingRootLl.setVisibility(View.VISIBLE);
        } else {
            binding.ratingsInclude.ratingRootLl.setVisibility(View.GONE);
        }

        binding.sliderInclude.businessNameTv.setOnClickListener(v -> {
            if (business == null) return;
            Intent intent = new Intent(this, BusinessActivity.class);
            intent.putExtra(BusinessActivity.BUSINESS_ID, business.getBusinessRefId());
            startActivity(intent);
        });

        binding.sliderInclude.businessRatingCountTv.setOnClickListener(v -> {
            if (business == null) return;
            Intent businessReviewIntent = new Intent(this, BusinessReviewActivity.class);
            businessReviewIntent.putExtra(BusinessReviewActivity.BUSINESS, business.toString());
            startActivity(businessReviewIntent);
        });
    }

    private void onClickedSubmitOrderReview(View view) {
        float rating = binding.ratingsInclude.orderRbar.getRating();
        String msg = binding.ratingsInclude.orderReviewTiet.getText().toString();

        if (rating <= 0) {
            // Minimum require 1 star
            OSMessage.showSToast(this, "Rating required!!");
            return;
        }

        if (customerReview.getMsg() != null && customerReview.getRating() != null) {
            if (customerReview.getRating() == rating && customerReview.getMsg().equals(msg)) {
                // User didn't change any content, so no need to send request
                OSMessage.showSToast(this, "Review updated!!");
                return;
            }
        }

        loadingDialog.show();
        if (customerReview.getReviewId() == null) {
            customerReview.setBusiness(business.getBusinessRefId());
            customerReview.setCustomer(customer.getUserId());
            customerReview.setOrder(order.getOrderId());
        }
        customerReview.setRating((double) rating);
        customerReview.setMsg(msg);
        OSReviewClassUtils.updateReview(customerReview, () -> {
            loadingDialog.dismiss();
            OSMessage.showSToast(this, "Review added");
        }, ((e, msg1) -> {
            loadingDialog.dismiss();
            OSMessage.showSToast(this, "Failed to update review");
        }));
    }

    private void onClickedReviewMore(View view) {
        if (orderItems.size() == 0 || business == null) return;
        Intent intent = new Intent(this, OrderRatingActivity.class);
        intent.putExtra(OrderRatingActivity.ORDER, gson.toJson(order));
        intent.putExtra(OrderRatingActivity.BUSINESS, gson.toJson(business));
        intent.putExtra(OrderRatingActivity.ORDER_ITEMS, gson.toJson(orderItems));
        if (delivery != null) intent.putExtra(OrderRatingActivity.DELIVERY, gson.toJson(delivery));
        startActivity(intent);
    }

    private void getOrderDetails() {
        loadingDialog.show();
        OSVolley.getInstance(getApplicationContext()).addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiOrderDetails, response -> {
            loadingDialog.dismiss();
            try {
                JSONObject obj = new JSONObject(response);
                customer = gson.fromJson(OSJsonParse.stringFromObject(obj, OSString.fieldCustomer), UserV3.class);
                business = gson.fromJson(OSJsonParse.stringFromObject(obj, OSString.refBusiness), BusinessV2a.class);
                JSONArray itemsJson = OSJsonParse.arrayFromObject(obj, OSString.fieldItems);
                for (int i = 0; i < itemsJson.length(); i++) {
                    try {
                        BusinessItemV4 businessItemV4 = gson.fromJson(itemsJson.get(i) + "", BusinessItemV4.class);
                        sliderImages.addAll(businessItemV4.getImageUrls());
                        orderItems.add(businessItemV4);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                JSONObject deliveryJson = obj.getJSONObject(OSString.fieldDelivery);
                if (deliveryJson.length() == 0)
                    binding.deliveryInclude.deliveryRootCl.setVisibility(View.GONE);
                else delivery = gson.fromJson(deliveryJson.toString(), UserV3.class);
                JSONObject reviewJson = obj.getJSONObject(OSString.refReview);
                if (reviewJson.length() != 0)
                    customerReview = OSRUOrderByCustomer.fromJson(reviewJson.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                OSMessage.showSToast(this, "Something went wrong!!");
                onBackPressed();
            }
            imageAdapter.notifyDataSetChanged();
            setUpUi();
        }, error -> {
            error.printStackTrace();
            loadingDialog.dismiss();
            OSMessage.showSToast(this, "Something went wrong!!");
            onBackPressed();
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                param.put(OSString.fieldOrderId, order.getOrderId());
                return param;
            }
        });
    }

    // *Call this method only after all the details are received from the server
    private void setUpUi() {
        if (business != null) {
            binding.sliderInclude.businessNameTv.setText(business.getDisplayName());
            if (business.getMobileNumber() != null && !business.getMobileNumber().trim().equals("")) {
                binding.sliderInclude.businessCallIv.setVisibility(View.VISIBLE);
                binding.sliderInclude.businessCallIv.setOnClickListener(v -> OSCommonIntents.onIntentCallRequest(this, business.getMobileNumber()));
            }
            if (business.getEmail() != null && !business.getEmail().trim().equals("")) {
                binding.sliderInclude.businessEmailIv.setVisibility(View.VISIBLE);
                binding.sliderInclude.businessEmailIv.setOnClickListener(v -> OSCommonIntents.onIntentEmailTo(this, business.getEmail()));
            }
            if (business.getWebsite() != null && !business.getWebsite().trim().equals("")) {
                binding.sliderInclude.businessWebsiteIv.setVisibility(View.VISIBLE);
                binding.sliderInclude.businessWebsiteIv.setOnClickListener(v -> OSCommonIntents.onIntentLink(this, business.getWebsite()));
            }

            OSRatingUtils.getReviewInfo(business.getBusinessRating(), (average, review) -> {
                binding.sliderInclude.businessRatingRbar.setRating(Float.parseFloat(average));
                binding.sliderInclude.businessRatingCountTv.setText(review);
            });
        }

        if (delivery != null) {
            binding.deliveryInclude.deliveryNameTv.setText(delivery.getDisplayName());
            OSRatingUtils.getReviewInfo(delivery.getDeliveryRating(), (average, review) -> {
                binding.deliveryInclude.deliveryRbar.setRating(Float.parseFloat(average));
                binding.deliveryInclude.reviewCountTv.setText(review);
            });
            binding.deliveryInclude.deliveryCallIv.setOnClickListener(v -> OSCommonIntents.onIntentCallRequest(this, delivery.getPhoneNumber()));
            Glide.with(this).load(delivery.getProfileImageUrl()).apply(new RequestOptions().circleCrop()).into(binding.deliveryInclude.deliveryImageIv);
        }

        for (OSCartLite item : order.getItems()) {
            binding.orderInclude.orderItemOiv.addChild((int) (long) item.getQuantity(), item.getItemName(), (int) (item.getPrice().getPrice() * item.getQuantity()));
        }

        binding.orderInclude.summaryInclude.productCostTv.setText(String.format(Locale.ENGLISH, "%s%d", OSString.inrSymbol, order.getProductPrice()));
        binding.orderInclude.summaryInclude.taxTv.setText(String.format(Locale.ENGLISH, "+ %s%d", OSString.inrSymbol, order.getTotalTax()));
        binding.orderInclude.summaryInclude.discountTv.setText(String.format(Locale.ENGLISH, "- %s%d", OSString.inrSymbol, order.getTotalDiscount()));
        binding.orderInclude.summaryInclude.finalPriceTv.setText(String.format(Locale.ENGLISH, "%s%d", OSString.inrSymbol, order.getFinalPrice()));
        if (order.getHodAvailable() != null && order.getHodAvailable()) {
            binding.orderInclude.summaryInclude.deliveryChargeTv.setText(String.format(Locale.ENGLISH, "%s%d", OSString.inrSymbol, order.getShippingCharge()));
        } else {
            binding.orderInclude.summaryInclude.deliveryChargeLabel.setVisibility(View.GONE);
            binding.orderInclude.summaryInclude.deliveryChargeTv.setVisibility(View.GONE);
        }

        List<OrderStatusRecord> statusRecords = order.getStatusRecord();
        binding.timeline.setPrePath(order.getStatus() != OrderStatus.CANCELED && order.getStatus() != OrderStatus.DELIVERED);
        for (OrderStatusRecord record : statusRecords) {
            long sec = record.getTimestamp().getSeconds();
            OrderPassView passView = new OrderPassView(this);
            passView.bind(OSTimeUtils.getDay(sec), OSTimeUtils.getTime(sec), record.getStatus().getTimelineLabel(), record.getStatus().name(), true);
            binding.timeline.add(passView);
        }

        if (!TextUtils.isEmpty(customerReview.getReviewId())) {
            binding.ratingsInclude.orderRbar.setRating(customerReview.getRating() != null ? (float) (double) customerReview.getRating() : 0);
            binding.ratingsInclude.orderReviewTiet.setText(TextUtils.isEmpty(customerReview.getMsg()) ? "" : customerReview.getMsg());
        }
    }
}