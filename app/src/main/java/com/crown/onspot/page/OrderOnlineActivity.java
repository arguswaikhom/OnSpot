package com.crown.onspot.page;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.business.BusinessV4;
import com.crown.library.onspotlibrary.model.cart.OSCart;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.callback.OSCartQuantityViewClickListener;
import com.crown.library.onspotlibrary.utils.emun.BusinessItemStatus;
import com.crown.onspot.R;
import com.crown.onspot.databinding.ActivityOrderOnlineBinding;
import com.crown.onspot.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class OrderOnlineActivity extends AppCompatActivity implements OSCartQuantityViewClickListener {

    public static final String BUSINESS = "BUSINESS";
    private final String TAG = OrderOnlineActivity.class.getName();
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private BusinessV4 business;
    private ActivityOrderOnlineBinding binding;
    private ListenerRegistration mBusinessItemChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderOnlineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Order online");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initUi();

        String json = getIntent().getStringExtra(BUSINESS);
        if (json != null && !json.isEmpty()) {
            business = new Gson().fromJson(json, BusinessV4.class);
        }
    }

    private void initUi() {
        binding.orderBtn.setOnClickListener(this::onClickedOrder);

        binding.itemListRv.setHasFixedSize(true);
        binding.itemListRv.setLayoutManager(new LinearLayoutManager(this));
        mDataset = new ArrayList<>();
        mAdapter = new ListItemAdapter(mDataset);
        binding.itemListRv.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (business != null) {
            mBusinessItemChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.sub_ref_item))
                    .whereEqualTo(getString(R.string.field_business_ref_id), business.getBusinessRefId().trim())
                    .addSnapshotListener(this::onEvent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBusinessItemChangeListener.remove();
    }

    void onClickedOrder(View view) {
        ArrayList<OSCart> orderList = getSelectedOrder();
        if (orderList.isEmpty()) {
            Toast.makeText(this, "Select some item", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, OrderSummaryActivity.class);
            intent.putExtra(OrderSummaryActivity.CART, new Gson().toJson(orderList));
            intent.putExtra(OrderSummaryActivity.BUSINESS, new Gson().toJson(business));
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
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + business.getMobileNumber()));
        startActivity(intent);
    }

    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
        if (snapshots != null && !snapshots.isEmpty()) {
            updateItemList(snapshots.getDocuments());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void updateItemList(List<DocumentSnapshot> documents) {
        List<OSCart> selectedItems = getSelectedOrder();
        boolean hasStatusChanged = false;
        mDataset.clear();
        for (DocumentSnapshot doc : documents) {
            try {
                OSCart item = doc.toObject(OSCart.class);
                item.setItemId(doc.getId());
                if (doc.get(getString(R.string.field_archived)) == null || !((Boolean) doc.get(getString(R.string.field_archived)))) {
                    for (OSCart sItem : selectedItems) {
                        if (sItem.getItemId().equals(item.getItemId())) {
                            if (item.getStatus() == null || item.getStatus() == BusinessItemStatus.AVAILABLE) {
                                item.setQuantity(sItem.getQuantity());
                            } else {
                                hasStatusChanged = true;
                            }
                        }
                    }
                    mDataset.add(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // todo: implement unsupported content
            }
        }
        Collections.sort(mDataset, ((o1, o2) -> ((OSCart) o1).getItemName().compareToIgnoreCase(((OSCart) o2).getItemName())));
        updateTotalSummary();
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

    private ArrayList<OSCart> getSelectedOrder() {
        ArrayList<OSCart> listItems = new ArrayList<>();
        for (ListItem item : mDataset) {
            if (((OSCart) item).getQuantity() > 0) {
                listItems.add((OSCart) item);
            }
        }
        Log.v(TAG, "Length: " + mDataset.size());
        return listItems;
    }

    private void updateTotalSummary() {
        int totalItem = 0;
        long totalAmount = 0;
        for (OSCart item : getSelectedOrder()) {
            totalItem += item.getQuantity();
            totalAmount += BusinessItemUtils.getFinalPrice(item.getPrice()) * (item.getQuantity());
        }
        binding.totalItemTv.setText(String.format(Locale.ENGLISH, "Total items: %d", totalItem));
        binding.totalAmountTv.setText(String.format(Locale.ENGLISH, "Total amount: %d", totalAmount));
    }

    @Override
    public void onQuantityChange(View view, int mode, int position) {
        if (mode == OSCartQuantityViewClickListener.ADD || mode == OSCartQuantityViewClickListener.SUB) {
            updateTotalSummary();
        }
    }
}
