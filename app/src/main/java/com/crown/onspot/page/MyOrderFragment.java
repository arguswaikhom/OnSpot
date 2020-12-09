package com.crown.onspot.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.UnSupportedContent;
import com.crown.library.onspotlibrary.model.order.OSOldOrder;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.user.UserOS;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.utils.emun.OrderStatus;
import com.crown.onspot.BuildConfig;
import com.crown.onspot.R;
import com.crown.onspot.databinding.FragmentMyOrderBinding;
import com.crown.onspot.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyOrderFragment extends Fragment {
    private static final String TAG = MyOrderFragment.class.getName();

    private UserOS user;
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private ListenerRegistration mMyOrderChangeListener;
    private FragmentMyOrderBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyOrderBinding.inflate(inflater, container, false);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolBar);
        setUpRecycler();
        user = OSPreferences.getInstance(getContext().getApplicationContext()).getObject(OSPreferenceKey.USER, UserOS.class);
        mMyOrderChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.ref_order))
                .whereEqualTo(FieldPath.of(getString(R.string.field_customer), getString(R.string.field_user_id)), user.getUserId())
                .addSnapshotListener(this::onEvent);
        return binding.getRoot();
    }

    private void setUpRecycler() {
        binding.orderListRv.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        binding.orderListRv.setLayoutManager(mLayoutManager);
        mDataset = new ArrayList<>();
        mAdapter = new ListItemAdapter(mDataset);
        binding.orderListRv.setAdapter(mAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMyOrderChangeListener.remove();
    }

    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
        if (snapshots == null || snapshots.isEmpty()) {
            binding.infoIllv.show(R.drawable.ill_undraw_confirmation_re_b6q5, "No order to display");
        } else {
            updateItemList(snapshots.getDocuments());
        }
    }

    private void updateItemList(List<DocumentSnapshot> documents) {
        mDataset.clear();
        boolean hasUnsupportedContent = false;
        UnSupportedContent unSupportedContent = new UnSupportedContent(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, user.getUserId(), MyOrderFragment.class.getName());
        for (DocumentSnapshot doc : documents) {
            try {
                OrderStatus status = OrderStatus.valueOf(doc.getString(getString(R.string.field_status)));
                if (status == OrderStatus.CANCELED || status == OrderStatus.DELIVERED) {
                    OSOldOrder order = doc.toObject(OSOldOrder.class);
                    assert order != null;
                    order.setOrderId(doc.getId());
                    mDataset.add(order);
                } else {
                    OSOrder order = doc.toObject(OSOrder.class);
                    assert order != null;
                    order.setOrderId(doc.getId());
                    mDataset.add(order);
                }
            } catch (Exception e) {
                e.printStackTrace();
                hasUnsupportedContent = true;
                unSupportedContent.addItem(doc);
                unSupportedContent.addException(new Gson().toJson(e));
            }
        }

        Collections.sort(mDataset, (o1, o2) -> ((OSOrder) o2).getOrderedAt().compareTo(((OSOrder) o1).getOrderedAt()));

        if (hasUnsupportedContent) {
            List<ListItem> temp = new ArrayList<>(mDataset);
            mDataset.clear();
            mDataset.add(unSupportedContent);
            mDataset.addAll(temp);
        }
        mAdapter.notifyDataSetChanged();

        if (OSListUtils.isEmpty(mDataset)) {
            binding.infoIllv.show(R.drawable.ill_undraw_confirmation_re_b6q5, "No order to display");
        } else binding.infoIllv.hide();
    }
}
