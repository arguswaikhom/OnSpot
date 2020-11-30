package com.crown.onspot.view.viewholder;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.crown.library.onspotlibrary.model.explore.OSExplore;
import com.crown.library.onspotlibrary.page.BusinessActivity;
import com.crown.library.onspotlibrary.page.ProductActivity;
import com.crown.onspot.R;
import com.crown.onspot.databinding.LiExploreItemBinding;

public class OSExploreVH extends RecyclerView.ViewHolder {
    private final Context context;
    private final LiExploreItemBinding binding;

    public OSExploreVH(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        binding = LiExploreItemBinding.bind(itemView);
    }

    public void bind(OSExplore explore) {
        Glide.with(context).load(explore.getImageUrl()).into(binding.imageIv);
        switch (explore.getOsClass()) {
            case PRODUCT: {
                binding.contentIndicatorIv.setImageResource(R.drawable.ic_round_shop_24);
                binding.getRoot().setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProductActivity.class);
                    intent.putExtra(ProductActivity.PRODUCT_ID, explore.getId());
                    context.startActivity(intent);
                });
                break;
            }
            case BUSINESS: {
                binding.contentIndicatorIv.setImageResource(R.drawable.ic_round_business_24);
                binding.getRoot().setOnClickListener(v -> {
                    Intent intent = new Intent(context, BusinessActivity.class);
                    intent.putExtra(BusinessActivity.BUSINESS_ID, explore.getId());
                    context.startActivity(intent);
                });
                break;
            }
        }
    }
}
