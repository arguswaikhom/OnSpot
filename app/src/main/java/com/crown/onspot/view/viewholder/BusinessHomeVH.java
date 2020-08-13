package com.crown.onspot.view.viewholder;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.crown.library.onspotlibrary.model.business.BusinessV4;
import com.crown.onspot.databinding.LiBusinessHomeBinding;
import com.crown.onspot.page.OrderOnlineActivity;
import com.google.gson.Gson;

public class BusinessHomeVH extends RecyclerView.ViewHolder {
    private Context context;
    private BusinessV4 business;
    private LiBusinessHomeBinding binding;

    public BusinessHomeVH(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        this.binding = LiBusinessHomeBinding.bind(itemView);
        binding.orderBtn.setOnClickListener(this::onClickedOrder);
    }

    private void onClickedOrder(View view) {
        Intent intent = new Intent(context, OrderOnlineActivity.class);
        intent.putExtra(OrderOnlineActivity.BUSINESS, new Gson().toJson(business));
        view.getContext().startActivity(intent);
    }

    public void bind(BusinessV4 business) {
        this.business = business;

        Glide.with(context).load(business.getImageUrls() == null || business.getImageUrls().size() == 0 ? null : business.getImageUrls().get(0)).into(binding.imageIv);
        binding.nameTv.setText(business.getDisplayName());
        binding.categoryTv.setText(business.getBusinessType());
        binding.addressTv.setText(business.getLocation().getAddressLine());
    }
}