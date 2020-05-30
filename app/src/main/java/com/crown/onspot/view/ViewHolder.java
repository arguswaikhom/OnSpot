package com.crown.onspot.view;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.onspot.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ViewHolder {
    static class BusinessVH extends RecyclerView.ViewHolder {
        TextView titleTV;
        TextView categoryTV;
        TextView ratingVoteCountTV;
        TextView locationTV;
        Button orderBtn;
        ImageView shopImageIV;
        RatingBar ratingBarRB;

        BusinessVH(View view) {
            super(view);
            titleTV = view.findViewById(R.id.tv_moi_title);
            categoryTV = view.findViewById(R.id.tv_s_category);
            ratingVoteCountTV = view.findViewById(R.id.tv_si_libi_vote);
            locationTV = view.findViewById(R.id.tv_libi_price);
            orderBtn = view.findViewById(R.id.btn_as_order);
            ratingBarRB = view.findViewById(R.id.rb_libi_rating_bar);
            shopImageIV = view.findViewById(R.id.iv_si_image);
        }
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView headerTV;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            headerTV = itemView.findViewById(R.id.tv_sh_header);
        }
    }

    static class BusinessItemVH extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_libi_image)
        ImageView imageIV;
        @BindView(R.id.tv_libi_title)
        TextView titleTV;
        @BindView(R.id.iv_libi_description_info)
        ImageView descriptionInfoIV;
        @BindView(R.id.tv_libi_category)
        TextView categoryTV;
        @BindView(R.id.rb_libi_rating_bar)
        RatingBar ratingRB;
        @BindView(R.id.tv_libi_vote)
        TextView ratingVotesTV;
        @BindView(R.id.tv_libi_price)
        TextView priceTV;
        @BindView(R.id.tv_libi_final_price)
        TextView finalPriceTV;
        @BindView(R.id.iv_libi_add_quantity)
        ImageView addQuantityIV;
        @BindView(R.id.iv_libi_sub_quantity)
        ImageView subQuantityIV;
        @BindView(R.id.tv_libi_quantity)
        TextView quantityTV;
        @BindView(R.id.tv_libi_status)
        TextView statusTV;

        BusinessItemVH(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class OrderVH extends RecyclerView.ViewHolder {
        public TableLayout orderTableTL;
        public ImageButton toggleInfoIBtn;
        TextView businessTitleTV;
        TextView orderTimeTV;
        TextView orderDataTV;
        TextView statusTv;
        TextView priceTV;
        CardView orderItemCV;
        Button cancelBtn;
        @BindView(R.id.ibtn_moi_more_overflow)
        ImageButton moreOverflowIBtn;

        OrderVH(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            businessTitleTV = itemView.findViewById(R.id.tv_moi_title);
            orderTimeTV = itemView.findViewById(R.id.tv_moi_order_time);
            orderDataTV = itemView.findViewById(R.id.tv_moi_order_date);
            statusTv = itemView.findViewById(R.id.tv_moi_status);
            priceTV = itemView.findViewById(R.id.tv_libi_price);
            orderTableTL = itemView.findViewById(R.id.tl_moi_order_list);
            orderItemCV = itemView.findViewById(R.id.cv_moi_order_item);
            toggleInfoIBtn = itemView.findViewById(R.id.ibtn_moi_toggle_info);
            cancelBtn = itemView.findViewById(R.id.btn_moi_cancel);
        }
    }
}
