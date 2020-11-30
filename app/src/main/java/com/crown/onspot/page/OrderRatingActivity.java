package com.crown.onspot.page;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.controller.OSVolley;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.business.BusinessV2a;
import com.crown.library.onspotlibrary.model.businessItem.BusinessItemV4;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.review.OSRItemByCustomer;
import com.crown.library.onspotlibrary.model.review.OSRUDeliveryByCustomer;
import com.crown.library.onspotlibrary.model.review.OSRUItemByCustomer;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.model.user.UserV3;
import com.crown.library.onspotlibrary.utils.OSColorUtils;
import com.crown.library.onspotlibrary.utils.OSJsonParse;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.OSVolleyCacheUtils;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspot.R;
import com.crown.onspot.databinding.ActivityOrderRatingBinding;
import com.crown.onspot.databinding.LiReviewOrderItemBinding;
import com.crown.onspot.view.ListItemAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRatingActivity extends AppCompatActivity {

    public static final String ORDER = "ORDER";
    public static final String DELIVERY = "DELIVERY";
    public static final String BUSINESS = "BUSINESS";
    public static final String ORDER_ITEMS = "ORDER_ITEMS";
    private final List<ListItem> itemReviewDataset = new ArrayList<>();
    private UserOS user;
    private OSOrder order;
    private UserV3 delivery;
    private BusinessV2a business;
    private ListItemAdapter adapter;
    private OSRUDeliveryByCustomer deliveryByCustomer;
    private LoadingBounceDialog loadingDialog;
    private ActivityOrderRatingBinding binding;
    private LiReviewOrderItemBinding deliveryReview;
    private List<BusinessItemV4> orderItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderRatingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        deliveryReview = binding.deliveryRatingInclude;
        loadingDialog = new LoadingBounceDialog(this);

        Gson gson = new Gson();
        Intent intent = getIntent();
        delivery = gson.fromJson(intent.getStringExtra(DELIVERY), UserV3.class);
        order = gson.fromJson(intent.getStringExtra(ORDER), OSOrder.class);
        business = gson.fromJson(intent.getStringExtra(BUSINESS), BusinessV2a.class);
        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        orderItems = gson.fromJson(intent.getStringExtra(ORDER_ITEMS), new TypeToken<List<BusinessItemV4>>() {
        }.getType());

        initUI();
        getReviewDetails();
        setUpUi();
    }

    private void getReviewDetails() {
        loadingDialog.show();
        OSVolley.getInstance(getApplicationContext()).addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiOrderItemReviewDetails, response -> {
            JSONObject apiResponse = OSJsonParse.stringToObject(response);

            // Display item reviews
            JSONArray reviews = OSJsonParse.arrayFromObject(apiResponse, OSString.keyItemReview);
            for (int i = 0; i < reviews.length(); i++) {
                try {
                    OSRUItemByCustomer itemByCustomer = OSRUItemByCustomer.fromJson(reviews.get(i) + "");
                    for (ListItem item : itemReviewDataset) {
                        if (((OSRItemByCustomer) item).getItem().getItemId().equals(itemByCustomer.getItem())) {
                            ((OSRItemByCustomer) item).setReviewId(itemByCustomer.getReviewId());
                            ((OSRItemByCustomer) item).setCreatedOn(itemByCustomer.getCreatedOn());
                            ((OSRItemByCustomer) item).setRating(itemByCustomer.getRating());
                            ((OSRItemByCustomer) item).setMsg(itemByCustomer.getMsg());
                            break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Display delivery review by the customer
            JSONObject deliveryRating = OSJsonParse.objectFromObject(apiResponse, OSString.keyDeliveryReview);
            if (deliveryRating.length() != 0) {
                deliveryByCustomer = OSRUDeliveryByCustomer.fromJson(deliveryRating.toString());
                deliveryReview.ratingBar.setRating(deliveryByCustomer.getRating() != null ? (float) (double) deliveryByCustomer.getRating() : 0f);
                deliveryReview.reviewMsgTiet.setText(deliveryByCustomer.getMsg());
            }

            adapter.notifyDataSetChanged();
            loadingDialog.dismiss();
        }, error -> {
            // Log.d("debug", error + "");
            loadingDialog.dismiss();
            OSMessage.showSToast(this, "Unable to get review info!!");
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

    private void setUpUi() {
        if (delivery == null) {
            binding.deliveryRatingHeaderLl.setVisibility(View.GONE);
            deliveryReview.rootMcv.setVisibility(View.GONE);
        } else {
            deliveryReview.rootMcv.setCardBackgroundColor(OSColorUtils.getDefinedRandomColor(this));
            Glide.with(this).load(delivery.getProfileImageUrl()).into(deliveryReview.imageIv);
            deliveryReview.displayNameTv.setText(delivery.getDisplayName());
            binding.deliveryRatingMsgTv.setText(Html.fromHtml(getString(R.string.msg_delivery_rating, delivery.getDisplayName())));
        }

        for (BusinessItemV4 businessItemV4 : orderItems) {
            if (businessItemV4.getImageUrls() != null && !businessItemV4.getImageUrls().isEmpty()) {
                OSVolleyCacheUtils.setBusinessItemImages(getApplicationContext(), businessItemV4);
            }
            OSRItemByCustomer itemByCustomer = new OSRItemByCustomer();
            itemByCustomer.setBusiness(business);
            itemByCustomer.setCustomer(user);
            itemByCustomer.setItem(businessItemV4);
            itemReviewDataset.add(itemByCustomer);
        }
        adapter.notifyDataSetChanged();
    }

    private void initUI() {
        adapter = new ListItemAdapter(itemReviewDataset);
        binding.itemListRv.setHasFixedSize(true);
        binding.itemListRv.setLayoutManager(new LinearLayoutManager(this));
        binding.itemListRv.setAdapter(adapter);
        deliveryReview.submitBtn.setOnClickListener(this::onClickedSubmitDeliveryReview);
    }

    private void onClickedSubmitDeliveryReview(View view) {
        if (deliveryByCustomer == null) {
            deliveryByCustomer = new OSRUDeliveryByCustomer();
            deliveryByCustomer.setBusiness(business.getBusinessRefId());
            deliveryByCustomer.setOrder(order.getOrderId());
            deliveryByCustomer.setCustomer(user.getUserId());
            deliveryByCustomer.setDelivery(delivery.getUserId());
        }

        float rating = deliveryReview.ratingBar.getRating();
        String msg = deliveryReview.reviewMsgTiet.getText().toString();

        if (rating <= 0) {
            // Minimum require 1 star
            OSMessage.showSToast(this, "Rating required!!");
            return;
        }

        if (deliveryByCustomer.getReviewId() != null && deliveryByCustomer.getMsg() != null && rating == deliveryByCustomer.getRating() && msg.equals(deliveryByCustomer.getMsg())) {
            // User didn't change any content, so no need to send request
            OSMessage.showSToast(this, "Review updated!!");
            return;
        }

        String reviewId = deliveryByCustomer.getReviewId() == null ? FirebaseFirestore.getInstance().collection(OSString.refReview).document().getId() : deliveryByCustomer.getReviewId();
        if (deliveryByCustomer.getReviewId() == null) {
            // 1* User is giving review for the first time
            // 2* Needs to update to the review object not to satisfy this "if" condition again
            // if the user tries to update the review content just after they created (while the user is in the same page)
            deliveryByCustomer.setReviewId(reviewId);
            deliveryByCustomer.setCreatedOn(new Timestamp(new Date()));
        }

        deliveryByCustomer.setMsg(msg);
        deliveryByCustomer.setRating((double) rating);
        deliveryByCustomer.setModifiedOn(new Timestamp(new Date()));

        loadingDialog.show();
        FirebaseFirestore.getInstance().collection(OSString.refReview).document(reviewId).set(deliveryByCustomer, SetOptions.merge()).addOnSuccessListener(result -> {
            OSMessage.showSToast(this, "Review added");
            loadingDialog.dismiss();
        }).addOnFailureListener(e -> {
            OSMessage.showSToast(this, "Failed to update review");
            loadingDialog.dismiss();
        });
    }
}