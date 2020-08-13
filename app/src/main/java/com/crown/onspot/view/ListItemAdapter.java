package com.crown.onspot.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.UnSupportedContent;
import com.crown.library.onspotlibrary.model.business.BusinessV4;
import com.crown.library.onspotlibrary.model.cart.OSCart;
import com.crown.library.onspotlibrary.model.order.OSOldOrder;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.utils.ListItemType;
import com.crown.library.onspotlibrary.views.viewholder.UnSupportedContentVH;
import com.crown.onspot.R;
import com.crown.onspot.view.viewholder.BusinessHomeVH;
import com.crown.onspot.view.viewholder.BusinessItemOrderOnlineVH;
import com.crown.onspot.view.viewholder.CurrentOrderVH;
import com.crown.onspot.view.viewholder.OldOrderVH;

import java.util.List;

public class ListItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ListItemAdapter.class.getName();
    private List<ListItem> mDataset;

    public ListItemAdapter(List<ListItem> dataset) {
        this.mDataset = dataset;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView;
        switch (viewType) {
            case ListItemType.OS_ORDER: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_current_order, parent, false);
                return new CurrentOrderVH(rootView);
            }
            case ListItemType.OS_OLD_ORDER: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_old_order, parent, false);
                return new OldOrderVH(rootView);
            }
            case ListItemType.CART: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_business_item_order_online, parent, false);
                return new BusinessItemOrderOnlineVH(this, rootView);
            }
            case ListItemType.BUSINESS_V4: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_business_home, parent, false);
                return new BusinessHomeVH(rootView);
            }
            case ListItemType.UNSUPPORTED_CONTENT:
            default: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_unsupported_content, parent, false);
                return new UnSupportedContentVH(rootView);
            }
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ListItemType.UNSUPPORTED_CONTENT: {
                ((UnSupportedContentVH) holder).bind((UnSupportedContent) mDataset.get(position));
                break;
            }
            case ListItemType.OS_ORDER: {
                ((CurrentOrderVH) holder).bind(((OSOrder) mDataset.get(position)));
                break;
            }
            case ListItemType.OS_OLD_ORDER: {
                ((OldOrderVH) holder).bind((OSOldOrder) mDataset.get(position));
                break;
            }
            case ListItemType.CART: {
                ((BusinessItemOrderOnlineVH) holder).bind((OSCart) mDataset.get(position));
                break;
            }
            case ListItemType.BUSINESS_V4: {
                ((BusinessHomeVH) holder).bind((BusinessV4) mDataset.get(position));
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).getItemType();
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
