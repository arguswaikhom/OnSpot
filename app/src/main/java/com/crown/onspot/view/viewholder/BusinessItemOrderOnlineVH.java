package com.crown.onspot.view.viewholder;

import android.content.Context;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.crown.library.onspotlibrary.model.OSPrice;
import com.crown.library.onspotlibrary.model.cart.OSCart;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.callback.OSCartQuantityViewClickListener;
import com.crown.library.onspotlibrary.utils.emun.BusinessItemStatus;
import com.crown.library.onspotlibrary.views.OSCartQuantityView;
import com.crown.onspot.R;
import com.crown.onspot.databinding.LiBusinessItemOrderOnlineBinding;
import com.crown.onspot.view.ListItemAdapter;

import java.util.Locale;

public class BusinessItemOrderOnlineVH extends RecyclerView.ViewHolder {
    private OSCart item;
    private Context context;
    private ListItemAdapter adapter;
    private LiBusinessItemOrderOnlineBinding binding;
    private OSCartQuantityViewClickListener quantityChangeListener;

    public BusinessItemOrderOnlineVH(ListItemAdapter adapter, @NonNull View itemView) {
        super(itemView);
        this.adapter = adapter;
        this.context = itemView.getContext();
        this.binding = LiBusinessItemOrderOnlineBinding.bind(itemView);
        this.quantityChangeListener = (OSCartQuantityViewClickListener) context;

        binding.descriptionIv.setOnClickListener(this::onClickedInfo);
        binding.quantityQv.setOnQuantityChangeListener(this::onClickedQuantityChange);
    }

    public void bind(OSCart item) {
        this.item = item;

        binding.nameTv.setText(item.getItemName());
        binding.categoryTv.setText(item.getCategory());
        binding.quantityQv.setQuantity((int) item.getQuantity());
        Glide.with(context).load(item.getImageUrls() == null || item.getImageUrls().isEmpty() ? null : item.getImageUrls().get(0)).into(binding.imageIv);

        OSPrice price = item.getPrice();
        int finalPrice = (int) BusinessItemUtils.getFinalPrice(price);
        int priceWithTax = (int) (price.getPrice() + BusinessItemUtils.getTaxAmount(price));
        if (priceWithTax != finalPrice) {
            binding.actualPriceTv.setEnabled(false);
            binding.finalPriceTv.setVisibility(View.VISIBLE);
            binding.actualPriceTv.setText(Html.fromHtml("<del>₹ " + priceWithTax + "</del>"));
            binding.finalPriceTv.setText(String.format(Locale.ENGLISH, "₹ %s", finalPrice));
        } else {
            binding.actualPriceTv.setEnabled(true);
            binding.finalPriceTv.setVisibility(View.INVISIBLE);
            binding.actualPriceTv.setText(String.format(Locale.ENGLISH, "₹ %s", priceWithTax));
        }

        BusinessItemStatus status = item.getStatus() == null ? BusinessItemStatus.AVAILABLE : item.getStatus();
        switch (status) {
            case AVAILABLE:
                binding.quantityQv.setEnable(true);
                binding.availabilityTv.setText(status.getName());
                binding.availabilityTv.setBackgroundColor(context.getColor(R.color.item_status_available));
                break;
            case NOT_AVAILABLE:
                binding.quantityQv.setEnable(false);
                binding.availabilityTv.setText(status.getName());
                binding.availabilityTv.setBackgroundColor(context.getColor(R.color.item_status_not_available));
                break;
            case OUT_OF_STOCK:
                binding.quantityQv.setEnable(false);
                binding.availabilityTv.setText(status.getName());
                binding.availabilityTv.setBackgroundColor(context.getColor(R.color.item_status_out_of_stock));
                break;
        }
    }

    private void onClickedQuantityChange(View view, int mode) {
        if (mode == OSCartQuantityView.OnQuantityChangeListener.ADD) {
            item.setQuantity(item.getQuantity() + 1);
            quantityChangeListener.onQuantityChange(view, OSCartQuantityViewClickListener.ADD, getAdapterPosition());
            adapter.notifyDataSetChanged();
        } else if (mode == OSCartQuantityView.OnQuantityChangeListener.SUB) {
            if (item.getQuantity() <= 0) return;
            item.setQuantity(item.getQuantity() - 1);
            quantityChangeListener.onQuantityChange(view, OSCartQuantityViewClickListener.SUB, getAdapterPosition());
            adapter.notifyDataSetChanged();
        }
    }

    private void onClickedInfo(View view) {
        String message = item.getDescription() == null || item.getDescription().equals("") ? "No item description" : item.getDescription();
        new AlertDialog.Builder(context).setTitle(item.getItemName()).setMessage(message).setPositiveButton("OK", null).create().show();
    }
}
