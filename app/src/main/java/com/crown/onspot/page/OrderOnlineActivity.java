package com.crown.onspot.page;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.onspot.R;
import com.crown.onspot.model.OrderItem;
import com.crown.onspot.model.Shop;
import com.crown.onspot.utils.abstracts.HidingScrollListener;
import com.crown.onspot.utils.abstracts.ListItem;
import com.crown.onspot.utils.abstracts.OnChangeShopItemCart;
import com.crown.onspot.view.ListItemAdapter;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class OrderOnlineActivity extends AppCompatActivity implements OnChangeShopItemCart,
        EventListener<QuerySnapshot> {

    public static final String KEY_SHOP = "SHOP";
    private final String TAG = OrderOnlineActivity.class.getName();
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private MaterialCardView mOrderCartCV;
    private TextView mTotalItem;
    private TextView mTotalPrice;
    private Shop mShop;
    private ListenerRegistration mBusinessItemChangeListener;


    private HidingScrollListener mScrollListener = new HidingScrollListener() {
        @Override
        public void onHide() {
            // mToolbar.animate().translationY(-mToolbar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mOrderCartCV.getLayoutParams();
            mOrderCartCV.animate().translationY(mOrderCartCV.getHeight() + lp.bottomMargin).setInterpolator(new AccelerateInterpolator(2)).start();
        }

        @Override
        public void onShow() {
            mOrderCartCV.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            // mFabButton.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        ButterKnife.bind(this);

        getSupportActionBar().setTitle("Order online");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setUpRecycler();
        setUpUi();

        String json = getIntent().getStringExtra(KEY_SHOP);
        if (json != null && !json.isEmpty()) {
            mShop = new Gson().fromJson(json, Shop.class);
            Log.v("TAG", "Business: " + mShop);
        }
    }

    private void setUpUi() {
        mOrderCartCV = findViewById(R.id.cv_as_cart_bar);
        mTotalItem = findViewById(R.id.tv_as_total_item);
        mTotalPrice = findViewById(R.id.tv_as_total_amount);
    }

    private void setUpRecycler() {
        RecyclerView mRecyclerView = findViewById(R.id.rv_rvl_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mDataset = new ArrayList<>();
        mAdapter = new ListItemAdapter(this, mDataset);
        mRecyclerView.setAdapter(mAdapter);
        // mRecyclerView.setOnScrollListener(mScrollListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mShop != null) {
            mBusinessItemChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.sub_ref_item))
                    .whereEqualTo("businessRefId", mShop.getBusinessRefId().trim())
                    .addSnapshotListener(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBusinessItemChangeListener.remove();
    }

    @OnClick(R.id.btn_as_order)
    void onClickedOrder() {
        ArrayList<OrderItem> orderList = getSelectedOrder();
        if (orderList.isEmpty()) {
            Toast.makeText(this, "Select some item", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, OrderSummaryActivity.class);
            intent.putExtra(OrderSummaryActivity.KEY_ORDER, new Gson().toJson(orderList));
            intent.putExtra(OrderSummaryActivity.KEY_SHOP, new Gson().toJson(mShop));
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_order_online_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.nav_oooa_call_business: {
                onClickedCallBusiness();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void onClickedCallBusiness() {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mShop.getMobileNumber()));
        startActivity(intent);
    }

    @Override
    public void onChangeShopItemCart(int position, int mode) {
        if (mode == OnChangeShopItemCart.ADD || mode == OnChangeShopItemCart.SUB) {
            updateTotalSummary();
        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
        if (snapshots != null && !snapshots.isEmpty()) {
            updateItemList(snapshots.getDocuments());
        }
    }

    private void updateItemList(List<DocumentSnapshot> documents) {
        List<OrderItem> selectedItems = getSelectedOrder();
        boolean hasStatusChanged = false;
        mDataset.clear();
        for (DocumentSnapshot doc : documents) {
            if (doc != null && doc.exists()) {
                OrderItem item = doc.toObject(OrderItem.class);
                if (item == null) continue;
                item.setItemId(doc.getId());
                if (doc.get("isDeleted") == null || !((Boolean) doc.get("isDeleted"))) {
                    for (OrderItem sItem : selectedItems) {
                        if (sItem.getItemId().equals(item.getItemId())) {
                            if (item.getStatus() == null || item.getStatus().equalsIgnoreCase("available")) {
                                item.setQuantity(sItem.getQuantity());
                            } else {
                                hasStatusChanged = true;
                            }
                        }
                    }
                    mDataset.add(item);
                }
            }
        }
        Collections.sort(mDataset, ((o1, o2) -> ((OrderItem) o1).getItemName().compareToIgnoreCase(((OrderItem) o2).getItemName())));
        updateTotalSummary();
        Log.v(TAG, "Length: " + mDataset.size());
        mAdapter.notifyDataSetChanged();

        if (hasStatusChanged) {
            new AlertDialog.Builder(this)
                    .setTitle("Item status changed")
                    .setMessage("Some of the item you have selected is not currently available. Those items will remove from your cart.")
                    .setCancelable(false)
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                    }))
                    .show();
        }
    }

    private ArrayList<OrderItem> getSelectedOrder() {
        ArrayList<OrderItem> listItems = new ArrayList<>();
        for (ListItem item : mDataset) {
            if (((OrderItem) item).getQuantity() > 0) {
                listItems.add((OrderItem) item);
            }
        }
        Log.v(TAG, "Length: " + mDataset.size());
        return listItems;
    }

    private void updateTotalSummary() {
        int totalItem = 0;
        long totalAmount = 0;
        for (OrderItem item : getSelectedOrder()) {
            totalItem += item.getQuantity();
            totalAmount += item.getFinalPrice() * (item.getQuantity());
        }
        mTotalItem.setText(String.format(Locale.ENGLISH, "Total items: %d", totalItem));
        mTotalPrice.setText(String.format(Locale.ENGLISH, "Total amount: %d", totalAmount));
    }
}
