package com.crown.onspot.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crown.onspot.R;
import com.crown.onspot.view.viewholder.BusinessImageOrderDetailsVH;
import com.smarteist.autoimageslider.SliderViewAdapter;

import java.util.List;

@Deprecated
public class ImageSliderAdapter extends SliderViewAdapter<BusinessImageOrderDetailsVH> {

    private final List<String> imageUrls;

    public ImageSliderAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @Override
    public BusinessImageOrderDetailsVH onCreateViewHolder(ViewGroup parent) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_business_image_order_details, null);
        return new BusinessImageOrderDetailsVH(inflate);
    }


    @Override
    public void onBindViewHolder(BusinessImageOrderDetailsVH viewHolder, int position) {
        viewHolder.bind(imageUrls.get(position));
    }


    @Override
    public int getCount() {
        return imageUrls.size();
    }
}
