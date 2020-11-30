package com.crown.onspot.view.viewholder;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.order.OSOldOrder;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.onspot.R;
import com.crown.onspot.databinding.LiOldOrderBinding;
import com.crown.onspot.page.OrderDetailsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;

public class OldOrderVH extends RecyclerView.ViewHolder {

    private final Context context;
    private final LiOldOrderBinding binding;
    private OSOldOrder order;

    public OldOrderVH(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        this.binding = LiOldOrderBinding.bind(itemView);
        binding.navDetailsIbtn.setOnClickListener(this::onClickedNavDetails);
    }

    private void onClickedNavDetails(View view) {
        Intent intent = new Intent(context, OrderDetailsActivity.class);
        intent.putExtra(OrderDetailsActivity.ORDER, new Gson().toJson(order));
        context.startActivity(intent);
    }

    public void bind(OSOldOrder order) {
        this.order = order;

        int color = order.getStatus().getColor(context);
        if (color != 0) {
            binding.statusBtn.setTextColor(color);
            ((MaterialButton) binding.statusBtn).setStrokeColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_enabled}}, new int[]{color}));
        }

        binding.statusBtn.setText(order.getStatus().getStatus());
        binding.businessNameTv.setText(Html.fromHtml("<b>" + order.getBusiness().getDisplayName() + "</b>"));
        binding.orderTimeTv.setText(OSTimeUtils.getTimeAgo(order.getOrderedAt().getSeconds()));
        StringBuilder builder = new StringBuilder();
        List<OSCartLite> cart = order.getItems();
        for (int i = 0; i < cart.size(); i++) {
            builder.append("<b>x").append(cart.get(i).getQuantity()).append("</b> ").append(cart.get(i).getItemName());
            if (i != cart.size() - 1) {
                builder.append(", ");
            }
        }
        binding.orderItemsTv.setText(Html.fromHtml(builder.toString()));
        binding.finalPriceTv.setText(Html.fromHtml(String.format(Locale.ENGLISH, "Total price: %s %d", context.getString(R.string.inr), order.getFinalPrice())));
    }
}
