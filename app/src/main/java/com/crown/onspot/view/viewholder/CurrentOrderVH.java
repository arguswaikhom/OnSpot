package com.crown.onspot.view.viewholder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.controller.OSGlideLoader;
import com.crown.library.onspotlibrary.model.OSPrice;
import com.crown.library.onspotlibrary.model.OrderStatusRecord;
import com.crown.library.onspotlibrary.model.business.BusinessOrder;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.user.UserOrder;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.OSContactReacher;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.library.onspotlibrary.utils.callback.OnStringResponse;
import com.crown.library.onspotlibrary.utils.emun.OrderStatus;
import com.crown.onspot.R;
import com.crown.onspot.databinding.LiCurrentOrderBinding;
import com.crown.onspot.page.OrderDetailsActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CurrentOrderVH extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {

    private final Context context;
    private final LiCurrentOrderBinding binding;
    private OSOrder order;

    public CurrentOrderVH(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        this.binding = LiCurrentOrderBinding.bind(itemView);

        binding.moreIbtn.setOnClickListener(this::onClickedMore);
        binding.negativeBtn.setOnClickListener(this::onClickedNegative);
    }

    void onClickedMore(View view) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.order_more_option);
        if (order.getDelivery() == null)
            popupMenu.getMenu().findItem(R.id.action_omo_call_delivery).setVisible(false);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_omo_call_business:
                OSContactReacher.getBusinessMobileNumber(context, order.getBusiness().getBusinessRefId(), value -> {
                    if (((Activity) context).isFinishing()) return;
                    context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + value)));
                }, (e, msg) -> {
                    if (((Activity) context).isFinishing()) return;
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                });
                return true;
            case R.id.action_omo_call_delivery:
                OSContactReacher.getUserMobileNumber(context, order.getDelivery().getUserId(), (OnStringResponse) value -> {
                    if (((Activity) context).isFinishing()) return;
                    context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + value)));
                }, (e, msg) -> {
                    if (((Activity) context).isFinishing()) return;
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                });
                return true;
            case R.id.action_omo_more_details:
                Intent intent = new Intent(context, OrderDetailsActivity.class);
                intent.putExtra(OrderDetailsActivity.ORDER, new Gson().toJson(order));
                context.startActivity(intent);
                return true;
        }
        return false;
    }

    void onClickedNegative(View view) {
        new AlertDialog.Builder(context).setTitle("Cancel Order").setMessage(Html.fromHtml("Are you sure you want to cancel this order from <b>" + order.getBusiness().getDisplayName() + "</b>?")).setPositiveButton("Yes", (dialog, which) -> {
            Map<String, Object> map = new HashMap<>();
            view.setEnabled(false);
            map.put("status", OrderStatus.CANCELED);
            map.put("statusRecord", FieldValue.arrayUnion(new OrderStatusRecord(OrderStatus.CANCELED, new Timestamp(new Date()))));
            update(map);
        }).setNegativeButton("No", (dialog, which) -> dialog.cancel()).show();

    }

    private void update(Map<String, Object> param) {
        FirebaseFirestore.getInstance().collection(context.getString(R.string.ref_order))
                .document(order.getOrderId()).update(param)
                .addOnFailureListener(error -> Toast.makeText(context, "Update failed!!", Toast.LENGTH_SHORT).show());
    }

    public void bind(OSOrder order) {
        this.order = order;
        UserOrder delivery = order.getDelivery();
        BusinessOrder business = order.getBusiness();

        if (!binding.negativeBtn.isEnabled()) binding.negativeBtn.setEnabled(true);
        if (order.getStatus() == OrderStatus.ORDERED) {
            binding.negativeBtn.setVisibility(View.VISIBLE);
        } else {
            binding.negativeBtn.setVisibility(View.GONE);
        }

        int totalItems = 0;
        binding.orderItemOiv.clear();
        for (OSCartLite cart : order.getItems()) {
            int q = (int) (long) cart.getQuantity();
            OSPrice price = cart.getPrice();
            int itemFinalPrice = (int) BusinessItemUtils.getFinalPrice(price);
            totalItems += q;
            binding.orderItemOiv.addChild(q, cart.getItemName(), itemFinalPrice * q);
        }

        int color = order.getStatus().getColor(context);
        if (color != 0) {
            binding.statusTv.setBackgroundColor(order.getStatus().getColor(context));
            binding.negativeBtn.setTextColor(color);
        }

        binding.statusTv.setText(order.getStatus().getStatus());
        // todo: implement business image loader
        // OSGlideLoader.loadUserProfileImage(context, customer.getUserId(), binding.customerImageIv);
        binding.businessNameTv.setText(Html.fromHtml("<b>" + business.getDisplayName() + "</>"));
        binding.orderTimeTv.setText(Html.fromHtml(OSTimeUtils.getTime(order.getOrderedAt().getSeconds()) + ", " + OSTimeUtils.getDay(order.getOrderedAt().getSeconds())));
        binding.itemCountTv.setText(String.format(Locale.ENGLISH, "%d items", totalItems));
        binding.finalPriceTv.setText(String.format("%s %s", context.getString(R.string.inr), order.getFinalPrice()));

        if (delivery != null) {
            if (binding.deliveryLl.getVisibility() == View.GONE) {
                binding.deliveryLl.setVisibility(View.VISIBLE);
            }

            OSGlideLoader.loadUserProfileImage(context, delivery.getUserId(), binding.deliveryImageTv);
            binding.deliveryMsgTv.setText(Html.fromHtml("<b>" + delivery.getDisplayName() + "</b> accepted to deliver this order"));
        } else {
            binding.deliveryLl.setVisibility(View.GONE);
        }
    }
}
