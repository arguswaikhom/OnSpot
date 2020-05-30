package com.crown.onspot.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class OrderItem extends MenuItem {
    private Long quantity = 0L;

    public OrderItem() {
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Discount getDiscountType() {
        return discountType;
    }

    public void setDiscountType(Discount discountType) {
        this.discountType = discountType;
    }

    public Long getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Long discountValue) {
        this.discountValue = discountValue;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Long getTax() {
        return tax;
    }

    public void setTax(Long tax) {
        this.tax = tax;
    }

    public Map<String, Object> getUploadable() {
        Map<String, Object> map = new HashMap<>();
        map.put("category", category);
        map.put("quantity", quantity);
        map.put("discountType", discountType);
        map.put("discountValue", discountValue);
        map.put("itemId", itemId);
        map.put("itemName", itemName);
        map.put("price", price);
        map.put("tax", tax);
        return map;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
