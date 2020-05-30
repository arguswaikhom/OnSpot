package com.crown.onspot.utils.abstracts;

public interface OnChangeShopItemCart {
    int ADD = 1;
    int SUB = 2;

    void onChangeShopItemCart(int position, int mode);
}