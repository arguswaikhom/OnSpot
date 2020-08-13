package com.crown.onspot.page;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.crown.onspot.R;

public class OrderDetailsActivity extends AppCompatActivity {

    public static final String ORDER = "ORDER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
    }
}