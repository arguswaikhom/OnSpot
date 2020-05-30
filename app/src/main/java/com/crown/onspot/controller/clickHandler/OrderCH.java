package com.crown.onspot.controller.clickHandler;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.crown.onspot.R;
import com.crown.onspot.controller.ViewAnimation;
import com.crown.onspot.model.Order;
import com.crown.onspot.model.StatusRecord;
import com.crown.onspot.view.ViewHolder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OrderCH implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    private Activity activity;
    private ViewHolder.OrderVH holder;
    private Order order;

    public OrderCH(Activity activity, ViewHolder.OrderVH holder, Order order) {
        this.activity = activity;
        this.holder = holder;
        this.order = order;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cv_moi_order_item: {
                rotateReflect(holder.toggleInfoIBtn, holder.orderTableTL);
                break;
            }
            case R.id.btn_moi_cancel: {
                onClickedCancelOrder();
                break;
            }
            case R.id.ibtn_moi_more_overflow: {
                onClickedMoreOverflow(v);
                break;
            }
        }

    }

    private void onClickedCancelOrder() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", StatusRecord.Status.CANCELED);
        map.put("statusRecord", FieldValue.arrayUnion(new StatusRecord(StatusRecord.Status.CANCELED, new Timestamp(new Date()))));
        FirebaseFirestore.getInstance().collection(activity.getString(R.string.ref_order))
                .document(order.getOrderId()).update(map)
                .addOnFailureListener(error -> {
                    Toast.makeText(activity, "Update failed!!", Toast.LENGTH_SHORT).show();
                });

    }

    private void onClickedMoreOverflow(View v) {
        PopupMenu popupMenu = new PopupMenu(activity, v);
        popupMenu.inflate(R.menu.order_more_option);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    public void rotateReflect(View clickedView, View reflectView) {
        if (performRotation(clickedView)) {
            ViewAnimation.expand(reflectView, () -> {
                // onFinish
            });
        } else {
            ViewAnimation.collapse(reflectView);
        }
    }

    private boolean performRotation(View view) {
        if (view.getRotation() == 0.0f) {
            view.animate().setDuration(200).rotation(180.0f);
            return true;
        }
        view.animate().setDuration(200).rotation(0.0f);
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_omo_call_business) {
            onClickedCallBusiness();
            return true;
        }
        return false;
    }

    private void onClickedCallBusiness() {
        FirebaseFirestore.getInstance().collection(activity.getString(R.string.ref_business))
                .document(order.getBusinessRefId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (activity == null) return;
                    String phoneNumber = (String) documentSnapshot.get("mobileNumber");
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                    activity.startActivity(intent);
                });
    }
}
