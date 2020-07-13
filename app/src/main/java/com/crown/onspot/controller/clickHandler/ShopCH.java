package com.crown.onspot.controller.clickHandler;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.crown.onspot.R;
import com.crown.onspot.model.Shop;
import com.crown.onspot.page.OrderOnlineActivity;
import com.crown.onspot.view.ListItemAdapter;
import com.google.gson.Gson;

public class ShopCH implements View.OnClickListener {

    private ListItemAdapter mAdapter;
    private Shop item;
    private Context context;

    public ShopCH(Context context, ListItemAdapter adapter, Shop item) {
        this.mAdapter = adapter;
        this.item = item;
        this.context = context;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_as_order) {
            Intent intent = new Intent(context, OrderOnlineActivity.class);
            intent.putExtra(OrderOnlineActivity.KEY_SHOP, new Gson().toJson(item));
            view.getContext().startActivity(intent);
        }
    }
}
