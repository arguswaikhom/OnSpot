package com.crown.onspot.view.viewholder;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.crown.library.onspotlibrary.controller.OSImageLoader;
import com.crown.library.onspotlibrary.model.businessItem.BusinessItemV2;
import com.crown.library.onspotlibrary.model.review.OSRItemByCustomer;
import com.crown.library.onspotlibrary.model.review.OSRUItemByCustomer;
import com.crown.library.onspotlibrary.utils.OSColorUtils;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSReviewClassUtils;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspot.databinding.LiReviewOrderItemBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;

public class ReviewOrderItemVH extends RecyclerView.ViewHolder {

    private final Context context;
    private final LiReviewOrderItemBinding binding;
    private OSRItemByCustomer review;

    public ReviewOrderItemVH(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        binding = LiReviewOrderItemBinding.bind(itemView);
    }

    public void bind(OSRItemByCustomer review) {
        this.review = review;
        BusinessItemV2 item = review.getItem();
        binding.displayNameTv.setText(item.getItemName());
        binding.submitBtn.setOnClickListener(this::onCLickedSubmitReview);
        binding.reviewMsgTiet.setText(review.getMsg() == null ? "" : review.getMsg());
        binding.rootMcv.setCardBackgroundColor(OSColorUtils.getDefinedRandomColor(context));
        binding.ratingBar.setRating(review.getRating() == null ? 0 : (float) (double) review.getRating());
        OSImageLoader.getBusinessItemImage(context.getApplicationContext(), item, url -> Glide.with(context).load(url).into(binding.imageIv));
    }

    private void onCLickedSubmitReview(View view) {
        float rating = binding.ratingBar.getRating();
        String msg = binding.reviewMsgTiet.getText().toString();

        if (rating <= 0) {
            // Minimum require 1 star
            OSMessage.showSToast(context, "Rating required!!");
            return;
        }

        if (review.getReviewId() != null && review.getMsg() != null && rating == review.getRating() && msg.equals(review.getMsg())) {
            // User didn't change any content, so no need to send request
            OSMessage.showSToast(context, "Review updated!!");
            return;
        }

        OSRUItemByCustomer uploadReview = OSReviewClassUtils.getUploadItemByCustomerReview(review);
        String reviewId = uploadReview.getReviewId() == null ? FirebaseFirestore.getInstance().collection(OSString.refReview).document().getId() : uploadReview.getReviewId();
        if (uploadReview.getReviewId() == null) {
            // 1* User is giving review for the first time
            // 2* Needs to update to the review object not to satisfy this "if" condition again
            // if the user tries to update the review content just after they created (while the user is in the same page)
            review.setReviewId(reviewId);
            review.setCreatedOn(new Timestamp(new Date()));

            uploadReview.setReviewId(reviewId);
            uploadReview.setCreatedOn(new Timestamp(new Date()));
        }

        uploadReview.setMsg(msg);
        uploadReview.setRating((double) rating);
        uploadReview.setModifiedOn(new Timestamp(new Date()));
        LoadingBounceDialog loadingDialog = new LoadingBounceDialog((Activity) context);

        loadingDialog.show();
        FirebaseFirestore.getInstance().collection(OSString.refReview).document(reviewId).set(uploadReview, SetOptions.merge()).addOnSuccessListener(result -> {
            OSMessage.showSToast(context, "Review added");
            loadingDialog.dismiss();
        }).addOnFailureListener(e -> {
            OSMessage.showSToast(context, "Failed to update review");
            loadingDialog.dismiss();
        });
    }
}
