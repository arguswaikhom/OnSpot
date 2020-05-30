package com.crown.onspot.page;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.onspot.R;
import com.crown.onspot.model.Order;
import com.crown.onspot.model.User;
import com.crown.onspot.utils.abstracts.ListItem;
import com.crown.onspot.utils.preference.PreferenceKey;
import com.crown.onspot.utils.preference.Preferences;
import com.crown.onspot.view.ListItemAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyOrderFragment extends Fragment implements EventListener<QuerySnapshot> {
    private static final String TAG = MyOrderFragment.class.getName();

    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private ListenerRegistration mMyOrderChangeListener;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_order, container, false);
        Toolbar toolbar = root.findViewById(R.id.tbar_fmo_tool_bar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("My Order");

        setUpRecycler(root);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        getContactData();
    }

    private void getContactData() {
        User user = Preferences.getInstance(getContext().getApplicationContext()).getObject(PreferenceKey.USER, User.class);
        mMyOrderChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.ref_order)).whereEqualTo("customerId", user.getUserId()).addSnapshotListener(this);
    }

    private void setUpRecycler(View root) {
        RecyclerView mRecyclerView = root.findViewById(R.id.rv_bnrvl_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mDataset = new ArrayList<>();
        mAdapter = new ListItemAdapter(getContext(), mDataset);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mMyOrderChangeListener.remove();
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
            updateItemList(queryDocumentSnapshots.getDocuments());
        }
    }

    private void updateItemList(List<DocumentSnapshot> documents) {
        mDataset.clear();
        for (DocumentSnapshot doc : documents) {
            if (doc.exists()) {
                Order order = doc.toObject(Order.class);
                order.setOrderId(doc.getId());
                mDataset.add(order);
            }
            Log.v(TAG, doc.getId());
        }
        Collections.sort(mDataset, ((o1, o2) -> {
            Timestamp o1T = ((Order) o1).getStatusRecord().get(0).getTimestamp();
            Timestamp o2T = ((Order) o2).getStatusRecord().get(0).getTimestamp();

            return (int) (o2T.getSeconds() - o1T.getSeconds());
        }));
        mAdapter.notifyDataSetChanged();
    }
}
