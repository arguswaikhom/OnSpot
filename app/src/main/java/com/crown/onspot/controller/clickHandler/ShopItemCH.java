package com.crown.onspot.controller.clickHandler;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.crown.onspot.R;
import com.crown.onspot.model.OrderItem;
import com.crown.onspot.utils.abstracts.OnChangeShopItemCart;
import com.crown.onspot.view.ListItemAdapter;

public class ShopItemCH implements View.OnClickListener {
    private final String TAG = ShopItemCH.class.getName();

    private ListItemAdapter mAdapter;
    private OrderItem item;
    private Context mContext;
    private OnChangeShopItemCart mOnChangeCart;
    private int mPosition;

    public ShopItemCH(ListItemAdapter adapter, Context context, OrderItem item) {
        this.mAdapter = adapter;
        this.mContext = context;
        this.mOnChangeCart = (OnChangeShopItemCart) mContext;
        this.item = item;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_libi_description_info: {
                onClickedInfo();
                break;
            }
            case R.id.iv_libi_add_quantity: {
                onClickAdd();
                break;
            }
            case R.id.iv_libi_sub_quantity: {
                onClickSub();
                break;
            }
        }
    }

    private void onClickedInfo() {
        String message = item.getDescription() == null || item.getDescription().equals("") ? "No item description" : item.getDescription();
        new AlertDialog.Builder(mContext).setTitle(item.getItemName()).setMessage(message).setPositiveButton("OK", null).create().show();
    }

    private void onClickAdd() {
        item.setQuantity(item.getQuantity() + 1);
        mOnChangeCart.onChangeShopItemCart(mPosition, OnChangeShopItemCart.ADD);
        mAdapter.notifyDataSetChanged();
    }

    private void onClickSub() {
        if (item.getQuantity() <= 0) return;
        item.setQuantity(item.getQuantity() - 1);
        mOnChangeCart.onChangeShopItemCart(mPosition, OnChangeShopItemCart.SUB);
        mAdapter.notifyDataSetChanged();
    }
}
