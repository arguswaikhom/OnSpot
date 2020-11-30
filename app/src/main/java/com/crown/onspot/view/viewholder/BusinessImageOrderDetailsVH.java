package com.crown.onspot.view.viewholder;

import android.content.Context;
import android.view.View;

import com.bumptech.glide.Glide;
import com.crown.onspot.databinding.LiBusinessImageOrderDetailsBinding;
import com.smarteist.autoimageslider.SliderViewAdapter;

@Deprecated
public class BusinessImageOrderDetailsVH extends SliderViewAdapter.ViewHolder {
    private final Context context;
    private final LiBusinessImageOrderDetailsBinding binding;

    public BusinessImageOrderDetailsVH(View itemView) {
        super(itemView);
        context = itemView.getContext();
        binding = LiBusinessImageOrderDetailsBinding.bind(itemView);
    }

    public void bind(String url) {
        Glide.with(context).load(url).into(binding.imageIv);
    }
}
