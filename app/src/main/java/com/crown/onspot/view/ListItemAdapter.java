package com.crown.onspot.view;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.crown.onspot.R;
import com.crown.onspot.controller.clickHandler.OrderCH;
import com.crown.onspot.controller.clickHandler.ShopCH;
import com.crown.onspot.controller.clickHandler.ShopItemCH;
import com.crown.onspot.model.Header;
import com.crown.onspot.model.Order;
import com.crown.onspot.model.OrderItem;
import com.crown.onspot.model.Shop;
import com.crown.onspot.model.StatusRecord;
import com.crown.onspot.utils.ListItemKey;
import com.crown.onspot.utils.TimeUtils;
import com.crown.onspot.utils.abstracts.ListItem;
import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Locale;

public class ListItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ListItemAdapter.class.getName();
    private Context mContext;
    private List<ListItem> mDataset;

    public ListItemAdapter(Context context, List<ListItem> dataset) {
        this.mContext = context;
        this.mDataset = dataset;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView;
        switch (viewType) {
            case ListItemKey.HEADER: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.section_header, parent, false);
                return new ViewHolder.HeaderVH(rootView);
            }
            case ListItemKey.MENU_ITEM: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_business_item, parent, false);
                return new ViewHolder.BusinessItemVH(rootView);
            }
            case ListItemKey.BUSINESS: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_shop, parent, false);
                return new ViewHolder.BusinessVH(rootView);
            }
            case ListItemKey.ORDER:
            default: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_my_order, parent, false);
                return new ViewHolder.OrderVH(rootView);
            }
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ListItemKey.ORDER: {
                setUpOrder((ViewHolder.OrderVH) holder, (Order) mDataset.get(position));
                break;
            }
            case ListItemKey.MENU_ITEM: {
                setUpBusinessItemView((OrderItem) mDataset.get(position), (ViewHolder.BusinessItemVH) holder);
                break;
            }
            case ListItemKey.BUSINESS: {
                setUpBusinessView((Shop) mDataset.get(position), (ViewHolder.BusinessVH) holder);
                break;
            }
            case ListItemKey.HEADER: {
                Header header = (Header) mDataset.get(position);
                ViewHolder.HeaderVH vh = (ViewHolder.HeaderVH) holder;

                vh.headerTV.setText(header.getHeader());
                break;
            }
        }
    }

    private void setUpBusinessView(Shop shop, ViewHolder.BusinessVH holder) {
        ShopCH clickListener = new ShopCH(mContext, this, shop);

        holder.orderBtn.setOnClickListener(clickListener);
        Glide.with(this.mContext).load(shop.getImageUrl()).apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(32))).into(holder.shopImageIV);
        holder.titleTV.setText(shop.getDisplayName());
        holder.categoryTV.setText(shop.getBusinessType());
        holder.locationTV.setText(shop.getLocation().getAddressLine());
    }

    private void setUpBusinessItemView(OrderItem orderItem, ViewHolder.BusinessItemVH holder) {
        ShopItemCH clickHandler = new ShopItemCH(this, mContext, orderItem);

        long finalPrice = orderItem.getFinalPrice();
        Log.v(TAG, orderItem.getItemName() + " : " + orderItem.getPriceWithTax() + " : " + orderItem.getFinalPrice());
        if (finalPrice != orderItem.getPriceWithTax()) {
            String oldPrice = "<del>₹ " + orderItem.getPriceWithTax() + "</del>";
            holder.priceTV.setText(Html.fromHtml(oldPrice));
            holder.finalPriceTV.setVisibility(View.VISIBLE);
            holder.finalPriceTV.setText(String.format(Locale.ENGLISH, "₹ %d", finalPrice));
            holder.priceTV.setEnabled(false);
        } else {
            holder.priceTV.setEnabled(true);
            holder.finalPriceTV.setVisibility(View.INVISIBLE);
            holder.priceTV.setText(String.format(Locale.ENGLISH, "₹ %d", orderItem.getPriceWithTax()));
        }

        String status = orderItem.getStatus();
        if (status != null && !status.equals("AVAILABLE")) {
            holder.statusTV.setVisibility(View.VISIBLE);
            holder.statusTV.setText(status.replace("_", " "));
            holder.addQuantityIV.setEnabled(false);
            holder.subQuantityIV.setEnabled(false);
        } else {
            holder.statusTV.setVisibility(View.INVISIBLE);
            holder.addQuantityIV.setEnabled(true);
            holder.subQuantityIV.setEnabled(true);
        }

        holder.quantityTV.setText(String.valueOf(orderItem.getQuantity()));
        holder.categoryTV.setText(orderItem.getCategory());
        holder.addQuantityIV.setOnClickListener(clickHandler);
        holder.subQuantityIV.setOnClickListener(clickHandler);
        holder.descriptionInfoIV.setOnClickListener(clickHandler);
        holder.titleTV.setText(orderItem.getItemName());
        Glide.with(this.mContext).load(orderItem.getImageUrl()).into(holder.imageIV);
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).getItemType();
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    private void setUpOrder(ViewHolder.OrderVH holder, Order orderOld) {
        Timestamp orderedTime = orderOld.getStatusRecord().get(0).getTimestamp();
        OrderCH handler = new OrderCH((Activity) mContext, holder, orderOld);
        holder.businessTitleTV.setText(orderOld.getBusinessDisplayName());
        holder.orderTimeTV.setText(String.format("Ordered at %s", TimeUtils.getTime(orderedTime.getSeconds())));
        holder.orderDataTV.setText(TimeUtils.getDay(orderedTime.getSeconds()));
        holder.priceTV.setText(String.format("Total price: ₹ %s", orderOld.getFinalPrice()));
        holder.statusTv.setText(orderOld.getStatus().toString().replace("_", " "));
        holder.orderItemCV.setOnClickListener(handler);
        handler.rotateReflect(holder.toggleInfoIBtn, holder.orderTableTL);
        holder.moreOverflowIBtn.setOnClickListener(handler);

        if (orderOld.getStatus() != StatusRecord.Status.ORDERED) {
            holder.cancelBtn.setEnabled(false);
        } else {
            holder.cancelBtn.setEnabled(true);
            holder.cancelBtn.setOnClickListener(handler);
        }

        holder.orderTableTL.removeAllViews();
        for (OrderItem item : orderOld.getItems()) {
            long price = item.getQuantity() * item.getFinalPrice();

            LinearLayout root = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.order__order_item, null);
            ((TextView) root.findViewById(R.id.tv_ooi_quantity)).setText(String.format(Locale.ENGLISH, "x%d", item.getQuantity()));
            ((TextView) root.findViewById(R.id.tv_ooi_item_name)).setText(item.getItemName());
            ((TextView) root.findViewById(R.id.tv_ooi_price)).setText(String.format(Locale.ENGLISH, "₹ %d", price));
            holder.orderTableTL.addView(root);
        }
    }
}
